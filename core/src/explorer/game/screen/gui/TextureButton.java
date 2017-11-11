package explorer.game.screen.gui;

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
        void touched();

        /**
         * Fires when button is released
         */
        void released();
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
    public void touched() {
        if(button_listener != null)
            button_listener.touched();
    }

    @Override
    public void released() {
        if(button_listener != null)
            button_listener.released();
    }

    @Override
    public void tick(float delta) {}

    @Override
    public void render(SpriteBatch batch) {
        if(!isVisible())
            return;

        batch.draw(getTextureRegion(), getPosition().x, getPosition().y, getWH().x, getWH().y);
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
}
