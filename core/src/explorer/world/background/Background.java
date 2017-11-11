package explorer.world.background;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class Background {

	private Vector2 position;

	//list of this backgronund layers
	private Array<BackgroundLayer> layers;
	private Vector2 velocity;
	
	private Vector2 tmp_vec2 = new Vector2();

	private Vector3 background_color;

	public Background() {
		position = new Vector2();

		layers = new Array<BackgroundLayer>();
		velocity = new Vector2();

		background_color = new Vector3(1, 1, 1);
	}

	public Background(Vector2 position) {
		this.position = position;

		layers = new Array<BackgroundLayer>();
		velocity = new Vector2();

		background_color = new Vector3(1, 1, 1);
	}
	
	public Background(Vector2 position, BackgroundLayer... background_layers) {
		this.position = position;

		layers = new Array<BackgroundLayer>();
		velocity = new Vector2();
		
		for(BackgroundLayer layer : background_layers) {
			layers.add(layer);
		}

		background_color = new Vector3(1, 1, 1);
	}

	//func to add new layer
	public void addLayer(BackgroundLayer layer) {
		layers.add(layer);
	}
	
	//func that move all layers by velocity * layer move_factor
	public void update(Vector2 velocity) {
		this.velocity = velocity;
	}
	
	public void tick(float delta) {	
		for(int i = 0; i < layers.size; i++) {
			BackgroundLayer layer = layers.get(i);
			tmp_vec2.set(layer.getPosition());

			layer.getPosition().add(new Vector2(velocity).scl(layer.getMoveFactor()).scl(delta));
			layer.getPosition().add(layer.getBaseVelocity().cpy().scl(delta));
		}
	}

	public void render(SpriteBatch batch) {
		for(int i = 0; i < layers.size; i++) {
			layers.get(i).render(batch);
		}
	}

	public void dispose() {}

	public Vector2 getPosition() {
		return position;
	}

	public Vector3 getColor() {
		return background_color;
	}
}
