package explorer.world.planet.planet_type.types;

import com.badlogic.gdx.graphics.Color;

import explorer.world.block.blocks.DirtBlock;
import explorer.world.block.blocks.GrassBlock;
import explorer.world.planet.planet_type.BlocksProperties;

/**
 * Created by RYZEN on 13.10.2017.
 */

public class TestBlocksProperties extends BlocksProperties {


    public TestBlocksProperties() {
        blocks_properties.put(DirtBlock.class, new BlockProperties(new Color(137f / 255f, 82f / 255f, 48f / 255f, 1f).mul(1f), new Color(86f / 255f, 60f / 255f, 42f / 255f, 1f).mul(1f)));
        blocks_properties.put(GrassBlock.class, new BlockProperties(new Color(132f / 255f, 224f / 255f, 255f / 255f, 1f), new Color(78f / 255f, 0f / 255f, 25f / 255f, 1f)));
    }

}
