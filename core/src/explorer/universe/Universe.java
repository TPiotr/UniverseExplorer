package explorer.universe;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;

import explorer.game.framework.Game;
import explorer.game.screen.ScreenComponent;
import explorer.universe.chunk.UniverseChunk;
import explorer.universe.generator.BasicUniverseGenerator;
import explorer.universe.generator.UniverseChunkDataProvider;

/**
 * Created by RYZEN on 14.11.2017.
 */

public class Universe implements ScreenComponent {

    public static final int UNIVERSE_CHUNK_SIZE = 1024;

    private Game game;

    private UniverseChunk[][] chunks;

    private UniverseChunkDataProvider chunks_data_provider;

    private boolean last_can_move;
    private long last_time_chunk_changed;
    private long load_chunk_after = 0;

    private ShapeRenderer shape_renderer = new ShapeRenderer();

    public Universe(Game game) {
        this.game = game;

        chunks = new UniverseChunk[3][3];
        for(int i = 0; i < chunks.length; i++) {
            for(int j = 0; j < chunks[0].length; j++) {
                chunks[i][j] = new UniverseChunk(new Vector2(i * UNIVERSE_CHUNK_SIZE, j * UNIVERSE_CHUNK_SIZE), this, game);
            }
        }

        chunks_data_provider = new BasicUniverseGenerator();
    }

    @Override
    public void tick(float delta) {
         /* CHUNKS FOLLOWING MECHANISM */
        UniverseChunk center_chunk = chunks[1][1];

        int center_chunk_pos_x = (int) center_chunk.getPosition().x / UNIVERSE_CHUNK_SIZE;
        int center_chunk_pos_y = (int) center_chunk.getPosition().y / UNIVERSE_CHUNK_SIZE;

        int camera_chunk_pos_x = (int) game.getMainCamera().position.x / UNIVERSE_CHUNK_SIZE;
        int camera_chunk_pos_y = (int) game.getMainCamera().position.y / UNIVERSE_CHUNK_SIZE;

        int move_factor_x = camera_chunk_pos_x - center_chunk_pos_x;
        int move_factor_y = camera_chunk_pos_y - center_chunk_pos_y;

        System.out.println("MFX: " + move_factor_x + " MFY: " + move_factor_y);

        //check if move will not cause going into negative position
        boolean can_move = (move_factor_x != 0 || move_factor_y != 0);

        if(chunks[0][0].getPosition().x + (move_factor_x * UNIVERSE_CHUNK_SIZE) < 0) {
            //can_move = false;
        } else if(chunks[0][0].getPosition().y + (move_factor_y * UNIVERSE_CHUNK_SIZE) < 0) {
            //can_move = false;
        }

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
                /*PlanetScreen game_screen = game.getScreen(Screens.GAME_SCREEN_NAME, PlanetScreen.class);
                WorldLoadingScreen loading_screen = game.getScreen(Screens.WORLD_LOADING_SCREEN_NAME, WorldLoadingScreen.class);

                game_screen.setVisible(false);
                loading_screen.setVisible(true);*/
                //TODO make and show generating progress screen
                return;
            }
        }

        if(can_move && (System.currentTimeMillis() - last_time_chunk_changed > load_chunk_after)) {
            long time_start = System.currentTimeMillis();

            //if we can move check if we can copy some chunks to save processing time
            if(move_factor_x == -1 && move_factor_y == 0) {
                //so we move to the left
                chunks[2][0].dispose();
                chunks[2][1].dispose();
                chunks[2][2].dispose();

                //physics_engine.getPhysicsEngineChunksHelper().destroyChunkPhysicsBody(chunks[2][0]);
                //physics_engine.getPhysicsEngineChunksHelper().destroyChunkPhysicsBody(chunks[2][1]);
                //physics_engine.getPhysicsEngineChunksHelper().destroyChunkPhysicsBody(chunks[2][2]);

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

            System.out.println("(Universe) Chunks operation time: " + TimeUtils.timeSinceMillis(time_start));
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
                /*PlanetScreen game_screen = game.getScreen(Screens.GAME_SCREEN_NAME, PlanetScreen.class);
                WorldLoadingScreen loading_screen = game.getScreen(Screens.WORLD_LOADING_SCREEN_NAME, WorldLoadingScreen.class);

                game_screen.setVisible(false);
                loading_screen.setVisible(true);*/
                //TODO make and show generating progress screen

                //return;
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
        for(int i = 0; i < chunks.length; i++) {
            for(int j = 0; j < chunks[0].length; j++) {
                chunks[i][j].render(batch);
            }
        }

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
    }

    public UniverseChunkDataProvider getChunksDataProvider() {
        return chunks_data_provider;
    }
    
    public UniverseChunk[][] getUniverseChunks() {
        return chunks;
    }
}
