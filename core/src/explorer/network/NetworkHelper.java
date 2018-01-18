package explorer.network;

import explorer.game.framework.Game;

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
}
