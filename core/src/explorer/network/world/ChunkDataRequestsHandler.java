package explorer.network.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import explorer.game.framework.Game;
import explorer.network.NetworkClasses;
import explorer.world.ChunkDataProvider;
import explorer.world.FileChunkDataProvider;
import explorer.world.World;
import explorer.world.object.WorldObject;

/**
 * Class used in World class responsible for sending response to clients for their ChunkDataRequestPacket requests
 * So read file from disk into byte array and send over network on request
 * Created by RYZEN on 27.12.2017.
 */

public class ChunkDataRequestsHandler {

    /**
     * World instance
     */
    private World world;

    /**
     * Game instance
     */
    private Game game;

    /**
     * Construct new Chunk data request handler
     * @param world world instance
     * @param game game instance
     */
    public ChunkDataRequestsHandler(World world, Game game) {
        this.world = world;
        this.game = game;
    }

    /**
     * Handle client request for chunk data
     * @param request request packet
     */
    public void handleRequest(final NetworkClasses.ChunkDataRequestPacket request) {
        //just load file as bytes and send it over network
        //client have to parse it so this is faster method for host
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    //if world is not generated yet wait until it will be generated
                    while (world.isGenerating()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            //just ignore
                        }
                    }

                    //load file from disk into byte array

                    //get file handle to proper file on disk
                    int x = (int) request.position.x / World.CHUNK_WORLD_SIZE;
                    int y = (int) request.position.y / World.CHUNK_WORLD_SIZE;

                    int planet_width = world.getPlanetProperties().PLANET_SIZE;
                    x %= planet_width;

                    final String path = FileChunkDataProvider.getPathToChunkFile(((FileChunkDataProvider) world.getChunksDataProvider()).getWorldDir(), new Vector2(x * World.CHUNK_WORLD_SIZE, y * World.CHUNK_WORLD_SIZE));

                    FileHandle handle = Gdx.files.local(path);

                    //load whole file into byte array
                    byte[] chunk_data = handle.readBytes();

                    //fill packet with data
                    NetworkClasses.ChunkDataPacket data_packet = new NetworkClasses.ChunkDataPacket();
                    data_packet.request_id = request.request_id;
                    data_packet.file_bytes = chunk_data;

                    //finally send packet to client
                    //System.out.println("sending chunk data to: " + request.connection_id);
                    game.getGameServer().getServer().sendToTCP(request.connection_id, data_packet);
                } catch(Exception e) {
                    System.err.println("(Chunk Data Requests Handler) loading chunk file for client failed! ("+e.getClass().getSimpleName()+ "):");
                    System.err.println(e.getMessage());
                }
            }
        };

        //run task on main engine thread pool
        game.getThreadPool().runTask(r);
    }

}
