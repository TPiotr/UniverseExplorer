package explorer.world.object;

import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.physics.shapes.PhysicsShape;

/**
 * Created by RYZEN on 07.10.2017.
 */

public abstract class StaticWorldObject extends WorldObject {

    /**
     * This object physics shape
     */
    protected PhysicsShape physics_shape;

    public StaticWorldObject(Vector2 position, World world, Game game) {
        super(position, world, game);
    }

    /**
     * Getter for this object physics shape
     * @return instance of this object physics shape
     */
    public PhysicsShape getPhysicsShape() {
        return physics_shape;
    }
}
