package explorer.universe.generator;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.OrderedSet;

import java.util.Set;
import java.util.concurrent.Future;

import explorer.game.framework.Game;
import explorer.universe.Universe;
import explorer.universe.object.PlanetUniverseObject;
import explorer.world.planet.generator.HeightsGenerator;

/**
 * Created by RYZEN on 14.11.2017.
 */

public class BasicUniverseGenerator extends UniverseChunkDataProvider {

    private HeightsGenerator perlin_noise;

    public BasicUniverseGenerator() {
        int seed = 1;
        perlin_noise = new HeightsGenerator(75, 5, .3f, seed);
    }

    @Override
    public Future<?> getUniverseChunkData(final UniverseChunkDataLoaded callback, final Vector2 chunk_position, final Universe universe, final Game game) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                UniverseChunkData data = new UniverseChunkData();

                int steps = Universe.UNIVERSE_CHUNK_SIZE / 100;
                float step_size = Universe.UNIVERSE_CHUNK_SIZE / (float) steps;
                for(int i = 0; i < steps; i++) {
                    for(int j = 0; j < steps; j++) {
                        int x = (int) ((i * step_size) + chunk_position.x);
                        int y = (int) ((j * step_size) + chunk_position.y);

                        //int index = (int) (1f/2f * (x + y) * (x + y + 1) + y);
                        int index = (x >= y) ? x * y + x + y : x + y * y;

                        float noise = perlin_noise.getNoise((x * 100), (y * 100));
                        if(noise > .7f) {
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
