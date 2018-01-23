package explorer.world.inventory.item_types;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.block.Block;
import explorer.world.block.CustomRenderingBlock;
import explorer.world.inventory.Item;
import explorer.world.object.objects.player.Player;

/**
 * Created by RYZEN on 21.01.2018.
 */

public class BlockItem extends Item implements Item.InHandItemRenderer {

    private static AtomicInteger id_assigner = new AtomicInteger(1000);
    private static HashMap<Integer, Integer> id_per_block_type = new HashMap<Integer, Integer>();

    private int representing_block_id;

    private World world;

    //
    private static final float w = 16;
    private static final float h = w;
    private static final Affine2 offset_transform = new Affine2().idt().translate(w, 0).rotate(180);

    public BlockItem(Game game, int block_id, World world) {
        super(game);

        this.world = world;

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

    @Override
    public void render(float x, float y, float angle, int direction, Player player_instance, Affine2 transform, SpriteBatch batch) {
        //batch.draw(item_icon, x, y, 0, 0, w, h, direction, 1, angle + ((direction == -1) ? 180 : 0));

        if(direction == -1) {
            transform.mul(offset_transform);
        }

        batch.draw(item_icon, w, h, transform);

    }
}
