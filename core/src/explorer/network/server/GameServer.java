package explorer.network.server;

import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;

import java.io.IOException;

import explorer.game.framework.Game;
import explorer.game.screen.screens.planet.PlanetScreen;
import explorer.network.NetworkClasses;
import explorer.network.client.ServerPlayer;

/**
 * Game server class
 * Created by RYZEN on 27.12.2017.
 */

public class GameServer {

    /**
     * Interfacing informing us about progress on creating an server
     */
    public interface GameServerCreatedCallback {
        void created();
        void failed();
    }

    /**
     * Kryonet Server class instance
     */
    private Server server;

    /**
     * Port which will be used by server for TDP
     */
    public static final int TCP_PORT = 54555;

    /**
     * Port which will be used by server for UDP
     */
    public static final int UDP_PORT = 54777;

    /**
     * Array that contains all currently connected players (and registered)
     */
    private Array<ServerPlayer> players;

    /**
     * If some ServerPlayer.connection_id equals this variable it means that this server player is an host
     */
    public static final int SERVER_CONNECTION_ID = -1;

    public void start(GameServerCreatedCallback callback, final PlanetScreen planet_screen, final Game game) {
        server = new Server(65536 * 5, 65536 * 5);
        server.start();

        Log.INFO();

        //register all classes that are send over network
        NetworkClasses.register(server.getKryo());

        try {
            server.bind(TCP_PORT, UDP_PORT);
            System.out.println("(Network Server) Server binded (TCP port: " + TCP_PORT + " UDP port: " + UDP_PORT + ")");

            if(callback != null)
                callback.created();

        } catch (IOException e) {
            e.printStackTrace();

            if(callback != null)
                callback.failed();
        }

        players = new Array<ServerPlayer>();

        server.addListener(new Listener() {

            @Override
            public void received(Connection connection, Object o) {
                super.received(connection, o);

                if(o instanceof NetworkClasses.OnServerRegistrationPacket) {
                    NetworkClasses.OnServerRegistrationPacket registration_info = (NetworkClasses.OnServerRegistrationPacket) o;

                    System.out.println("(Network Server) New player registered! (" + registration_info.username + ")");

                    //send to this player info about all other players
                    for(int i = 0; i < players.size; i++) {
                        ServerPlayer player = players.get(i);

                        //don't send info about himself as another player
                        if(player.connection_id == connection.getID())
                            continue;

                        NetworkClasses.NewPlayerPacket new_player_info = new NetworkClasses.NewPlayerPacket();
                        new_player_info.connection_id = player.connection_id;
                        new_player_info.username = player.username;
                        new_player_info.is_host = false;

                        server.sendToTCP(connection.getID(), new_player_info);
                    }

                    //finally send info about server player so about host
                    NetworkClasses.NewPlayerPacket host_player_info = new NetworkClasses.NewPlayerPacket();
                    host_player_info.connection_id = SERVER_CONNECTION_ID;
                    host_player_info.username = game.getUsername();
                    host_player_info.is_host = true;

                    server.sendToTCP(connection.getID(), host_player_info);

                    //add new player to list at this place because to not send to new player info about himself
                    if(getPlayerInstanceByConnection(connection) == null)
                        players.add(new ServerPlayer(connection.getID(), registration_info.username, false));

                    //send to all other players about new player
                    NetworkClasses.NewPlayerPacket new_player_info = new NetworkClasses.NewPlayerPacket();
                    new_player_info.connection_id = connection.getID();
                    new_player_info.username = registration_info.username;

                    server.sendToAllExceptTCP(connection.getID(), new_player_info);
                }

                //universe stuff
                else if(o instanceof NetworkClasses.GoToPlanetRequestPacket) {
                    //so at this point send to client information about new voting about going to some planet
                    NetworkClasses.GoToPlanetRequestPacket goto_planet_request = (NetworkClasses.GoToPlanetRequestPacket) o;

                    System.out.println("Player: " + getPlayerInstanceByConnection(connection).username + " (" + goto_planet_request.connection_id + "), want to go planet with index: " + goto_planet_request.planet_index);

                    NetworkClasses.VoteForGoingToPlanetPacket voting_packet = new NetworkClasses.VoteForGoingToPlanetPacket();
                    voting_packet.planet_index = goto_planet_request.planet_index;
                    server.sendToAllTCP(voting_packet);
                }
            }

            @Override
            public void connected(Connection connection) {
                System.out.println("(Network Server) New client connected");

            }

            @Override
            public void disconnected(Connection connection) {
                System.out.println("(Network Server) Client disconnected");

                ServerPlayer instance = getPlayerInstanceByConnection(connection);
                if (instance != null) {
                    players.removeValue(instance, true);
                    System.out.println("(Network Server) Player disconnected (" + instance.username + ")");

                    //send info to other clients about him
                    NetworkClasses.PlayerDisconnectedPacket disconnected_packet = new NetworkClasses.PlayerDisconnectedPacket();
                    disconnected_packet.connection_id = connection.getID();
                    server.sendToAllTCP(disconnected_packet);
                }
            }
        });
    }

    /**
     * Method that gives ServerPlayer instance by connection which belongs to player that we are looking for
     * @param c connection of server player we instance want to get
     * @return ServerPlayer instance which represents given connection in parameter, could be null!
     */
    public ServerPlayer getPlayerInstanceByConnection(Connection c) {
        for(int i = 0; i < players.size; i++) {
            if(players.get(i).connection_id == c.getID())
                return players.get(i);
        }

        return null;
    }

    /**
     * Almost same as getPlayerInstanceByConnection(Connection c) but instead of connection as parameter we pass connection_id which is unique for every connection
     * @param c connection id
     * @return ServerPlayer instance which represents given connection id in parameter, could be null!
     */
    public ServerPlayer getPlayerInstanceByConnectionID(int c) {
        for(int i = 0; i < players.size; i++) {
            if(players.get(i).connection_id == c)
                return players.get(i);
        }

        return null;
    }

    /**
     * Getter for kryonet server class instance
     * @return kryonet Server class instance
     */
    public Server getServer() {
        return server;
    }

    /**
     * Get Array that contains all registered players currently on server and info about them
     * @return instance of array, read only!
     */
    public Array<ServerPlayer> getPlayers() {
        return players;
    }

    /**
     * Freeup resources used by this instance
     */
    public void dispose() {
        if(server != null) {
            try {
                server.dispose();
            } catch (IOException e) {
                System.err.println("Exception while disposing server: " + e.getMessage());
            }
        }
    }

}
