package explorer.world.block;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.HashMap;

import explorer.game.framework.Game;
import explorer.world.inventory.item_types.ToolItem;

/**
 * Created by RYZEN on 07.10.2017.
 */

public class Block {

    /**
     * This enum determines how block is connected (graphically) with other blocks
     */
    public enum BlockGroup {
        CONNECT_WITH_EVERYTHING, CONNECT_WITH_NOTHING, CONNECT_WITH_SAME_BLOCK;
    }

    /**
     * Game instance
     */
    protected Game game;

    /**
     * Unique per block type id
     */
    protected int block_id;

    /** POSSIBLE BLOCK CONNECTIONS: */
    public static final short COLLIDE_ALL_SIDES = 0;
    public static final short COLLIDE_NONE = 1;

    public static final short COLLIDE_LEFT = 2;
    public static final short COLLIDE_LEFT_DOWN = 3;
    public static final short COLLIDE_LEFT_UP = 4;
    public static final short COLLIDE_LEFT_UP_DOWN = 5;
    public static final short COLLIDE_LEFT_DOWN_RIGHT = 6;
    public static final short COLLIDE_LEFT_UP_RIGHT = 7;
    public static final short COLLIDE_LEFT_RIGHT = 8;

    public static final short COLLIDE_RIGHT = 9;
    public static final short COLLIDE_RIGHT_DOWN = 10;

    public static final short COLLIDE_UP = 11;
    public static final short COLLIDE_UP_RIGHT = 12;
    public static final short COLLIDE_UP_DOWN = 13;
    public static final short COLLIDE_UP_RIGHT_DOWN = 14;

    public static final short COLLIDE_DOWN = 15;

    /**
     * Store all block possible connections with other blocks
     */
    protected HashMap<Short, TextureRegion> tile_positions;

    /**
     * determine how block is connecting with other blocks
     */
    protected BlockGroup block_group;

    /**
     * Is block collidable in physics terms
     */
    protected boolean collidable = true;

    /**
     * Flag that determines if background block under this foreground block should be rendered if this block texture used to be rendered is not COLLIDE_ALL_SIDES
     */
    protected boolean need_background_block_rendered_if_not_fully_surrounded = false;

    /**
     * Flag that determines if background block must be rendered if this block is a foreground one
     */
    protected boolean need_background_block_rendered = false;

    /**
     * Hardness of block, how much we have to use some tool to break it
     */
    protected float hardness = 1f;

    /**
     * Tool type which is proffered for this block to break it
     */
    protected ToolItem.ToolType proffered_tool_type = ToolItem.ToolType.PICKAXE;

    /**
     * Flag determining if this block can be dropped by just destroying it
     */
    protected boolean dropable = true;

    /**
     * Flag determining if this block blocks ground light (useful when some block is like Grass plant and he have to pass through light)
     */
    protected boolean blocks_ground_light = true;

    /**
     * Flag determining if this block need any block under (except AIR)
     */
    protected boolean need_block_under;

    /**
     * Flag determining if this block need any block over (except AIR)
     */
    protected boolean need_block_over;

    /**
     * Flag determining if some other block can be just placed on this block (f.e. GrassPlant sets this to true so player don't have to first destroy grass and then place other block)
     */
    protected boolean can_place_other_block_on;

    /**
     * @param game game instance for assets loading
     */
    public Block(Game game) {
        this.game = game;
        block_group = BlockGroup.CONNECT_WITH_EVERYTHING;
    }

    /**
     * @return unique block id (using id you can get reference to block)
     */
    public int getBlockID() {
        return block_id;
    }

    /**
     * Getter for BlockGroup
     * @return block group of this block
     */
    public BlockGroup getBlockGroup() {
        return block_group;
    }

    /**
     * @param texture_id one of the shorts you can get staticaly from this class
     * @return proper TextureRegon for rendering
     */
    public TextureRegion getTextureRegion(short texture_id) {
        return tile_positions.get(texture_id);
    }

    /**
     * Returns flag that determines if background block on position of this foreground block should be rendered if this used to rendering block texture is not COLLIDE_ALL_SIDES
     * @return flag that determines if background block on position of this foreground block should be rendered if this used to rendering block texture is not COLLIDE_ALL_SIDES
     */
    public boolean needBackgroundBlockRenderedIfNotFullySurrounded() {
        return need_background_block_rendered_if_not_fully_surrounded;
    }

    /**
     * Returns flag that determines if background block must be rendered is this is a foreground block
     * @return true if background block must be rendered is this is a foreground block
     */
    public boolean needBackgroundBlockRendered() {
        return need_background_block_rendered;
    }

    /**
     * Flag that determines if this block is collidable
     * @return physics property if this block collides with physics world
     */
    public boolean isCollidable() {
        return collidable;
    }

    /**
     * Getter for block hardness value
     * @return block hardness
     */
    public float getHardness() {
        return hardness;
    }

    /**
     * Getter for proffered tool type to break this block
     * @return proffered tool type to break this block (so tool type that will be the most effective while destroying that block)
     */
    public ToolItem.ToolType getProfferedToolType() {
        return proffered_tool_type;
    }

    /**
     * Getter for dropable flag, if true we can collect this block as item just by breaking him
     * @return dropable flag
     */
    public boolean isDropable() {
        return dropable;
    }

    /**
     * Getter for blocks ground light flag, if true this block will block ground light (so ground light will be spawned on top of this block)
     * @return blocking ground light flag
     */
    public boolean isBlockingGroundLight() {
        return blocks_ground_light;
    }

    /**
     * Getter for flag that tells if this block need any block (except AIR) under when placing
     * @return need block under flag
     */
    public boolean needBlockUnder() {
        return need_block_under;
    }

    /**
     * Getter for flag that tells if this block need any block (except AIR) over when placing
     * @return need block over flag
     */
    public boolean needBlockOver() {
        return need_block_over;
    }

    /**
     * Getter for a flag that if is true allows player to replace this block with some other without need to break this block first
     * @return can place other block on flag
     */
    public boolean canPlaceOtherBlockOn() {
        return can_place_other_block_on;
    }
}
