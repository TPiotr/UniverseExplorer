package explorer.game.framework.utils;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Simply modificated sprite batch that counts all render calls until reset() method is called in main rendering loop in Game class
 * With that we can calculate total count of render calls per 1 frame
 * Created by RYZEN on 13.10.2017.
 */

public class RenderCallsCounterSpriteBatch extends SpriteBatch {

    /**
     * render calls counter
     */
    private int render_calls;

    public void end() {
        render_calls += renderCalls;
        super.end();
    }

    /**
     * Set counter to 0 call after whole frame is rendered
     */
    public void resetCounter() {
        render_calls = 0;
    }

    /**
     * Amount of render calls until last resetCounter() was called
     * @return
     */
    public int getRenderCalls() {
        return render_calls;
    }
}
