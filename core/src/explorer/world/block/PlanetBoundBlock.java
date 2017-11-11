package explorer.world.block;

import explorer.game.framework.Game;
import explorer.world.World;

/**
 * Created by RYZEN on 13.10.2017.
 */

public abstract class PlanetBoundBlock extends CustomColorBlock {

    /**
     * @param game game instance for assets loading
     */
    public PlanetBoundBlock(World world, Game game) {
        super(game);

    }

}
