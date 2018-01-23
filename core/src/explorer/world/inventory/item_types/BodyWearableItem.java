package explorer.world.inventory.item_types;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import explorer.game.framework.Game;

/**
 * Created by RYZEN on 22.01.2018.
 */

public class BodyWearableItem extends WearableItem {

    protected TextureRegion arm_texture;

    public BodyWearableItem(Game game) {
        super(game);
        this.wearable_type = WearableType.TORSO_ARMOR;
    }

    public TextureRegion getArmRegion() {
        return arm_texture;
    }
}
