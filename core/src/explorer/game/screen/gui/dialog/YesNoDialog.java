package explorer.game.screen.gui.dialog;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

import explorer.game.framework.Game;
import explorer.game.screen.gui.TextButton;
import explorer.game.screen.gui.TextureButton;

/**
 * Created by RYZEN on 06.01.2018.
 */

public class YesNoDialog extends DialogHandler.Dialog {

    public static interface YesNoDialogListener {
        void yesOption();
        void noOption();
    }

    private TextureRegion background;

    private final float text_width = 400;
    private float text_x_offset;

    private BitmapFont font;
    private String message;

    private TextButton yes_button, no_button;

    private InputAdapter blocking_adapter;

    private YesNoDialogListener listener;

    public YesNoDialog(String message, Viewport component_game_viewport, Game game) {
        super(component_game_viewport, game);

        this.message = message;

        font = game.getAssetsManager().getFont("fonts/pixel_font.ttf", 15);
        yes_button = new TextButton(font, "Yes", new Vector2(100, 0), component_game_viewport, game);
        no_button = new TextButton(font, "No", new Vector2(-100, 0), component_game_viewport, game);

        GlyphLayout layout = new GlyphLayout();
        layout.setText(font, message, Color.WHITE, text_width, 10, true);
        text_x_offset = -layout.width / 2f;

        background = game.getAssetsManager().getTextureRegion("white_texture");

        yes_button.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched() {
                if(listener != null)
                    listener.yesOption();

                destroySelf();
            }

            @Override
            public void released() {}
        });

        no_button.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched() {
                if(listener != null)
                    listener.noOption();

                destroySelf();
            }

            @Override
            public void released() {}
        });

        //create blocking adapter
        blocking_adapter = new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                return true;
            }
        };
        game.getInputEngine().addInputProcessor(blocking_adapter, 0);

        game.getInputEngine().remove(yes_button.getClickableInputAdapter());
        game.getInputEngine().addInputProcessor(yes_button.getClickableInputAdapter(), 0);

        game.getInputEngine().remove(no_button.getClickableInputAdapter());
        game.getInputEngine().addInputProcessor(no_button.getClickableInputAdapter(), 0);
    }

    public YesNoDialog setListener(YesNoDialogListener listener) {
        this.listener = listener;
        return this;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        yes_button.setVisible(visible);
        no_button.setVisible(visible);
    }

    @Override
    public void tick(float delta) {
        if(!isVisible())
            return;

        yes_button.tick(delta);
        no_button.tick(delta);
    }

    @Override
    public void render(SpriteBatch batch) {
        if(!isVisible())
            return;

        font.draw(batch, message, text_x_offset ,100, text_width, 10, true);

        yes_button.render(batch);
        no_button.render(batch);
    }

    @Override
    public void dispose() {
        game.getInputEngine().remove(blocking_adapter);
        game.getInputEngine().remove(yes_button.getClickableInputAdapter());
        game.getInputEngine().remove(no_button.getClickableInputAdapter());
    }
}
