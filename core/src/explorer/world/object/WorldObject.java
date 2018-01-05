package explorer.world.object;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

import java.util.HashMap;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.chunk.WorldChunk;

/**
 * Created by RYZEN on 07.10.2017.
 */

public abstract class WorldObject {

    /**
     * Very simple class that just gives unique id per next() method call
     */
    public static class IDAssigner {

        private static int acc_id = 0;

        /**
         * Get new unique id
         * @return new unique id
         */
        public static synchronized int next() {
            return acc_id++;
        }

        /**
         * AccValue of acc_id var so in fact this is the last generated id
         * @return
         */
        public static synchronized int accValue() {
            return acc_id;
        }

        public static synchronized void set(int new_val) {
            acc_id = new_val;
        }
    }

    protected Game game;

    /**
     * Unique id per object, same on every client object instance
     * -1 means that id for this object wasn't assigned yet
     */
    public int OBJECT_ID = -1;

    /**
     * Vec2's to store position and WH(width&height)
     */
    protected Vector2 position, wh;
    /**
     * Store object rotation in deg
     */
    protected float rotation;

    /**
     * World instance
     */
    protected World world;

    /**
     * Flag that determines if this objects should be saved
     */
    protected boolean saveable = true;

    /**
     * Chunk on which object is actually staying
     */
    protected WorldChunk parent_chunk;

    /**
     * Custom properties of this object, could be null!
     */
    protected HashMap<String, String> object_properties;

    /**
     * The most basic constructor that every saveable object have to have!
     * @param position position of new object
     * @param world world instance
     * @param game game instance
     */
    public WorldObject(Vector2 position, World world, Game game) {
        this.game = game;
        this.position = position;
        this.world = world;

        this.wh = new Vector2();
    }

    /**
     * just tick
     * @param delta delta time
     */
    public abstract void tick(float delta);

    /**
     * render method
     * @param batch sprite batch instance
     */
    public abstract void render(SpriteBatch batch);

    /**
     * Move object new_position = (position + move_vector)
     * @param move_vector move vector
     */
    public void move(Vector2 move_vector) {
        getPosition().add(move_vector);
    }

    /**
     * Called when objects removed from world or game is closing
     */
    public abstract void dispose();

    /**
     * If object will be saved
     * @return true if object will be saved after going out from visible chunks etc.
     */
    public boolean isSaveable() {
        return saveable;
    }

    /**
     * @return rotation in degrees
     */
    public float getRotation() {
        return rotation;
    }

    /**
     * Set rotation
     * @param rotation_deg new rotation in deg
     */
    public void setRotation(float rotation_deg) {
        this.rotation = rotation_deg;
    }

    /**
     * Getter for position of this objects
     * @return vec2 instance of this object position
     */
    public Vector2 getPosition() {
        return position;
    }

    /**
     * Getter for this objects vec2 that contains width & height
     * @return width & height vector
     */
    public Vector2 getWH() {
        return wh;
    }

    /**
     * Getter for this objects chunk parent
     * @return parent chunk of this object
     */
    public WorldChunk getParentChunk() {
        return parent_chunk;
    }

    /**
     * Setter for parent chunk
     * @param parent_chunk new parent chunk
     */
    public void setParentChunk(WorldChunk parent_chunk) {
        this.parent_chunk = parent_chunk;
    }

    /**
     * Getter for hashmap that contains custom properties of this object, used in save/load/send system to save/load object state
     * @return hashmap containing custom properties for this instance of object, could be null!
     */
    public HashMap<String, String> getObjectProperties() {
        return object_properties;
    }

    /**
     * Set hashmap properties instance to new one given in parameter
     * @param properties new hashmap contains new properties for this object
     */
    public void setObjectProperties(HashMap<String, String> properties) {
        this.object_properties = properties;
    }
}
