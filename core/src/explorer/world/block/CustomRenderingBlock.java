package explorer.world.block;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import explorer.game.framework.Game;

/**
 * Created by RYZEN on 16.10.2017.
 */

public abstract class CustomRenderingBlock extends Block {

    /**
     * @param game game instance for assets loading
     */
    public CustomRenderingBlock(Game game) {
        super(game);
    }

    /**
     * Write here you custom rendering method for block
     * @param batch sprite batch instance
     * @param connection_info info about how this block is colliding with surrounding blocks to use proper old_assets.textures (see Short variables in {@link Block})
     * @param x global x pos
     * @param y global y pos
     * @param w width of block
     * @param h height of block
     * @param background true if we are rendering background block, false if foreground
     */
    public abstract void render(SpriteBatch batch, short connection_info, float x, float y, float w, float h, boolean background);
}
