package explorer.world.inventory.items.wearables;

import explorer.game.framework.Game;
import explorer.world.inventory.item_types.WearableItem;

/**
 * Created by RYZEN on 22.01.2018.
 */

public class BasicLegsItem extends WearableItem {

    public BasicLegsItem(Game game) {
        super(game);
        this.cloth_texture = game.getAssetsManager().getTextureRegion("player/basic_legs");
        this.item_icon = cloth_texture;
        this.item_on_ground_texture = cloth_texture;
        this.wearable_type = WearableType.HEAD_ARMOR;
    }
}
