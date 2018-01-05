package explorer.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import explorer.game.framework.Game;
import explorer.world.chunk.WorldChunk;
import explorer.world.object.CustomDataWorldObject;
import explorer.world.object.WorldObject;

/**
 * Created by RYZEN on 07.10.2017.
 */

public class FileChunkDataProvider extends ChunkDataProvider {

    private String world_dir;

    public void setWorldDir(String world_dir) {
        this.world_dir = world_dir;
    }

    public String getWorldDir() {
        return world_dir;
    }

    /**
     * Get path to chunk
     * @param world_dir world directory, this method assume that world_dir ends with "\" symbol
     * @param chunk_position in world space
     * @return path to chunk
     */
    public synchronized static String getPathToChunkFile(String world_dir, final Vector2 chunk_position) {
        int x = (int) chunk_position.x / World.CHUNK_WORLD_SIZE;
        int y = (int) chunk_position.y / World.CHUNK_WORLD_SIZE;

        return world_dir + x + "_" + y + ".chunk";
    }

    @Override
    public Future<?> getChunkData(final DataLoaded callback, final Vector2 chunk_position, final World world, final Game game) {
        //CALCULATION THESE COORDS JUST STOLEN FROM WorldChunk getGlobalChunkXIndex, getGlobalChunkYIndex
        int x = (int) chunk_position.x / World.CHUNK_WORLD_SIZE;
        int y = (int) chunk_position.y / World.CHUNK_WORLD_SIZE;

        int planet_width = world.getPlanetProperties().PLANET_SIZE;
        x %= planet_width;

        final String path = getPathToChunkFile(world_dir, new Vector2(x * World.CHUNK_WORLD_SIZE, y * World.CHUNK_WORLD_SIZE));

        Runnable r = new Runnable() {
            @Override
            public void run() {
                long file_loading_start = System.currentTimeMillis();

                ChunkData data = new ChunkData();
                FileHandle handle = Gdx.files.local(path);

                //if file for this chunk doesn't exist yet return empty chunk with all blocks as air
                if(!handle.exists()) {
                    for(int i = 0; i < World.CHUNK_SIZE; i++) {
                        for (int j = 0; j < World.CHUNK_SIZE; j++) {
                            data.foreground_blocks[i][j] = world.getBlocks().AIR.getBlockID();
                            data.background_blocks[i][j] = world.getBlocks().AIR.getBlockID();
                        }
                    }

                    data.chunk_loaded_position = new Vector2(chunk_position);
                    data.chunk_loaded_position.x %= world.getPlanetProperties().PLANET_SIZE * World.CHUNK_WORLD_SIZE;

                    callback.loaded(data);
                    return;
                }

                //try to load data from file until all data is loaded
                while(!loadData(data, handle, chunk_position, world, game)) {
                    data.objects.clear();
                }

                System.out.println("Reading file time: " + TimeUtils.timeSinceMillis(file_loading_start) + "ms");

                callback.loaded(data);
            }
        };
        return game.getThreadPool().runTaskFuture(r);
    }

    private boolean loadData(ChunkData data, FileHandle handle, Vector2 chunk_position, World world, Game game) {
        try {
            InflaterInputStream input = new InflaterInputStream(handle.read(1024));
            DataInputStream data_input = new DataInputStream(input);

            int chunk_x = data_input.readInt() * World.CHUNK_WORLD_SIZE;
            int chunk_y = data_input.readInt() * World.CHUNK_WORLD_SIZE;
            data.chunk_loaded_position.set(chunk_x, chunk_y);

            //load blocks
            for (int i = 0; i < World.CHUNK_SIZE; i++) {
                for (int j = 0; j < World.CHUNK_SIZE; j++) {
                    int foreground_id = data_input.readInt();
                    int background_id = data_input.readInt();

                    data.foreground_blocks[i][j] = foreground_id;
                    data.background_blocks[i][j] = background_id;

                    if(WorldChunk.YIELD) {
                        Thread.yield();
                    }

                    //check if loading is interrupted
                    if(Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                }
            }

            //load objects
            int objects_count = data_input.readInt();
            for (int i = 0; i < objects_count; i++) {
                String class_name = data_input.readUTF();

                Vector2 position = new Vector2(data_input.readFloat(), data_input.readFloat());
                position.add(chunk_position);

                int object_id = data_input.readInt();

                WorldObject new_object = createInstanceFromClass(class_name, position, world, game);

                if (new_object != null) {
                    new_object.OBJECT_ID = object_id;

                    boolean have_properties = data_input.readBoolean();

                    if(have_properties) {
                        HashMap<String, String> properties = new HashMap<String, String>();
                        int properties_count = data_input.readInt();

                        for(int j = 0; j < properties_count; j++) {
                            String key = data_input.readUTF();
                            String val = data_input.readUTF();
                            properties.put(key, val);
                        }

                        //set object properties to new one
                        new_object.setObjectProperties(properties);
                    }

                    //if object has custom data load it
                    if (new_object instanceof CustomDataWorldObject) {
                        ((CustomDataWorldObject) new_object).load(data_input);
                    }

                    //finally add out new object to chunk data
                    data.objects.add(new_object);
                }

                if(WorldChunk.YIELD) {
                    Thread.yield();
                }

                //check if loading is interrupted
                if(Thread.interrupted()) {
                    throw new InterruptedException();
                }
            }

            input.close();

            return true;
        } catch(EOFException e) {
            System.out.println("Failed to read whole chunk file (EOF exception, unexpected end of ZLIB input stream)");

            //wait some time because this exception was thrown probably because
            //system wasn't able to provide access to file at that time so wait and try to load chunk file again
            try {
                Thread.sleep(100);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
                return false;
            }

            return false;
        } catch(IOException e) {
            e.printStackTrace();
            return false;
        } catch(InterruptedException e) {
            return true;
        }
    }

    private WorldObject createInstanceFromClass(String class_name, Vector2 position, World world, Game game) {
        try {
            Class<?> clazz = Class.forName(class_name);

            //so every saveable game object have to have constructor (Vector2, World, Game)!
            Constructor<?> ctor = clazz.getConstructor(Vector2.class, World.class, Game.class);

            Object object = ctor.newInstance(position, world, game);
            return (WorldObject) object;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void saveChunkData(final DataSaved callback, final explorer.world.chunk.WorldChunk chunk, Vector2 chunk_poss, final World world, Game game) {
        //store chunk pos in final variable to avoid situation when chunk will be saved on wrong pos which would destroy players world
        final Vector2 chunk_pos = new Vector2(chunk_poss);

        int x = (int) chunk_pos.x / World.CHUNK_WORLD_SIZE;
        int y = (int) chunk_pos.y / World.CHUNK_WORLD_SIZE;

        int planet_width = world.getPlanetProperties().PLANET_SIZE;
        x %= planet_width;

        final String path = getPathToChunkFile(world_dir, new Vector2(x * World.CHUNK_WORLD_SIZE, y * World.CHUNK_WORLD_SIZE));

        //because I want to save in background we have to copy chunk data here
        final int[][] foreground_blocks = new int[World.CHUNK_SIZE][World.CHUNK_SIZE];
        final int[][] background_blocks = new int[World.CHUNK_SIZE][World.CHUNK_SIZE];

        for(int i = 0; i < foreground_blocks.length; i++) {
            for(int j = 0; j < foreground_blocks[0].length; j++) {
                foreground_blocks[i][j] = chunk.getBlocks()[i][j].getForegroundBlock().getBlockID();
                background_blocks[i][j] = chunk.getBlocks()[i][j].getBackgroundBlock().getBlockID();
            }
        }

        //copy chunk objects and chunk position
        final Array<WorldObject> objects = new Array<WorldObject>(chunk.getObjects());
        final Vector2 chunk_position = new Vector2(chunk_pos);

        System.out.println("Save request: " + path);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    DeflaterOutputStream output = new DeflaterOutputStream(Gdx.files.local(path).write(false, 1024));
                    DataOutputStream data_output = new DataOutputStream(output);

                    //at first save chunk position at
                    chunk_position.x %= world.getPlanetProperties().PLANET_SIZE * World.CHUNK_WORLD_SIZE;
                    data_output.writeInt((int) chunk_position.x / World.CHUNK_WORLD_SIZE);
                    data_output.writeInt((int) chunk_position.y / World.CHUNK_WORLD_SIZE);

                    //save blocks
                    for(int i = 0; i < foreground_blocks.length; i++) {
                        for(int j = 0; j < foreground_blocks[0].length; j++) {
                            data_output.writeInt(foreground_blocks[i][j]);
                            data_output.writeInt(background_blocks[i][j]);

                            if(WorldChunk.YIELD) {
                                Thread.yield();
                            }
                        }
                    }

                    //objects part
                    data_output.writeInt(objects.size);

                    for(WorldObject object : objects) {
                        if(!object.isSaveable())
                            continue;

                        String class_name = object.getClass().getName();
                        data_output.writeUTF(class_name);

                        //save position
                        data_output.writeFloat(object.getPosition().x - chunk_position.x);
                        data_output.writeFloat(object.getPosition().y - chunk_position.y);

                        //save object id
                        data_output.writeInt(object.OBJECT_ID);

                        //check if we have to save properties
                        if(object.getObjectProperties() == null) {
                            //save info that this object does not contain any properties
                            data_output.writeBoolean(false);
                        } else {
                            //save info that this object have some properties that were saved
                            data_output.writeBoolean(true);

                            //write info about amount of properties
                            data_output.writeInt(object.getObjectProperties().size());

                            for(String key : object.getObjectProperties().keySet()) {
                                String val = object.getObjectProperties().get(key);

                                //save key, val couple
                                data_output.writeUTF(key);
                                data_output.writeUTF(val);
                            }
                        }

                        //if object implements CustomDataWorldObject use it
                        if(object instanceof CustomDataWorldObject) {
                            ((CustomDataWorldObject) object).save(data_output);
                        }

                        if(WorldChunk.YIELD) {
                            Thread.yield();
                        }
                    }

                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //finally call callback saved() method
                if(callback != null)
                    callback.saved();
            }
        };
        game.getThreadPool().runTask(r);
    }
}
