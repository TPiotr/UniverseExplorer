package explorer.network.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.minlog.Log;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import explorer.game.framework.Game;
import explorer.network.NetworkClasses;
import explorer.network.server.GameServer;
import explorer.world.World;
import explorer.world.chunk.WorldChunk;
import explorer.world.object.WorldObject;

/**
 * Custom NetworkChunkDataProvider, difference is that this data provider don't send request to server because it is server so it just pushes request to ChunkDataRequestsHandler
 * same with ChunkDataSaveRequest packet
 * Created by RYZEN on 27.12.2017.
 */

public class HostNetworkChunkDataProvider extends NetworkChunkDataProvider {

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
    private Array<NetworkChunkDataProvider.ChunkDataRequestFuture> requests_queue;

    /**
     * Index value to keep everything in sync
     */
    private AtomicInteger index;


    /**
     * Construct new network chunk data provider that gets data about chunk from server by request/response system
     * @param game game instance
     * @param world world instance
     */
    public HostNetworkChunkDataProvider(final Game game, final World world) {
        this.game = game;
        this.world = world;

        index = new AtomicInteger();

        requests_queue = new Array<NetworkChunkDataProvider.ChunkDataRequestFuture>();
    }

    /**
     * Method that parses chunk data packet
     * @param chunk_data data packet
     */
    public synchronized void parseChunkDataPacket(NetworkClasses.ChunkDataPacket chunk_data) {
        Log.debug("(NetworkChunkDataProvider) New chunk data (rid: " + chunk_data.request_id + ")");

        //find future with proper ID
        int remove_id = -1;
        for(int i = 0; i < requests_queue.size; i++) {
            if(requests_queue.get(i).ID == chunk_data.request_id) {
                //get future instance
                NetworkChunkDataProvider.ChunkDataRequestFuture future = requests_queue.get(i);
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

    @Override
    public Future<?> getChunkData(DataLoaded callback, Vector2 chunk_position, World world, Game game) {
        //from there send request to server and on server side load from file
        NetworkClasses.ChunkDataRequestPacket data_request = new NetworkClasses.ChunkDataRequestPacket();
        data_request.connection_id = GameServer.SERVER_CONNECTION_ID;
        data_request.position = chunk_position;
        data_request.request_id = index.getAndIncrement();

        world.getServerChunkDataRequestsHandler().handleRequest(data_request);

        NetworkChunkDataProvider.ChunkDataRequestFuture future = new NetworkChunkDataProvider.ChunkDataRequestFuture(data_request.request_id, callback, chunk_position);
        requests_queue.add(future);

        return future;
    }

    @Override
    public void saveChunkData(DataSaved callback, WorldChunk chunk, Vector2 chunk_pos, final World world, Game game) {
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
                save_request_packet.connection_id = GameServer.SERVER_CONNECTION_ID;
                save_request_packet.position = chunk_position;

                world.getServerSaveChunkDataRequestsHandler().handleRequest(save_request_packet);
            }
        };
        game.getThreadPool().runTask(r);
    }
}
