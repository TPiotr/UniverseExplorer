package explorer.world.block.blocks;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.HashMap;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.block.Block;
import explorer.world.block.Blocks;
import explorer.world.block.CustomRenderingBlock;
import explorer.world.planet.planet_type.BlocksProperties;

/**
 * Created by RYZEN on 16.10.2017.
 */

public class GrassBlock extends CustomRenderingBlock {

    private Color dirt_block_color;

    private Color grass_color;
    private HashMap<Short, TextureRegion> grass_regions;

    /**
     * @param game game instance for assets loading
     */
    public GrassBlock(World world, Blocks blocks, Game game) {
        super(game);

        this.block_id = 3;
        this.block_group = BlockGroup.CONNECT_WITH_SAME_BLOCK;

        this.tile_positions = new HashMap<Short, TextureRegion>();

        //get dirt color
        dirt_block_color = new Color(blocks.DIRT.getBlockColor());

        //generate grass color
        float alpha = world.getPlanetProperties().random.nextFloat();
        System.out.println(alpha);
        BlocksProperties.BlockProperties properties = world.getPlanetProperties().PLANET_TYPE.BLOCKS_PROPERTIES.getBlockProperties(getClass());

        grass_color = new Color();
        grass_color.set(properties.MIN_COLOR).lerp(properties.MAX_COLOR, alpha);

        System.out.println("grass color R: "+grass_color.r + " G: "+grass_color.g + " B: "+grass_color.b);

        loadTextures();
    }

    private void loadTextures() {
        //load textures
        TextureRegion[][] textures = game.getAssetsManager().getTextureRegion("blocks/stone").split(256, 256);

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

        //
        grass_regions = new HashMap<Short, TextureRegion>();

        TextureRegion[][] grass_textures = game.getAssetsManager().getTextureRegion("blocks/grass1_mask").split(256, 256);

        grass_regions.put(Block.COLLIDE_NONE, grass_textures[0][0]);
        grass_regions.put(Block.COLLIDE_ALL_SIDES, grass_textures[1][2]);

        grass_regions.put(Block.COLLIDE_LEFT, grass_textures[3][1]);
        grass_regions.put(Block.COLLIDE_LEFT_DOWN, grass_textures[0][3]);
        grass_regions.put(Block.COLLIDE_LEFT_DOWN_RIGHT, grass_textures[0][2]);
        grass_regions.put(Block.COLLIDE_LEFT_RIGHT, grass_textures[3][2]);
        grass_regions.put(Block.COLLIDE_LEFT_UP, grass_textures[2][3]);
        grass_regions.put(Block.COLLIDE_LEFT_UP_DOWN, grass_textures[1][3]);
        grass_regions.put(Block.COLLIDE_LEFT_UP_RIGHT, grass_textures[2][2]);
        grass_regions.put(Block.COLLIDE_DOWN, grass_textures[1][0]);
        grass_regions.put(Block.COLLIDE_RIGHT, grass_textures[3][0]);
        grass_regions.put(Block.COLLIDE_RIGHT_DOWN, grass_textures[0][1]);
        grass_regions.put(Block.COLLIDE_UP, grass_textures[2][0]);
        grass_regions.put(Block.COLLIDE_UP_DOWN, grass_textures[3][3]);
        grass_regions.put(Block.COLLIDE_UP_RIGHT, grass_textures[2][1]);
        grass_regions.put(Block.COLLIDE_UP_RIGHT_DOWN, grass_textures[1][1]);
    }

    @Override
    public void render(SpriteBatch batch, short connection_info, float x, float y, float w, float h, boolean background) {
        batch.setColor(dirt_block_color);
        batch.draw(getTextureRegion(connection_info), x, y, w, h);

        batch.setColor(grass_color);
        batch.draw(grass_regions.get(connection_info), x, y, w, h);
    }
}
