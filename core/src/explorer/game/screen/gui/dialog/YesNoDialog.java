package explorer.game.screen.gui.dialog;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.esotericsoftware.minlog.Log;

import explorer.game.framework.Game;
import explorer.game.screen.gui.GUIComponent;
import explorer.game.screen.gui.TextButton;
import explorer.game.screen.gui.TextureButton;

/**
 * Created by RYZEN on 06.01.2018.
 */

public class YesNoDialog extends DialogHandler.Dialog {

    public interface YesNoDialogListener {
        void yesOption();
        void noOption();
    }

    private NinePatch background;

    private static final float BORDER_SIZE = 10;
    private static final float MAX_TEXT_WIDTH = 400;
    private static final float BUTTONS_PLACE_HEIGHT = 60;

    //if text height is above scrolling is enabled
    private static final float MAX_TEXT_HEIGHT = 200;
    private static final int FONT_SIZE = 15;

    private Vector2 OFFSET;

    private Vector2 text_wh;

    private BitmapFont font;
    private String message;

    private TextButton yes_button, no_button;

    private InputAdapter blocking_adapter;
    private YesNoDialogListener listener;

    public YesNoDialog(String message, Viewport component_game_viewport, Game game) {
        super(component_game_viewport, game);

        this.message = message;

        font = game.getAssetsManager().getFont("fonts/pixel_font.ttf", FONT_SIZE);

        GlyphLayout layout = new GlyphLayout();
        layout.setText(font, message, Color.WHITE, MAX_TEXT_WIDTH, 10, true);
        text_wh = new Vector2().set(layout.width, layout.height);

        OFFSET = new Vector2(-MAX_TEXT_WIDTH / 2f, 0);

        Vector2 yes_size = getTextSize(font, "Yes");
        Vector2 no_size = getTextSize(font, "No");
        yes_button = new TextButton(font, "Yes", new Vector2((MAX_TEXT_WIDTH * 1/4f) - (yes_size.x / 2f), (BUTTONS_PLACE_HEIGHT / 2f) - (yes_size.y / 2f)).add(OFFSET), component_game_viewport, game);
        no_button = new TextButton(font, "No", new Vector2((MAX_TEXT_WIDTH * 3/4f) - (no_size.x / 2f), (BUTTONS_PLACE_HEIGHT / 2f) - (no_size.y / 2f)).add(OFFSET), component_game_viewport, game);

        background = new NinePatch(game.getAssetsManager().getTextureRegion("gui/dialog_background"), 5, 5, 5, 5);

        yes_button.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched(GUIComponent instance) {
                if(listener != null)
                    listener.yesOption();

                destroySelf();
            }

            @Override
            public void released(GUIComponent instance) {}
        });

        no_button.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched(GUIComponent instance) {
                if(listener != null)
                    listener.noOption();

                destroySelf();
            }

            @Override
            public void released(GUIComponent instance) {}
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

    private Vector2 getTextSize(BitmapFont font, String text) {
        GlyphLayout layout = new GlyphLayout();
        layout.setText(font, text);
        return new Vector2(layout.width, layout.height);
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


    private static final float scale = 5f;

    @Override
    public void render(SpriteBatch batch) {
        if(!isVisible())
            return;

        batch.setColor(Color.WHITE);
        batch.setProjectionMatrix(component_game_viewport.getCamera().combined);

        background.draw(batch, OFFSET.x - BORDER_SIZE, OFFSET.y - BORDER_SIZE, 0, 0, (MAX_TEXT_WIDTH + BORDER_SIZE) / scale, (MAX_TEXT_HEIGHT + BUTTONS_PLACE_HEIGHT + BORDER_SIZE) / scale, scale, scale,0);

        font.draw(batch, message, OFFSET.x + (BORDER_SIZE * 1.5f), OFFSET.y + (MAX_TEXT_HEIGHT + BUTTONS_PLACE_HEIGHT - (FONT_SIZE * 2f)), MAX_TEXT_WIDTH - (BORDER_SIZE * 4f), 10, true);

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
