package explorer.world.block.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.HashMap;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.block.Block;
import explorer.world.block.PlanetBoundBlock;
import explorer.world.planet.planet_type.BlocksProperties;

/**
 * Created by RYZEN on 07.10.2017.
 */

public class DirtBlock extends PlanetBoundBlock {

    /**
     * @param game game instance for assets loading
     */
    public DirtBlock(World world, Game game) {
        super(world, game);

        this.block_id = 2;
        this.block_group = BlockGroup.CONNECT_WITH_SAME_BLOCK;

        this.tile_positions = new HashMap<Short, TextureRegion>();

        //generate dirt color by planet seed
        float alpha = world.getPlanetProperties().random.nextFloat();

        BlocksProperties.BlockProperties properties = world.getPlanetProperties().PLANET_TYPE.BLOCKS_PROPERTIES.getBlockProperties(getClass());
        block_color.set(properties.MIN_COLOR).lerp(properties.MAX_COLOR, alpha);

        //load textures
        TextureRegion[][] textures = game.getAssetsManager().getTextureRegion("blocks/stone").split(64, 64);

       tile_positions.put(Block.COLLIDE_NONE, textures[0][0]);
       tile_positions.put(Block.COLLIDE_ALL_SIDES, textures[1][2]);

       tile_positions.put(Block.COLLIDE_LEFT, textures[3][1]);
       tile_positions.put(Block.COLLIDE_LEFT_DOWN, textures[0][3]);
       tile_positions.put(Block.COLLIDE_LEFT_DOWN_RIGHT, textures[0][2]);
       tile_positions.put(Block.COLLIDE_LEFT_RIGHT, textures[3][2]);
       tile_positions.put(Block.COLLIDE_LEFT_UP, textures[2][3]);
       tile_positions.put(Block.COLLIDE_LEFT_UP_DOWN, textures[1][3]);
       tile_positions.put(Block.COLLIDE_LEFT_UP_RIGHT, textures[2][2]);
       tile_positions.put(Block.COLLIDE_DOWN, textures[1][0]);
       tile_positions.put(Block.COLLIDE_RIGHT, textures[3][0]);
       tile_positions.put(Block.COLLIDE_RIGHT_DOWN, textures[0][1]);
       tile_positions.put(Block.COLLIDE_UP, textures[2][0]);
       tile_positions.put(Block.COLLIDE_UP_DOWN, textures[3][3]);
       tile_positions.put(Block.COLLIDE_UP_RIGHT, textures[2][1]);
       tile_positions.put(Block.COLLIDE_UP_RIGHT_DOWN, textures[1][1]);
    }
}
