package explorer.universe.generator;

import com.badlogic.gdx.math.Vector2;

import java.util.concurrent.Future;

import explorer.game.framework.Game;
import explorer.universe.Universe;

/**
 * Created by RYZEN on 14.11.2017.
 */

public class BasicUniverseGenerator extends UniverseChunkDataProvider {
    @Override
    public Future<?> getUniverseChunkData(UniverseChunkDataLoaded callback, final Vector2 chunk_position, Universe universe, Game game) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                System.out.println("generating " + chunk_position);
            }
        };
        return game.getThreadPool().runTaskFuture(r);
    }
}
