package explorer.universe.generator;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.concurrent.Future;

import explorer.game.framework.Game;
import explorer.universe.Universe;
import explorer.universe.object.UniverseObject;
import explorer.world.ChunkDataProvider;
import explorer.world.World;
import explorer.world.object.WorldObject;

/**
 * Created by RYZEN on 14.11.2017.
 */

public abstract class UniverseChunkDataProvider {

    /**
     * Result of task
     */
    public static class UniverseChunkData {
        public Array<UniverseObject> objects = new Array<UniverseObject>();
    }

    /**
     * Callback for getChunkData method
     */
    public interface UniverseChunkDataLoaded {
        void loaded(UniverseChunkData data);
    }

    /**
     * Call this function to get chunk data based on its position
     * @param callback callback which will revice chunk data
     * @param chunk_position world pos of chunk
     * @param universe universe instance
     * @param game game instance
     */
    public abstract Future<?> getUniverseChunkData(UniverseChunkDataLoaded callback, Vector2 chunk_position, Universe universe, Game game);
}
