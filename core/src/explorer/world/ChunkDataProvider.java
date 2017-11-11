package explorer.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.concurrent.Future;

import explorer.game.framework.Game;
import explorer.world.object.WorldObject;

/**
 * Created by RYZEN on 07.10.2017.
 */

public abstract class ChunkDataProvider {

    /**
     * Result of task
     */
    public static class ChunkData {
        public int[][] foreground_blocks = new int[World.CHUNK_SIZE][World.CHUNK_SIZE];
        public int[][] background_blocks = new int[World.CHUNK_SIZE][World.CHUNK_SIZE];

        public Array<WorldObject> objects = new Array<WorldObject>();
    }

    /**
     * Callback for getChunkData method
     */
    public interface DataLoaded {
        void loaded(ChunkData data);
    }

    /**
     * callback when chunk data is saved
     */
    public interface DataSaved {
        void saved();
    }

    /**
     * Call this function to get chunk data based on its position
     * @param callback callback which will revice chunk data
     * @param chunk_position world pos of chunk
     * @param world world instance
     * @param game game instance
     */
    public abstract Future<?> getChunkData(DataLoaded callback, Vector2 chunk_position, World world, Game game);

    /**
     * Call this function to save data somewhere depends on provider type
     * @param callback callback which will call when saving operation will be done
     * @param chunk chunk that is going to be saved
     * @param chunk_position world pos of chunk
     * @param world world instance
     * @param game game instance
     */
    public abstract void saveChunkData(DataSaved callback, explorer.world.chunk.WorldChunk chunk, Vector2 chunk_position, World world, Game game);

}
