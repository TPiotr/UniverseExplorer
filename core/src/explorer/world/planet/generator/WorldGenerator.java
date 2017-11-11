package explorer.world.planet.generator;

import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;
import explorer.world.ChunkDataProvider;
import explorer.world.World;

/**
 * World generator base class
 * Created by RYZEN on 07.10.2017.
 */

public abstract class WorldGenerator {

    protected World world;
    protected Game game;

    public WorldGenerator(World world, Game game) {
        this.world = world;
        this.game = game;
    }

    public abstract ChunkDataProvider.ChunkData getChunkData(Vector2 chunk_position);

    public abstract void generateAndSaveChunk(String chunk_path, Vector2 chunk_position);

    /**
     * Get maximum value of world height in chunks
     * @return max height of world that can be generated using this generator
     */
    public abstract int getMaxHeight();
}
