package explorer.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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

    public FileChunkDataProvider(String world_dir) {
        this.world_dir = world_dir;
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
        int x = (int) chunk_position.x / World.CHUNK_WORLD_SIZE;
        int y = (int) chunk_position.y / World.CHUNK_WORLD_SIZE;

        int planet_width = world.getPlanetProperties().PLANET_SIZE;
        x %= planet_width;

        final String path = getPathToChunkFile(world_dir, new Vector2(x * World.CHUNK_WORLD_SIZE, y * World.CHUNK_WORLD_SIZE));

        Runnable r = new Runnable() {
            @Override
            public void run() {
                ChunkData data = new ChunkData();
                FileHandle handle = Gdx.files.local(path);

                //if file for this chunk doesen't exist yet return empty chunk with all blocks as air
                if(!handle.exists()) {
                    for(int i = 0; i < World.CHUNK_SIZE; i++) {
                        for (int j = 0; j < World.CHUNK_SIZE; j++) {
                            data.foreground_blocks[i][j] = world.getBlocks().AIR.getBlockID();
                            data.background_blocks[i][j] = world.getBlocks().AIR.getBlockID();
                        }
                    }

                    callback.loaded(data);
                    return;
                }

                //try to load data from file until all data is loaded
                while(!loadData(data, handle, chunk_position, world, game)) {
                    data.objects.clear();
                }

                callback.loaded(data);
            }
        };
        return game.getThreadPool().runTaskFuture(r);
    }

    private boolean loadData(ChunkData data, FileHandle handle, Vector2 chunk_position, World world, Game game) {
        try {
            InflaterInputStream input = new InflaterInputStream(handle.read(1024));
            DataInputStream data_input = new DataInputStream(input);

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
                }
            }

            //load objects
            int objects_count = data_input.readInt();
            for (int i = 0; i < objects_count; i++) {
                String class_name = data_input.readUTF();

                Vector2 position = new Vector2(data_input.readFloat(), data_input.readFloat());
                position.add(chunk_position);

                WorldObject new_object = createInstanceFromClass(class_name, position, world, game);
                if (new_object != null) {
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
            }

            input.close();

            return true;
        } catch(IOException e) {
            e.printStackTrace();
            return false;
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
    public void saveChunkData(final DataSaved callback, final explorer.world.chunk.WorldChunk chunk, Vector2 chunk_pos, World world, Game game) {
        int x = (int) chunk_pos.x / World.CHUNK_WORLD_SIZE;
        int y = (int) chunk_pos.y / World.CHUNK_WORLD_SIZE;

        int planet_width = world.getPlanetProperties().PLANET_SIZE;
        x %= planet_width;

        final String path = getPathToChunkFile(world_dir, new Vector2(x * World.CHUNK_WORLD_SIZE, y * World.CHUNK_WORLD_SIZE));

        System.out.println("Save request: "+path);

        //because I want to save in background we have to copy chunk data here
        final int[][] foreground_blocks = new int[World.CHUNK_SIZE][World.CHUNK_SIZE];
        final int[][] background_blocks = new int[World.CHUNK_SIZE][World.CHUNK_SIZE];

        for(int i = 0; i < foreground_blocks.length; i++) {
            for(int j = 0; j < foreground_blocks[0].length; j++) {
                foreground_blocks[i][j] = chunk.getBlocks()[i][j].getForegroundBlock();
                background_blocks[i][j] = chunk.getBlocks()[i][j].getBackgroundBlock();
            }
        }

        //copy chunk objects and chunk position
        final Array<WorldObject> objects = new Array<WorldObject>(chunk.getObjects());
        final Vector2 chunk_position = new Vector2(chunk_pos);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    DeflaterOutputStream output = new DeflaterOutputStream(Gdx.files.local(path).write(false, 1024));
                    DataOutputStream data_output = new DataOutputStream(output);

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
