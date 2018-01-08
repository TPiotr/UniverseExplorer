package explorer.world.planet.planet_type.types;

import com.badlogic.gdx.graphics.Color;

import explorer.game.framework.Game;
import explorer.world.block.blocks.DirtBlock;
import explorer.world.block.blocks.GrassBlock;
import explorer.world.block.blocks.StoneBlock;
import explorer.world.block.blocks.WoodenPlanksBlock;
import explorer.world.planet.planet_type.BlocksProperties;

/**
 * Created by RYZEN on 13.10.2017.
 */

public class TestBlocksProperties extends BlocksProperties {


    public TestBlocksProperties() {

        blocks_properties.put(DirtBlock.class, new BlockProperties("blocks/dirt_colorpack"));
        blocks_properties.put(GrassBlock.class, new BlockProperties("blocks/grass_colorpack"));
        blocks_properties.put(StoneBlock.class, new BlockProperties("blocks/stone_colorpack"));
        blocks_properties.put(WoodenPlanksBlock.class, new BlockProperties("blocks/wooden_planks_colorpack"));
    }

}
