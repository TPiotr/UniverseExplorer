package explorer.game.screen.gui;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

import explorer.game.framework.Game;
import explorer.game.screen.ScreenComponent;

/**
 * All guis components parent class
 * Created by RYZEN on 04.11.2017.
 */

public abstract class GUIComponent implements ScreenComponent {

    /**
     * Game instance
     */
    protected Game game;

    /**
     * Viewport used to render this component instance
     */
    protected Viewport component_game_viewport;

    /**
     * Vectors for storing position and wh
     */
    protected Vector2 position, wh;

    /**
     * Visibility flag
     */
    protected boolean visible = true;

    public GUIComponent(Viewport component_game_viewport, Game game) {
        this.game = game;
        this.component_game_viewport = component_game_viewport;
    }

    /**
     * Getter for posiiton
     * @return get component position
     */
    public Vector2 getPosition() {
        return position;
    }

    /**
     * Getter for wh
     * @return get vector with width(x) and height(y)
     */
    public Vector2 getWH() {
        return wh;
    }

    /**
     * Set visibility flag of component
     * @param visible
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Return visibility flag
     * @return component visibility flag
     */
    public boolean isVisible() {
        return visible;
    }
}
