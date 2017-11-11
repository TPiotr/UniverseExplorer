package explorer.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.concurrent.Future;

import explorer.game.framework.Game;
import explorer.world.planet.generator.HeightsGenerator;
import explorer.world.object.objects.TestObject;

/**
 * Created by RYZEN on 07.10.2017.
 */

public class PerlinNoiseDataProvider extends ChunkDataProvider {

    //private HeightsGenerator heights_generator;
    private Array<HeightsGenerator> heights_generators;

    private World world;

    private float AMPLITUDE = 70;
    private int OCTAVES = 5;
    private float ROUGHNESS = .7f;

    public PerlinNoiseDataProvider(World world) {
        this.world = world;

        heights_generators = new Array<HeightsGenerator>();

        //PlanetGeneratorProperties properties = world.getPlanetProperties().PLANET_TYPE.GENERATOR_PROPERTIES;
        //AMPLITUDE = (world.getPlanetProperties().random.nextFloat() * (properties.MAX_AMPLITUDE - properties.MIN_AMPLITUDE)) + properties.MIN_AMPLITUDE;
        //OCTAVES = (int) (world.getPlanetProperties().random.nextFloat() * (properties.MAX_OCTAVES - properties.MIN_OCTAVES)) + properties.MIN_OCTAVES;
        //ROUGHNESS = (world.getPlanetProperties().random.nextFloat() * (properties.MAX_ROUGHNESS - properties.MIN_ROUGHNESS)) + properties.MIN_ROUGHNESS;

        System.out.println("A: " + AMPLITUDE + " O: " + OCTAVES + " R: " + ROUGHNESS);
    }

    private synchronized HeightsGenerator get() {
        if(heights_generators.size > 0) {
            HeightsGenerator out = heights_generators.get(0);
            heights_generators.removeIndex(0);

            return out;
        } else {
            return new HeightsGenerator(AMPLITUDE, OCTAVES, ROUGHNESS, world.getPlanetProperties().PLANET_SEED);
        }
    }
    private synchronized void free(HeightsGenerator generator) {
        heights_generators.add(generator);
    }

    @Override
    public Future<?> getChunkData(final DataLoaded callback, final Vector2 chunk_position, final World world, final Game game) {
        Runnable r = new Runnable() {
            public void run() {
                ChunkData out = new ChunkData();

                int chunk_pos_x = (int) chunk_position.x / World.CHUNK_WORLD_SIZE;
                int chunk_pos_y = (int) chunk_position.y / World.CHUNK_WORLD_SIZE;

                HeightsGenerator heights_generator = get();

                int planet_width = world.getPlanetProperties().PLANET_SIZE;
                chunk_pos_x %= planet_width;

                for (int i = 0; i < out.foreground_blocks.length; i++) {
                    int y = (int) heights_generator.generateHeight(i + (chunk_pos_x * World.CHUNK_SIZE), 0) + 100;

                    for (int j = 0; j < out.foreground_blocks[0].length; j++) {
                        if ((j + (chunk_pos_y * World.CHUNK_SIZE)) > y)
                            out.foreground_blocks[i][j] = 1;
                        else
                            out.foreground_blocks[i][j] = 2;

                        out.background_blocks[i][j] = out.foreground_blocks[i][j];

                        if ((j + (chunk_pos_y * World.CHUNK_SIZE)) == y) {
                            //use some noise func to check if can spawn never again use random!
                            if(heights_generator.getNoise(i + (chunk_pos_x * World.CHUNK_SIZE), j) >= .9f) {
                                out.objects.add(new TestObject(new Vector2(chunk_position).add(i * World.BLOCK_SIZE, (j + ((chunk_pos_y - 1) * World.CHUNK_SIZE)) * World.BLOCK_SIZE), world, game));
                            }
                        }
                    }
                }

                free(heights_generator);
                callback.loaded(out);
            }
        };

        return game.getThreadPool().runTaskFuture(r);
    }

    @Override
    public void saveChunkData(DataSaved callback, explorer.world.chunk.WorldChunk chunk, Vector2 chunk_position, World world, Game game) {
        callback.saved();
    }
}
