package explorer.world.inventory;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;
import explorer.world.object.objects.player.Player;

public abstract class Item {

	public interface InHandItemRenderer {
		void render(float x, float y, float angle, int direction, Player player_instance, Affine2 transform, SpriteBatch batch);
	}

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
