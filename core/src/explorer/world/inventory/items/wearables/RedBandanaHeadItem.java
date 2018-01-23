package explorer.world.inventory.items.wearables;

import explorer.game.framework.Game;
import explorer.world.inventory.item_types.WearableItem;

/**
 * Created by RYZEN on 22.01.2018.
 */

public class RedBandanaHeadItem extends WearableItem {

    public RedBandanaHeadItem(Game game) {
        super(game);
        this.wearable_type = WearableType.HEAD_ARMOR;

        this.cloth_texture = game.getAssetsManager().getTextureRegion("player/red_bandana_head");
        this.item_icon = game.getAssetsManager().getTextureRegion("inventory/items/red_bandana_item_icon");
        this.item_on_ground_texture = item_icon;

        this.dropable = true;
        this.stackable = false;
    }
}
