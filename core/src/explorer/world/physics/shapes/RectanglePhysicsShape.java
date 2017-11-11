package explorer.world.physics.shapes;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import explorer.world.object.WorldObject;

/**
 * Created by RYZEN on 24.10.2017.
 */

public class RectanglePhysicsShape extends PhysicsShape {

    private static Rectangle rect1, rect2;

    static {
        rect1 = new Rectangle();
        rect2 = new Rectangle();
    }

    private Vector2 positon, wh;

    /**
     *
     * @param position local to parent coords (so final cords are parent.pos + position)
     * @param wh
     * @param parent
     */
    public RectanglePhysicsShape(Vector2 position, Vector2 wh, WorldObject parent) {
        super(parent);

        this.positon = position;
        this.wh = wh;
    }

    @Override
    public boolean overlaps(WorldObject object, PhysicsShape other) {
        if(other instanceof RectanglePhysicsShape) {
            RectanglePhysicsShape other_body = (RectanglePhysicsShape) other;

            rect1.set(parent.getPosition().x + positon.x, parent.getPosition().y + positon.y, wh.x, wh.y);
            rect2.set(object.getPosition().x + other_body.positon.x, object.getPosition().y + other_body.positon.y, object.getWH().x, object.getWH().y);

            return rect1.overlaps(rect2);
        }

        return false;
    }

    @Override
    public boolean overlaps(WorldObject object, PhysicsShape other, float offset_x_parent, float offset_y_parent, float offset_x_other, float offset_y_other) {
        if(other instanceof RectanglePhysicsShape) {
            RectanglePhysicsShape other_body = (RectanglePhysicsShape) other;

            rect1.set(parent.getPosition().x + positon.x + offset_x_parent, parent.getPosition().y + positon.y + offset_y_parent, wh.x, wh.y);
            rect2.set(object.getPosition().x + other_body.positon.x + offset_x_other, object.getPosition().y + other_body.positon.y + offset_y_other, object.getWH().x, object.getWH().y);

            return rect1.overlaps(rect2);
        }

        return false;
    }
}
