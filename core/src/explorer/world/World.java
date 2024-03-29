package explorer.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;

import org.apache.commons.lang3.SerializationUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import explorer.game.framework.AssetsManager;
import explorer.game.framework.Game;
import explorer.game.framework.utils.ShaderFactory;
import explorer.game.screen.gui.dialog.InfoDialog;
import explorer.game.screen.screens.planet.PlanetGUIScreen;
import explorer.game.screen.screens.planet.PlanetScreen;
import explorer.game.screen.screens.Screens;
import explorer.game.screen.screens.planet.WorldGeneratingScreen;
import explorer.game.screen.screens.planet.WorldLoadingScreen;
import explorer.network.NetworkClasses;
import explorer.network.NetworkHelper;
import explorer.network.client.ServerPlayer;
import explorer.network.server.GameServer;
import explorer.network.world.ChunkDataSaveRequestsHandler;
import explorer.network.world.HostNetworkChunkDataProvider;
import explorer.network.world.NetworkChunkDataProvider;
import explorer.network.world.CanReceivePacketWorldObject;
import explorer.network.world.ChunkDataRequestsHandler;
import explorer.network.world.ClientChunkDataRequestsHandler;
import explorer.world.block.Blocks;
import explorer.world.chunk.WorldChunk;
import explorer.world.lighting.LightEngine;
import explorer.world.object.StaticWorldObject;
import explorer.world.object.WorldObject;
import explorer.world.object.objects.player.Player;
import explorer.world.physics.PhysicsEngine;
import explorer.world.planet.PlanetProperties;
import explorer.world.object.WorldObject.IDAssigner;

/**
 * Created by RYZEN on 07.10.2017.
 */

public class World {

    /**
     * World properties
     */
    public static final int CHUNK_SIZE = 64;
    public static final int BLOCK_SIZE = 32;

    public static final int CHUNK_WORLD_SIZE = CHUNK_SIZE * BLOCK_SIZE;

    /**
     * Special variable that we take care of in world objects tick() method where we will calculate given world object behaviour or wait for it from network if given client is not 'local host of local region'
     */
    public static boolean SIMULATE_LOGIC = true;

    /**
     * All chunks that are in memory
     */
    private explorer.world.chunk.WorldChunk[][] chunks;

    /**
     * Determines in milis how long wait with chunks loading when player is on new chunk
     */
    private long load_chunk_after = 1000;
    /**
     * Last time when player changed chunk
     */
    private long last_time_chunk_changed;
    /**
     * Last state of can_move var in chunks following system, used in delayed chunk loading mechanism
     */
    private boolean last_can_move;

    /**
     * Instance of this world physics engine
     */
    private PhysicsEngine physics_engine;

    /**
     * This planet properties
     */
    private PlanetProperties planet_properties;

    /**
     * Planet seed
     */
    private int planet_seed;

    /**
     * Flag that determines if world is generating right now
     */
    private AtomicBoolean generating;

    /**
     * Flat that determines if world is initializated
     */
    private AtomicBoolean initializated;

    /**
     * Float that is same on every client so very useful variable to synchronize objects
     * (in fact this is time in seconds passed since this world was started)
     */
    public static float TIME;

    /**
     * Last TIME variable value when update packet was send to players
     */
    private int last_time_send;

    /**
     * All blocks
     */
    private Blocks blocks;

    /**
     * Player that we control
     */
    private Player player;

    /**
     * Game instance
     */
    private Game game;

    /**
     * Chunk data provider
     */
    protected ChunkDataProvider data_provider;

    /**
     * World light engine
     */
    private LightEngine light_engine;

    /**
     * Custom shader used to connect sprites with light map
     */
    private ShaderProgram combine_shader;

    /**
     * World bound packets listener for client or server depends on what game is at the moment
     */
    private Listener listener;

    /**
     * Server bound instance (only if server this is not null) handles ChunkDataRequest packets
     */
    private ChunkDataRequestsHandler server_request_handler;

    /**
     * Server bound instance, responsible for receiving ChunkDataSaveRequestPacket and save received chunk data to file
     */
    private ChunkDataSaveRequestsHandler server_save_request_handler;

    /**
     * Handles ChunkDataRequest when not loading from file but grabbing data from some player
     */
    private ClientChunkDataRequestsHandler client_request_handler;

    /**
     * Array that contains array of players that are just clones to renderer other players on client
     */
    private Array<Player> server_players;

    /** DEBUG **/
    private ShapeRenderer shape_renderer;

    public World(Game game, int planet_seed) {
        this.game = game;
        this.planet_seed = planet_seed;

        initializated = new AtomicBoolean(false);
        generating = new AtomicBoolean(false);

        server_players = new Array<Player>();
    }

    public void init() {
        //
        planet_properties = new PlanetProperties(this, game, planet_seed);

        //physics engine
        physics_engine = new PhysicsEngine(this, game);

        //light engine
        light_engine = new LightEngine(this, game);

        //load combine shader
        combine_shader = ShaderFactory.createShaderProgram("shaders/basic_vertex_shader.vs", "shaders/combine_shader.fs", "COMBINE SHADER");

        combine_shader.begin();
        combine_shader.setUniformf("viewport_size", new Vector2(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        combine_shader.setUniformi("light_map", 1);
        combine_shader.setUniformf("ambient_color", getLightEngine().getAmbientColor());
        combine_shader.end();

        //init IDAssigner
        IDAssigner.init(game, this);

        //load blocks
        blocks = new Blocks(this, game);

        //create chunk data provider
        if(Game.IS_CLIENT) {
            data_provider = new NetworkChunkDataProvider(game, this);
        } else if(Game.IS_HOST) {
            data_provider = new HostNetworkChunkDataProvider(game, this);
        } else {
            data_provider = new FileChunkDataProvider();
            ((FileChunkDataProvider) data_provider).setWorldDir(getWorldDirectory(planet_seed));
        }

        client_request_handler = new ClientChunkDataRequestsHandler(this, game);

        final World world = this;
        //create server listener if is host for things combined with world, like sending chunk data, block updates etc.
        if(Game.IS_HOST) {
            server_request_handler = new ChunkDataRequestsHandler(this, game);
            server_save_request_handler = new ChunkDataSaveRequestsHandler(this, game);

            //create players clones
            for(int i = 0; i < game.getGameServer().getPlayers().size; i++) {
                ServerPlayer server_player = game.getGameServer().getPlayers().get(i);

                Player new_player = new Player(new Vector2(), World.this, true, game);
                server_players.add(new_player);

                new_player.setRepresentingPlayer(server_player);
            }

            //inform all players about current IDAssigner value
            NetworkClasses.UpdateCurrentIDAssignerValuePacket id_assigner_packet = new NetworkClasses.UpdateCurrentIDAssignerValuePacket();
            id_assigner_packet.new_current_id = IDAssigner.accValue();
            game.getGameServer().getServer().sendToAllTCP(id_assigner_packet);

            //here is listener that receives packets on server side which concerns about handling chunk data requests, sending world changes through server to other players from another etc.
            listener = new Listener() {
                @Override
                public void disconnected(Connection connection) {
                    //destroy player clone
                    for(int i = 0; i < server_players.size; i++) {
                        if(server_players.get(i).getRepresentingPlayer().connection_id == connection.getID()) {
                            server_players.get(i).dispose();
                            server_players.removeIndex(i);
                        }
                    }
                }

                @Override
                public void received(Connection connection, Object o) {
                    if(o instanceof NetworkClasses.ChunkDataRequestPacket) {
                        server_request_handler.handleRequest((NetworkClasses.ChunkDataRequestPacket) o);
                    } else if(o instanceof NetworkClasses.ChunkDataSaveRequestPacket) {
                        server_save_request_handler.handleRequest((NetworkClasses.ChunkDataSaveRequestPacket) o);
                    } else if(o instanceof NetworkClasses.ChunkDataPacket) {
                        //send through data packet to proper client
                        NetworkClasses.ChunkDataPacket packet = (NetworkClasses.ChunkDataPacket) o;

                        if(packet.connection_id != GameServer.SERVER_CONNECTION_ID) {
                            game.getGameServer().getServer().sendToTCP(packet.connection_id, packet);
                        } else {
                            //parse new data
                            ((HostNetworkChunkDataProvider) getChunksDataProvider()).parseChunkDataPacket(packet);
                        }
                    }

                    else if(o instanceof NetworkClasses.BlockChangedPacket) {
                        NetworkClasses.BlockChangedPacket info = (NetworkClasses.BlockChangedPacket) o;

                        //so we are the server over there so try to make changed for this player and send that info to all other players
                        game.getGameServer().getServer().sendToAllExceptTCP(connection.getID(), info);

                        //handle new data
                        search_loop:
                        for(int i = 0; i < world.getWorldChunks().length; i++) {
                            for(int j = 0; j < world.getWorldChunks()[0].length; j++) {
                                WorldChunk chunk = world.getWorldChunks()[i][j];

                                //found proper chunk
                                if(chunk.getGlobalChunkXIndex() == info.chunk_x && chunk.getGlobalChunkYIndex() == info.chunk_y) {
                                    //don't notify network because we are there because of some other notify from network
                                    chunk.setBlock(info.block_x, info.block_y, info.new_block_id, info.background, false);

                                    break search_loop;
                                }
                            }
                        }
                    } else if(o instanceof NetworkClasses.ObjectRemovedPacket) {
                        NetworkClasses.ObjectRemovedPacket removed_object_packet = (NetworkClasses.ObjectRemovedPacket) o;

                        //send it to other players
                        game.getGameServer().getServer().sendToAllExceptUDP(connection.getID(), o);

                        //parse it like client
                        search_loop:
                        for(int i = 0; i < world.getWorldChunks().length; i++) {
                            for (int j = 0; j < world.getWorldChunks()[0].length; j++) {
                                WorldChunk chunk = world.getWorldChunks()[i][j];

                                //search for removed object in currently loaded world
                                for(int k = 0; k < chunk.getObjects().size; k++) {
                                    //if true we found our object that was removed so just remove it
                                    WorldObject object = chunk.getObjects().get(k);
                                    if(object != null && object.OBJECT_ID == removed_object_packet.removed_object_id) {
                                        world.removeObject(object, false);
                                        break search_loop;
                                    }
                                }
                            }
                        }
                    } else if(o instanceof NetworkClasses.NewObjectPacket) {
                        final NetworkClasses.NewObjectPacket new_object_packet = (NetworkClasses.NewObjectPacket) o;

                        int object_id = new_object_packet.OBJECT_ID;

                        //IDAssigner.accValue() != object_id not IDAssigner.accValue() + 1 != object_id because when we create new object on client/server side firstly TCP packet from IDassigner which informs
                        //server and then other players about new proper value of IDAssigner packet is send and then packet about new WorldObject so because TCP order is not randomly like in UDP
                        //we are sure that in this place in code current IDAssigner value should be same as new object ID

                        if(IDAssigner.accValue() != object_id) {
                            //if we are here that means new object has wrong ID
                            Log.info("(Network Server) New object with wrong id! (has: " + object_id + " should have: " + IDAssigner.accValue());

                            //send packet to this client to update this object id on client side to proper one
                            NetworkClasses.UpdateObjectIDPacket update_object_id_packet = new NetworkClasses.UpdateObjectIDPacket();
                            update_object_id_packet.acc_id = object_id;
                            update_object_id_packet.new_id = IDAssigner.next();
                            game.getGameServer().getServer().sendToTCP(connection.getID(), update_object_id_packet);

                            //correct object id and send packet to all other players
                            new_object_packet.OBJECT_ID = update_object_id_packet.new_id;
                        }

                        //send this packet to all other players
                        game.getGameServer().getServer().sendToAllExceptTCP(connection.getID(), new_object_packet);

                        //try to deserialize properties hashmap
                        final HashMap<String, String> object_properties = (new_object_packet.properties_bytes != null) ? (HashMap<String, String>) SerializationUtils.deserialize(new_object_packet.properties_bytes) : null;

                        Runnable create_object_runnable = new Runnable() {
                            @Override
                            public void run() {
                                WorldObject instance = FileChunkDataProvider.createInstanceFromClass(new_object_packet.new_object_class_name, new Vector2(new_object_packet.x, new_object_packet.y), World.this, game);
                                if(instance != null) {
                                    instance.OBJECT_ID = new_object_packet.OBJECT_ID;

                                    if (object_properties != null) {
                                        instance.setObjectProperties(object_properties);
                                    }

                                    World.this.addObject(instance, false);
                                }
                            }
                        };
                        Gdx.app.postRunnable(create_object_runnable);
                    } else if(o instanceof NetworkClasses.ObjectBoundPacket) {
                        NetworkClasses.ObjectBoundPacket packet = (NetworkClasses.ObjectBoundPacket) o;

                        //handle and send packets to all remaining players
                        if(packet.tcp)
                            game.getGameServer().getServer().sendToAllExceptTCP(connection.getID(), o);
                        else
                            game.getGameServer().getServer().sendToAllExceptUDP(connection.getID(), o);

                        //find object to which this is addressing
                        search_object_loop:
                        for(int i = 0; i < chunks.length; i++) {
                            for(int j = 0; j < chunks[0].length; j++) {
                                for(int k = 0; k < chunks[i][j].getObjects().size; k++) {
                                    WorldObject object = chunks[i][j].getObjects().get(k);

                                    if(object == null)
                                        continue;

                                    if(object instanceof CanReceivePacketWorldObject && object.OBJECT_ID == packet.object_id) {
                                        //System.out.println("Server o bound packet: " + o.getClass().getSimpleName() + " o_id: " + packet.object_id);
                                        ((CanReceivePacketWorldObject) object).receivedPacket(o);
                                        break search_object_loop;
                                    }
                                }
                            }
                        }
                    } else if(o instanceof NetworkClasses.UpdateCurrentIDAssignerValuePacket) {
                        NetworkClasses.UpdateCurrentIDAssignerValuePacket update_value_packet = (NetworkClasses.UpdateCurrentIDAssignerValuePacket) o;

                        int current_value = IDAssigner.accValue();

                        //decide here if new id is proper if not send to client which sended this information about acc proper id
                        if(update_value_packet.new_current_id - 1 == current_value) {
                            //proper value

                            //so update here current val and send current value to all other players
                            IDAssigner.set(update_value_packet.new_current_id);
                            game.getGameServer().getServer().sendToAllExceptTCP(connection.getID(), o);
                        } else {
                            //wrong value

                            //send to sender of this packet proper current value
                            NetworkClasses.UpdateCurrentIDAssignerValuePacket update_index_packet = new NetworkClasses.UpdateCurrentIDAssignerValuePacket();
                            update_index_packet.new_current_id = IDAssigner.accValue();
                            game.getGameServer().getServer().sendToTCP(connection.getID(), update_index_packet);
                            Log.error("(Network server) Player " + connection.getID() + " had wrong IDAssigner value (" + update_value_packet.new_current_id + ") proper is: " + current_value);
                        }
                    }

                    //PLAYER STUFF

                    //make player clone
                    else if(o instanceof NetworkClasses.OnServerRegistrationPacket) {
                        NetworkClasses.OnServerRegistrationPacket new_player_info = (NetworkClasses.OnServerRegistrationPacket) o;

                        Player new_player = new Player(new Vector2(), World.this, true, game);
                        server_players.add(new_player);

                        final int con_id = connection.getID();
                        ServerPlayer representing_player = game.getGameServer().getPlayerInstanceByConnectionID(con_id);

                        if(representing_player == null) {
                            //create new server player if it wasn't created yet, we need this because problem is order of registered listeners, sometimes this listener recives info about new player
                            //faster than listener in GameServer.java and we get null reference here
                            game.getGameServer().getPlayers().add(new ServerPlayer(connection.getID(), new_player_info.username, false));
                        }

                        new_player.setRepresentingPlayer(game.getGameServer().getPlayerInstanceByConnectionID(con_id));

                        //if some player connected to server when host world is not null we force that player to go to current planet
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                //wait a second until client will receive this packet because we can get ChunkDataRequestPacket when we don't even have world instance on client
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    //ignore
                                }

                                NetworkClasses.GoToPlanetPacket goto_planet_request = new NetworkClasses.GoToPlanetPacket();
                                goto_planet_request.planet_index = getPlanetProperties().PLANET_SEED;

                                game.getGameServer().getServer().sendToTCP(con_id, goto_planet_request);
                            }
                        };
                        new Thread(r).start();

                        //send world time update packet to have this variable in sync
                        NetworkClasses.UpdateGameTimePacket update_time_packet = new NetworkClasses.UpdateGameTimePacket();
                        update_time_packet.new_time = World.TIME;
                        game.getGameServer().getServer().sendToTCP(connection.getID(), update_time_packet);

                        //send current IDAssigner value
                        NetworkClasses.UpdateCurrentIDAssignerValuePacket update_id_assigner_packet = new NetworkClasses.UpdateCurrentIDAssignerValuePacket();
                        update_id_assigner_packet.new_current_id = IDAssigner.accValue();
                        game.getGameServer().getServer().sendToTCP(connection.getID(), update_id_assigner_packet);

                    } else if(o instanceof NetworkClasses.PlayerBoundPacket) {
                        NetworkClasses.PlayerBoundPacket packet = (NetworkClasses.PlayerBoundPacket) o;

                        //send this packet to all other clients
                        if(packet.tcp)
                            game.getGameServer().getServer().sendToAllExceptTCP(connection.getID(), o);
                        else
                            game.getGameServer().getServer().sendToAllExceptUDP(connection.getID(), o);

                        boolean found = false;
                        for(int i = 0; i < server_players.size; i++) {
                            Player player_clone = server_players.get(i);

                            if(player_clone.getRepresentingPlayer().connection_id == packet.player_connection_id) {
                                player_clone.processToClonePacket(o);
                                found = true;
                                break;
                            }
                        }

                        if(!found) {
                            if(NetworkHelper.getConnectionID(game) == packet.player_connection_id) {
                                getPlayer().processToPlayerPacket(packet);
                            }
                        }
                    }
                }
            };

            game.getGameServer().getServer().addListener(listener);
            Log.info("(World) Listener made");
        } else if(Game.IS_CLIENT) {
            //create connected & registered players clones
            for(int i = 0; i < game.getGameClient().getPlayers().size; i++) {
                ServerPlayer server_player = game.getGameClient().getPlayers().get(i);

                Player new_player = new Player(new Vector2(), World.this, true, game);
                server_players.add(new_player);

                new_player.setRepresentingPlayer(server_player);
            }

            //here we have listener which takes info from server which concerns about world changes, requests etc on client side
            listener = new Listener() {
                @Override
                public void received(Connection connection, Object o) {
                    if(!isInitializated())
                        return;

                    if(o instanceof NetworkClasses.ChunkDataRequestPacket) {
                        client_request_handler.handleRequest((NetworkClasses.ChunkDataRequestPacket) o);
                    } else if(o instanceof NetworkClasses.BlockChangedPacket) {
                        NetworkClasses.BlockChangedPacket info = (NetworkClasses.BlockChangedPacket) o;

                        //handle packet
                        search_block_loop:
                        for(int i = 0; i < world.getWorldChunks().length; i++) {
                            for(int j = 0; j < world.getWorldChunks()[0].length; j++) {
                                WorldChunk chunk = world.getWorldChunks()[i][j];

                                //found proper chunk
                                if(chunk.getGlobalChunkXIndex() == info.chunk_x && chunk.getGlobalChunkYIndex() == info.chunk_y) {
                                    //don't notify network because we are there because of some other notify from network
                                    chunk.setBlock(info.block_x, info.block_y, info.new_block_id, info.background, false);
                                    break search_block_loop;
                                }
                            }
                        }
                    } else if(o instanceof NetworkClasses.ObjectRemovedPacket) {
                        NetworkClasses.ObjectRemovedPacket removed_object_packet = (NetworkClasses.ObjectRemovedPacket) o;

                        remove_search_loop:
                        for(int i = 0; i < world.getWorldChunks().length; i++) {
                            for (int j = 0; j < world.getWorldChunks()[0].length; j++) {
                                WorldChunk chunk = world.getWorldChunks()[i][j];

                                //search for removed object in currently loaded world
                                for(int k = 0; k < chunk.getObjects().size; k++) {
                                    //if true we found our object that was removed so just remove it
                                    WorldObject object = chunk.getObjects().get(k);
                                    if(object != null && object.OBJECT_ID == removed_object_packet.removed_object_id) {
                                        world.removeObject(object, false);
                                        break remove_search_loop;
                                    }
                                }
                            }
                        }

                    } else if(o instanceof NetworkClasses.NewObjectPacket) {
                        final NetworkClasses.NewObjectPacket new_object_packet = (NetworkClasses.NewObjectPacket) o;

                        //try to deserialize properties hashmap
                        final HashMap<String, String> object_properties = (new_object_packet.properties_bytes != null) ? (HashMap<String, String>) SerializationUtils.deserialize(new_object_packet.properties_bytes) : null;

                        Log.debug("(World NetworkClient) New object packet! ( ID: " + new_object_packet.OBJECT_ID+ ")");
                        Runnable instantine_runnable = new Runnable() {
                            @Override
                            public void run() {
                                WorldObject instance = FileChunkDataProvider.createInstanceFromClass(new_object_packet.new_object_class_name, new Vector2(new_object_packet.x, new_object_packet.y), World.this, game);
                                if(instance != null) {
                                    instance.OBJECT_ID = new_object_packet.OBJECT_ID;

                                    if (object_properties != null) {
                                        instance.setObjectProperties(object_properties);
                                    }

                                    boolean added =  World.this.addObject(instance, false);
                                    Log.debug("(World NetworkClient) Placing object from network! " + added);
                                }
                            }
                        };
                        Gdx.app.postRunnable(instantine_runnable);
                    } else if(o instanceof NetworkClasses.ObjectBoundPacket) {
                        NetworkClasses.ObjectBoundPacket packet = (NetworkClasses.ObjectBoundPacket) o;

                        //find object to which this is addressing
                        search_loop:
                        for(int i = 0; i < chunks.length; i++) {
                            for(int j = 0; j < chunks[0].length; j++) {
                                for(int k = 0; k < chunks[i][j].getObjects().size; k++) {
                                    WorldObject object = chunks[i][j].getObjects().get(k);

                                    if(object == null)
                                        continue;

                                    if(object instanceof CanReceivePacketWorldObject && object.OBJECT_ID == packet.object_id) {
                                        //System.out.println("Client o bound packet: " + o.getClass().getSimpleName() + " o_id: " + packet.object_id);
                                        ((CanReceivePacketWorldObject) object).receivedPacket(o);
                                        break search_loop;
                                    }
                                }
                            }
                        }
                    } else if(o instanceof NetworkClasses.UpdateObjectIDPacket) {
                        //packet that updates some object ID to a new one because was unproperly assigned
                        NetworkClasses.UpdateObjectIDPacket update_id_packet = (NetworkClasses.UpdateObjectIDPacket) o;

                        search_loop:
                        for(int i = 0; i < chunks.length; i++) {
                            for (int j = 0; j < chunks[0].length; j++) {
                                for (int k = 0; k < chunks[i][j].getObjects().size; k++) {
                                    WorldObject object = chunks[i][j].getObjects().get(k);

                                    if (object == null)
                                        continue;

                                    if(object.OBJECT_ID == update_id_packet.acc_id) {
                                        object.OBJECT_ID = update_id_packet.new_id;
                                        break search_loop;
                                    }
                                }
                            }
                        }
                    } else if(o instanceof NetworkClasses.UpdateGameTimePacket) {
                        NetworkClasses.UpdateGameTimePacket update_time_packet = (NetworkClasses.UpdateGameTimePacket) o;
                        TIME = update_time_packet.new_time;
                    }

                    //PLAYER STUFF

                    //make player clone
                    else if(o instanceof NetworkClasses.NewPlayerPacket) {
                        NetworkClasses.NewPlayerPacket new_player_info = (NetworkClasses.NewPlayerPacket) o;

                        int con_id = new_player_info.connection_id;

                        //if new player is not registered by GameClient yet register it
                        if(game.getGameClient().getPlayerInstanceByConnectionID(con_id) == null) {
                            game.getGameClient().newPlayerPacket(new_player_info);
                        }

                        ServerPlayer server_player = game.getGameClient().getPlayerInstanceByConnectionID(con_id);
                        Player new_player = new Player(new Vector2(), World.this, true, game);

                        new_player.setRepresentingPlayer(server_player);

                        server_players.add(new_player);
                    }

                    //destroy player clone
                    else if(o instanceof NetworkClasses.PlayerDisconnectedPacket) {
                        int con_id = ((NetworkClasses.PlayerDisconnectedPacket) o).connection_id;

                        for(int i = 0; i < server_players.size; i++) {
                            if(server_players.get(i).getRepresentingPlayer().connection_id == con_id) {
                                server_players.get(i).dispose();
                                server_players.removeIndex(i);
                                break;
                            }
                        }
                    }

                    //update players position
                    else if(o instanceof NetworkClasses.PlayerBoundPacket) {
                        //handle packet
                        NetworkClasses.PlayerBoundPacket packet = (NetworkClasses.PlayerBoundPacket) o;

                        boolean found = false;
                        for(int i = 0; i < server_players.size; i++) {
                            Player player_clone = server_players.get(i);

                            if(player_clone.getRepresentingPlayer() == null)
                                continue;

                            if(player_clone.getRepresentingPlayer().connection_id == packet.player_connection_id) {
                                player_clone.processToClonePacket(o);
                                found = true;
                                break;
                            }
                        }

                        if(!found) {
                            if(NetworkHelper.getConnectionID(game) == packet.player_connection_id) {
                                player.processToPlayerPacket(o);
                            }
                        }
                    }
                }

                @Override
                public void disconnected(Connection connection) {
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            game.getDialogHandler().showDialog(new InfoDialog("Disconnected!", game.getGUIViewport(), game));
                            game.getScreen(Screens.PLANET_SCREEN_NAME, PlanetScreen.class).cleanUpAfterDisconnection();
                        }
                    };
                    Gdx.app.postRunnable(r);

                }
            };

            game.getGameClient().getClient().addListener(listener);
        }

        //create chunks instances
        chunks = new explorer.world.chunk.WorldChunk[3][3];

        for(int i = 0; i < chunks.length; i++) {
            for(int j = 0; j < chunks[0].length; j++) {
                chunks[i][j] = new explorer.world.chunk.WorldChunk(new Vector2(i * CHUNK_WORLD_SIZE, j * CHUNK_WORLD_SIZE), this, game);
            }
        }

        //generate world if there is need for it
        generateWorldIfHaveTo();

        //create player
        player = new Player(new Vector2(getPlanetProperties().PLANET_TYPE.PLANET_GENERATOR.getPlayerSpawn()), this, false, game);
        physics_engine.addWorldObject(player);

        initializated.set(true);

        /* DEBUG*/
        shape_renderer = new ShapeRenderer();
    }

    /**
     * Get planet directory by its seed
     * @param seed
     * @return
     */
    public static String getWorldDirectory(int seed) {
        return "game_data/universe/planets/" + seed + "/";
    }


    /**
     * Generate this planet and save to file if this planet wasnt generated before
     */
    protected void generateWorldIfHaveTo() {
        //first check if this planet folder exists
        final String world_dir = getWorldDirectory(getPlanetProperties().PLANET_SEED);

        FileHandle handle = Gdx.files.local(world_dir);
        if((!handle.exists()) && !Game.IS_CLIENT || true) {
            //so we have to generate our world, first create dir for this planet
            handle.mkdirs();

            //set generating flag
            generating.set(true);

            //reset indexing stuff
            IDAssigner.set(0);

            //set this screen visible to false and show loading screen
            final PlanetScreen game_screen = game.getScreen(Screens.PLANET_SCREEN_NAME, PlanetScreen.class);
            final WorldGeneratingScreen generating_screen = game.getScreen(Screens.WORLD_GENERATING_SCREEN_NAME, WorldGeneratingScreen.class);

            game_screen.setVisible(false);
            generating_screen.setVisible(true);

            //run multithreaded world generation
            Log.info("(World) ------------\nGenerating world:");

            //first split work into parts
            int threads = Runtime.getRuntime().availableProcessors();
            threads = (threads <= 0) ? 1 : threads;
            final int threads_num = threads;
            Log.info("(World) Threads generating count: " + threads_num);

            //calc how much every thread rows have to generate
            final int one_thread_x_width = Math.round((float) getPlanetProperties().PLANET_SIZE / (float) threads_num);
            Log.info("(World) Work per thread: "+one_thread_x_width);

            //thread safe for assigning indexes for working runnables and progress calculating
            final AtomicInteger index_integer = new AtomicInteger(0);
            final AtomicInteger done_integer = new AtomicInteger(0);

            //store when world generator was started
            final long start_generating_time = System.currentTimeMillis();

            for(int i = 0; i < threads_num; i++) {
                Runnable generating_runnable = new Runnable() {
                    @Override
                    public void run() {
                        final int idx = index_integer.getAndAdd(1);

                        for(int i = 0; i < one_thread_x_width; i++) {
                            for(int j = 0; j < getPlanetProperties().PLANET_TYPE.PLANET_GENERATOR.getMaxHeight(); j++) {
                                int chunk_index = idx * one_thread_x_width + i;
                                if(chunk_index >= getPlanetProperties().PLANET_SIZE) {
                                    continue;
                                }

                                Vector2 chunk_pos = new Vector2(chunk_index * World.CHUNK_WORLD_SIZE, j * World.CHUNK_WORLD_SIZE);

                                String chunk_path = FileChunkDataProvider.getPathToChunkFile(world_dir, chunk_pos);
                                getPlanetProperties().PLANET_TYPE.PLANET_GENERATOR.generateAndSaveChunk(chunk_path, chunk_pos);
                            }
                        }

                        //update generating progress
                        generating_screen.setProgress((float) done_integer.addAndGet(1) / (float) threads_num);

                        //if this is the last generating runnable show game screen again
                        if(done_integer.get() == threads_num) {
                            //save world info to file
                            saveWorldInfoToFile();

                            Log.info("(World) Generating time: " + TimeUtils.timeSinceMillis(start_generating_time) + " milis" + "\n------------");

                            generating_screen.setVisible(false);
                            game_screen.setVisible(true);

                            //move by 0,0 to force chunks to load
                            for(int i = 0; i < chunks.length; i++) {
                                for(int j = 0; j < chunks[0].length; j++) {
                                    chunks[i][j].move(0, 0);
                                }
                            }

                            //if we are server send info about current id assigner id
                            int acc_index = IDAssigner.accValue();

                            if(Game.IS_HOST) {
                                NetworkClasses.UpdateCurrentIDAssignerValuePacket update_assigner_id_packet = new NetworkClasses.UpdateCurrentIDAssignerValuePacket();
                                update_assigner_id_packet.new_current_id = acc_index + 1;
                                game.getGameServer().getServer().sendToAllTCP(update_assigner_id_packet);

                                NetworkClasses.UpdateGameTimePacket update_time_packet = new NetworkClasses.UpdateGameTimePacket();
                                update_time_packet.new_time = World.TIME;
                                game.getGameServer().getServer().sendToAllTCP(update_time_packet);
                            }

                            generating.set(false);
                        }
                    }
                };
                game.getThreadPool().runTask(generating_runnable);
            }

            if(one_thread_x_width * threads_num < getPlanetProperties().PLANET_SIZE) {
                int diff = getPlanetProperties().PLANET_SIZE - (one_thread_x_width * threads_num);

                Log.debug("(World) Have to generate: " + diff);
            }
        } else {
            //if there is no need for generating world or we are client just force chunks to load themselves and load world properties file
            if(!Game.IS_CLIENT)
                loadWorldInfoFromFile();

            for(int i = 0; i < chunks.length; i++) {
                for(int j = 0; j < chunks[0].length; j++) {
                    chunks[i][j].move(0, 0);
                }
            }
        }

    }

    /**
     * Save world properties to world properties file
     */
    protected void saveWorldInfoToFile() {
        //
        FileHandle handle = Gdx.files.local(getWorldDirectory(getPlanetProperties().PLANET_SEED) + "world.properties");
        //handle.mkdirs();

        DataOutputStream writer = new DataOutputStream(handle.write(false, 128));
        try {
            //write value from IDAssigner to next time properly assign id's to objects
            writer.writeInt(IDAssigner.accValue());

            writer.close();
            Log.info("(World) Saving world info done!");
        } catch (IOException e) {
            Log.error("(World) Failed to save world info file!", e);
        }
    }

    /**
     * Load world properties from world properties file
     */
    protected void loadWorldInfoFromFile() {
        long start_time = System.currentTimeMillis();
        DataInputStream reader = new DataInputStream(Gdx.files.local(getWorldDirectory(getPlanetProperties().PLANET_SEED) + "world.properties").read(128));
        try {
            //read value for current IDAssigner value
            int acc_id = reader.readInt();
            IDAssigner.set(acc_id);

            reader.close();
            Log.info("(World) World info read successful! (Time: " + (TimeUtils.timeSinceMillis(start_time)) + "ms");
        } catch (IOException e) {
            Log.error("(World) Failed to read world info file", e);

            //TODO handle corrupted world file(push back to menu and show dialog or whatever)
            game.getDialogHandler().showDialog(new InfoDialog("Failed to load world! (file is probably corrupted)", game.getGUIViewport(), game));
        }
    }

    /**
     * Call this when screen size is changed
     * @param new_w new screen width
     * @param new_h new screen height
     */
    public void screenSizeChanged(int new_w, int new_h) {
        if(combine_shader == null)
            return;

        combine_shader.begin();
        combine_shader.setUniformf("viewport_size", new Vector2(new_w, new_h));
        combine_shader.end();
    }

    /**
     * Add object to world
     * @param object given object
     * @param notify_network flag determining if proper packet informing other clients about new object will be send (true = send info)
     * @return if object was added
     */
    public synchronized boolean addObject(WorldObject object, boolean notify_network) {
        for(int i = 0; i < getWorldChunks().length; i++) {
            for(int j = 0; j < getWorldChunks()[0].length; j++) {
                WorldChunk chunk = getWorldChunks()[i][j];
                Rectangle chunk_rect = new Rectangle(chunk.getPosition().x, chunk.getPosition().y, chunk.getWH().x, chunk.getWH().y);

                //so we have proper chunk now just transform global cords to local blocks coords
                if(chunk_rect.contains(object.getPosition())) {
                    return chunk.addObject(object, notify_network);
                }
            }
        }
        return false;
    }

    /**
     * Removes given object from world
     * @param object object we want to remove
     * @param notify_network flag determining if proper packet informing other clients about removed object will be send (true = send info)
     */
    public synchronized void removeObject(WorldObject object, boolean notify_network) {
        if(object.getParentChunk() != null)
            object.getParentChunk().removeObject(object, notify_network);
    }

    /**
     * Tick world
     * @param delta delta time
     */
    public void tick(float delta) {
        //first check status about chunks if more than 70% procents of them are loading hide planet screen and show loading screen and wait until everything will be ready for playing a game
        if(isInitializated()) {
            int dirty_count = 0;
            for (int i = 0; i < chunks.length; i++) {
                for (int j = 0; j < chunks[0].length; j++) {
                    if (chunks[i][j].isDirty()) {
                        dirty_count++;
                    }
                }
            }

            //if more than 70% of chunks are generating stop the game and wait
            if (dirty_count >= (chunks.length * chunks.length) * .75f) {
                Log.info("(World) >= 75% of chunks dirty showing loading screen!");

                PlanetScreen game_screen = game.getScreen(Screens.PLANET_SCREEN_NAME, PlanetScreen.class);
                WorldLoadingScreen loading_screen = game.getScreen(Screens.WORLD_LOADING_SCREEN_NAME, WorldLoadingScreen.class);

                game_screen.setVisible(false);
                loading_screen.setVisible(true);
                return;
            }

            //if 1, 1 chunk is dirty we have to stop the game because chunk 1,1 is chunk where player is so we have to stop game to prevent from bugs
            if(chunks[1][1].isDirty()) {
                Log.info("(World) Showing loading screen because (1, 1) chunk is dirty");

                PlanetScreen game_screen = game.getScreen(Screens.PLANET_SCREEN_NAME, PlanetScreen.class);
                WorldLoadingScreen loading_screen = game.getScreen(Screens.WORLD_LOADING_SCREEN_NAME, WorldLoadingScreen.class);

                game_screen.setVisible(false);
                loading_screen.setVisible(true);
                return;
            }

            //every 10 seconds resend info about current world.Time to have everything in sync
            if((((int) TIME) - last_time_send >= 10) && Game.IS_HOST) {
                Log.info("(World, Host) Sending time update packet to players (TIME: " + TIME + ")");

                NetworkClasses.UpdateGameTimePacket update_time_packet = new NetworkClasses.UpdateGameTimePacket();
                update_time_packet.new_time = TIME;
                game.getGameServer().getServer().sendToAllTCP(update_time_packet);

                last_time_send = ((int) TIME);
            }
        }

        //can't tick if world is generating or is not initializated
        if(isGenerating() || !isInitializated()) {
            Log.debug("(World) Returning world tick method! (generating: " + isGenerating() + " initializated: " + isInitializated() + ")");
            return;
        }

        //calculate SIMULATE_LOGIC value

        //well this system is simple and doesn't forces host to calculate whole logic for all players
        //just to synchronized everything we will have some kind of local hosts of local regions where they are
        //so we have to calculate value SIMULATE_LOGIC based on distance from other players on server if true this client will calculate whole logic and send
        //result to other clients if some clients are near enough which were use this info

        //calculate this value only if game is in networking mode otherwise setting SIMULATE_LOGIC to true once is enough because noting will change it to other value

        if(Game.IS_HOST || Game.IS_CLIENT) {
            SIMULATE_LOGIC = true;
            for (int i = 0; i < server_players.size; i++) {
                Player clone = server_players.get(i);

                if(clone == null)
                    continue;

                if (clone.isClone() && clone.getRepresentingPlayer() != null) {
                    //host has priority to calculate local region logic
                    if (clone.getRepresentingPlayer().is_host) {
                        if (clone.getPosition().dst(getPlayer().getPosition()) < PhysicsEngine.DYNAMIC_WORK_RANGE) {
                            //if true host have to calculate logic and this client have only to wait
                            SIMULATE_LOGIC = false;
                        }
                    }
                }

                //well if we are host we have priority to calculate everything in surrounding region
                if (Game.IS_HOST) {
                    SIMULATE_LOGIC = true;
                }
            }
        }

        /* CHUNKS FOLLOWING MECHANISM */
        explorer.world.chunk.WorldChunk center_chunk = chunks[1][1];

        int center_chunk_pos_x = (int) center_chunk.getPosition().x / CHUNK_WORLD_SIZE;
        int center_chunk_pos_y = (int) center_chunk.getPosition().y / CHUNK_WORLD_SIZE;

        int camera_chunk_pos_x = (int) player.getPosition().x / CHUNK_WORLD_SIZE;
        int camera_chunk_pos_y = (int) player.getPosition().y / CHUNK_WORLD_SIZE;

        int move_factor_x = camera_chunk_pos_x - center_chunk_pos_x;
        int move_factor_y = camera_chunk_pos_y - center_chunk_pos_y;

        //check if move will not cause going into negative position
        boolean can_move = (move_factor_x != 0 || move_factor_y != 0);

        boolean teleport_chunks = false;
        boolean teleport_to_max = false;
        boolean teleport_to_zero = false;

        float chunk_max_x = World.CHUNK_WORLD_SIZE * getPlanetProperties().PLANET_SIZE;

        //TODO for now scrolling world to make planet like feeling is turned off to make multiplayer for now

        if (chunks[0][0].getPosition().x + (move_factor_x * CHUNK_WORLD_SIZE) < 0) {
            can_move = false;

            //teleport_chunks = true;
            //teleport_to_max = true;
        } else if(chunks[0][0].getPosition().x + (move_factor_x * CHUNK_WORLD_SIZE) > getPlanetProperties().PLANET_SIZE * CHUNK_WORLD_SIZE) {
            can_move = false;

            //teleport to zero here
        } else if (chunks[0][0].getPosition().y + (move_factor_y * CHUNK_WORLD_SIZE) < 0) {
            can_move = false;
        }

        //time delayed chunks loading mechanism
        if(can_move && !last_can_move) {
            last_time_chunk_changed = System.currentTimeMillis();
        }

        //if we have to load all chunks avoid delayed chunks loading system
        if(Math.abs(move_factor_x) > 1 || Math.abs(move_factor_y) > 1) {
            last_time_chunk_changed -= load_chunk_after * 2;
        }

        //if chunk that will be new center is not loaded yet show loading screen load it and then move
        if(chunks.length > (1 + move_factor_x) && (1 + move_factor_x) >= 0) {
            if(getWorldChunks()[1 + move_factor_x][1].isDirty()) {
                PlanetScreen game_screen = game.getScreen(Screens.PLANET_SCREEN_NAME, PlanetScreen.class);
                WorldLoadingScreen loading_screen = game.getScreen(Screens.WORLD_LOADING_SCREEN_NAME, WorldLoadingScreen.class);

                game_screen.setVisible(false);
                loading_screen.setVisible(true);
                return;
            }
        }

        if(can_move && (System.currentTimeMillis() - last_time_chunk_changed > load_chunk_after)) {
            long time_start = System.currentTimeMillis();

            //if we can move check if we can copy some chunks to save processing time
            if(move_factor_x == -1 && move_factor_y == 0) {
                //so we move to the left
                if(chunks[2][0].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[2][0], chunks[2][0].getPosition(), this, game);
                if(chunks[2][1].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[2][1], chunks[2][1].getPosition(), this, game);
                if(chunks[2][2].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[2][2], chunks[2][2].getPosition(), this, game);

                chunks[2][0].dispose();
                chunks[2][1].dispose();
                chunks[2][2].dispose();

                chunks[2][0].moveAndCopy(move_factor_x, move_factor_y, chunks[1][0]);
                chunks[2][1].moveAndCopy(move_factor_x, move_factor_y, chunks[1][1]);
                chunks[2][2].moveAndCopy(move_factor_x, move_factor_y, chunks[1][2]);

                chunks[1][0].moveAndCopy(move_factor_x, move_factor_y, chunks[0][0]);
                chunks[1][1].moveAndCopy(move_factor_x, move_factor_y, chunks[0][1]);
                chunks[1][2].moveAndCopy(move_factor_x, move_factor_y, chunks[0][2]);

                chunks[0][0].move(move_factor_x, move_factor_y);
                chunks[0][1].move(move_factor_x, move_factor_y);
                chunks[0][2].move(move_factor_x, move_factor_y);
            } else if(move_factor_x == 1 && move_factor_y == 0) {
                //right
                if(chunks[0][0].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[0][0], chunks[0][0].getPosition(), this, game);
                if(chunks[0][1].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[0][1], chunks[0][1].getPosition(), this, game);
                if(chunks[0][2].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[0][2], chunks[0][2].getPosition(), this, game);

                chunks[0][0].dispose();
                chunks[0][1].dispose();
                chunks[0][2].dispose();

                chunks[0][0].moveAndCopy(move_factor_x, move_factor_y, chunks[1][0]);
                chunks[0][1].moveAndCopy(move_factor_x, move_factor_y, chunks[1][1]);
                chunks[0][2].moveAndCopy(move_factor_x, move_factor_y, chunks[1][2]);

                chunks[1][0].moveAndCopy(move_factor_x, move_factor_y, chunks[2][0]);
                chunks[1][1].moveAndCopy(move_factor_x, move_factor_y, chunks[2][1]);
                chunks[1][2].moveAndCopy(move_factor_x, move_factor_y, chunks[2][2]);

                chunks[2][0].move(move_factor_x, move_factor_y);
                chunks[2][1].move(move_factor_x, move_factor_y);
                chunks[2][2].move(move_factor_x, move_factor_y);
            } else if(move_factor_x == 0 && move_factor_y == 1) {
                //up
                if(chunks[0][0].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[0][0], chunks[0][0].getPosition(), this, game);
                if(chunks[1][0].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[1][0], chunks[1][0].getPosition(), this, game);
                if(chunks[2][0].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[2][0], chunks[2][0].getPosition(), this, game);

                chunks[0][0].dispose();
                chunks[1][0].dispose();
                chunks[2][0].dispose();

                chunks[0][0].moveAndCopy(move_factor_x, move_factor_y, chunks[0][1]);
                chunks[1][0].moveAndCopy(move_factor_x, move_factor_y, chunks[1][1]);
                chunks[2][0].moveAndCopy(move_factor_x, move_factor_y, chunks[2][1]);

                chunks[0][1].moveAndCopy(move_factor_x, move_factor_y, chunks[0][2]);
                chunks[1][1].moveAndCopy(move_factor_x, move_factor_y, chunks[1][2]);
                chunks[2][1].moveAndCopy(move_factor_x, move_factor_y, chunks[2][2]);

                chunks[0][2].move(move_factor_x, move_factor_y);
                chunks[1][2].move(move_factor_x, move_factor_y);
                chunks[2][2].move(move_factor_x, move_factor_y);
            } else if(move_factor_x == 0 && move_factor_y == -1) {
                //down
                if(chunks[0][2].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[0][2], chunks[0][2].getPosition(), this, game);
                if(chunks[1][2].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[1][2], chunks[1][2].getPosition(), this, game);
                if(chunks[2][2].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[2][2], chunks[2][2].getPosition(), this, game);

                chunks[0][2].dispose();
                chunks[1][2].dispose();
                chunks[2][2].dispose();

                chunks[0][2].moveAndCopy(move_factor_x, move_factor_y, chunks[0][1]);
                chunks[1][2].moveAndCopy(move_factor_x, move_factor_y, chunks[1][1]);
                chunks[2][2].moveAndCopy(move_factor_x, move_factor_y, chunks[2][1]);

                chunks[0][1].moveAndCopy(move_factor_x, move_factor_y, chunks[0][0]);
                chunks[1][1].moveAndCopy(move_factor_x, move_factor_y, chunks[1][0]);
                chunks[2][1].moveAndCopy(move_factor_x, move_factor_y, chunks[2][0]);

                chunks[0][0].move(move_factor_x, move_factor_y);
                chunks[1][0].move(move_factor_x, move_factor_y);
                chunks[2][0].move(move_factor_x, move_factor_y);
            }
            /* CHUNKS DIAGONAL MOVING */
            else if(move_factor_x == 1 && move_factor_y == 1) {
                //right up
                if(chunks[0][0].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[0][0], chunks[0][0].getPosition(), this, game);
                if(chunks[1][0].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[1][0], chunks[1][0].getPosition(), this, game);
                if(chunks[2][0].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[2][0], chunks[2][0].getPosition(), this, game);
                if(chunks[0][1].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[0][1], chunks[0][1].getPosition(), this, game);
                if(chunks[0][2].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[0][2], chunks[0][2].getPosition(), this, game);

                chunks[0][0].dispose();
                chunks[1][0].dispose();
                chunks[2][0].dispose();
                chunks[0][1].dispose();
                chunks[0][2].dispose();

                //copy
                chunks[0][0].moveAndCopy(move_factor_x, move_factor_y, chunks[1][1]);
                chunks[1][0].moveAndCopy(move_factor_x, move_factor_y, chunks[2][1]);

                chunks[0][1].moveAndCopy(move_factor_x, move_factor_y, chunks[1][2]);
                chunks[1][1].moveAndCopy(move_factor_x, move_factor_y, chunks[2][2]);

                //load
                chunks[2][0].move(move_factor_x, move_factor_y);
                chunks[2][1].move(move_factor_x, move_factor_y);

                chunks[1][2].move(move_factor_x, move_factor_y);
                chunks[2][2].move(move_factor_x, move_factor_y);

                chunks[0][2].move(move_factor_x, move_factor_y);
            } else if(move_factor_x == -1 && move_factor_y == 1) {
                //left up
                if(chunks[0][0].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[0][0], chunks[0][0].getPosition(), this, game);
                if(chunks[1][0].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[1][0], chunks[1][0].getPosition(), this, game);
                if(chunks[2][0].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[2][0], chunks[2][0].getPosition(), this, game);
                if(chunks[2][1].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[2][1], chunks[2][1].getPosition(), this, game);
                if(chunks[2][2].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[2][2], chunks[2][2].getPosition(), this, game);

                chunks[0][0].dispose();
                chunks[1][0].dispose();
                chunks[2][0].dispose();
                chunks[2][1].dispose();
                chunks[2][2].dispose();

                //copy
                chunks[1][0].moveAndCopy(move_factor_x, move_factor_y, chunks[0][1]);
                chunks[2][0].moveAndCopy(move_factor_x, move_factor_y, chunks[1][1]);

                chunks[2][1].moveAndCopy(move_factor_x, move_factor_y, chunks[1][2]);
                chunks[1][1].moveAndCopy(move_factor_x, move_factor_y, chunks[0][2]);

                //load
                chunks[0][0].move(move_factor_x, move_factor_y);
                chunks[0][1].move(move_factor_x, move_factor_y);
                chunks[0][2].move(move_factor_x, move_factor_y);

                chunks[1][2].move(move_factor_x, move_factor_y);
                chunks[2][2].move(move_factor_x, move_factor_y);
            } else if(move_factor_x == 1 && move_factor_y == -1) {
                //right down
                if(chunks[0][0].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[0][0], chunks[0][0].getPosition(), this, game);
                if(chunks[0][1].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[0][1], chunks[0][1].getPosition(), this, game);
                if(chunks[0][2].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[0][2], chunks[0][2].getPosition(), this, game);
                if(chunks[1][2].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[1][2], chunks[1][2].getPosition(), this, game);
                if(chunks[2][2].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[2][2], chunks[2][2].getPosition(), this, game);

                chunks[0][0].dispose();
                chunks[0][1].dispose();
                chunks[0][2].dispose();
                chunks[1][2].dispose();
                chunks[2][2].dispose();

                //copy
                chunks[0][2].moveAndCopy(move_factor_x, move_factor_y, chunks[1][1]);
                chunks[1][2].moveAndCopy(move_factor_x, move_factor_y, chunks[2][1]);

                chunks[0][1].moveAndCopy(move_factor_x, move_factor_y, chunks[1][0]);
                chunks[1][1].moveAndCopy(move_factor_x, move_factor_y, chunks[2][0]);

                //load
                chunks[0][0].move(move_factor_x, move_factor_y);
                chunks[1][0].move(move_factor_x, move_factor_y);
                chunks[2][0].move(move_factor_x, move_factor_y);

                chunks[2][1].move(move_factor_x, move_factor_y);
                chunks[2][2].move(move_factor_x, move_factor_y);
            } else if(move_factor_x == -1 && move_factor_y == -1) {
                //left down
                if(chunks[2][0].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[2][0], chunks[2][0].getPosition(), this, game);
                if(chunks[2][1].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[2][1], chunks[2][1].getPosition(), this, game);
                if(chunks[2][2].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[2][2], chunks[2][2].getPosition(), this, game);
                if(chunks[0][2].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[0][2], chunks[0][2].getPosition(), this, game);
                if(chunks[1][2].isSaveRequest())
                    getChunksDataProvider().saveChunkData(null, chunks[1][2], chunks[1][2].getPosition(), this, game);

                chunks[2][0].dispose();
                chunks[2][1].dispose();
                chunks[2][2].dispose();
                chunks[0][2].dispose();
                chunks[1][2].dispose();

                //copy
                chunks[1][2].moveAndCopy(move_factor_x, move_factor_y, chunks[0][1]);
                chunks[2][2].moveAndCopy(move_factor_x, move_factor_y, chunks[1][1]);
                chunks[2][1].moveAndCopy(move_factor_x, move_factor_y, chunks[1][0]);
                chunks[1][1].moveAndCopy(move_factor_x, move_factor_y, chunks[0][0]);

                //load
                chunks[0][2].move(move_factor_x, move_factor_y);
                chunks[0][1].move(move_factor_x, move_factor_y);
                chunks[0][0].move(move_factor_x, move_factor_y);

                chunks[2][0].move(move_factor_x, move_factor_y);
                chunks[1][0].move(move_factor_x, move_factor_y);
            }
            /* CHUNKS TELEPORTING AND UPDATING STUFF */
            else {
                //clear all ground light lights because they have to be recalculated again
                getLightEngine().getGroundLineRenderer().getPositions().clear();

                //this is used if we f.e. teleported so we have to load all 9 chunks
                for (int i = 0; i < chunks.length; i++) {
                    for (int j = 0; j < chunks[0].length; j++) {
                        //destroy chunks and its physics bodies because everything needs to be recalculated
                        chunks[i][j].dispose();

                        if (chunks[i][j].isSaveRequest())
                            getChunksDataProvider().saveChunkData(null, chunks[i][j], chunks[i][j].getPosition(), this, game);

                        chunks[i][j].move(move_factor_x, move_factor_y);
                    }
                }
            }

            //filter out old lights from light engine
            getLightEngine().getGroundLineRenderer().filterOut();

            Log.debug("(World) Chunks operation time: " + TimeUtils.timeSinceMillis(time_start));
        } else if(teleport_chunks) {
            Log.debug("(World) Teleporting! (tp_to_max: " + teleport_to_max + ", tp_to_zero: " + teleport_to_zero + ")");
            if(teleport_to_max) {
                //to avoid this situation when 0,0 chunk is on pos 0,0 translate to (pos.xy + vec2(10000.0, 0.0))
                for (int i = 0; i < chunks.length; i++) {
                    for (int j = 0; j < chunks[0].length; j++) {
                        chunks[i][j].moveToPosition(getPlanetProperties().PLANET_SIZE, 0);
                    }
                }

                //because player is special object not bound to any chunk call move() function manually
                player.move(new Vector2((getPlanetProperties().PLANET_SIZE) * CHUNK_WORLD_SIZE, 0));
            }
        } else {
            //update chunks
            for (int i = 0; i < chunks.length; i++) {
                for (int j = 0; j < chunks[0].length; j++) {
                    chunks[i][j].tick(delta);
                }
            }
        }

        //update server players
        for(int i = 0; i < server_players.size; i++) {
            server_players.get(i).tick(delta);
        }

        //update player
        player.tick(delta);

        //physics step
        physics_engine.tick(delta);

        //update sky
        getPlanetProperties().PLANET_TYPE.PLANET_BACKGROUND.tick(delta);

        last_can_move = can_move;

        //tick gui
        game.getScreen(Screens.PLANET_GUI_SCREEN_NAME, PlanetGUIScreen.class).tick(delta);
    }

    /**
     * Render world
     * @param batch sprite batch instance
     */
    public void render(SpriteBatch batch) {
        if(!isInitializated() || isGenerating())
            return;

        //first light map
        light_engine.render(batch);

        //now rendering sprites combined with light map
        batch.setShader(combine_shader);

        //bind light map texture as second texture
        light_engine.getLightMap().getColorBufferTexture().bind(1);
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

        //firstly render background
        combine_shader.setUniformf("ambient_color", getPlanetProperties().PLANET_TYPE.PLANET_BACKGROUND.getColor());

        batch.setProjectionMatrix(game.getGUICamera().combined);
        getPlanetProperties().PLANET_TYPE.PLANET_BACKGROUND.render(batch);
        batch.flush();

        //pass other uniforms
        combine_shader.setUniformf("ambient_color", getLightEngine().getAmbientColor());

        //next render world
        batch.setProjectionMatrix(game.getMainCamera().combined);

        for(int i = 0; i < chunks.length; i++) {
            for(int j = 0; j < chunks[0].length; j++) {
                WorldChunk chunk = chunks[i][j];
                chunk.render(batch);
            }
        }

        //render server players
        for(int i = 0; i < server_players.size; i++) {
            server_players.get(i).render(batch);
        }

        //System.out.println("render: " + TimeUtils.timeSinceMillis(start_time) + " blocks rendered: " + blocks_rendered);

        player.render(batch);

        //set back to normal shader
        batch.setShader(null);

        //render gui screen
        game.getScreen(Screens.PLANET_GUI_SCREEN_NAME, PlanetGUIScreen.class).render(batch);

        if(true)
            return;

        /** DEBUG SHAPE RENDERING **/
        batch.end();

        shape_renderer.setProjectionMatrix(game.getMainCamera().combined);
        shape_renderer.begin(ShapeRenderer.ShapeType.Line);
        for(int i = 0; i < chunks.length; i++) {
            for(int j = 0; j < chunks[0].length; j++) {
                explorer.world.chunk.WorldChunk chunk = chunks[i][j];
                shape_renderer.rect(chunk.getPosition().x, chunk.getPosition().y, chunk.getWH().x, chunk.getWH().y);

                batch.begin();
                AssetsManager.font.draw(batch, "Objects count: " + chunk.getObjects().size, chunk.getPosition().x + 50, chunk.getPosition().y + 50);
                batch.end();

                //pseudo render chunk
                /*shape_renderer.setColor(Color.BLUE);
                for(int ii = 0; ii < chunk.getBlocks().length; ii++) {
                    for(int jj = 0; jj < chunk.getBlocks()[0].length; jj++) {
                        int block = chunk.getBlocks()[ii][jj].getForegroundBlock();

                        //if is not air
                        if(block != getBlocks().AIR.getBlockID()) {
                            shape_renderer.rect(chunk.getPosition().x + (ii * World.BLOCK_SIZE), chunk.getPosition().y + (jj * World.BLOCK_SIZE), World.BLOCK_SIZE, World.BLOCK_SIZE);
                        }
                    }
                }
                shape_renderer.setColor(Color.YELLOW);*/
            }
        }
        shape_renderer.end();

        //debug physics engine rendering
        physics_engine.debugRender(shape_renderer);

        batch.begin();
    }

    /**
     * Dispose this world instance (save world info, chunks, dispose shaders, remove server/client listeners etc)
     */
    public void dispose() {
        if(isInitializated() && player != null)
            player.dispose();

        if(isInitializated() && (Game.IS_HOST || (!Game.IS_HOST || !Game.IS_CLIENT)))
            saveWorldInfoToFile();

        if(light_engine != null)
            light_engine.dispose();

        if(Game.IS_HOST && listener != null) {
            game.getGameServer().getServer().removeListener(listener);

            if(getServerSaveChunkDataRequestsHandler() != null) {
                getServerSaveChunkDataRequestsHandler().dispose();
            }
        } else if(Game.IS_CLIENT && listener != null) {
            game.getGameClient().getClient().removeListener(listener);

            //send back all pending chunk data requests
            for (int i = 0; i < getClientChunkDataRequestHandler().getPendingRequests().size; i++) {
                getClientChunkDataRequestHandler().getPendingRequests().get(i).rejected_id = NetworkHelper.getConnectionID(game);
                game.getGameClient().getClient().sendTCP(getClientChunkDataRequestHandler().getPendingRequests().get(i));
            }
            getClientChunkDataRequestHandler().getPendingRequests().clear();
        }
    }

    /**
     * @return get all chunks that are in memory
     */
    public WorldChunk[][] getWorldChunks() {
        return chunks;
    }

    /**
     * Get physics engine instance
     * @return physics engine
     */
    public PhysicsEngine getPhysicsEngine() {
        return physics_engine;
    }

    /**
     * Getter for player instance
     * @return get player instance
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Get all blocks instance
     * @return class that holds all blocks
     */
    public Blocks getBlocks() {
        return blocks;
    }

    /**
     * Get chunks data provider
     * @return data provider
     */
    public ChunkDataProvider getChunksDataProvider() {
        return data_provider;
    }

    /**
     * Get light engine instance for this world
     * @return light engine instance
     */
    public LightEngine getLightEngine() {
        return light_engine;
    }

    /**
     * Get this planet properties
     * @return planet properties instance
     */
    public synchronized PlanetProperties getPlanetProperties() {
        return planet_properties;
    }

    /**
     * Getter for generating flag
     * @return true if world is generating in background
     */
    public boolean isGenerating() {
        return generating.get();
    }

    /**
     * Is world initalizated flag
     * @return initalizated flag
     */
    public boolean isInitializated() {
        return initializated.get();
    }

    /**
     * Getter for array which contains cloned players (visual representation of other players)
     * @return array instance
     */
    public Array<Player> getClonedPlayers() {
        return server_players;
    }

    /**
     * Getter for ClientChunkDataRequestsHandler
     * @return instance
     */
    public ClientChunkDataRequestsHandler getClientChunkDataRequestHandler() {
        return client_request_handler;
    }

    /**
     * Getter for chunk data requests handle when game instance is server
     * @return instance
     */
    public ChunkDataRequestsHandler getServerChunkDataRequestsHandler() {
        return server_request_handler;
    }

    /**
     * Getter for save chunk data requests handler not null when game is server
     * @return instance
     */
    public ChunkDataSaveRequestsHandler getServerSaveChunkDataRequestsHandler() {
        return server_save_request_handler;
    }
}
