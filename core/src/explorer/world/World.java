package explorer.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import explorer.game.framework.AssetsManager;
import explorer.game.framework.Game;
import explorer.game.Helper;
import explorer.game.screen.screens.planet.PlanetGUIScreen;
import explorer.game.screen.screens.planet.PlanetScreen;
import explorer.game.screen.screens.Screens;
import explorer.game.screen.screens.planet.WorldGeneratingScreen;
import explorer.game.screen.screens.planet.WorldLoadingScreen;
import explorer.network.NetworkClasses;
import explorer.network.client.ServerPlayer;
import explorer.network.server.NetworkChunkDataProvider;
import explorer.network.world.CanReceivePacketWorldObject;
import explorer.network.world.ChunkDataRequestsHandler;
import explorer.world.block.Blocks;
import explorer.world.chunk.WorldChunk;
import explorer.world.lighting.LightEngine;
import explorer.world.object.StaticWorldObject;
import explorer.world.object.WorldObject;
import explorer.world.object.objects.player.Player;
import explorer.world.physics.PhysicsEngine;
import explorer.world.planet.PlanetProperties;

/**
 * Created by RYZEN on 07.10.2017.
 */

public class World extends StaticWorldObject {

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
     * Determines in milis how long wait with chunks loading when player is on new one
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
    private boolean generating;

    /**
     * Flat that determines if world is initializated
     */
    private boolean initializated;

    /**
     * All blocks
     */
    private Blocks blocks;

    /**
     * Player that we control
     */
    private Player player;

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
     * Temp rect used in addObject() to check if given objects is inside some chunk
     */
    private Rectangle chunk_rect;

    /**
     * World bound packets listener for client or server depends on what game is at the moment
     */
    private Listener listener;

    /**
     * Server bound instance (only if server this is not null) handles ChunkDataRequest packets
     */
    private ChunkDataRequestsHandler server_request_handler;

    /**
     * Array that contains array of players that are just clones to renderer other players on client
     */
    private Array<Player> server_players;

    /** DEBUG **/
    private ShapeRenderer shape_renderer;

    public World(Game game, int planet_seed) {
        super(new Vector2(0, 0), null, game);
        world = this;

        this.planet_seed = planet_seed;

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
        combine_shader = Helper.createShaderProgram("shaders/basic_vertex_shader.vs", "shaders/combine_shader.fs", "COMBINE SHADER");

        combine_shader.begin();
        combine_shader.setUniformf("viewport_size", new Vector2(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        combine_shader.setUniformi("light_map", 1);
        combine_shader.setUniformf("ambient_color", getLightEngine().getAmbientColor());
        combine_shader.end();

        //load blocks
        blocks = new Blocks(this, game);

        //create chunk data provider
        if(!Game.IS_CLIENT) {
            data_provider = new FileChunkDataProvider();
            ((FileChunkDataProvider) data_provider).setWorldDir(getWorldDirectory(planet_seed));
        } else {
            data_provider = new NetworkChunkDataProvider(game, this);
        }

        //create server listener if is host for things combined with world, like sending chunk data, block updates etc.
        if(Game.IS_HOST) {
            server_request_handler = new ChunkDataRequestsHandler(this, game);

            //create players clones
            for(int i = 0; i < game.getGameServer().getPlayers().size; i++) {
                ServerPlayer server_player = game.getGameServer().getPlayers().get(i);

                Player new_player = new Player(new Vector2(), World.this, true, game);
                server_players.add(new_player);

                new_player.setRepresentingPlayer(server_player);
            }

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
                    }

                    else if(o instanceof NetworkClasses.BlockChangedPacket) {
                        NetworkClasses.BlockChangedPacket info = (NetworkClasses.BlockChangedPacket) o;

                        //so we are the server over there so try to make changed for this player and send that info to all other players
                        game.getGameServer().getServer().sendToAllExceptTCP(connection.getID(), info);

                        //handle new data
                        for(int i = 0; i < world.getWorldChunks().length; i++) {
                            for(int j = 0; j < world.getWorldChunks()[0].length; j++) {
                                WorldChunk chunk = world.getWorldChunks()[i][j];

                                //found proper chunk
                                if(chunk.getGlobalChunkXIndex() == info.chunk_x && chunk.getGlobalChunkYIndex() == info.chunk_y) {
                                    //don't notify network because we are there because of some other notify from network
                                    chunk.setBlock(info.block_x, info.block_y, info.new_block_id, info.background, false);
                                }
                            }
                        }
                    }

                    else if(o instanceof NetworkClasses.ObjectBoundPacket) {
                        //handle and send packets to all remaining players
                        game.getGameServer().getServer().sendToAllExceptTCP(connection.getID(), o);

                        NetworkClasses.ObjectBoundPacket packet = (NetworkClasses.ObjectBoundPacket) o;

                        //find object to which this is addressing
                        for(int i = 0; i < chunks.length; i++) {
                            for(int j = 0; j < chunks[0].length; j++) {
                                for(int k = 0; k < chunks[i][j].getObjects().size; k++) {
                                    WorldObject object = chunks[i][j].getObjects().get(k);

                                    if(object == null)
                                        continue;

                                    if(object instanceof CanReceivePacketWorldObject && object.OBJECT_ID == packet.object_id) {
                                        //System.out.println("Server o bound packet: " + o.getClass().getSimpleName() + " o_id: " + packet.object_id);
                                        ((CanReceivePacketWorldObject) object).receivedPacket(o);
                                    }
                                }
                            }
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
                            //create new server player if it wasn't created yet, we need this because problem was order of registered listener, sometimes this listener recives info about new player
                            //faster than listener in GameServer.java and we get null reference here
                            game.getGameServer().getPlayers().add(new ServerPlayer(connection.getID(), new_player_info.username, false));
                        }

                        new_player.setRepresentingPlayer(game.getGameServer().getPlayerInstanceByConnectionID(con_id));

                        //if some player connected to server when host world is not null we force that player to go to this planet
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                //wait a second until client will receive this packet because we can get ChunkDataRequestPacket when we don't even have world instance on client
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    //ignore
                                    //e.printStackTrace();
                                }

                                NetworkClasses.GoToPlanetPacket goto_planet_request = new NetworkClasses.GoToPlanetPacket();
                                goto_planet_request.planet_index = getPlanetProperties().PLANET_SEED;

                                game.getGameServer().getServer().sendToTCP(con_id, goto_planet_request);
                            }
                        };
                        new Thread(r).start();

                    } else if(o instanceof NetworkClasses.PlayerBoundPacket) {
                        //send this packet to all other clients
                        game.getGameServer().getServer().sendToAllExceptTCP(connection.getID(), o);

                        NetworkClasses.PlayerBoundPacket packet = (NetworkClasses.PlayerBoundPacket) o;

                        for(int i = 0; i < server_players.size; i++) {
                            Player player_clone = server_players.get(i);

                            if(player_clone.getRepresentingPlayer().connection_id == packet.player_connection_id) {
                                player_clone.processPacket(o);
                            }
                        }
                    }
                }
            };

            game.getGameServer().getServer().addListener(listener);
            System.out.println("Listener made");
        } else if(Game.IS_CLIENT) {

            //create players clones
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

                    if(o instanceof NetworkClasses.BlockChangedPacket) {
                        NetworkClasses.BlockChangedPacket info = (NetworkClasses.BlockChangedPacket) o;

                        //handle packet
                        for(int i = 0; i < world.getWorldChunks().length; i++) {
                            for(int j = 0; j < world.getWorldChunks()[0].length; j++) {
                                WorldChunk chunk = world.getWorldChunks()[i][j];

                                //found proper chunk
                                if(chunk.getGlobalChunkXIndex() == info.chunk_x && chunk.getGlobalChunkYIndex() == info.chunk_y) {
                                    //don't notify network because we are there because of some other notify from network
                                    chunk.setBlock(info.block_x, info.block_y, info.new_block_id, info.background, false);
                                }
                            }
                        }
                    }

                    else if(o instanceof NetworkClasses.ObjectBoundPacket) {
                        NetworkClasses.ObjectBoundPacket packet = (NetworkClasses.ObjectBoundPacket) o;

                        //find object to which this is addressing
                        for(int i = 0; i < chunks.length; i++) {
                            for(int j = 0; j < chunks[0].length; j++) {
                                for(int k = 0; k < chunks[i][j].getObjects().size; k++) {
                                    WorldObject object = chunks[i][j].getObjects().get(k);

                                    if(object == null)
                                        continue;

                                    if(object instanceof CanReceivePacketWorldObject && object.OBJECT_ID == packet.object_id) {
                                        //System.out.println("Client o bound packet: " + o.getClass().getSimpleName() + " o_id: " + packet.object_id);
                                        ((CanReceivePacketWorldObject) object).receivedPacket(o);
                                    }
                                }
                            }
                        }
                    }

                    //PLAYER STUFF

                    //make player clone
                    else if(o instanceof NetworkClasses.NewPlayerPacket) {
                        NetworkClasses.NewPlayerPacket new_player_info = (NetworkClasses.NewPlayerPacket) o;

                        Player new_player = new Player(new Vector2(), World.this, true, game);
                        server_players.add(new_player);

                        int con_id = new_player_info.connection_id;
                        new_player.setRepresentingPlayer(game.getGameClient().getPlayerInstanceByConnectionID(con_id));
                    }

                    //destroy player clone
                    else if(o instanceof NetworkClasses.PlayerDisconnectedPacket) {
                        int con_id = ((NetworkClasses.PlayerDisconnectedPacket) o).connection_id;

                        for(int i = 0; i < server_players.size; i++) {
                            if(server_players.get(i).getRepresentingPlayer().connection_id == con_id) {
                                server_players.get(i).dispose();
                                server_players.removeIndex(i);
                            }
                        }
                    }

                    //update players position
                    else if(o instanceof NetworkClasses.PlayerBoundPacket) {
                        //handle packet
                        NetworkClasses.PlayerBoundPacket packet = (NetworkClasses.PlayerBoundPacket) o;

                        for(int i = 0; i < server_players.size; i++) {
                            Player player_clone = server_players.get(i);

                            if(player_clone.getRepresentingPlayer().connection_id == packet.player_connection_id) {
                                player_clone.processPacket(o);
                            }
                        }
                    }
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

        chunk_rect = new Rectangle();

        initializated = true;

        /* DEBUG*/
        shape_renderer = new ShapeRenderer();
    }

    /**
     * Get planet directory by its seed
     * @param seed
     * @return
     */
    public static String getWorldDirectory(int seed) {
        return "universe/planets/" + seed + "/";
    }


    /**
     * Generate this planet and save to file if this planet wasnt generated before
     */
    protected void generateWorldIfHaveTo() {
        //first check if this planet folder exists
        final String world_dir = getWorldDirectory(getPlanetProperties().PLANET_SEED);

        FileHandle handle = Gdx.files.local(world_dir);
        if((!handle.exists() || true) && !Game.IS_CLIENT) {
            //so we have to generate our world :), first create dir for this planet
            handle.mkdirs();

            //set generating flag
            generating = true;

            //reset indexing stuff
            IDAssigner.set(0);

            //set this screen visible to false and show loading screen
            final PlanetScreen game_screen = game.getScreen(Screens.PLANET_SCREEN_NAME, PlanetScreen.class);
            final WorldGeneratingScreen generating_screen = game.getScreen(Screens.WORLD_GENERATING_SCREEN_NAME, WorldGeneratingScreen.class);

            game_screen.setVisible(false);
            generating_screen.setVisible(true);

            //run multithreaded world generation
            System.out.println("------------\nGenerating world:");

            //first split work into parts
            int threads = Runtime.getRuntime().availableProcessors();
            threads = (threads <= 0) ? 1 : threads;
            final int threads_num = threads;
            System.out.println("Threads generating count: " + threads_num);

            //calc how much every thread rows have to generate
            final int one_thread_x_width = Math.round((float) getPlanetProperties().PLANET_SIZE / (float) threads_num);
            System.out.println("Work per thread: "+one_thread_x_width);

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
                            System.out.println("Generating time: " + TimeUtils.timeSinceMillis(start_generating_time) + " milis" + "\n------------");

                            generating_screen.setVisible(false);
                            game_screen.setVisible(true);

                            //move by 0,0 to force chunks to load
                            for(int i = 0; i < chunks.length; i++) {
                                for(int j = 0; j < chunks[0].length; j++) {
                                    chunks[i][j].move(0, 0);
                                }
                            }

                            //TODO
                            int last_index = IDAssigner.accValue();

                            generating = false;
                        }
                    }
                };
                game.getThreadPool().runTask(generating_runnable);
            }

            if(one_thread_x_width * threads_num < getPlanetProperties().PLANET_SIZE) {
                int diff = getPlanetProperties().PLANET_SIZE - (one_thread_x_width * threads_num);
                System.out.println("have to generate: "+diff);
            }
        } else {
            //if there is no need for generating world or we are client just force chunks to load themselves
            for(int i = 0; i < chunks.length; i++) {
                for(int j = 0; j < chunks[0].length; j++) {
                    chunks[i][j].move(0, 0);
                }
            }
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
     * @return if object was added
     */
    public synchronized boolean addObject(WorldObject object) {
        for(int i = 0; i < getWorldChunks().length; i++) {
            for(int j = 0; j < getWorldChunks()[0].length; j++) {
                WorldChunk chunk = getWorldChunks()[i][j];
                Rectangle chunk_rect = new Rectangle(chunk.getPosition().x, chunk.getPosition().y, chunk.getWH().x, chunk.getWH().y);

                //so we have proper chunk now just transform global cords to local blocks coords
                if(chunk_rect.contains(object.getPosition())) {
                    return chunk.addObject(object);
                }
            }
        }
        return false;
    }

    @Override
    public void tick(float delta) {
        //can't tick if world is generating
        if(isGenerating() || !isInitializated())
            return;

        //calculate SIMULATE_LOGIC value

        //well this system is simple and doesn't forces host to calculate whole logic for all players
        //just to synchronized everything we will have some kind of local hosts of local regions where they are
        //so we have to calculate value SIMULATE_LOGIC based on distance from other players on server if true this client will calculate whole logic and send
        //result to other clients if some clients are near enough he will use this info and everything will be nice and synchronized

        //calculate this value only if game is in networking mode otherwise setting SIMULATE_LOGIC to true once is enough because noting will change it to other value

        if(Game.IS_HOST || Game.IS_CLIENT) {
            SIMULATE_LOGIC = true;
            for (int i = 0; i < server_players.size; i++) {
                Player clone = server_players.get(i);

                if (clone.isClone()) {
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

            System.out.println("Chunks operation time: " + TimeUtils.timeSinceMillis(time_start));
        } else if(teleport_chunks) {
            System.out.println("Teleporting! (tp_to_max: " + teleport_to_max + ", tp_to_zero: " + teleport_to_zero + ")");
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

            //first check if all 9 chunks are generating so we have to stop the game and show loading screen
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
                PlanetScreen game_screen = game.getScreen(Screens.PLANET_SCREEN_NAME, PlanetScreen.class);
                WorldLoadingScreen loading_screen = game.getScreen(Screens.WORLD_LOADING_SCREEN_NAME, WorldLoadingScreen.class);

                game_screen.setVisible(false);
                loading_screen.setVisible(true);
                //return;
            }

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

    @Override
    public void render(SpriteBatch batch) {
        if(!isInitializated() || isGenerating())
            return;

        //first light map
        light_engine.render(batch);

        //now rendering sprites combined with light map
        batch.setShader(combine_shader);

        //here 1~3 ms

        //bind light map texture as second texture
        light_engine.getLightMap().getColorBufferTexture().bind(1);
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

        //here ~0ms lol

        //firstly render background
        combine_shader.setUniformf("ambient_color", getPlanetProperties().PLANET_TYPE.PLANET_BACKGROUND.getColor());

        batch.setProjectionMatrix(game.getGUICamera().combined);
        getPlanetProperties().PLANET_TYPE.PLANET_BACKGROUND.render(batch);
        batch.flush();

        //pass other uniforms
        combine_shader.setUniformf("ambient_color", getLightEngine().getAmbientColor());

        //umm ~0ms

        //next render world
        batch.setProjectionMatrix(game.getMainCamera().combined);

        long start_time = System.currentTimeMillis();

        int blocks_rendered = 0;
        for(int i = 0; i < chunks.length; i++) {
            for(int j = 0; j < chunks[0].length; j++) {
                WorldChunk chunk = chunks[i][j];
                chunk.render(batch);

                blocks_rendered += chunk.blocks_rendered;
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

    @Override
    public void dispose() {
        if(light_engine != null)
            light_engine.dispose();

        if(combine_shader != null)
            combine_shader.dispose();

        if(Game.IS_HOST && listener != null)
            game.getGameServer().getServer().removeListener(listener);
        else if(Game.IS_CLIENT && listener != null)
            game.getGameClient().getClient().removeListener(listener);
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
        return generating;
    }

    /**
     * Is world initalizated flag
     * @return initalizated flag
     */
    public boolean isInitializated() {
        return initializated;
    }

    /**
     * Getter for array which contains cloned players (visual representation of other players)
     * @return array instance
     */
    public Array<Player> getClonedPlayers() {
        return server_players;
    }
}
