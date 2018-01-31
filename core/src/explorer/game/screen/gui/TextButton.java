package explorer.game.screen.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

import explorer.game.framework.Game;

/**
 * Created by RYZEN on 05.11.2017.
 */

public class TextButton extends ClickableGUIComponent {

    /**
     * Font used to render text
     */
    private BitmapFont font;

    /**
     * Button text
     */
    private String text;

    /**
     * Color of button font
     */
    private Color font_color;

    /**
     * Button events listener
     */
    private TextureButton.ButtonListener button_listener;

    /**
     * Construct new text button
     * @param font font used to render text instance
     * @param text button text
     * @param position position of component
     * @param component_game_viewport viewport where button will be rendered
     * @param game game instance
     */
    public TextButton(BitmapFont font, String text, Vector2 position, Viewport component_game_viewport, Game game) {
        super(component_game_viewport, game);

        this.position = position;
        this.wh = new Vector2();

        this.font = font;
        this.text = text;

        this.font_color = new Color(Color.WHITE);

        calculateWH();
        createClickableInputAdapter(true);
    }

    private void calculateWH() {
        GlyphLayout layout = new GlyphLayout();
        layout.setText(font, text);
        wh.set(layout.width, layout.height);
    }

    @Override
    public void tick(float delta) {

    }

    @Override
    public void render(SpriteBatch batch) {
        if(!isVisible())
            return;

        batch.setColor(font_color);
        font.draw(batch, text, getPosition().x, getPosition().y + getWH().y);
        batch.setColor(Color.WHITE);
    }

    @Override
    public void touched(GUIComponent instance) {
        if(button_listener != null)
            button_listener.touched(instance);
    }

    @Override
    public void released(GUIComponent instance) {
        if(button_listener != null)
            button_listener.released(instance);
    }

    /**
     * Get font instance used to render text
     * @return button font instace
     */
    public BitmapFont getFont() {
        return font;
    }

    /**
     * Set button text
     * @param text new button text
     */
    public void setText(String text) {
        this.text = text;

        //we have to calculate new wh vector
        calculateWH();
    }

    /**
     * Get button text
     * @return button text
     */
    public String getText() {
        return text;
    }

    /**
     * Instance of font rendering color
     * @return font color
     */
    public Color getTextColor() {
        return font_color;
    }

    /**
     * Set button listener
     * @param listener new listener instance
     */
    public void setButtonListener(TextureButton.ButtonListener listener) {
        this.button_listener = listener;
    }

    /**
     * Getter for button listener instance
     * @return button listener instance
     */
    public TextureButton.ButtonListener getButtonListener() {
        return button_listener;
    }
}
