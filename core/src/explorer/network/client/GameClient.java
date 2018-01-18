package explorer.network.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;

import java.io.IOException;
import java.net.InetAddress;

import explorer.game.framework.Game;
import explorer.game.screen.gui.dialog.YesNoDialog;
import explorer.game.screen.screens.Screens;
import explorer.game.screen.screens.planet.PlanetScreen;
import explorer.network.server.GameServer;
import explorer.network.NetworkClasses;
import explorer.world.object.WorldObject;

/**
 * Class responsible for receiving basic packets from net like new player, registering player on server etc.
 *
 * Created by RYZEN on 27.12.2017.
 */

public class GameClient {

    /**
     * Interface to receive events about connecting to server status
     */
    public interface ConnectedToServerCallback {
        void connected();
        void failed();
    }

    /**
     * Kryonet client class instance
     */
    private Client client;

    /**
     * Array that contains all server players data
     */
    private Array<ServerPlayer> players;

    /**
     * Game class instance
     */
    private Game game;

    /**
     * Flag determines if Client instance start() method was called
     */
    private volatile boolean started;

    /**
     * Instantiate new GameClient object
     * @param game game instance
     */
    public GameClient(Game game) {
        this.game = game;

        //write buffer, read buffer sizes
        client = new Client(65536, 65536);

        Log.INFO();

        //register all classes that are send over network
        NetworkClasses.register(client.getKryo(), game);

        setupClientStuff();
    }

    public void connect(InetAddress address, final ConnectedToServerCallback connected_callback) {
        try {
            if(!started) {
                client.start();
                started = true;
            }

            client.connect(10000, address, GameServer.TCP_PORT, GameServer.UDP_PORT);

            Game.IS_CONNECTED = true;

            //inform connected listener about success
            if(connected_callback != null)
                connected_callback.connected();

            sendRegistrationPacket(game.getUsername());
        } catch (IOException e) {
            e.printStackTrace();

            //inform listener about failure
            if(connected_callback != null) {
                connected_callback.failed();
            }
        }
    }

    /**
     * Method that send to the server packet that contains info about this player so server can register him and game can begin
     * @param username
     */
    private void sendRegistrationPacket(String username) {
        NetworkClasses.OnServerRegistrationPacket registration_packet = new NetworkClasses.OnServerRegistrationPacket();
        registration_packet.username = username;

        client.sendTCP(registration_packet);
        System.out.println("(Network Client) Sending registration packet!");
    }

    /**
     * Method that setsup all stuff like packets listener and proper way of handling them, creates players array etc.
     */
    private void setupClientStuff() {
        players = new Array<ServerPlayer>();

        client.addListener(new Listener() {

            @Override
            public void received(Connection connection, Object o) {
                super.received(connection, o);

                //read info about new player or when connected to server and server sends us whole list of players
                if(o instanceof NetworkClasses.NewPlayerPacket) {
                    NetworkClasses.NewPlayerPacket new_player_info = (NetworkClasses.NewPlayerPacket) o;

                    ServerPlayer new_player = new ServerPlayer(new_player_info.connection_id, new_player_info.username, new_player_info.is_host);
                    players.add(new_player);

                    System.out.println("New player info: " + new_player.username + " cid: " + new_player.connection_id + " is_host: " + new_player.is_host);
                }
                //info received when some player left server
                else if(o instanceof NetworkClasses.PlayerDisconnectedPacket) {
                    int con_id = ((NetworkClasses.PlayerDisconnectedPacket) o).connection_id;
                    players.removeValue(getPlayerInstanceByConnectionID(con_id), true);
                }

                //universe stuff
                else if(o instanceof NetworkClasses.GoToPlanetPacket) {
                    //this packet just forces client to load given planet
                    PlanetScreen planet_screen = game.getScreen(Screens.PLANET_SCREEN_NAME, PlanetScreen.class);

                    NetworkClasses.GoToPlanetPacket goto_packet = (NetworkClasses.GoToPlanetPacket) o;
                    planet_screen.createWorld(goto_packet.planet_index);

                    //show planet screen and hide universe screen
                    planet_screen.setVisible(true);
                    game.getScreen(Screens.UNIVERSE_SCREEN_NAME).setVisible(false);
                } else if(o instanceof NetworkClasses.VoteForGoingToPlanetPacket) {
                    final NetworkClasses.VoteForGoingToPlanetPacket vote_packet = (NetworkClasses.VoteForGoingToPlanetPacket) o;

                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            game.getDialogHandler().showDialog(new YesNoDialog("Go to planet: " + vote_packet.planet_index + " voting", game.getGUIViewport(), game).setListener(new YesNoDialog.YesNoDialogListener() {
                                @Override
                                public void yesOption() {
                                    NetworkClasses.VotingVotePacket vote_send_packet = new NetworkClasses.VotingVotePacket();
                                    vote_send_packet.voting_index = vote_packet.voting_index;
                                    vote_send_packet.vote = 1;
                                    getClient().sendTCP(vote_send_packet);
                                }

                                @Override
                                public void noOption() {
                                    NetworkClasses.VotingVotePacket vote_send_packet = new NetworkClasses.VotingVotePacket();
                                    vote_send_packet.voting_index = vote_packet.voting_index;
                                    vote_send_packet.vote = -1;
                                    getClient().sendTCP(vote_send_packet);
                                }
                            }));
                        }
                    };
                    Gdx.app.postRunnable(r);
                } else if(o instanceof NetworkClasses.UpdateCurrentIDAssignerValuePacket) {
                    NetworkClasses.UpdateCurrentIDAssignerValuePacket update_assigner_id = (NetworkClasses.UpdateCurrentIDAssignerValuePacket) o;
                    WorldObject.IDAssigner.set(update_assigner_id.new_current_id);
                }
            }

            @Override
            public void connected(Connection connection) {
                System.out.println("(Network Client) Someone connected to server! But he needs to register himself first!");
            }

            @Override
            public void disconnected(Connection connection) {
                //TODO this method is called when server will this kick player or some internet problems etc.
                //just connection lost and we have to handle it
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
     * Get Array that contains all registered players currently on server and info about them
     * @return instance of array, read only!
     */
    public Array<ServerPlayer> getPlayers() {
        return players;
    }

    /**
     * Getter for kryonet Client class instance
     * @return kryonet Client class instance
     */
    public Client getClient() {
        return client;
    }

    /**
     * Close client (connection sockets, destroy buffers etc done automatically by kryonet close(), just free memory)
     */
    public void dispose() {
        if(client != null) {
            try {
                client.dispose();
            } catch (IOException e) {
                System.err.println("Exception while disposing client: " + e.getMessage());
            }
        }
    }

}
