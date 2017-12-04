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
import com.badlogic.gdx.utils.TimeUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import explorer.game.framework.Game;
import explorer.game.Helper;
import explorer.game.screen.screens.planet.PlanetGUIScreen;
import explorer.game.screen.screens.planet.PlanetScreen;
import explorer.game.screen.screens.Screens;
import explorer.game.screen.screens.planet.WorldGeneratingScreen;
import explorer.game.screen.screens.planet.WorldLoadingScreen;
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

    /** DEBUG **/
    private ShapeRenderer shape_renderer;
    private BitmapFont font;

    public World(Game game, int planet_seed) {
        super(new Vector2(0, 0), null, game);
        world = this;

        data_provider = new FileChunkDataProvider();

        this.planet_seed = planet_seed;
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
        ((FileChunkDataProvider) data_provider).setWorldDir(getWorldDirectory(planet_seed));

        //generate world if there is need for it
        generateWorldIfHaveTo();

        //create chunks instances
        chunks = new explorer.world.chunk.WorldChunk[3][3];

        for(int i = 0; i < chunks.length; i++) {
            for(int j = 0; j < chunks[0].length; j++) {
                chunks[i][j] = new explorer.world.chunk.WorldChunk(new Vector2(i * CHUNK_WORLD_SIZE, j * CHUNK_WORLD_SIZE), this, game);
            }
        }

        for(int i = 0; i < chunks.length; i++) {
            for(int j = 0; j < chunks[0].length; j++) {
                //chunks[i][j].move(0, 0);
            }
        }

        //create player
        player = new Player(new Vector2(getPlanetProperties().PLANET_TYPE.PLANET_GENERATOR.getPlayerSpawn()), this, game);
        physics_engine.addWorldObject(player);

        chunk_rect = new Rectangle();

        initializated = true;

        /* DEBUG*/
        shape_renderer = new ShapeRenderer();
        font = new BitmapFont();
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
        if(!handle.exists() || true) {
            //so we have to generate our world :), first create dir for this planet
            handle.mkdirs();

            //set generating flag
            generating = true;

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

        if(chunks[0][0].getPosition().x + (move_factor_x * CHUNK_WORLD_SIZE) < 0) {
            can_move = false;
            teleport_chunks = true;
        } else if(chunks[0][0].getPosition().y + (move_factor_y * CHUNK_WORLD_SIZE) < 0) {
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
            System.out.println("Teleporting!");
            //to avoid this situation when 0,0 chunk is on pos 0,0 translate to (pos.xy + vec2(10000.0, 0.0))
            for(int i = 0; i < chunks.length; i++) {
                for(int j = 0; j < chunks[0].length; j++) {
                    chunks[i][j].moveToPosition(getPlanetProperties().PLANET_SIZE, 0);
                }
            }

            //because player is special object not bound to any chunk call move() function manually
            player.move(new Vector2(getPlanetProperties().PLANET_SIZE * CHUNK_WORLD_SIZE, 0));
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
                font.draw(batch, "Objects count: " + chunk.getObjects().size, chunk.getPosition().x + 50, chunk.getPosition().y + 50);
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
}
