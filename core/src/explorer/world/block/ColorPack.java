package explorer.world.block;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import explorer.game.framework.Game;

/**
 * Class that holds all possible color for some type of object/block
 * Colors are loaded from one dimensional texture (texture is just x width and 1 in height)
 * Created by RYZEN on 21.12.2017.
 */

public class ColorPack {

    /**
     * Array that contains all colors loaded from texture
     */
    private Array<Color> colors;

    /**
     * Color which was calculated using factor variable passed in load function, this instance is only read only modyfing it will not take any effect
     */
    private Color calculated_color;

    /**
     * Constructs new class that works as holder for pixel colors in texture region, call load(float, TextureRegon) to make use of this instance
     */
    public ColorPack() {
        colors = new Array<Color>(true, 8);
    }

    /**
     * Load all pixels from one dimensional texture region passed as parameter
     * @param factor factor used to calculate final color (named calculated color), almost 100% probability this is PlanetType.PLANET_FACTOR
     * @param color_region color region from which colors were read
     * @return calculated color based on factor and colors in passed texture region
     */
    public Color load(float factor, TextureRegion color_region) {
        color_region.getTexture().getTextureData().prepare();

        //wait until texture is prepared to make pixmap and read data
        while(!color_region.getTexture().getTextureData().isPrepared()) {
            System.out.println("Waiting for texture to be prepared... (ColorPack class)");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Pixmap pixels_map = color_region.getTexture().getTextureData().consumePixmap();
        for(int i = 0; i < color_region.getRegionWidth(); i++) {
            Color color = new Color(pixels_map.getPixel(color_region.getRegionX() + i, color_region.getRegionY()));
            colors.add(color);
        }
        color_region.getTexture().getTextureData().disposePixmap();

        calculated_color = new Color(colors.get((int) (colors.size * factor)));

        return calculated_color;
    }

    /**
     * Get calculated color instance, read only
     * @return calculated color instance, read only
     */
    public Color getCalculatedColor() {
        return calculated_color;
    }
}
