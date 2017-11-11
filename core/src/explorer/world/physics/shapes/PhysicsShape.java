package explorer.world.physics.shapes;

import explorer.world.object.WorldObject;

/**
 * Class that defines dynamic or static world object physics shape so how collision will perform
 * Created by RYZEN on 24.10.2017.
 */

public abstract class PhysicsShape {

    /**
     * Parent of this shape
     */
    protected WorldObject parent;

    /**
     * Basic constructor
     * @param parent parent of this shape
     */
    public PhysicsShape(WorldObject parent) {
        this.parent = parent;
    }

    /**
     * Check if this physics body is overlapping with other world object
     * @param object other world object
     * @param other other world object physics shape
     * @return boolean that determines if given objects collides with each other
     */
    public abstract boolean overlaps(WorldObject object, PhysicsShape other);

    /**
     * Check if this physics body is overlapping with other world object
     * @param object other world object
     * @param other other world object physics shape
     * @param offset_x_parent position x offset for parent of this shape
     * @param offset_y_parent position y offset for parent of this shape
     * @param offset_x_other position x offset for other object
     * @param offset_y_other position y offset for other object
     * @return boolean that determines if given objects collides with each other
     */
    public abstract boolean overlaps(WorldObject object, PhysicsShape other, float offset_x_parent, float offset_y_parent, float offset_x_other, float offset_y_other);

}
