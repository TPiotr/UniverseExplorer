package explorer.network.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.minlog.Log;

import explorer.game.framework.Game;
import explorer.network.NetworkClasses;
import explorer.network.server.GameServer;
import explorer.world.FileChunkDataProvider;
import explorer.world.World;
import explorer.world.chunk.WorldChunk;
import explorer.world.object.objects.player.Player;

/**
 * Class used in World class responsible for sending response to clients for their ChunkDataRequestPacket requests
 * So this class reads chunk files from disk or send this task to other clients which have to send data to request sender
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
     * If we want to request some data from player he needs to be on current chunk for more than this value
     */
    private static final float MUST_BE_ON_CHUNK_TO_REQUEST_DATA = 2000f;

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

                    //calc chunk x & y
                    int x = (int) request.position.x / World.CHUNK_WORLD_SIZE;
                    int y = (int) request.position.y / World.CHUNK_WORLD_SIZE;

                    int planet_width = world.getPlanetProperties().PLANET_SIZE;
                    x %= planet_width;

                    //out byte array
                    byte[] chunk_data = null;

                    //first check if we can't grab data from some pending save chunk data request
                    byte[] packet_data = world.getServerSaveChunkDataRequestsHandler().getPendingData(x, y);
                    if(packet_data != null) {
                        chunk_data = packet_data;
                    }

                    //check if we can grab this chunk data from some player which is on requested chunk already is byte chunk data is still null
                    if(chunk_data == null) {
                        for (int i = 0; i < world.getClonedPlayers().size; i++) {
                            Player clone = world.getClonedPlayers().get(i);

                            int player_x = (int) clone.getCurrentCenterChunkPosition().x / World.CHUNK_WORLD_SIZE;
                            int player_y = (int) clone.getCurrentCenterChunkPosition().y / World.CHUNK_WORLD_SIZE;

                            if (canSendData(x, y, player_x, player_y) && clone.getRepresentingPlayer().connection_id != request.connection_id
                                    && clone.getRepresentingPlayer().connection_id != request.rejected_id && clone.getOnCurrentChunkTime() > MUST_BE_ON_CHUNK_TO_REQUEST_DATA) {
                                game.getGameServer().getServer().sendToTCP(clone.getRepresentingPlayer().connection_id, request);
                                Log.debug("(ChunkDataRequestsHandler) Sending data request from:" + request.connection_id + " to another client: " + clone.getRepresentingPlayer().connection_id);
                                return;
                            }
                        }
                    }

                    //check if host player can give data if byte data is still null
                    if(chunk_data == null && request.connection_id != GameServer.SERVER_CONNECTION_ID && request.rejected_id != GameServer.SERVER_CONNECTION_ID && world.getPlayer().getOnCurrentChunkTime() > MUST_BE_ON_CHUNK_TO_REQUEST_DATA) {
                        int host_player_x = (int) world.getWorldChunks()[1][1].getPosition().x / World.CHUNK_WORLD_SIZE;
                        int host_player_y = (int) world.getWorldChunks()[1][1].getPosition().y / World.CHUNK_WORLD_SIZE;

                        loop:
                        if (canSendData(x, y, host_player_x, host_player_y)) {
                            for (int i = 0; i < world.getWorldChunks().length; i++) {
                                for (int j = 0; j < world.getWorldChunks()[0].length; j++) {
                                    WorldChunk chunk = world.getWorldChunks()[i][j];
                                    int chx = (int) chunk.getPosition().x / World.CHUNK_WORLD_SIZE;
                                    int chy = (int) chunk.getPosition().y / World.CHUNK_WORLD_SIZE;

                                    if (x == chx && y == chy) {
                                        //wait until chunk will be fully loaded
                                        while (chunk.isDirty());

                                        //parse this chunk data to byte array
                                        chunk_data = world.getClientChunkDataRequestHandler().getChunkBytes(chunk);
                                        break loop;
                                    }
                                }
                            }
                        }
                    }

                    //if chunk data is still null just load it from file
                    if(chunk_data == null)
                        chunk_data = getChunkBytesFromFile(x, y);

                    //fill packet with data
                    NetworkClasses.ChunkDataPacket data_packet = new NetworkClasses.ChunkDataPacket();
                    data_packet.request_id = request.request_id;
                    data_packet.file_bytes = chunk_data;

                    //finally send packet to client, check if we have to send data over network or directly pass it to data provider instance if requester is host
                    if(request.connection_id != GameServer.SERVER_CONNECTION_ID) {
                        game.getGameServer().getServer().sendToTCP(request.connection_id, data_packet);
                    } else {
                        ((HostNetworkChunkDataProvider) world.getChunksDataProvider()).parseChunkDataPacket(data_packet);
                    }
                } catch(Exception e) {
                    System.err.println("(Chunk Data Requests Handler) loading chunk file for client failed! ("+e.getClass().getSimpleName()+ "):");
                    e.printStackTrace();
                }
            }
        };

        //run task on main engine thread pool
        game.getThreadPool().runTask(r);
    }

    /**
     * Method that reads chunk file into byte array
     * @param chunk_x chunk x ( 1 = World.CHUNK_WORLD_SIZE)
     * @param chunk_y chunk y
     * @return byte array containing whole chunk file
     */
    private byte[] getChunkBytesFromFile(int chunk_x, int chunk_y) {
        String world_dir = World.getWorldDirectory(world.getPlanetProperties().PLANET_SEED);
        final String path = FileChunkDataProvider.getPathToChunkFile(world_dir, new Vector2(chunk_x * World.CHUNK_WORLD_SIZE, chunk_y * World.CHUNK_WORLD_SIZE));
        FileHandle handle = Gdx.files.local(path);

        //well if chunk file don't exists yet just return null
        if(!handle.exists()) {
            return null;
        }

        return handle.readBytes();
    }

    /**
     * Method that determines if player at coords player_x & player_y can give us chunk data at coords chunk_x & chunk_y
     * @param chunk_x chunk x ( 1 = World.CHUNK_WORLD_SIZE)
     * @param chunk_y chunk y ( 1 = World.CHUNK_WORLD_SIZE)
     * @param player_x player x ( 1 = World.CHUNK_WORLD_SIZE)
     * @param player_y player y ( 1 = World.CHUNK_WORLD_SIZE)
     * @return
     */
    private boolean canSendData(int chunk_x, int chunk_y, int player_x, int player_y) {
        //so because we know that player have 3 x 3 chunks grid in memory we can calculate if he can give us data
        int max_x = player_x + 1;
        int min_x = player_x - 1;

        int max_y = player_y + 1;
        int min_y = player_y - 1;

        if(chunk_x >= min_x && chunk_x <= max_x) {
            if(chunk_y >= min_y && chunk_y <= max_y) {
                return true;
            }
        }

        return false;
    }
}
