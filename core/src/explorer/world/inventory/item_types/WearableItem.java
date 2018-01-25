package explorer.world.inventory.item_types;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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

    /**
     * Interface allowing wearable item to be rendered in custom way
     */
    public interface CustomWearableRenderer {

        /**
         * Custom rendering method for wearable items
         * @param x x global position in game world at which should be rendered
         * @param y y global position in game world at which should be rendered
         * @param w width
         * @param h height
         * @param texture texture which should be rendered now, especially useful when custom rendering legs so we don't have to care about handling animation here
         * @param direction -1 if player orientation is left, 1 if player orientation is right
         * @param batch batch instance for rendering
         */
        void render(float x, float y, float w, float h, TextureRegion texture, int direction, SpriteBatch batch);

    }

    /**
     * Enum representating types of wearables
     */
    public enum WearableType {
        IN_HAND_WEAPON, IN_HAND_TOOL, TORSO_ARMOR, LEGS_ARMOR, HEAD_ARMOR
    }

    /**
     * Texture that will be placed over basic player texture
     * For legs wearable type feed here whole spritesheet with animation frames
     */
    protected TextureRegion cloth_texture;

    /**
     * Type of wearable (if this is torso, legs, head etc.)
     */
    protected WearableType wearable_type;

    /**
     * Flag determining if when this item is worn engine should also render basic item (f.e. if true and player is wearing some armor engine will also renderer basic representation of this item so basic torso)
     * Useful when worn armor is at some point transparent etc.
     */
    protected boolean render_basic = true;

    /**
     * Basic constructor of wearable item
     * @param game game instance to access assets etc.
     */
    public WearableItem(Game game) {
        super(game);
    }

    /**
     * Get cloth texture region which will be rendered in place of basic texture
     * @return texture region which will be rendered over basic wearable type texture on player
     */
    public TextureRegion getClothTexture() {
        return cloth_texture;
    }

    /**
     * Getter for wearable type
     * @return get this item wearable type
     */
    public WearableType getWearableType() {
        return wearable_type;
    }

    /**
     * Getter for render_basic boolean
     * @return getter for a flag that determines if when this item is worn basic one should be rendered too (default true)
     */
    public boolean renderBasic() {
        return render_basic;
    }
}
