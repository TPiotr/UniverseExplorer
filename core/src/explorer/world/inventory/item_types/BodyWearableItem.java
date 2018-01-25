package explorer.world.inventory.item_types;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;

/**
 * Extended WearableItem with arm textures and properties
 * Created by RYZEN on 22.01.2018.
 */

public class BodyWearableItem extends WearableItem {

    /**
     * Texture region of an arm
     */
    protected TextureRegion arm_texture;

    /**
     * Local coords in pixels on arm texture, informs engine where arm origin is
     */
    protected Vector2 local_arm_origin;

    /**
     * Local coords in pixels on arm texture, point where tool/weapon/whatever holding in hand should be connected to
     */
    protected Vector2 arm_tool_socket;

    /**
     * Local coords in pixels on body texture when arm is rendered as foreground one (y axis up!), at this cords arm will be connected to body
     */
    protected Vector2 foreground_body_arm_socket;

    /**
     * Local coords in pixels on body texture when arm is rendered as background one (y axis up!), at this cords arm will be connected to body
     */
    protected Vector2 background_body_arm_socket;

    public BodyWearableItem(Game game) {
        super(game);
        this.wearable_type = WearableType.TORSO_ARMOR;
    }

    /**
     * Getter for texture region of an arm
     * @return returns texture region which will be rendered as arm over basic arm texture
     */
    public TextureRegion getArmRegion() {
        return arm_texture;
    }

    /**
     * Getter for local arm origin, used to render game properly, coords in pixels on texture (y axis up)
     * @return vector instance that contains information about local arm origin
     */
    public Vector2 getLocalArmOrigin() {
        return local_arm_origin;
    }

    /**
     * Getter for coords of point on body texture in pixels which represents point where arm should be rendered when arm is rendered as foreground one
     * @return vector instance that contains info about point (socket) where arm should be attached to when arm is rendered as foreground one
     */
    public Vector2 getForegroundBodyArmSocket() {
        return foreground_body_arm_socket;
    }

    /**
     * Getter for coords of point on body texture in pixels which represents point where arm should be rendered when arm is rendered as background one
     * @return vector instance that contains info about point (socket) where arm should be attached to when arm is rendered as background one
     */
    public Vector2 getBackgroundBodyArmSocket() {
        return background_body_arm_socket;
    }

    /**
     * Getter for coords of point on arm texture in pixels which represents point where tool/block etc (everything that implements InHandItemRenderer) should be rendered
     * @return vector instance that contains info about point (socket) where InHandItemRenderer should be rendered
     */
    public Vector2 getArmToolSocket() {
        return arm_tool_socket;
    }
}
