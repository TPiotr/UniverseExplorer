package explorer.world.inventory;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import explorer.game.framework.Game;

public abstract class Item {
	
	//can lay on ground and can be pickable
	protected boolean dropable;
	
	protected boolean stackable;
	protected int max_in_stack;

	protected TextureRegion item_icon;
	protected TextureRegion item_on_ground_texture;
	
	protected int ID;
	
	protected Game game;
	
	public Item(Game game) {
		this.game = game;

		ID = ItemsIDAssigner.getID(getClass());
	}
	
	//getters
	public int getItemID() {
		return ID;
	}
	
	public boolean isDropable() {
		return dropable;
	}
	
	public boolean isStackable() {
		return stackable;
	}
	public int getMaxInStack() {
		return max_in_stack;
	}
	
	public TextureRegion getItemIcon() {
		return item_icon;
	}
	public TextureRegion getItemOnGroundTexture() {
		return item_on_ground_texture;
	}
}
