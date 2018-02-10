package explorer.world.block.blocks;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.HashMap;

import explorer.game.framework.Game;
import explorer.world.block.Block;
import explorer.world.block.Blocks;
import explorer.world.block.CustomRenderingBlock;
import explorer.world.inventory.item_types.ToolItem;

/**
 * Created by RYZEN on 31.01.2018.
 */

public class GrassPlantBlock extends CustomRenderingBlock {

    private static TextureRegion[] grasses;

    private Color color;

    /**
     * @param game game instance for assets loading
     */
    public GrassPlantBlock(Blocks blocks, Game game) {
        super(game);

        this.block_id = 6;
        this.block_group = BlockGroup.CONNECT_WITH_NOTHING;
        this.dropable = false;
        this.need_background_block_rendered = true;
        this.collidable = false;
        this.proffered_tool_type = ToolItem.ToolType.PICKAXE;
        this.hardness = 1f;
        this.blocks_ground_light = false;
        this.can_place_other_block_on = true;
        this.need_block_under = true;
        this.need_block_over = false;

        this.tile_positions = new HashMap<Short, TextureRegion>();

        if(grasses == null) {
            TextureRegion[][] splited_grass = game.getAssetsManager().getTextureRegion("blocks/grass_plants_spritesheet").split(16, 16);
            grasses = new TextureRegion[splited_grass[0].length];
            for(int i = 0; i < splited_grass[0].length; i++) {
                grasses[i] = splited_grass[0][i];
            }
        }

        color = blocks.GRASS.getGrassColor();
    }

    @Override
    public void render(SpriteBatch batch, short connection_info, float x, float y, float w, float h, boolean background) {
        batch.setColor(color);
        int i = (int) x % (grasses.length - 1);
        i = (i < 0) ? 0 : i;
        batch.draw(grasses[i], x, y - (w * 1 / 16f), w, h);
    }
}
