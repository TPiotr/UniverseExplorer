package explorer.world.inventory.items.wearables;

import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;
import explorer.world.inventory.item_types.BodyWearableItem;
import explorer.world.inventory.item_types.WearableItem;

/**
 * This is basic body of player in fact this is not type of armor that player can access from game,
 * This body is rendered when player is wearing nothing, or wearing item has render_basic flag equal to true
 * Created by RYZEN on 20.01.2018.
 */

public class BasicBodyItem extends BodyWearableItem {

    public BasicBodyItem(Game game) {
        super(game);
        this.cloth_texture = game.getAssetsManager().getTextureRegion("player/base_body");
        this.arm_texture = game.getAssetsManager().getTextureRegion("player/basic_arm");
        this.item_icon = cloth_texture;
        this.item_on_ground_texture = cloth_texture;
        this.wearable_type = WearableType.TORSO_ARMOR;

        this.foreground_body_arm_socket = new Vector2(9f, 19.5f);
        this.background_body_arm_socket = new Vector2(15.5f, 19.5f);
        this.local_arm_origin = new Vector2(5.5f, 23f);
        this.arm_tool_socket = new Vector2(8f, 3f);
    }
}
