package explorer.network;

import explorer.game.framework.Game;
import explorer.network.server.GameServer;

/**
 * Created by RYZEN on 12.01.2018.
 */

public class NetworkHelper {

    private static Game game;

    public static void init(Game g) {
        game = g;
    }

    /**
     * Class to prevent from writing boiler code every time we want to send some packet
     * @param packet packet object
     */
    public static synchronized void send(Object packet) {
        boolean tcp = true;
        if(packet instanceof NetworkClasses.ObjectBoundPacket) {
            tcp = ((NetworkClasses.ObjectBoundPacket) packet).tcp;
        } else if(packet instanceof NetworkClasses.PlayerBoundPacket) {
            tcp = ((NetworkClasses.PlayerBoundPacket) packet).tcp;
        }

        if(tcp) {
            if(Game.IS_CLIENT) {
                game.getGameClient().getClient().sendTCP(packet);
            } else if(Game.IS_HOST) {
                game.getGameServer().getServer().sendToAllTCP(packet);
            }
        } else {
            if(Game.IS_CLIENT) {
                game.getGameClient().getClient().sendUDP(packet);
            } else if(Game.IS_HOST) {
                game.getGameServer().getServer().sendToAllUDP(packet);
            }
        }
    }

    /**
     * Simple function to get connection id while sending PlayerBoundPacket, ObjectBoundPacket or when you just need it
     * @param game game instance to get access to GameClient instance
     * @return connection id, -2 means that game is not even in network mode, to check if returned value is host compare with GameServer.SERVER_CONNECTION_ID
     */
    public static int getConnectionID(Game game) {
        if(!Game.IS_HOST && !Game.IS_CLIENT)
            return -2;

        return (Game.IS_HOST) ? GameServer.SERVER_CONNECTION_ID : game.getGameClient().getClient().getID();
    }
}
