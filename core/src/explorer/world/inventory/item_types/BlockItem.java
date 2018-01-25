package explorer.world.inventory.item_types;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import explorer.game.framework.Game;
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

public class BlockItem extends Item implements Item.InHandItemRenderer {

    /**
     * ID of block that this item represents
     */
    private int representing_block_id;
    private Block block;

    /**
     * World instance
     */
    private World world;

    /**
     * Variables used in rendering item when player holds it in his arm
     */
    private static final float w = 16;
    private static final float h = w;

    //static offset
    private static final Affine2 offset_transform = new Affine2().idt().translate(w, 0).rotate(180);

    public BlockItem(Game game, int block_id, World world) {
        super(game);

        this.world = world;

        //1000 offset to never collide with some other item type id
        ID = block_id + 1000;

        representing_block_id = block_id;
        block = world.getBlocks().getBlock(representing_block_id);

        //get texture
        item_icon = block.getTextureRegion(Block.COLLIDE_NONE);
        item_on_ground_texture = item_icon;
        System.out.println("New laying item icon null? " + (item_icon == null));

        stackable = true;
        max_in_stack = 64;
    }

    /**
     * Getter for id of block which this item represents
     * @return id of block which this item represents
     */
    public int getRepresentingBlockID() {
        return representing_block_id;
    }

    @Override
    public void render(float x, float y, float angle, int direction, Player player_instance, Affine2 transform, SpriteBatch batch) {
        //if we don't want to use transform we can use this method
        //batch.draw(item_icon, x, y, 0, 0, w, h, direction, 1, angle + ((direction == -1) ? 180 : 0));

        if(block instanceof CustomColorBlock) {
            CustomColorBlock cblock = (CustomColorBlock) block;
            batch.setColor(cblock.getBlockColor());
        }

        batch.draw(item_icon, w, h, transform);
        batch.setColor(Color.WHITE);
    }

    @Override
    public boolean firstArmThenTool() {
        return false;
    }
}
