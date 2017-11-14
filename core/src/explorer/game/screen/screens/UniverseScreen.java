package explorer.game.screen.screens;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;
import explorer.game.screen.Screen;
import explorer.universe.Universe;

/**
 * Created by RYZEN on 14.11.2017.
 */

public class UniverseScreen extends Screen {

    private Universe universe;

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

                game.getMainCamera().position.set(pos, 0);
                game.getMainCamera().update();

                return false;
            }

            @Override
            public boolean scrolled(int amount) {
                if(!isVisible())
                    return false;

                game.getMainCamera().zoom += .1f * game.getMainCamera().zoom * amount;
                game.getMainCamera().update();

                return false;
            }
        };
        game.getInputEngine().addInputProcessor(i);
    }

    @Override
    public void tick(float delta) {
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
}
