package explorer.world.planet.planet_type;

import com.badlogic.gdx.graphics.Color;

import java.util.HashMap;

/**
 * Created by RYZEN on 13.10.2017.
 */

public class BlocksProperties {

    public static class BlockProperties {

        /**
         * Max color of block
         */
        public Color MAX_COLOR;

        /**
         * Min color of block
         */
        public Color MIN_COLOR;

        public BlockProperties(Color max_color, Color min_color) {
            this.MAX_COLOR = max_color;
            this.MIN_COLOR = min_color;
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
