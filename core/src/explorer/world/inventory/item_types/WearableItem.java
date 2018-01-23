package explorer.world.inventory.item_types;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import explorer.game.framework.Game;
import explorer.world.inventory.Item;

/**
 * Wearable items so player can wear them
 * System used is fairly simple we render texture one over another but they have to have same width & height proportions (basic res. is 26x34)
 *
 * Created by RYZEN on 20.01.2018.
 */

public abstract class WearableItem extends Item {

    public static enum WearableType {
        IN_HAND_WEAPON, IN_HAND_TOOL, TORSO_ARMOR, LEGS_ARMOR, HEAD_ARMOR
    }

    /**
     * Texture that will be placed over basic player texture
     * For legs wearable type feed here whole spritesheet with animation frames
     */
    protected TextureRegion cloth_texture;

    protected WearableType wearable_type;

    public WearableItem(Game game) {
        super(game);
    }

    public TextureRegion getClothTexture() {
        return cloth_texture;
    }

    public WearableType getWearableType() {
        return wearable_type;
    }
}
