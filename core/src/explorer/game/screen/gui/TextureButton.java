package explorer.game.screen.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

import explorer.game.framework.Game;
import explorer.game.screen.ScreenComponent;

/**
 * Created by RYZEN on 04.11.2017.
 */

public class TextureButton extends ClickableGUIComponent {

    /**
     * Button events listener
     */
    public interface ButtonListener {
        /**
         * Fired when button is pressed
         */
        void touched(GUIComponent instance);

        /**
         * Fires when button is released
         */
        void released(GUIComponent instance);
    }

    /**
     * Button texture
     */
    private TextureRegion texture;

    /**
     * Instance of button listener, could be null
     */
    private ButtonListener button_listener;

    /**
     * Rotation of this texture button
     */
    private float rotation;

    /**
     * Color of this button
     */
    private Color color;

    /**
     * Make new texture button instance
     * @param position position of component
     * @param wh width&height vector of component
     * @param texture texture region used in rendering
     * @param component_game_viewport viewport with which component will be rendered
     * @param game game instance
     */
    public TextureButton(Vector2 position, Vector2 wh, TextureRegion texture, Viewport component_game_viewport, Game game) {
        super(component_game_viewport, game);

        this.position = position;
        this.wh = wh;

        this.texture = texture;

        this.color = new Color(Color.WHITE);

        //create input adapter
        createClickableInputAdapter(true);
    }

    /**
     * Make new texture button instance with width&height vector as width&height taken from texture instance
     * @param position position of component
     * @param texture texture region used in rendering
     * @param component_game_viewport viewport with which component will be rendered
     * @param game game instance
     */
    public TextureButton(Vector2 position, TextureRegion texture, Viewport component_game_viewport, Game game) {
        super(component_game_viewport, game);

        this.position = position;
        this.wh = new Vector2(texture.getRegionWidth(), texture.getRegionHeight());

        this.texture = texture;

        //create input adapter
        createClickableInputAdapter(true);
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

    @Override
    public void tick(float delta) {}

    @Override
    public void render(SpriteBatch batch) {
        if(!isVisible())
            return;

        batch.setColor(getColor());
        batch.draw(getTextureRegion(), getPosition().x, getPosition().y, getWH().x * .5f, getWH().y * .5f, getWH().x, getWH().y, 1, 1, getRotation());
        batch.setColor(Color.WHITE);
    }

    /**
     * Get button texture region
     * @return instance of button texture region
     */
    public TextureRegion getTextureRegion() {
        return texture;
    }

    /**
     * Set button listener
     * @param listener new listener instance
     */
    public void setButtonListener(ButtonListener listener) {
        this.button_listener = listener;
    }

    /**
     * Getter for button listener instance
     * @return button listener instance
     */
    public ButtonListener getButtonListener() {
        return button_listener;
    }

    /**
     * Getter for color instance
     * @return color instance
     */
    public Color getColor() {
        return color;
    }

    /**
     * Getter for button rotation
     * @return button rotation in degree
     */
    public float getRotation() {
        return rotation;
    }

    /**
     * Setter for button rotation
     * @param rotation new button rotation in degree
     */
    public void setRotation(float rotation) {
        this.rotation = rotation;
    }
}
