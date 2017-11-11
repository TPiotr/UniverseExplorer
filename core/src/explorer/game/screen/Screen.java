package explorer.game.screen;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;

import explorer.game.framework.Game;

/**
 * Basic screen class for gui framework
 * Created by RYZEN on 26.10.2017.
 */

public abstract class Screen {

    /**
     * Screen name used to get instance of screen from Game class
     */
    public String NAME = "NO_NAME";

    /**
     * Game class instance
     */
    protected Game game;

    /**
     * Array that contains all screen components
     */
    protected Array<ScreenComponent> screen_components;

    /**
     * Visibility flag
     */
    protected boolean visible = true;

    /**
     * Construct new screen instance
     * @param game game instance
     */
    public Screen(Game game) {
        this.game = game;
        screen_components = new Array<ScreenComponent>();
    }

    /**
     * Method that ticks all screen component, have to be called manually
     * @param delta delta time
     */
    protected void tickComponents(float delta) {
        for(int i = 0; i < screen_components.size; i++) {
            getScreenComponents().get(i).tick(delta);
        }
    }

    /**
     * Render screen components, have to call it manually
     * @param batch sprite batch instance
     */
    protected void renderComponents(SpriteBatch batch) {
        for(int i = 0; i < screen_components.size; i++) {
            getScreenComponents().get(i).render(batch);
        }
    }

    /**
     * Screen update method
     * @param delta delta time
     */
    public abstract void tick(float delta);

    /**
     * Screen render method
     * @param batch sprite batch instance, change it's matrices to proper one for screen
     */
    public abstract void render(SpriteBatch batch);

    /**
     * Dispose screen method, called on app close
     */
    public abstract void dispose();

    /**
     * Event called when game screen size is changed
     * @param new_w new screen width
     * @param new_h new screen height
     */
    public void screenSizeChanged(int new_w, int new_h) {}

    /**
     * Add new component to the screen
     * @param screen_component screen comp that will be added to screen
     */
    public synchronized void addScreenComponent(ScreenComponent screen_component) {
        screen_components.add(screen_component);
    }

    /**
     * Remove component from the screen
     * @param screen_component screen comp. that will be removed from this screen
     */
    public synchronized void removeScreenComponent(ScreenComponent screen_component) {
        screen_components.removeValue(screen_component, true);
    }

    /**
     * Get instance of array that contains all components of this screen
     * @return array with all screen components
     */
    public Array<ScreenComponent> getScreenComponents() {
        return screen_components;
    }

    /**
     * Set visibility of this screen
     * @param visible new visible flag
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * True if screen is visible
     * @return visibility flag
     */
    public boolean isVisible() {
        return visible;
    }
}
