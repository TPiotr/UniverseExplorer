package explorer.universe.object;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;
import explorer.universe.Universe;
import explorer.universe.chunk.UniverseChunk;

/**
 * Created by RYZEN on 14.11.2017.
 */

public abstract class UniverseObject {

    /**
     * Universe instance
     */
    protected Universe universe;

    /**
     * Game instance
     */
    protected Game game;

    /**
     * Position, width&height vector instances
     */
    protected Vector2 position, wh;

    /**
     * Instance of this object universe chunk parent
     */
    protected UniverseChunk parent_chunk;

    public UniverseObject(Vector2 position, Universe universe, Game game) {
        this.position = position;
        this.universe = universe;
        this.game = game;
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
    public UniverseChunk getParentChunk() {
        return parent_chunk;
    }

    /**
     * Setter for parent chunk
     * @param parent_chunk new parent chunk
     */
    public void setParentChunk(UniverseChunk parent_chunk) {
        this.parent_chunk = parent_chunk;
    }

}
