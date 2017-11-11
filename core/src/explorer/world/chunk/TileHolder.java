package explorer.world.chunk;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Class that holds all variables for one block stored in chunk
 * Created by RYZEN on 07.10.2017.
 */

public class TileHolder {

    /**
     * what is foreground block id
     */
    private int foreground_block;

    /**
     * What is background block id
     */
    private int background_block;

    /**
     * Texture regions for foreground and background blocks
     */
    private TextureRegion foreground_texture;
    private TextureRegion background_texture;

    /**
     * Using this variable we can say on what sides block is colliding with other ones in foreground
     */
    private short foreground_block_texture_id;

    /**
     * Using this variable we can say on what sides block is colliding with other ones in background
     */
    private short background_block_texture_id;

    public TileHolder(int foreground_block, int background_block) {
        this.foreground_block = foreground_block;
        this.background_block = background_block;
    }

    /**
     * Copy vars from given holder
     * @param holder
     */
    public void set(TileHolder holder) {
        this.foreground_block = holder.foreground_block;
        this.background_block = holder.background_block;

        this.foreground_texture = holder.foreground_texture;
        this.background_texture = holder.background_texture;

        this.foreground_block_texture_id = holder.foreground_block_texture_id;
        this.background_block_texture_id = holder.background_block_texture_id;
    }

    /**
     * @return id of foreground block
     */
    public int getForegroundBlock() {
        return foreground_block;
    }

    /**
     * @param block set foreground block by given ID
     */
    public void setForegroundBlock(int block) {
        this.foreground_block = block;
    }

    /**
     * @return id of background block
     */
    public int getBackgroundBlock() {
        return background_block;
    }

    /**
     * @param block set background block by given ID
     */
    public void setBackgroundBlock(int block) {
        this.background_block = block;
    }

    /**
     * @return this block foreground texture region
     */
    public TextureRegion getForegroundTextureRegion() {
        return foreground_texture;
    }

    /**
     * Set foreground block texture region
     * @param texture_region new texture region
     */
    public void setForegroundTextureRegion(TextureRegion texture_region) {
        this.foreground_texture = texture_region;
    }

    /**
     * @return this block background texture region
     */
    public TextureRegion getBackgroundTextureRegion() {
        return background_texture;
    }

    /**
     * Set background block texture region
     * @param texture_region new texture region
     */
    public void setBackgroundTextureRegion(TextureRegion texture_region) {
        this.background_texture = texture_region;
    }

    /**
     * Get foreground block texture id
     * @return Get foreground block texture id
     */
    public short getForegroundBlockTextureID() {
        return foreground_block_texture_id;
    }

    /**
     * Set foreground block texture id to given one
     * @param id new block texture id
     */
    public void setForegroundBlockTextureID(short id) {
        this.foreground_block_texture_id = id;
    }

    /**
     *
     * Get background block texture id
     * @return Get background block texture id
     */
    public short getBackgroundBlockTextureID() {
        return background_block_texture_id;
    }

    /**
     * Set background block texture id to given one
     * @param id new block texture id
     */
    public void setBackgroundBlockTextureID(short id) {
        this.background_block_texture_id = id;
    }
}
