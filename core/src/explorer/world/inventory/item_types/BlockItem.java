package explorer.world.inventory.item_types;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import explorer.game.framework.Game;
import explorer.game.screen.screens.Screens;
import explorer.game.screen.screens.planet.PlanetScreen;
import explorer.world.World;
import explorer.world.block.Block;
import explorer.world.block.CustomColorBlock;
import explorer.world.block.CustomRenderingBlock;
import explorer.world.inventory.Item;
import explorer.world.object.objects.player.Player;

/**
 * Universal item type that can represent any type of block as an item
 * Created by RYZEN on 21.01.2018.
 */

public class BlockItem extends Item implements Item.InHandItemRenderer, Item.InInventoryRenderer {

    /**
     * ID of block that this item represents
     */
    private int representing_block_id = 0;
    private Block block;

    /**
     * Variables used in rendering item when player holds it in his arm
     */
    private static final float w = 16;
    private static final float h = w;

    private PlanetScreen planet_screen;

    private static final Matrix4 idt_mat = new Matrix4().idt();
    private static final Matrix4 transform_matrix = new Matrix4();

    public BlockItem(Game game) {
        super(game);

        //1000 offset to never collide with some other item type id
        ID = 1000;

        stackable = true;
        max_in_stack = 64;

        planet_screen = game.getScreen(Screens.PLANET_SCREEN_NAME, PlanetScreen.class);

        item_icon = game.getAssetsManager().getTextureRegion("white_texture");
        item_on_ground_texture = item_icon;
    }

    @Override
    public String getItemProperty() {
        item_property = "" + representing_block_id;
        return item_property;
    }

    @Override
    public void setItemProperty(String item_property) {
        int block_id = Integer.parseInt(item_property);
        representing_block_id = block_id;
        this.ID = 1000 + block_id;

        tryToLoadTextures();

        super.setItemProperty(item_property);
    }

    /**
     * Assign some block to this block item
     * @param block_id id of block which this item will be representating
     * @param world world instance to grab block instance
     * @return this BlockItem instance
     */
    public BlockItem setBlock(int block_id, World world) {
        this.representing_block_id = block_id;
        this.block = world.getBlocks().getBlock(block_id);
        this.ID = 1000 + block_id;

        item_icon = block.getTextureRegion(Block.COLLIDE_NONE);
        item_on_ground_texture = item_icon;

        return this;
    }

    /**
     * Getter for id of block which this item represents
     * @return id of block which this item represents
     */
    public int getRepresentingBlockID() {
        return representing_block_id;
    }

    @Override
    public void renderInInventory(float x, float y, float w, float h, SpriteBatch batch) {
        //render block when is in some inventory slot

        if(block != null) {
            if(!(block instanceof CustomRenderingBlock)) {
                if (block instanceof CustomColorBlock) {
                    CustomColorBlock cblock = (CustomColorBlock) block;
                    batch.setColor(cblock.getBlockColor());
                }

                batch.draw(item_icon, x, y, w, h);
                batch.setColor(Color.WHITE);
            } else {
                ((CustomRenderingBlock) block).render(batch, Block.COLLIDE_NONE, x, y, w, h, false);
                batch.setColor(Color.WHITE);
            }
        }
    }

    @Override
    public void render(float x, float y, float angle, int direction, Player player_instance, Affine2 transform, SpriteBatch batch) {
        //if we don't want to use transform we can use this method
        //batch.draw(item_icon, x, y, 0, 0, w, h, direction, 1, angle + ((direction == -1) ? 180 : 0));

        tryToLoadTextures();

        if(block != null) {
            if(!(block instanceof CustomRenderingBlock)) {
                if (block instanceof CustomColorBlock) {
                    CustomColorBlock cblock = (CustomColorBlock) block;
                    batch.setColor(cblock.getBlockColor());
                }

                batch.draw(item_icon, w, h, transform);
                batch.setColor(Color.WHITE);
            } else {
                transform_matrix.set(transform);
                batch.setTransformMatrix(transform_matrix);
                ((CustomRenderingBlock) block).render(batch, Block.COLLIDE_NONE, 0, 0, w, h, false);
                batch.setTransformMatrix(idt_mat);

                batch.setColor(Color.WHITE);
            }
        }
    }

    private void tryToLoadTextures() {
        if(planet_screen.getWorld() == null)
            return;

        setBlock(representing_block_id, planet_screen.getWorld());
    }

    @Override
    public boolean firstArmThenTool() {
        return false;
    }

}
