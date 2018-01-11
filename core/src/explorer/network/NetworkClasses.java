package explorer.network;

import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;

import java.util.HashMap;

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
    }

    public static class ObjectPositionUpdatePacket extends ObjectBoundPacket {
        public float x, y;
    }

    public static class ObjectRemovedPacket {
        public int removed_object_id;
    }

    //PLAYER
    public static class PlayerBoundPacket {
        public int player_connection_id;
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

    public static void register(Kryo kryo) {
        kryo.register(NewPlayerPacket.class);
        kryo.register(OnServerRegistrationPacket.class);
        kryo.register(PlayerDisconnectedPacket.class);

        kryo.register(GoToPlanetRequestPacket.class);
        kryo.register(GoToPlanetPacket.class);
        kryo.register(VoteForGoingToPlanetPacket.class);

        kryo.register(Vector2.class);
        kryo.register(ChunkDataRequestPacket.class);

        kryo.register(String.class);

        kryo.register(ChunkDataPacket.class);
        kryo.register(byte[].class);

        kryo.register(BlockChangedPacket.class);

        kryo.register(ObjectBoundPacket.class);
        kryo.register(ObjectRemovedPacket.class);
        kryo.register(ObjectPositionUpdatePacket.class);
        kryo.register(UpdateCurrentIDAssignerValuePacket.class);

        kryo.register(PlayerPositionUpdatePacket.class);
        kryo.register(PlayerBoundPacket.class);
    }

}
