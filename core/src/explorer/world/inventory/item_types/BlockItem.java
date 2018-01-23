package explorer.world.inventory.item_types;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.block.Block;
import explorer.world.inventory.Item;

/**
 * Created by RYZEN on 21.01.2018.
 */

public class BlockItem extends Item {

    private static AtomicInteger id_assigner = new AtomicInteger(1000);
    private static HashMap<Integer, Integer> id_per_block_type = new HashMap<Integer, Integer>();

    private int representing_block_id;

    public BlockItem(Game game, int block_id, World world) {
        super(game);

        //assign proper item id
        if(!id_per_block_type.containsKey(block_id)) {
            int id = id_assigner.getAndIncrement();
            id_per_block_type.put(block_id, id);

            ID = id;
        } else {
            ID = id_per_block_type.get(block_id);
        }

        representing_block_id = block_id;

        //get texture
        item_icon = world.getBlocks().getBlock(block_id).getTextureRegion(Block.COLLIDE_NONE);
        item_on_ground_texture = item_icon;

        stackable = true;
        max_in_stack = 64;
    }

    public int getRepresentingBlockID() {
        return representing_block_id;
    }
}
