package explorer.network.server;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.InflaterInputStream;

import explorer.game.framework.Game;
import explorer.network.NetworkClasses;
import explorer.world.ChunkDataProvider;
import explorer.world.World;
import explorer.world.chunk.WorldChunk;
import explorer.world.object.CustomDataWorldObject;
import explorer.world.object.WorldObject;

/**
 * Custom Chunk Data provider that send request packet to server then on server side get chunk file bytes and send again to client then parse data on client side
 * Created by RYZEN on 27.12.2017.
 */

public class NetworkChunkDataProvider extends ChunkDataProvider {

    /**
     * Game instance
     */
    private Game game;

    /**
     * Instance of class that contains all request for data from clients that are currently waiting for it
     */
    private Array<ChunkDataRequestFuture> requests_queue;

    /**
     * Index value to keep everything in sync
     */
    private int index;

    /**
     * Custom future implementation to suit need of this method of getting data from network
     */
    public static class ChunkDataRequestFuture implements Future<ChunkDataRequestFuture> {

        /**
         * Flags about this future status
         */
        private boolean done = false;
        private boolean cancel = false;

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
     * Construct new network chunk data provider that gets data about chunk from server by request/response system
     * @param game game instance
     * @param world world instance
     */
    public NetworkChunkDataProvider(final Game game, final World world) {
        this.game = game;

        requests_queue = new Array<ChunkDataRequestFuture>();

        //add listener for chunk data packets here
        game.getGameClient().getClient().addListener(new Listener() {
            @Override
            public void received(Connection connection, Object o) {
                if(o instanceof NetworkClasses.ChunkDataPacket) {
                    //send this received data to chunk using created before future
                    NetworkClasses.ChunkDataPacket chunk_data = (NetworkClasses.ChunkDataPacket) o;

                    System.out.println("New chunk data (rid: " + chunk_data.request_id + ")");

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

                            future.callback.loaded(data);

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
    private boolean parseData(ChunkData data, byte[] file_bytes, Vector2 chunk_position, World world, Game game) {
        try {
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
            System.out.println("(Network Server Chunk Loader) Failed to read whole chunk file (EOF exception, unexpected end of ZLIB input stream)");

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

    @Override
    public Future<?> getChunkData(DataLoaded callback, Vector2 chunk_position, World world, Game game) {
        //from there send request to server and on server side load from file
        NetworkClasses.ChunkDataRequestPacket data_request = new NetworkClasses.ChunkDataRequestPacket();
        data_request.connection_id = game.getGameClient().getClient().getID();
        data_request.position = chunk_position;
        data_request.request_id = index++;

        game.getGameClient().getClient().sendTCP(data_request);

        ChunkDataRequestFuture future = new ChunkDataRequestFuture(data_request.request_id, callback, chunk_position);
        requests_queue.add(future);

        return future;
    }

    @Override
    public void saveChunkData(DataSaved callback, WorldChunk chunk, Vector2 chunk_position, World world, Game game) {
        //TODO
    }
}
