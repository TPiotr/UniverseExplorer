package explorer.network.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.minlog.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.zip.DeflaterOutputStream;

import explorer.game.framework.Game;
import explorer.network.NetworkClasses;
import explorer.world.World;
import explorer.world.chunk.WorldChunk;
import explorer.world.object.WorldObject;
import explorer.world.object.objects.player.Player;

/**
 * Class responsible for sending current chunk data to requester
 *
 * Created by RYZEN on 30.01.2018.
 */

public class ClientChunkDataRequestsHandler {

    /**
     * World instance
     */
    private World world;

    /**
     * Game instance
     */
    private Game game;

    /**
     * Array that contains all data requests packets which weren't realized yet
     */
    private Array<NetworkClasses.ChunkDataRequestPacket> pending_packets;

    /**
     * Construct new Chunk data request handler
     * @param world world instance
     * @param game game instance
     */
    public ClientChunkDataRequestsHandler(World world, Game game) {
        this.world = world;
        this.game = game;

        pending_packets = new Array<NetworkClasses.ChunkDataRequestPacket>();
    }

    /**
     * Handle some client request for chunk data
     * @param request request packet
     */
    public void handleRequest(final NetworkClasses.ChunkDataRequestPacket request) {
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

                    //calculate chunk x & y
                    int x = (int) request.position.x / World.CHUNK_WORLD_SIZE;
                    int y = (int) request.position.y / World.CHUNK_WORLD_SIZE;

                    int planet_width = world.getPlanetProperties().PLANET_SIZE;
                    x %= planet_width;

                    //load whole file into byte array
                    byte[] chunk_data = null;

                    loop:
                    for(int i = 0; i < world.getWorldChunks().length; i++) {
                        for(int j = 0; j < world.getWorldChunks()[0].length; j++) {
                            if(world.getWorldChunks()[i][j].getPosition().equals(request.position)) {
                                //if this chunk is loading wait until it will end
                                pending_packets.add(request);
                                while(world.getWorldChunks()[i][j].isDirty());


                                //if this request come to player when he was loading some chunk check after loading if requested chunk is still same as loaded
                                //if not send this request back to server and server will decide again who have to send data again
                                if(!world.getWorldChunks()[i][j].getPosition().equals(request.position)) {
                                    //send back to server this request
                                    game.getGameClient().getClient().sendTCP(request);
                                    return;
                                }

                                //parse this chunk data to byte array
                                chunk_data = getChunkBytes(world.getWorldChunks()[i][j]);
                                pending_packets.removeValue(request, true);
                                break loop;
                            }
                        }
                    }

                    //fill packet with data
                    NetworkClasses.ChunkDataPacket data_packet = new NetworkClasses.ChunkDataPacket();
                    data_packet.request_id = request.request_id;
                    data_packet.file_bytes = chunk_data;
                    data_packet.connection_id = request.connection_id;

                    //finally send packet to client
                    game.getGameClient().getClient().sendTCP(data_packet);
                } catch(Exception e) {
                    Log.error("(Client Chunk Data Requests Handler) Loading chunk file for client failed! ("+e.getClass().getSimpleName()+ "):", e);
                }
            }
        };

        //run task on main engine thread pool
        game.getThreadPool().runTask(r);
    }

    /**
     * Method that convents given world chunk into byte array
     * This method don't care if some object is saveable or not
     * @param chunk
     * @return
     */
    public byte[] getChunkBytes(WorldChunk chunk) {
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
        final Vector2 chunk_position = new Vector2(chunk.getPosition());

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
            int objects_count = objects.size;
            data_output.writeInt(objects_count);

            for(WorldObject object : objects) {
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

    /**
     * Getter for array that contains all requests that weren't done
     * @return array with pending requests
     */
    public Array<NetworkClasses.ChunkDataRequestPacket> getPendingRequests() {
        return pending_packets;
    }
}
