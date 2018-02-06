package explorer.world.block;

import com.badlogic.gdx.utils.ObjectMap;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.block.blocks.AirBlock;
import explorer.world.block.blocks.GrassBlock;
import explorer.world.block.blocks.GrassPlantBlock;
import explorer.world.block.blocks.StoneBlock;
import explorer.world.block.blocks.WoodenPlanksBlock;

/**
 * Class that stores all blocks instances
 * Created by RYZEN on 07.10.2017.
 */

public class Blocks {

    private ConcurrentHashMap<Integer, Block> all_blocks_instances;

    public explorer.world.block.blocks.AirBlock AIR;
    public explorer.world.block.blocks.DirtBlock DIRT;
    public GrassBlock GRASS;
    public StoneBlock STONE;

    public WoodenPlanksBlock WOODEN_PLANKS_BLOCK;

    public GrassPlantBlock GRASS_PLANT_BLOCK;

    public Blocks(World world, Game game) {
        all_blocks_instances = new ConcurrentHashMap<Integer, Block>();

        //TODO always add all blocks over here!
        AIR = new AirBlock(game);
        all_blocks_instances.put(AIR.getBlockID(), AIR);

        DIRT = new explorer.world.block.blocks.DirtBlock(world, game);
        all_blocks_instances.put(DIRT.getBlockID(), DIRT);

        GRASS = new GrassBlock(world, this, game);
        all_blocks_instances.put(GRASS.getBlockID(), GRASS);

        STONE = new StoneBlock(world, game);
        all_blocks_instances.put(STONE.getBlockID(), STONE);

        WOODEN_PLANKS_BLOCK = new WoodenPlanksBlock(world, game);
        all_blocks_instances.put(WOODEN_PLANKS_BLOCK.getBlockID(), WOODEN_PLANKS_BLOCK);

        GRASS_PLANT_BLOCK = new GrassPlantBlock(this, game);
        all_blocks_instances.put(GRASS_PLANT_BLOCK.getBlockID(), GRASS_PLANT_BLOCK);
    }

    /**
     * Get block instance by ID, returns AIR if block wasn't found by ID
     * @param ID  block ID
     * @return block instance
     */
    public synchronized Block getBlock(int ID) {
        Block out = all_blocks_instances.get(ID);
        if(out == null) {
            out = AIR;
            System.err.println("(Blocks) There is no block with ID: " + ID);
        }

        return out;
    }

    public ConcurrentHashMap<Integer, Block> getAllBlocksMap() {
        return all_blocks_instances;
    }
}
