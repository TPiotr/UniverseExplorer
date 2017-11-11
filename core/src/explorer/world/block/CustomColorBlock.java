package explorer.world.block;

import com.badlogic.gdx.graphics.Color;

import explorer.game.framework.Game;

/**
 * Block that use custom color defined while during world creation
 * Created by RYZEN on 13.10.2017.
 */

public class CustomColorBlock extends Block {

    /**
     * This block color used in render
     */
    protected Color block_color;

    /**
     * @param game game instance for assets loading
     */
    public CustomColorBlock(Game game) {
        super(game);

        block_color = new Color(Color.WHITE);
    }

    /**
     * This block color used in render
     * @return render color
     */
    public Color getBlockColor() {
        return block_color;
    }
}
