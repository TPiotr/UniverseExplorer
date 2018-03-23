package explorer.network.client;

import com.esotericsoftware.kryonet.Connection;

/**
 * Class that contains basic informations about other players that are currently on server (and registered)
 * Created by RYZEN on 27.12.2017.
 */

public class ServerPlayer {

    /**
     * Connection of their id used in sending packets only to some player
     */
    public final int connection_id;

    /**
     * Username of this player
     */
    public final String username;

    /**
     * Flag determining if this player is an host (also host connection_id is always -1)
     */
    public final boolean is_host;

    public ServerPlayer(int c_id, String username, boolean is_host) {
        this.connection_id = c_id;
        this.username = username;
        this.is_host = is_host;
    }

}
