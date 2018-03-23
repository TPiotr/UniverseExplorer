package explorer.universe;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import com.esotericsoftware.minlog.Log;

import explorer.game.framework.Game;
import explorer.game.screen.ScreenComponent;
import explorer.game.screen.screens.Screens;
import explorer.game.screen.screens.universe.UniverseLoadingScreen;
import explorer.game.screen.screens.universe.UniverseScreen;
import explorer.universe.chunk.UniverseChunk;
import explorer.universe.generator.BasicUniverseGenerator;
import explorer.universe.generator.UniverseChunkDataProvider;

/**
 * Created by RYZEN on 14.11.2017.
 */

public class Universe implements ScreenComponent {

    /**
     * Universe chunk size
     */
    public static final int UNIVERSE_CHUNK_SIZE = 24576;

    /**
     * Game instance
     */
    private Game game;

    /**
     * Array that holds all universe chunks instances (9 in total)
     */
    private UniverseChunk[][] chunks;

    /**
     * Data provider/universe generator instance
     */
    private UniverseChunkDataProvider chunks_data_provider;

    /**
     * Variables used in chunks moving system
     */
    private boolean last_can_move;
    private long last_time_chunk_changed;
    private long load_chunk_after = 100;

    /**
     * Debug shape renderer
     */
    private ShapeRenderer shape_renderer = new ShapeRenderer();

    /**
     * Variable used for sync f.e. planets orbiting
     */
    private float universe_time;

    public Universe(Game game) {
        this.game = game;

        //init data provider
        chunks_data_provider = new BasicUniverseGenerator();

        //create chunks instances and load them
        chunks = new UniverseChunk[3][3];
        for(int i = 0; i < chunks.length; i++) {
            for(int j = 0; j < chunks[0].length; j++) {
                chunks[i][j] = new UniverseChunk(new Vector2(i * UNIVERSE_CHUNK_SIZE, j * UNIVERSE_CHUNK_SIZE), this, game);
                chunks[i][j].move(0, 0);
            }
        }

        //load part
        for(int i = 0; i < chunks.length; i++) {
            for(int j = 0; j < chunks[0].length; j++) {
                chunks[i][j].move(0, 0);
            }
        }
    }

    @Override
    public void tick(float delta) {
        universe_time += delta;

         /* CHUNKS FOLLOWING MECHANISM */
        UniverseChunk center_chunk = chunks[1][1];

        int center_chunk_pos_x = (int) center_chunk.getPosition().x / UNIVERSE_CHUNK_SIZE;
        int center_chunk_pos_y = (int) center_chunk.getPosition().y / UNIVERSE_CHUNK_SIZE;

        /** Math.floor makes give it possiblity for moving in negative coords properly **/
        int camera_chunk_pos_x = (int) Math.floor(game.getMainCamera().position.x / UNIVERSE_CHUNK_SIZE);
        int camera_chunk_pos_y = (int) Math.floor(game.getMainCamera().position.y / UNIVERSE_CHUNK_SIZE);

        int move_factor_x = camera_chunk_pos_x - center_chunk_pos_x;
        int move_factor_y = camera_chunk_pos_y - center_chunk_pos_y;

        //check if we have to move chunks
        boolean can_move = (move_factor_x != 0 || move_factor_y != 0);

        //time delayed chunks loading mechanism
        if(can_move && !last_can_move) {
            last_time_chunk_changed = System.currentTimeMillis();
        }

        //if we have to load all chunks avoid delayed chunks loading system
        if(Math.abs(move_factor_x) > 1 || Math.abs(move_factor_y) > 1) {
            last_time_chunk_changed -= load_chunk_after * 2;
        }

        //if chunk that will be new center is not loaded yet show loading screen wait until it will be loaded and then move
        if(chunks.length > (1 + move_factor_x) && (1 + move_factor_x) >= 0) {
            if(getUniverseChunks()[1 + move_factor_x][1].isDirty()) {
                UniverseScreen universe_screen = game.getScreen(Screens.UNIVERSE_SCREEN_NAME, UniverseScreen.class);
                UniverseLoadingScreen loading_screen = game.getScreen(Screens.UNIVERSE_LOADING_SCREEN_NAME, UniverseLoadingScreen.class);

                universe_screen.setVisible(false);
                loading_screen.setVisible(true);

                Log.info("(Universe) Showing chunks loading screen");
                return;
            }
        }

        if(can_move && (System.currentTimeMillis() - last_time_chunk_changed > load_chunk_after)) {
            long time_start = System.currentTimeMillis();

            Log.debug("(Universe) Moving!");

            //if we can move check if we can copy some chunks to save processing time
            if(move_factor_x == -1 && move_factor_y == 0) {
                //so we move to the left
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
                //this is used if we f.e. teleported so we have to load all 9 chunks
                for (int i = 0; i < chunks.length; i++) {
                    for (int j = 0; j < chunks[0].length; j++) {
                        //destroy chunks and move them
                        chunks[i][j].dispose();
                        chunks[i][j].move(move_factor_x, move_factor_y);
                    }
                }
            }

            Log.debug("(Universe) Chunks operation time: " + TimeUtils.timeSinceMillis(time_start));
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
                UniverseScreen universe_screen = game.getScreen(Screens.UNIVERSE_SCREEN_NAME, UniverseScreen.class);
                UniverseLoadingScreen loading_screen = game.getScreen(Screens.UNIVERSE_LOADING_SCREEN_NAME, UniverseLoadingScreen.class);

                universe_screen.setVisible(false);
                loading_screen.setVisible(true);
                Log.info("(Universe) Showing chunks loading screen");

                return;
            }

            //update chunks
            for (int i = 0; i < chunks.length; i++) {
                for (int j = 0; j < chunks[0].length; j++) {
                    chunks[i][j].tick(delta);
                }
            }
        }

        last_can_move = can_move;
    }

    @Override
    public void render(SpriteBatch batch) {
        //render chunks
        for(int i = 0; i < chunks.length; i++) {
            for(int j = 0; j < chunks[0].length; j++) {
                chunks[i][j].render(batch);
            }
        }

        if(true)
            return;

        batch.end();

        //debug render
        shape_renderer.setProjectionMatrix(batch.getProjectionMatrix());
        shape_renderer.begin(ShapeRenderer.ShapeType.Line);

        shape_renderer.setColor(Color.WHITE);
        for(int i = 0; i < chunks.length; i++) {
            for (int j = 0; j < chunks[0].length; j++) {
                UniverseChunk chunk = chunks[i][j];
                shape_renderer.rect(chunk.getPosition().x, chunk.getPosition().y, chunk.getWH().x, chunk.getWH().y);
            }
        }

        shape_renderer.end();

        batch.begin();
    }

    /**
     * Get Universe chunks data provider (universe generator)
     * @return
     */
    public UniverseChunkDataProvider getChunksDataProvider() {
        return chunks_data_provider;
    }

    /**
     * Get universe array that contains all chunks instances
     * @return array with all universe chunks instances
     */
    public UniverseChunk[][] getUniverseChunks() {
        return chunks;
    }

    /**
     * Get local universe time
     * @return local universe time
     */
    public float getUniverseTime() {
        return universe_time;
    }
}
