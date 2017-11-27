package explorer.game.framework.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Matrix4;

/**
 * Simply modificated sprite batch that counts all render calls until reset() method is called in main rendering loop in Game class
 * With that we can calculate total count of render calls per 1 frame
 * Created by RYZEN on 13.10.2017.
 */

public class RenderCallsCounterSpriteBatch extends SpriteBatch implements Batch {

    /**
     * render calls counter
     */
    private int render_calls;

    /**
     * render method calls counter
     */
    private int render_method_calls;

    public void end() {
        render_calls += renderCalls;
        super.end();
    }

    @Override
    public void draw(Texture texture, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
        render_method_calls++;
        super.draw(texture, x, y, originX, originY, width, height, scaleX, scaleY, rotation, srcX, srcY, srcWidth, srcHeight, flipX, flipY);
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
        render_method_calls++;
        super.draw(texture, x, y, width, height, srcX, srcY, srcWidth, srcHeight, flipX, flipY);
    }

    @Override
    public void draw(Texture texture, float x, float y, int srcX, int srcY, int srcWidth, int srcHeight) {
        render_method_calls++;
        super.draw(texture, x, y, srcX, srcY, srcWidth, srcHeight);
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2) {
        render_method_calls++;
        super.draw(texture, x, y, width, height, u, v, u2, v2);
    }

    @Override
    public void draw(Texture texture, float x, float y) {
        render_method_calls++;
        super.draw(texture, x, y);
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height) {
        render_method_calls++;
        super.draw(texture, x, y, width, height);
    }

    @Override
    public void draw(Texture texture, float[] spriteVertices, int offset, int count) {
        render_method_calls++;
        super.draw(texture, spriteVertices, offset, count);
    }

    @Override
    public void draw(TextureRegion region, float x, float y) {
        render_method_calls++;
        super.draw(region, x, y);
    }

    @Override
    public void draw(TextureRegion region, float x, float y, float width, float height) {
        render_method_calls++;
        super.draw(region, x, y, width, height);
    }

    @Override
    public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
        render_method_calls++;
        super.draw(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation);
    }

    @Override
    public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation, boolean clockwise) {
        render_method_calls++;
        super.draw(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation, clockwise);
    }

    @Override
    public void draw(TextureRegion region, float width, float height, Affine2 transform) {
        render_method_calls++;
        super.draw(region, width, height, transform);
    }

    /**
     * Set counter to 0 call after whole frame is rendered
     */
    public void resetCounter() {
        render_method_calls = 0;
        render_calls = 0;
    }

    /**
     * Amount of render calls until last resetCounter() was called
     * @return
     */
    public int getRenderCalls() {
        return render_calls;
    }

    /**
     * Amount of render method calls until last resetCounter() method called
     * So every batch.draw adds 1 to this value
     * @return
     */
    public int getRenderMethodCalls() {
        return render_method_calls;
    }
}
