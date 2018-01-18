package explorer.world.lighting;

import com.badlogic.gdx.graphics.Color;

/**
 * Class that tells light engine to render WorldObject that implements that interface to render its light mask as silhouette of this object with solid color
 * Created by RYZEN on 13.01.2018.
 */

public interface SolidColorWorldObjectLight {

    /**
     * Color of light mask which will be rendered to light map, shape of light mask is just object texture with one solid color
     * @return instance of color of light mask
     */
    public Color getSolidColorOfLightMask();

}
