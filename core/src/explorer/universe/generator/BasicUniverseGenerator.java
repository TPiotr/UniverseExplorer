package explorer.universe.generator;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.OrderedSet;

import java.util.Set;
import java.util.concurrent.Future;

import explorer.game.framework.Game;
import explorer.game.framework.utils.FastNoise;
import explorer.game.framework.utils.PerlinNoise;
import explorer.universe.Universe;
import explorer.universe.object.PlanetUniverseObject;
import explorer.world.planet.generator.HeightsGenerator;

/**
 * Created by RYZEN on 14.11.2017.
 */

public class BasicUniverseGenerator extends UniverseChunkDataProvider {

    private FastNoise noise, noise1;

    public BasicUniverseGenerator() {
        int seed0 = 1;
        noise = new FastNoise(seed0);

        int seed1 = seed0 * 2;
        noise1 = new FastNoise(seed1);

        noise.SetFractalOctaves(3);

    }

    private float noise(float x, float y) {
        return noise.GetNoise(x, y) + 1f / 2f;
    }

    private float getPointNoise(float x, float y) {
        final float scale = .008f;
        float nx = x * scale;
        float ny = y * scale;

        float noise = 1f * noise(.1f * nx, .1f * ny)
        +  0.5f * noise(2 * nx, 2 * ny)
        + 0.25f * noise(4 * nx, 4 * ny);

        //float noise3 = perlin_noise.getNoise01(x * 4, y * 4) / 4f;
        //float noise4 = perlin_noise.getNoise01(x * 8, y * 8) / 8f;

        return noise; //+ (noise2 * noise3 * noise4);
    }

    @Override
    public synchronized Future<?> getUniverseChunkData(final UniverseChunkDataLoaded callback, final Vector2 chunk_position, final Universe universe, final Game game) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {

                /**
                 * Generating idea
                 * generate using noise couple stars in this chunk
                 * around each star generate planets in random distance from star so we have solar systems bigger or smaller
                 *
                 */

                UniverseChunkData data = new UniverseChunkData();

                //scale of generation
                final float SCALE = 1;

                //first phase generate stars

                /*float point_noise = getPointNoise((int) chunk_position.x, (int) chunk_position.y);
                System.out.println("noise: " + point_noise);

                int stars_count = (int) (point_noise * SCALE);
                System.out.println(stars_count);

                for(int i = 0; i < stars_count; i++) {
                    float noise_x = perlin_noise.getNoise01((int) chunk_position.x + (i * SCALE), (int) chunk_position.y - (i * SCALE));
                    float noise_y = perlin_noise.getNoise01((int) chunk_position.x - (i * SCALE), (int) chunk_position.y + (i * SCALE));

                    int x = (int) (chunk_position.x + (noise_x * Universe.UNIVERSE_CHUNK_SIZE));
                    int y = (int) (chunk_position.y + (noise_y * Universe.UNIVERSE_CHUNK_SIZE));

                    int index = (x >= y) ? x * y + x + y : x + y * y;

                    PlanetUniverseObject planet = new PlanetUniverseObject(index, new Vector2(x, y), universe, game);
                    data.objects.add(planet);
                }*/


                int steps = 200; //200
                float step_size = Universe.UNIVERSE_CHUNK_SIZE / (float) steps;

                for(int i = 0; i < steps; i++) {
                    for(int j = 0; j < steps; j++) {
                        int x = (int) ((i * step_size) + chunk_position.x);
                        int y = (int) ((j * step_size) + chunk_position.y);

                        //int index = (int) (1f/2f * (x + y) * (x + y + 1) + y);
                        int index = (x >= y) ? x * y + x + y : x + y * y;

                        float point_noise = getPointNoise(x * 2, y * 2);

                        //System.out.println(point_noise);

                        float noise = noise1.GetNoise(x, y);

                        if(point_noise > .8f && point_noise < .81f && noise > .1f) {
                            PlanetUniverseObject planet = new PlanetUniverseObject(index, new Vector2(x, y), universe, game);
                            data.objects.add(planet);
                        }
                    }
                }

                System.out.println("Objects count: " + data.objects.size);
                callback.loaded(data);
            }
        };
        return game.getThreadPool().runTaskFuture(r);
    }
}
