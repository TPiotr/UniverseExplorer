package explorer.world.planet.planet_type;

import com.badlogic.gdx.graphics.Color;

import java.util.HashMap;

import explorer.game.framework.Game;

/**
 * Created by RYZEN on 13.10.2017.
 */

public class BlocksProperties {

    public static class BlockProperties {

        /**
         * Path to texture region that contains all possible colors of block (texture is x width by 1 height)
         */
        public String COLOR_PACK_REGION_NAME;

        public BlockProperties(String color_pack_region_name) {
            this.COLOR_PACK_REGION_NAME = color_pack_region_name;
        }

    }

    /**
     * Store properties in hashmap and use class as key
     */
    protected HashMap<Class<?>, BlockProperties> blocks_properties;

    public BlocksProperties() {
        blocks_properties = new HashMap<Class<?>, BlockProperties>();
    }

    /**
     * Get block properties by given block class
     * @param block_class class of block
     * @return block properties if there are any for this type of block
     */
    public BlockProperties getBlockProperties(Class<?> block_class) {
        return blocks_properties.get(block_class);
    }
}
