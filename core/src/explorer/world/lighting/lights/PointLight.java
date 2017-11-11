package explorer.world.lighting.lights;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import explorer.game.framework.Game;

/**
 * Created by RYZEN on 07.10.2017.
 */

public class PointLight extends Light {

    /**
     * Radius of point light
     */
    private float radius;

    /**
     * Create new point light instance
     * @param position pos of light
     * @param color color of light
     * @param radius radius of light
     * @param game game instance
     */
    public PointLight(Vector2 position, Vector3 color, float radius, Game game) {
        super(position, game);

        this.color = color;
        this.radius = radius;

        this.bounds.set(0, 0, radius * 2, radius * 2);

        this.alpha_mask = new TextureRegion(game.getAssetsManager().getTextureRegion("blocks/light"));
    }

    /**
     * Getter for light radius
     * @return light radius
     */
    public float getRadius() {
        return radius;
    }

    /**
     * Set radius of this light
     * @param radius new light radius
     */
    public void setRadius(float radius) {
        this.radius = radius;
    }

    public Rectangle getLightBounds() {
        this.bounds.set(getLightPosition().x - radius, getLightPosition().y - radius, radius * 2, radius * 2);
        return bounds;
    }
}
