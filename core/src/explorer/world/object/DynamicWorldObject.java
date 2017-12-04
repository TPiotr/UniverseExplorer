package explorer.world.object;

import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.physics.shapes.PhysicsShape;

/**
 * Created by RYZEN on 07.10.2017.
 */

public abstract class DynamicWorldObject extends WorldObject {

    /**
     * This object physics shape
     */
    protected PhysicsShape physics_shape;

    /**
     * Velocity of object
     */
    protected Vector2 velocity;

    /**
     * Flag that determines if this dynamic object colliders with other dynamic ones (other dynamic had to set this flag to true too)
     */
    protected boolean collide_with_other_dynamics;

    public DynamicWorldObject(Vector2 position, World world, Game game) {
        super(position, world, game);

        velocity = new Vector2();
    }

    /**
     * Get velocity of this object
     * @return velocity of this object
     */
    public Vector2 getVelocity() {
        return velocity;
    }

    /**
     * Getter for this object physics shape
     * @return instance of this object physics shape
     */
    public PhysicsShape getPhysicsShape() {
        return physics_shape;
    }

    /**
     * Getter for a flag that describes if this dynamic object collide with other dynamic objects that have this flag set to true too
     * @return
     */
    public boolean isCollidingWithOtherDynamicObjects() {
        return collide_with_other_dynamics;
    }
}
