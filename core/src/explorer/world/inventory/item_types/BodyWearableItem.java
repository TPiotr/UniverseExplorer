package explorer.world.inventory.item_types;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;

/**
 * Created by RYZEN on 22.01.2018.
 */

public class BodyWearableItem extends WearableItem {

    protected TextureRegion arm_texture;

    /**
     * Local coords in pixels of arm texture
     */
    protected Vector2 local_arm_origin;

    /**
     * Local coords in pixels of arm texture, point where tool/weapon/whatever holding in hand should be connected to
     */
    protected Vector2 arm_tool_socket;

    /**
     * Local coords in pixels of body texture when arm is rendered as foreground one (y axis up!), at this cords arm will be connected to body
     */
    protected Vector2 foreground_body_arm_socket;

    /**
     * Local coords in pixels of body texture when arm is rendered as background one (y axis up!), at this cords arm will be connected to body
     */
    protected Vector2 background_body_arm_socket;

    /**
     * Flag determining if when this item is wear engine should also render basic arm & torso
     */
    protected boolean render_basic_item = true;

    public BodyWearableItem(Game game) {
        super(game);
        this.wearable_type = WearableType.TORSO_ARMOR;
    }

    public TextureRegion getArmRegion() {
        return arm_texture;
    }

    public Vector2 getLocalArmOrigin() {
        return local_arm_origin;
    }

    public Vector2 getForegroundBodyArmSocket() {
        return foreground_body_arm_socket;
    }

    public Vector2 getBackgroundBodyArmSocket() {
        return background_body_arm_socket;
    }

    public Vector2 getArmToolSocket() {
        return arm_tool_socket;
    }

    public boolean renderBasicItem() {
        return render_basic_item;
    }
}
