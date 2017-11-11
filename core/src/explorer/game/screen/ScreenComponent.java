package explorer.game.screen;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Created by RYZEN on 26.10.2017.
 */

public interface ScreenComponent {

    /**
     * Update method for screen component
     * @param delta delta time
     */
    public void tick(float delta);

    /**
     * Render method for screen component
     * @param batch sprite batch instance
     */
    public void render(SpriteBatch batch);

}
