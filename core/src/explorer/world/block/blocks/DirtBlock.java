package explorer.world.block.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.HashMap;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.block.Block;
import explorer.world.block.ColorPack;
import explorer.world.block.PlanetBoundBlock;
import explorer.world.planet.planet_type.BlocksProperties;

/**
 * Created by RYZEN on 07.10.2017.
 */

public class DirtBlock extends PlanetBoundBlock {

    private ColorPack color_pack;

    /**
     * @param game game instance for assets loading
     */
    public DirtBlock(World world, Game game) {
        super(world, game);

        this.block_id = 2;
        this.block_group = BlockGroup.CONNECT_WITH_EVERYTHING;

        this.need_background_block_rendered_if_not_fully_surrounded = true;

        this.tile_positions = new HashMap<Short, TextureRegion>();

        //get dirt color from its color pack texture
        color_pack = new ColorPack();

        String colorpack_region_name = world.getPlanetProperties().PLANET_TYPE.BLOCKS_PROPERTIES.getBlockProperties(DirtBlock.class).COLOR_PACK_REGION_NAME;
        block_color.set(color_pack.load(world.getPlanetProperties().PLANET_FACTOR, game.getAssetsManager().getTextureRegion(colorpack_region_name)));

        //load old_assets.textures
        final int BLOCK_PIXEL_SIZE = 16;
        TextureRegion[][] textures = game.getAssetsManager().getTextureRegion("blocks/dirt_spritesheet").split(BLOCK_PIXEL_SIZE, BLOCK_PIXEL_SIZE);

       tile_positions.put(Block.COLLIDE_NONE, textures[0][0]);
       tile_positions.put(Block.COLLIDE_ALL_SIDES, textures[1][2]);

       tile_positions.put(Block.COLLIDE_LEFT, textures[3][1]);
       tile_positions.put(Block.COLLIDE_LEFT_DOWN, textures[0][3]);
       tile_positions.put(Block.COLLIDE_LEFT_DOWN_RIGHT, textures[0][2]);
       tile_positions.put(Block.COLLIDE_LEFT_RIGHT, textures[3][3]); //
       tile_positions.put(Block.COLLIDE_LEFT_UP, textures[2][3]);
       tile_positions.put(Block.COLLIDE_LEFT_UP_DOWN, textures[1][3]);
       tile_positions.put(Block.COLLIDE_LEFT_UP_RIGHT, textures[2][2]);
       tile_positions.put(Block.COLLIDE_DOWN, textures[1][0]);
       tile_positions.put(Block.COLLIDE_RIGHT, textures[3][0]);
       tile_positions.put(Block.COLLIDE_RIGHT_DOWN, textures[0][1]);
       tile_positions.put(Block.COLLIDE_UP, textures[2][0]);
       tile_positions.put(Block.COLLIDE_UP_DOWN, textures[3][2]); //
       tile_positions.put(Block.COLLIDE_UP_RIGHT, textures[2][1]);
       tile_positions.put(Block.COLLIDE_UP_RIGHT_DOWN, textures[1][1]);
    }
}
