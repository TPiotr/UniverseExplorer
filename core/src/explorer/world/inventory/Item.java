package explorer.world.inventory;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;
import explorer.world.object.objects.player.Player;

/**
 * Item class contains basic informations like ID, basic textures, is_stackable etc. about item type
 * The most basic class in inventory system
 */
public abstract class Item {

	/**
	 * Interface that allows item type to be customly rendered when shown in inventory, useful when some item is rendering from some textures layers and have custom color on different planets so there is no problem
	 * to represent it in inventory
	 */
	public interface InInventoryRenderer {

		/**
		 * Render in custom way item in some inventory slot or wherever
		 * @param x x cord
		 * @param y y cord
		 * @param w width
		 * @param h height
		 * @param batch batch instance, take care about color after custom rendering! (just set to Color.WHITE after rendering)
		 */
		void renderInInventory(float x, float y, float w, float h, SpriteBatch batch);
	}

	/**
	 * Interface that when is implemented to some item type allows item type to be rendered in an hand
	 * f.e. holding block in an hand, weapon etc.
	 */
	public interface InHandItemRenderer {

		/**
		 * Method to render in hand item like block, weapon etc.
		 * Affine2 transform variable contains all info and is ready to be used in rendering process (contains x, y, angle and direction)
		 * So in rendering process user can use transform or just other given variables or mix of them whatever
		 * @param x global x position at which item should be rendered
		 * @param y global y position at which item should be rendered
		 * @param angle global angle in degree at which item should be rendered
		 * @param direction direction telling user in which direction player is oriented right now (-1 left, 1 right)
		 * @param player_instance instance of an player for whatever purpose
		 * @param transform transform that contains information about whole transformation how to render item in hand (so can be used directly with batch without any other variables given in that method)
		 * @param batch sprite batch instance used to render this item
		 */
		void render(float x, float y, float angle, int direction, Player player_instance, Affine2 transform, SpriteBatch batch);

		/**
		 * If true first will be rendered arm next this, otherwise first will be rendered in hand item then arm
		 * Useful to determining if arm should be over or under arm
		 * @return true first will be rendered arm next this, false first will be rendered in hand item then arm
		 */
		boolean firstArmThenTool();
	}

	/**
	 * Flag informing if item can be dropped on ground
	 */
	protected boolean dropable;

	/**
	 * Flag informing if this item is stackable one
	 */
	protected boolean stackable;

	/**
	 * Variables used only if stackable == true determines how much items can be in one stack of items
	 */
	protected int max_in_stack;

	/**
	 * TextureRegion used in rendering item in inventory
	 */
	protected TextureRegion item_icon;

	/**
	 * TextureRegion used in rendering item when it lays on an ground
	 */
	protected TextureRegion item_on_ground_texture;

	/**
	 * Custom item property or properties
	 */
	protected String item_property = "";

	/**
	 * Unique per item type ID of an object
	 */
	protected int ID;

	/**
	 * Game instance for assets etc.
	 */
	protected Game game;
	
	public Item(Game game) {
		this.game = game;

		ID = ItemsIDAssigner.getID(getClass());
	}

	/**
	 * Getter for item id
	 * @return item id
	 */
	public int getItemID() {
		return ID;
	}

	/**
	 * Getter for dropable flag
	 * @return true if item can be dropped on ground
	 */
	public boolean isDropable() {
		return dropable;
	}

	/**
	 * Getter for stackable flag
	 * @return true if item is stackable
	 */
	public boolean isStackable() {
		return stackable;
	}

	/**
	 * Getter for max in stack variable
	 * @return used in only if isStackable() == true determines how much items can be in one stack
	 */
	public int getMaxInStack() {
		return max_in_stack;
	}

	/**
	 * Getter for texture region of that item when is rendered in inventory
	 * @return get texture region representing icon this item
	 */
	public TextureRegion getItemIcon() {
		return item_icon;
	}

	/**
	 * Getter for texture region which represents item when it is laying on an ground
	 * @return get texture region representing item when it is laying on an ground
	 */
	public TextureRegion getItemOnGroundTexture() {
		return item_on_ground_texture;
	}

	/**
	 * Getter for custom item property which is stored in string
	 * @return string which contains item property
	 */
	public String getItemProperty() {
		return item_property;
	}

	/**
	 * Setter for custom item property
	 * @param item_property new item property
	 */
	public void setItemProperty(String item_property) {
		this.item_property = item_property;
	}
}
