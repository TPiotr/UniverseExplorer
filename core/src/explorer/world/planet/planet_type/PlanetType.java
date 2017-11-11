package explorer.world.planet.planet_type;


import explorer.world.background.Background;
import explorer.world.planet.generator.WorldGenerator;

/**
 * Created by RYZEN on 13.10.2017.
 */

public class PlanetType {

    /**
     * Properties of block properties needed to properly generate block instances
     */
    public BlocksProperties BLOCKS_PROPERTIES;

    /**
     * Properties of objects that contains all possible objects on planet type f.e. trees, bushes, plants
     */
    public ObjectsProperties OBJECTS_PROPERTIES;

    /**
     * Background of the planet
     */
    public Background PLANET_BACKGROUND;

    /**
     * Planet generator
     */
    public WorldGenerator PLANET_GENERATOR;

}
