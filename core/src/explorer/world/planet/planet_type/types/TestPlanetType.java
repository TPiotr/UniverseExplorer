package explorer.world.planet.planet_type.types;

import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.planet.PlanetProperties;
import explorer.world.planet.generator.generators.TestWorldGenerator;
import explorer.world.planet.planet_type.ObjectsProperties;
import explorer.world.planet.planet_type.PlanetType;

/**
 * Created by RYZEN on 13.10.2017.
 */

public class TestPlanetType extends PlanetType {

    private class EmptyObjectsProperties extends ObjectsProperties {

    }

    public TestPlanetType(PlanetProperties properties, World world, Game game) {
        OBJECTS_PROPERTIES = new EmptyObjectsProperties();

        BLOCKS_PROPERTIES = new TestBlocksProperties();

        PLANET_GENERATOR = new TestWorldGenerator(properties, world, game);

        PLANET_BACKGROUND = new TestPlanetBackground(new Vector2(0, 0), game);
    }

}
