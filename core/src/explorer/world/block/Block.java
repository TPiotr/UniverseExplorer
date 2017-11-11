package explorer.world.block;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.HashMap;

import explorer.game.framework.Game;

/**
 * Created by RYZEN on 07.10.2017.
 */

public class Block {

    /**
     * This enum determines how block is connected (graphicaly) with other blocks
     */
    public static enum BlockGroup {
        CONNECT_WITH_EVERYTHING, CONNECT_WITH_NOTHING, CONNECT_WITH_SAME_BLOCK;
    }

    /**
     * Game instance
     */
    protected Game game;

    /**
     * Unique per block id
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
     * Flag that determines if this block is collidable
     * @return physics property if this block collides with physics world
     */
    public boolean isCollidable() {
        return collidable;
    }
}
