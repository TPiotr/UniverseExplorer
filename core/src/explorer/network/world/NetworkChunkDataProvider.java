package explorer.network.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import explorer.game.framework.Game;
import explorer.network.NetworkClasses;
import explorer.network.NetworkHelper;
import explorer.world.ChunkDataProvider;
import explorer.world.World;
import explorer.world.chunk.WorldChunk;
import explorer.world.object.WorldObject;

/**
 * Custom Chunk Data provider that send request packet to server then on server side server loads chunk file into bytes and send again to client then parse data on client side
 * Created by RYZEN on 27.12.2017.
 */

public class NetworkChunkDataProvider extends ChunkDataProvider {

    /**
     * Game instance
     */
    private Game game;

    /**
     * World instance
     */
    private World world;

    /**
     * Instance of class that contains all request for data from clients that are currently waiting for it
     */
    private Array<ChunkDataRequestFuture> requests_queue;

    /**
     * Index value to keep everything in sync
     */
    private AtomicInteger index;

    /**
     * Custom future implementation to suit need of this method of getting data from network
     */
    public static class ChunkDataRequestFuture implements Future<ChunkDataRequestFuture> {

        /**
         * Flags about this future status
         */
        public boolean done = false;
        public boolean cancel = false;

        /**
         * ID of this future assigned by index var used to load proper data to proper chunk
         */
        public int ID;

        /**
         * From chunk DataLoaded callback
         */
        public DataLoaded callback;

        /**
         * Position to which data loaded in future belongs to
         */
        public Vector2 chunk_position;

        public ChunkDataRequestFuture(int ID, DataLoaded callback, Vector2 chunk_position) {
            this.ID = ID;
            this.callback = callback;
            this.chunk_position = chunk_position;
        }

        @Override
        public boolean cancel(boolean b) {
            cancel = true;
            return cancel;
        }

        @Override
        public boolean isCancelled() {
            return cancel;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public ChunkDataRequestFuture get() throws InterruptedException, ExecutionException {
            return this;
        }

        @Override
        public ChunkDataRequestFuture get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
            return this;
        }
    }

    /**
     * Method to don't write same code every time when we want to create world object from class name & position
     * @param class_name class name
     * @param position position
     * @param world world instance
     * @param game game instance
     * @return new world object instance
     */
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

    /**
     * Empty constructor for HostNetworkChunkDataProvider
     */
    public NetworkChunkDataProvider() {}

    /**
     * Construct new network chunk data provider that gets data about chunk from server by request/response system
     * @param game game instance
     * @param world world instance
     */
    public NetworkChunkDataProvider(final Game game, final World world) {
        this.game = game;

        requests_queue = new Array<ChunkDataRequestFuture>();

        index = new AtomicInteger();

        //add listener for chunk data packets here
        game.getGameClient().getClient().addListener(new Listener() {
            @Override
            public void received(Connection connection, Object o) {
                if(o instanceof NetworkClasses.ChunkDataPacket) {
                    //send this received data to chunk using created before future
                    NetworkClasses.ChunkDataPacket chunk_data = (NetworkClasses.ChunkDataPacket) o;

                    Log.debug("(NetworkChunkDataProvider) New chunk data (rid: " + chunk_data.request_id + ")");

                    //find future with proper ID
                    int remove_id = -1;
                    for(int i = 0; i < requests_queue.size; i++) {
                        if(requests_queue.get(i).ID == chunk_data.request_id) {
                            //get future instance
                            ChunkDataRequestFuture future = requests_queue.get(i);
                            future.done = true;

                            //construct new chunk data struct
                            ChunkData data = new ChunkData();

                            //parse chunk data from array of bytes
                            parseData(data, chunk_data.file_bytes, future.chunk_position, world, game);

                            //inform callback instance holder about task completed
                            future.callback.loaded(data);

                            //stored id which we have to remove from pending tasks array
                            remove_id = i;
                            break;
                        }
                    }

                    //if we found proper future and send data to proper chunk remove it from futures list
                    if(remove_id != -1)
                        requests_queue.removeIndex(remove_id);
                }
            }
        });
    }

    /**
     * Basically this need to be same as in FileChunkDataProvider to make networking work!
     */
    protected synchronized boolean parseData(ChunkData data, byte[] file_bytes, Vector2 chunk_position, World world, Game game) {
        try {
            //create new empty chunk if requested one don't exist yet
            if(file_bytes == null) {
                data.chunk_loaded_position.set(chunk_position);
                for(int i = 0; i < World.CHUNK_SIZE; i++) {
                    for (int j = 0; j < World.CHUNK_SIZE; j++) {
                        data.foreground_blocks[i][j] = world.getBlocks().AIR.getBlockID();
                        data.background_blocks[i][j] = world.getBlocks().AIR.getBlockID();
                    }
                }

                return true;
            }

            InflaterInputStream input = new InflaterInputStream(new ByteArrayInputStream(file_bytes));
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
            Log.debug("(Network Client Chunk Loader) Failed to read whole chunk file (EOF exception, unexpected end of ZLIB input stream)", e);

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

    /**
     * Method that generates chunk data into byte[] array ready to send over network or save to file
     * @param foreground_blocks foreground blocks array
     * @param background_blocks background blocks array
     * @param objects objects array
     * @param chunk_position chunk position
     * @return byte array which contains chunk data
     */
    protected byte[] getChunkData(int[][] foreground_blocks, int[][] background_blocks, Array<WorldObject> objects, Vector2 chunk_position, World world) {
        try {
            ByteArrayOutputStream byte_output = new ByteArrayOutputStream(512);
            DeflaterOutputStream output = new DeflaterOutputStream(byte_output);
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

                    if (WorldChunk.YIELD) {
                        Thread.yield();
                    }
                }
            }

            //objects part, when sending to other player we don't care about non saveable objects
            int objects_count = 0;
            for(int i = 0; i < objects.size; i++) {
                if(objects.get(i).isSaveable())
                    objects_count++;
            }
            data_output.writeInt(objects_count);

            for(WorldObject object : objects) {
                if(!object.isSaveable())
                    continue;

                String class_name = object.getClass().getName();
                data_output.writeUTF(class_name);

                //save position
                data_output.writeFloat(object.getPosition().x);
                data_output.writeFloat(object.getPosition().y);

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

                    HashMap<String, String> properties = object.getObjectProperties();
                    for(String key : properties.keySet()) {
                        String val = properties.get(key);

                        //save key, val couple
                        data_output.writeUTF(key);
                        data_output.writeUTF(val);
                    }
                }

                if(WorldChunk.YIELD) {
                    Thread.yield();
                }
            }

            output.close();
            return byte_output.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Future<?> getChunkData(DataLoaded callback, Vector2 chunk_position, World world, Game game) {
        //from there send request to server and on server side load from file
        NetworkClasses.ChunkDataRequestPacket data_request = new NetworkClasses.ChunkDataRequestPacket();
        data_request.connection_id = game.getGameClient().getClient().getID();
        data_request.position = chunk_position;
        data_request.request_id = index.getAndIncrement();

        game.getGameClient().getClient().sendTCP(data_request);

        ChunkDataRequestFuture future = new ChunkDataRequestFuture(data_request.request_id, callback, chunk_position);
        requests_queue.add(future);

        return future;
    }

    @Override
    public void saveChunkData(DataSaved callback, final WorldChunk chunk, final Vector2 chunk_pos, final World world, final Game game) {
        //copy whole chunk data and then parse to byte array and send on other thread
        final Vector2 chunk_position = new Vector2(chunk_pos);

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

        Runnable r = new Runnable() {
            @Override
            public void run() {
                byte[] chunk_data = getChunkData(foreground_blocks, background_blocks, objects, chunk_position, world);

                //send to server save request
                NetworkClasses.ChunkDataSaveRequestPacket save_request_packet = new NetworkClasses.ChunkDataSaveRequestPacket();
                save_request_packet.chunk_data = chunk_data;
                save_request_packet.connection_id = game.getGameClient().getClient().getID();
                save_request_packet.position = chunk_position;

                game.getGameClient().getClient().sendTCP(save_request_packet);
            }
        };
        game.getThreadPool().runTask(r);
    }
}
