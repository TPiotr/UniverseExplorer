package explorer.world.physics;

import explorer.world.object.WorldObject;

/**
 * Object that will receive info when it will collide with other objects
 * Created by RYZEN on 23.10.2017.
 */

public interface SensorObject {

    /**
     * Event called when this object collides with other one
     * @param other other object that this object collides with
     */
    void collide(WorldObject other);

}
