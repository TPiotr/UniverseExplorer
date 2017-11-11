package explorer.world.background;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class BackgroundLayer {
	
	//simply class that holds texture and speed factor
	private TextureRegion texture;
	private Vector2 move_factor;
	
	private Vector2 position;
	private Vector2 wh;
	
	private Background background;
	
	private Vector2 base_velocity;
	
	public BackgroundLayer(TextureRegion texture, Vector2 move_factor, Vector2 position, Background background) {
		this.texture = texture;
		this.move_factor = move_factor;
		this.position = position;
		this.background = background;
		this.wh = new Vector2(texture.getRegionWidth(), texture.getRegionHeight());
		base_velocity = new Vector2();
	}
	
	public BackgroundLayer(TextureRegion texture, Vector2 move_factor, Vector2 position, Vector2 wh, Background background) {
		this.texture = texture;
		this.move_factor = move_factor;
		this.position = position;
		this.wh = wh;
		this.background = background;
		this.base_velocity = new Vector2();
	}
	
	public void render(SpriteBatch batch) {
		batch.draw(texture, position.x + background.getPosition().x, position.y + background.getPosition().y, wh.x, wh.y);
	}
	
	//getters
	public TextureRegion getTexture() {
		return texture;
	}
	public Vector2 getMoveFactor() {
		return move_factor;
	}
	
	public Vector2 getPosition() {
		return position;
	}
	public Vector2 getWH() {
		return wh;
	}
	
	public Vector2 getBaseVelocity() {
		return base_velocity;
	}
}
