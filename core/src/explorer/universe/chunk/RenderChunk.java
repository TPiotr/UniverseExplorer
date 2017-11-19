package explorer.universe.chunk;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.Vector;

import explorer.universe.object.UniverseObject;

/**
 * Subclass of UniverseChunk used to store part of UniverseChunk objects
 * With this we have a lot faster rendering/logic
 *
 * Logic with this looks like:
 * Cull RenderChunks => If render chunk visible rendering their objects with culling too
 *
 * Created by RYZEN on 19.11.2017.
 */

public class RenderChunk {

    /**
     * Parent chunk instance
     */
    private UniverseChunk parent_chunk;

    /**
     * Local position of render chunk and vector with dimensions
     */
    private Vector2 position, wh;

    /**
     * Array that contains all objects that belongs to this render chunk
     */
    private Array<UniverseObject> objects;

    /**
     * Reused in logic/render for temporary storing objects bound for culling stuff
     */
    private Rectangle culling_rectangle;

    /**
     * Create new instance of render chunk
     * @param local_position local position (local mean 0.0 - Universe.ChunkSize)
     * @param parent chunk parent
     */
    public RenderChunk(Vector2 local_position, UniverseChunk parent) {
        parent_chunk = parent;

        this.position = local_position;
        this.wh = new Vector2(UniverseChunk.RENDER_CHUNK_SIZE, UniverseChunk.RENDER_CHUNK_SIZE);

        culling_rectangle = new Rectangle();

        objects = new Array<UniverseObject>();
    }

    /**
     * Add object to this render chunk
     * @param object new object instance
     */
    public synchronized void addObject(UniverseObject object) {
        objects.add(object);
    }

    /**
     * Remove object from this render chunk
     * @param object object instance that going to be removed
     */
    public synchronized void removeObject(UniverseObject object) {
        objects.removeValue(object, true);
    }

    /**
     * Copy objects from given RenderChunk instance, used in UniverseChunk moveAndCopy() method
     * @param other chunk from which data will be copied
     */
    public synchronized void copyObjects(RenderChunk other) {
        objects.clear();
        objects.addAll(other.objects);
    }

    /**
     * Clear render chunk from all objects
     */
    public synchronized void clear() {
        objects.clear();
    }

    /**
     * Update all objects on that render chunk
     * @param delta delta time
     */
    public void tick(float delta) {
        synchronized (objects) {
            for (int i = 0; i < objects.size; i++) {
                if(i > objects.size - 1)
                    break;

                UniverseObject o = objects.get(i);
                if(o == null)
                    continue;

                o.tick(delta);
            }
        }
    }

    /**
     * Render all objects that belongs to this render chunk
     * @param batch sprite batch instance ready for drawing
     * @param screen_rectangle rectangle for culling objects
     */
    public void render(SpriteBatch batch, Rectangle screen_rectangle) {
        synchronized (objects) {
            for (int i = 0; i < objects.size; i++) {
                //ehh multithreading problems temporary solving
                if(i > objects.size - 1)
                    break;

                UniverseObject o = objects.get(i);
                if(o == null)
                    continue;

                //cull objects
                culling_rectangle.set(o.getPosition().x, o.getPosition().y, o.getWH().x, o.getWH().y);
                if(culling_rectangle.overlaps(screen_rectangle))
                    o.render(batch);
            }
        }
    }

    /**
     * Get local position of render chunk (local means: 0.0 - Universe.ChunkSize)
     * @return local position vector instance
     */
    public Vector2 getLocalPosition() {
        return position;
    }

    /**
     * Get width&height vector instance (x,y of this vector = UniverseChunk.RENDER_CHUNK_SIZE)
     * @return w&h vector instance
     */
    public Vector2 getWH() {
        return wh;
    }
}
