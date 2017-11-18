package explorer.game.screen.screens.universe;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import explorer.game.framework.Game;
import explorer.game.screen.Screen;
import explorer.game.screen.screens.Screens;
import explorer.universe.Universe;
import explorer.universe.chunk.UniverseChunk;
import explorer.universe.object.PlanetUniverseObject;
import explorer.universe.object.UniverseObject;

/**
 * Created by RYZEN on 14.11.2017.
 */

public class UniverseScreen extends Screen {

    private Universe universe;

    private boolean zoom_in;
    private float target_zoom = .1f;
    private Vector2 target_pos = new Vector2();

    /**
     * Construct new screen instance
     * @param game game instance
     */
    public UniverseScreen(final Game game) {
        super(game);

        NAME = Screens.UNIVERSE_SCREEN_NAME;

        universe = new Universe(game);

        InputAdapter i = new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if(!isVisible())
                    return false;

                Vector2 pos = new Vector2(screenX, screenY);
                game.getMainViewport().unproject(pos);

                boolean planet = false;

                //zoom to planet
                for(int i = 0; i < universe.getUniverseChunks().length; i++) {
                    for(int j = 0; j < universe.getUniverseChunks()[0].length; j++) {
                        UniverseChunk chunk = universe.getUniverseChunks()[i][j];
                        Rectangle chunk_rect = new Rectangle(chunk.getPosition().x, chunk.getPosition().y, chunk.getWH().x, chunk.getWH().y);

                        if(chunk_rect.contains(pos)) {
                            Rectangle object_rect = new Rectangle();
                            for(int k = 0; k < chunk.getObjects().size; k++) {
                                UniverseObject o = chunk.getObjects().get(k);

                                if(o instanceof PlanetUniverseObject) {
                                    object_rect.set(o.getPosition().x, o.getPosition().y, o.getWH().x, o.getWH().y);
                                    if(object_rect.contains(pos)) {
                                        planet = true;

                                        zoom_in = true;
                                        target_pos.set(o.getPosition()).add(o.getWH().x / 2f, o.getWH().y / 2f);
                                    }
                                }
                            }
                        }
                    }
                }

                if(!planet) {
                    game.getMainCamera().position.set(pos, 0);
                    game.getMainCamera().update();
                }

                return false;
            }

            @Override
            public boolean scrolled(int amount) {
                if(!isVisible())
                    return false;

                game.getMainCamera().zoom += .1f * game.getMainCamera().zoom * amount;
                game.getMainCamera().update();

                zoom_in = false;

                return false;
            }
        };
        game.getInputEngine().addInputProcessor(i);
    }

    @Override
    public void tick(float delta) {
        if(zoom_in) {
            float mul = game.getMainCamera().zoom;

            game.getMainCamera().zoom = MathUtils.lerp(game.getMainCamera().zoom, target_zoom, delta * .2f);
            game.getMainCamera().position.lerp(new Vector3(target_pos, 0), delta * .8f * mul);

            if(game.getMainCamera().zoom <= target_zoom + .05f) {
                zoom_in = false;
            }

            game.getMainCamera().update();
        }
        universe.tick(delta);
    }

    @Override
    public void render(SpriteBatch batch) {
        batch.setProjectionMatrix(game.getMainCamera().combined);
        universe.render(batch);
    }

    @Override
    public void dispose() {

    }

    public Universe getUniverse() {
        return universe;
    }
}
