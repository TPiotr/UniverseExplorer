package explorer.world.lighting.lights;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import explorer.game.framework.Game;

/**
 * Created by RYZEN on 07.10.2017.
 */

public class Light {

    /**
     * Alpha mask of this light
     */
    protected TextureRegion alpha_mask;
    /**
     * Color of this light
     */
    protected Vector3 color;

    /**
     * Position of this light
     */
    protected Vector2 position;
    /**
     * Bounds of this light (in global coords so bounds.xy = position.xy)
     */
    protected Rectangle bounds;

    /**
     * Light visiblity flag
     */
    protected boolean visible;

    /**
     * Basic constructor
     * @param position pos of light
     * @param game game instance
     */
    public Light(Vector2 position, Game game) {
        this.position = position;
        bounds = new Rectangle();
        visible = true;
    }

    /**
     * Getter for light alpha mask texture
     * @return alpha mask of this light
     */
    public TextureRegion getAlphaMask() {
        return alpha_mask;
    }

    /**
     * Getter for light position
     * @return get position of this light
     */
    public Vector2 getLightPosition() {
        return position;
    }

    /**
     * Getter for this light color
     * @return get color of this light
     */
    public Vector3 getLightColor() {
        return color;
    }

    /**
     * Getter for bounding rect for this light
     * @return light bounding rectangle (in global coords so bounds.xy = position.xy)
     */
    public Rectangle getLightBounds() {
        return bounds;
    }

    /**
     * Getter for light visiblity flag
     * @return true if light is visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Set visiblity of light
     * @param visible new visiblity flag
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
