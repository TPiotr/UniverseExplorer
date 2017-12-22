package explorer.world.block;

import com.badlogic.gdx.utils.ObjectMap;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.block.blocks.AirBlock;
import explorer.world.block.blocks.GrassBlock;

/**
 * Class that stores all blocks instances
 * Created by RYZEN on 07.10.2017.
 */

public class Blocks {

    private ConcurrentHashMap<Integer, Block> all_blocks_instances;

    public explorer.world.block.blocks.AirBlock AIR;
    public explorer.world.block.blocks.DirtBlock DIRT;
    public GrassBlock GRASS;

    public Blocks(World world, Game game) {
        all_blocks_instances = new ConcurrentHashMap<Integer, Block>();

        //TODO always add all blocks over here!
        AIR = new AirBlock(game);
        all_blocks_instances.put(AIR.getBlockID(), AIR);

        DIRT = new explorer.world.block.blocks.DirtBlock(world, game);
        all_blocks_instances.put(DIRT.getBlockID(), DIRT);

        GRASS = new GrassBlock(world, this, game);
        all_blocks_instances.put(GRASS.getBlockID(), GRASS);
    }

    /**
     * Get block instance by ID, returns AIR if block wasnt found by ID
     * @param ID  block ID
     * @return block instance
     */
    public synchronized Block getBlock(int ID) {
        Block out = all_blocks_instances.get(ID);
        return (out == null) ? AIR : out;
    }

    public ConcurrentHashMap<Integer, Block> getAllBlocksHashmap() {
        return all_blocks_instances;
    }
}
