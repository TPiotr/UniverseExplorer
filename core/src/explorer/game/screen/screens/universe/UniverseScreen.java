package explorer.game.screen.screens.universe;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import explorer.game.framework.Game;
import explorer.game.screen.Screen;
import explorer.game.screen.screens.Screens;
import explorer.game.screen.screens.planet.PlanetScreen;
import explorer.universe.Universe;
import explorer.universe.chunk.UniverseChunk;
import explorer.universe.object.PlanetUniverseObject;
import explorer.universe.object.UniverseObject;

/**
 * Created by RYZEN on 14.11.2017.
 */

public class UniverseScreen extends Screen {

    private Universe universe;

    //moving stuff
    private Vector2 camera_target_pos = new Vector2();

    //zooming to planet stuff
    private boolean zoom_in;

    private float target_zoom = .1f;
    private final float max_zoom = 35f;

    private Vector2 target_pos = new Vector2();
    private PlanetUniverseObject clicked_planet;

    private BitmapFont font;

    /**
     * Construct new screen instance
     * @param game game instance
     */
    public UniverseScreen(final Game game) {
        super(game);

        font = new BitmapFont();

        NAME = Screens.UNIVERSE_SCREEN_NAME;

        universe = new Universe(game);

        game.getMainCamera().zoom = max_zoom;
        game.getMainCamera().update();

        //universe user input stuff
        GestureDetector.GestureAdapter gesture_adapter = new GestureDetector.GestureAdapter() {

            float last_distance;

            @Override
            public boolean zoom(float initialDistance, float distance) {
                if(!isVisible())
                    return false;

                int dir = (distance - last_distance > 0) ? -1 : 1;
                dir = (distance == last_distance) ? 0 : dir;

                game.getMainCamera().zoom += distance * .00005f * dir * game.getMainCamera().zoom;

                game.getMainCamera().zoom = MathUtils.clamp(game.getMainCamera().zoom, target_zoom * 5, max_zoom);
                game.getMainCamera().update();

                last_distance = distance;
                return true;
            }

            @Override
            public boolean tap(float x, float y, int count, int button) {
                if(!isVisible())
                    return false;

                Vector2 pos = new Vector2(x, y);
                game.getMainViewport().unproject(pos);

                boolean planet = false;

                //try to find and zoom to planet after double tap
                if(count == 2) {
                    for (int i = 0; i < universe.getUniverseChunks().length; i++) {
                        for (int j = 0; j < universe.getUniverseChunks()[0].length; j++) {
                            UniverseChunk chunk = universe.getUniverseChunks()[i][j];
                            Rectangle chunk_rect = new Rectangle(chunk.getPosition().x, chunk.getPosition().y, chunk.getWH().x, chunk.getWH().y);

                            if (chunk_rect.contains(pos)) {
                                Rectangle object_rect = new Rectangle();
                                for (int k = 0; k < chunk.getObjects().size; k++) {
                                    UniverseObject o = chunk.getObjects().get(k);

                                    if (o instanceof PlanetUniverseObject) {
                                        object_rect.set(o.getPosition().x, o.getPosition().y, o.getWH().x, o.getWH().y);
                                        if (object_rect.contains(pos)) {
                                            planet = true;

                                            zoom_in = true;
                                            target_pos.set(o.getPosition()).add(o.getWH().x / 2f, o.getWH().y / 2f);

                                            clicked_planet = (PlanetUniverseObject) o;
                                            System.out.println("Planet index/seed: " + clicked_planet.getPlanetIndex());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if(!planet) {
                    //set camera position new target
                    zoom_in = false;
                    camera_target_pos.set(pos);
                }

                return true;
            }
        };
        GestureDetector gesture_detector = new GestureDetector(gesture_adapter);
        game.getInputEngine().addInputProcessor(gesture_detector);

        InputAdapter i = new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if(!isVisible())
                    return false;

                return false;
            }

            @Override
            public boolean scrolled(int amount) {
                if(!isVisible())
                    return false;

                game.getMainCamera().zoom += .1f * game.getMainCamera().zoom * amount;
                //game.getMainCamera().zoom = MathUtils.clamp(game.getMainCamera().zoom, target_zoom * 5, max_zoom);
                game.getMainCamera().update();

                zoom_in = false;

                System.out.println("(Universe) zoom: " + game.getMainCamera().zoom);

                return false;
            }
        };
        game.getInputEngine().addInputProcessor(i);
    }

    @Override
    public void tick(float delta) {
        if(zoom_in) {
            float mul = game.getMainCamera().zoom;

            game.getMainCamera().zoom = MathUtils.lerp(game.getMainCamera().zoom, target_zoom, delta * 3f);
            game.getMainCamera().position.lerp(new Vector3(target_pos, 0), delta * 5f * mul);

            if(game.getMainCamera().zoom <= target_zoom + .05f) {
                zoom_in = false;

                PlanetScreen planet_screen = game.getScreen(Screens.PLANET_SCREEN_NAME, PlanetScreen.class);
                planet_screen.createWorld(clicked_planet.getPlanetIndex());

                planet_screen.setVisible(true);
                setVisible(false);
            }

            game.getMainCamera().update();
        } else {
            //update main camera pos
            game.getMainCamera().position.lerp(new Vector3(camera_target_pos, 0), delta * 3.5f);
            game.getMainCamera().update();
        }
        universe.tick(delta);
    }

    @Override
    public void render(SpriteBatch batch) {
        batch.setProjectionMatrix(game.getMainCamera().combined);
        universe.render(batch);

        batch.setProjectionMatrix(game.getGUICamera().combined);
        font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), -620, 350);
    }

    @Override
    public void dispose() {

    }

    public Universe getUniverse() {
        return universe;
    }
}
