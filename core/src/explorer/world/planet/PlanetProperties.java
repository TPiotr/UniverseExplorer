package explorer.world.planet;

import com.badlogic.gdx.math.RandomXS128;

import java.util.Random;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.planet.planet_type.PlanetType;
import explorer.world.planet.planet_type.types.TestPlanetType;

/**
 * Class that have all properties describing planet
 * Created by RYZEN on 13.10.2017.
 */

public class PlanetProperties {

    /**
     * Planet seed used in future in blocks, chunks generating process
     */
    public int PLANET_SEED;

    /**
     * Planet size in chunks count (wide)
     */
    public int PLANET_SIZE;

    /**
     * Type of this planet (jungle, desert etc.)
     */
    public PlanetType PLANET_TYPE;

    /**
     * Use this random instance everywhere where you have to have same results every time
     */
    public Random random;

    /**
     * Generate planet properties only by seed
     * @param world world instance
     * @param game game instance
     * @param seed planet seed
     */
    public PlanetProperties(World world, Game game, int seed) {
        PLANET_SEED = seed;

        random = new RandomXS128(seed);

        //generate planet properties based on seed
        generate(world, game);
    }

    /**
     * Generate planet properties by seed value
     * @param world world instance
     * @param game game instance
     */
    private void generate(World world, Game game) {
        int size = random.nextInt(3);
        PLANET_SIZE = 10 * (size + 1);
        System.out.println("Planet size: " + PLANET_SIZE);

        //TODO in future use random generator to generate different planets types, for now we have just test planet type
        PLANET_TYPE = new TestPlanetType(this, world, game);
    }

}
