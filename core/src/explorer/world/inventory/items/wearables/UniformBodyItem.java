package explorer.world.inventory.items.wearables;

import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;
import explorer.world.inventory.item_types.BodyWearableItem;

/**
 * Created by RYZEN on 26.02.2018.
 */

public class UniformBodyItem extends BodyWearableItem {

    public UniformBodyItem(Game game) {
        super(game);
        this.cloth_texture = game.getAssetsManager().getTextureRegion("player/uniform_body");
        this.arm_texture = game.getAssetsManager().getTextureRegion("player/uniform_arm");
        this.item_icon = game.getAssetsManager().getTextureRegion("inventory/items/uniform_body_item_icon");
        this.item_on_ground_texture = item_icon;
        this.wearable_type = WearableType.TORSO_ARMOR;
        this.render_basic = false;

        this.foreground_body_arm_socket = new Vector2(9f, 19.5f);
        this.background_body_arm_socket = new Vector2(15.5f, 19.5f);
        this.local_arm_origin = new Vector2(5.5f, 23f);
        this.arm_tool_socket = new Vector2(8f, 3f);
    }
}
