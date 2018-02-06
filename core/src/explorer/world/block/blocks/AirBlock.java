package explorer.world.block.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.HashMap;

import explorer.game.framework.Game;
import explorer.world.block.Block;

/**
 * Created by RYZEN on 07.10.2017.
 */

public class AirBlock extends Block {

    /**
     * @param game game instance for assets loading
     */
    public AirBlock(Game game) {
        super(game);

        this.block_id = 1;

        this.block_group = BlockGroup.CONNECT_WITH_NOTHING;
        this.tile_positions = new HashMap<Short, TextureRegion>();
        this.blocks_ground_light = false;
        this.can_place_other_block_on = true;

        this.collidable = false;
    }
}
