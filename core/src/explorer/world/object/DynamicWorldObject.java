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
}
