package explorer.world.lighting;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.DelayedRemovalArray;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.chunk.WorldChunk;
import explorer.world.lighting.lights.PointLight;

/**
 * Created by RYZEN on 07.10.2017.
 */

public class GroundLineRenderer {

    /**
     * Class that uses point light alpha mask and contains all positions for ground lights
     */

    /**
     * world instance for getting chunks
     */
    private World world;

    /**
     * Store all current ground lights in this array
     */
    private DelayedRemovalArray<Vector2> lights_positions;

    /**
     * reference point light edit this light to edit global ground lighting
     */
    private PointLight point_light;

    /**
     * Rectangles for in class calculations
     */
    private Rectangle chunks_rectangle, chunk_rectangle;

    public GroundLineRenderer(World world, Game game) {
        this.world = world;

        lights_positions = new DelayedRemovalArray<Vector2>();

        point_light = new PointLight(new Vector2(), new Vector3(1, 1, 1), 150, game);

        chunks_rectangle = new Rectangle();
        chunk_rectangle = new Rectangle();
    }

    /**
     * Remove lights that are out of loaded world
     */
    public void filterOut() {
        //first calc new chunks rect
        chunks_rectangle.set(world.getWorldChunks()[0][0].getPosition().x, world.getWorldChunks()[0][0].getPosition().y, World.CHUNK_WORLD_SIZE * world.getWorldChunks().length, World.CHUNK_WORLD_SIZE * world.getWorldChunks()[0].length);

        lights_positions.begin();

        for(int i = 0; i < lights_positions.size; i++) {
            point_light.getLightPosition().set(lights_positions.get(i));

            //if light out of chunks bounds just remove it
            if(!chunks_rectangle.overlaps(point_light.getLightBounds())) {
                lights_positions.removeIndex(i);
            }
        }

        lights_positions.end();
    }

    /**
     * Remove all ground lights that belongs to given chunk,
     * used when after placing block you have to calculate ground light again so previous one have to be removed
     * @param chunk chunk from which ground light have to be removed
     */
    public synchronized void removeChunkBoundLights(WorldChunk chunk) {
        chunk_rectangle.set(chunk.getPosition().x, chunk.getPosition().y, World.CHUNK_WORLD_SIZE, World.CHUNK_WORLD_SIZE);

        lights_positions.begin();

        for(int i = 0; i < lights_positions.size; i++) {
            //if light is in chunk bounding rect remove it
            if(chunk_rectangle.contains(lights_positions.get(i))) {
                lights_positions.removeIndex(i);
            }
        }

        lights_positions.end();
    }

    /**
     * Add new ground light position
     * @param pos new position
     */
    public synchronized void addPosition(Vector2 pos) {
        lights_positions.add(pos);
    }

    /**
     * Get array that contains all ground lights positions
     * @return array that contains all ground lights
     */
    public Array<Vector2> getPositions() {
        return lights_positions;
    }

    /**
     * Get point light that is using as reference for all ground lights
     * @return reference point light
     */
    public PointLight getPointLight() {
        return point_light;
    }
}
