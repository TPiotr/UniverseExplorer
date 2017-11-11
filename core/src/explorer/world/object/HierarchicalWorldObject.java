package explorer.world.object;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import explorer.game.framework.Game;
import explorer.world.World;

/**
 * Created by RYZEN on 22.10.2017.
 */

public class HierarchicalWorldObject extends WorldObject {

    /**
     * Parent, could be null
     */
    private HierarchicalWorldObject parent;

    /**
     * Position vector
     */
    private Vector2 local_position;

    /**
     * Local rotation in degree
     */
    private float local_rotation;

    /**
     * Origin of object
     */
    private Vector2 origin;

    /**
     * Array of all childrens of this objects
     */
    private Array<HierarchicalWorldObject> childrens;

    /**
     * Flag is object is visible
     */
    private boolean visible = true;

    /** Basic constructor of hierarchical world object
     * @param position local position of new object
     * @param world    world instance
     * @param game     game instance
     * @param parent   instance of this object parent
     */
    public HierarchicalWorldObject(Vector2 position, Vector2 wh, Vector2 origin, float rotation, HierarchicalWorldObject parent, World world, Game game) {
        super(new Vector2(), world, game);

        this.local_position = position;
        this.parent = parent;

        this.origin = origin;
        this.wh = wh;
        this.local_rotation = rotation;

        if(parent != null)
            parent.childrens.add(this);

        childrens = new Array<HierarchicalWorldObject>();
    }


    @Override
    public void tick(float delta) {
        if(visible) {
            for(int i = 0; i < childrens.size; i++)
                childrens.get(i).tick(delta);
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        if(visible) {
            for(int i = 0; i < childrens.size; i++)
                childrens.get(i).render(batch);
        }
    }

    /**
     * Getter for local position
     * @return local position, modify this vec to change object position
     */
    public Vector2 getLocalPosition() {
        return local_position;
    }

    /**
     * Getter for local rotation
     * @return local rotation degrees
     */
    public float getLocalRotation() {
        return local_rotation;
    }

    /**
     * Set local rotation to given value
     * @param rot_deg new local rotation value in degree
     */
    public void setLocalRotation(float rot_deg) {
        this.local_rotation = rot_deg;
    }

    /**
     * Get global rotation of this objects
     * @return global rotation [degress]
     */
    public float getRotation() {
        rotation = local_rotation;

        if(parent != null) {
            rotation += parent.getRotation();
        }

        return rotation;
    }

    /**
     * Returns this object global position
     * @return global position
     */
    public Vector2 getPosition() {
        position.set(0, 0);
        if(parent != null) {
            position.add(parent.getPosition());
        }

        /*if(parent != null) {
            final float cos = (float) Math.cos(-getLocalRotation() * MathUtils.degreesToRadians);
            final float sin = (float) Math.sin(-getLocalRotation() * MathUtils.degreesToRadians);

            final float ox = getOrigin().x * getWH().x;
            final float oy = getOrigin().y * getWH().y;

            final float tox = (getLocalPosition().x - ox);
            final float toy = (getLocalPosition().y - oy);

            position.x = (tox * cos + toy * sin) + ox + position.x;
            position.y = (tox * -sin + toy * cos) + oy + position.y;
        } else {
            position.add(getLocalPosition());
        }*/
        position.add(getLocalPosition());

        return position;
    }

    /**
     * Get origin of this object 0.0 - 1.0 range
     * @return origin of rotation
     */
    public Vector2 getOrigin() {
        return origin;
    }

    /**
     * Get all childrens of this object
     * @return array that contains all childrens of this object
     */
    public Array<HierarchicalWorldObject> getChildrens() {
        return childrens;
    }

    /**
     * Flag for object visibility
     * @return visibility flag
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Set visiblity of this object
     * @param visible new visiblity flag
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void dispose() {
        for(int i = 0; i < childrens.size; i++)
            childrens.get(i).dispose();
    }
}
