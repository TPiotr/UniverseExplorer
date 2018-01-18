package explorer.network;

import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;

import java.util.ArrayList;
import java.util.HashMap;

import explorer.game.framework.Game;
import explorer.world.ChunkDataProvider;
import explorer.world.object.WorldObject;

/**
 * Created by RYZEN on 27.12.2017.
 */

public class NetworkClasses {

    //CLASS THAT CONTAINS PACKETS FOR NETWORKING

    //PLAYER BOUND PACKETS
    public static class NewPlayerPacket {
        public String username;
        public int connection_id;

        public boolean is_host;
    }

    public static class PlayerDisconnectedPacket {
        public int connection_id;
    }

    public static class OnServerRegistrationPacket {
        public String username;
    }

    //UNIVERSE PACKETS
    public static class GoToPlanetRequestPacket {
        /**
         * connection_id is id of player that wants to go to given planet
         */
        public int connection_id;

        public int planet_index;
    }

    public static class VoteForGoingToPlanetPacket {
        public int planet_index;

        public int voting_index;
    }

    public static class VotingVotePacket {
        public int voting_index;

        /**
         * vote = 1 = yes option
         * vote = -1 = no option
         */
        public int vote;
    }

    //this is different from upper packet because this is not request but task that every client have to do
    public static class GoToPlanetPacket {
        public int planet_index;
    }

    //WORLD OBJECT BOUND PACKETS

    public static class UpdateCurrentIDAssignerValuePacket {
        public int new_current_id;
    }

    public static class ObjectBoundPacket {
        public int object_id;

        /**
         * Flag determining which protocol will be used to send this packet to server and from server to other players
         * true = tcp
         * false = udp
         */
        public boolean tcp = true;
    }

    public static class ObjectPositionUpdatePacket extends ObjectBoundPacket {
        public float x, y;
    }

    public static class ObjectRemovedPacket {
        public int removed_object_id;
    }

    public static class NewObjectPacket {
        public String new_object_class_name;
        public int OBJECT_ID;

        public float x, y;

        /**
         * bytes which represents object_properties HashMap null if object don't have any properties
         */
        public byte[] properties_bytes;
    }

    /**
     * Packet used in situation where client send to server new object with bad ID, so server sets proper id to new object informs client about proper current IDAssigner value
     * and send packet to update object ID which has bad ID
     */
    public static class UpdateObjectIDPacket {
        public int acc_id;
        public int new_id;
    }

    public static class UpdateGameTimePacket {
        public float new_time;
    }

    //PLAYER
    public static class PlayerBoundPacket {
        public int player_connection_id;

        /**
         * Flag determining which protocol will be used to send this packet to server and from server to other players
         * true = tcp
         * false = udp
         */
        public boolean tcp = true;
    }

    public static class PlayerPositionUpdatePacket extends PlayerBoundPacket {
        public float x, y;
    }

    //CHUNKS PACKETS
    public static class ChunkDataRequestPacket {
        public Vector2 position;

        //id of connection which request this data
        public int connection_id;

        public int request_id;
    }

    public static class BlockChangedPacket {
        public int chunk_x;
        public int chunk_y;

        public int block_x;
        public int block_y;

        public boolean background;
        public int new_block_id;
    }

    public static class ChunkDataPacket {
        public byte[] file_bytes;
        public int request_id;
    }

    private static Game game;

    //array which contains classes that are waiting to be registered into Kryo
    private static ArrayList<Class<?>> pending_classes = new ArrayList<Class<?>>();

    public static void register(Kryo kryo, Game g) {
        game = g;

        kryo.register(NewPlayerPacket.class);
        kryo.register(OnServerRegistrationPacket.class);
        kryo.register(PlayerDisconnectedPacket.class);

        kryo.register(GoToPlanetRequestPacket.class);
        kryo.register(GoToPlanetPacket.class);
        kryo.register(VoteForGoingToPlanetPacket.class);
        kryo.register(VotingVotePacket.class);

        kryo.register(Vector2.class);
        kryo.register(ChunkDataRequestPacket.class);

        kryo.register(String.class);

        kryo.register(ChunkDataPacket.class);
        kryo.register(byte[].class);

        kryo.register(BlockChangedPacket.class);

        kryo.register(ObjectBoundPacket.class);
        kryo.register(ObjectRemovedPacket.class);
        kryo.register(NewObjectPacket.class);
        kryo.register(ObjectPositionUpdatePacket.class);
        kryo.register(UpdateCurrentIDAssignerValuePacket.class);
        kryo.register(UpdateGameTimePacket.class);
        kryo.register(UpdateObjectIDPacket.class);

        kryo.register(PlayerPositionUpdatePacket.class);
        kryo.register(PlayerBoundPacket.class);

        //register pending classes
        for(Class<?> c : pending_classes)
            kryo.register(c);

        pending_classes.clear();
    }

    /**
     * Register new packet
     * @param c packet class
     */
    public static void register(Class<?> c) {
        if(!Game.IS_CLIENT && !Game.IS_HOST)
            return;

        //if register(kryo, game) wasn't called yet queue class and when register(kryo, game) will be called all pending classes will be also registered
        if(game == null) {
            pending_classes.add(c);
            return;
        }

        if(Game.IS_CLIENT) {
            game.getGameClient().getClient().getKryo().register(c);
        } else if(Game.IS_HOST) {
            game.getGameServer().getServer().getKryo().register(c);
        }
    }

}
