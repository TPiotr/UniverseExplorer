package explorer.game.screen.screens;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;
import explorer.game.screen.Screen;
import explorer.game.screen.gui.TextureButton;
import explorer.world.World;

/**
 * Created by RYZEN on 05.11.2017.
 */

public class PlanetGUIScreen extends Screen {

    private TextureButton left_button, right_button, jump_button;

    /**
     * Construct new screen instance
     *
     * @param game game instance
     */
    public PlanetGUIScreen(final World world, Game game) {
        super(game);

        NAME = Screens.PLANET_GUI_SCREEN_NAME;

        TextureRegion white_texture = game.getAssetsManager().getTextureRegion("white_texture");
        left_button = new TextureButton(new Vector2(-550, -300), new Vector2(128, 128), new TextureRegion(white_texture), game.getGUIViewport(), game);
        addScreenComponent(left_button);

        right_button = new TextureButton(new Vector2(-200, -300), new Vector2(128, 128), new TextureRegion(white_texture), game.getGUIViewport(), game);
        addScreenComponent(right_button);

        jump_button = new TextureButton(new Vector2(450, -300), new Vector2(128, 128), new TextureRegion(white_texture), game.getGUIViewport(), game);
        addScreenComponent(jump_button);

        //create listeners
        left_button.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched() {
                world.getPlayer().setLeft(true);
            }

            @Override
            public void released() {
                world.getPlayer().setLeft(false);
            }
        });

        right_button.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched() {
                world.getPlayer().setRight(true);
            }

            @Override
            public void released() {
                world.getPlayer().setRight(false);
            }
        });

        jump_button.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched() {
                world.getPlayer().setJump(true);
            }

            @Override
            public void released() {
                world.getPlayer().setJump(false);
            }
        });
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        jump_button.setVisible(visible);
        left_button.setVisible(visible);
        right_button.setVisible(visible);
    }

    @Override
    public void tick(float delta) {
        if(!isVisible())
            return;

        tickComponents(delta);
    }

    @Override
    public void render(SpriteBatch batch) {
        if(!isVisible())
            return;

        batch.setProjectionMatrix(game.getGUICamera().combined);
        renderComponents(batch);
    }

    @Override
    public void dispose() {

    }
}
