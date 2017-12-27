package explorer.world.object.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.object.DynamicWorldObject;
import explorer.world.physics.shapes.RectanglePhysicsShape;

/**
 * Created by RYZEN on 27.10.2017.
 */

public class TestDynamicObject extends DynamicWorldObject {

    private static TextureRegion texture;

    public TestDynamicObject(Vector2 position, World world, Game game) {
        super(position, world, game);

        if(texture == null)
            texture = game.getAssetsManager().getTextureRegion("badlogic");

        getWH().set(World.BLOCK_SIZE, World.BLOCK_SIZE);

        physics_shape = new RectanglePhysicsShape(new Vector2(0, 0), getWH(), this);
    }

    @Override
    public void tick(float delta) {
        if(getVelocity().y == 0) {
            getVelocity().y = 500f;
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        batch.setColor(Color.WHITE);
        batch.draw(texture, getPosition().x, getPosition().y, getWH().x, getWH().y);
    }

    @Override
    public void dispose() {

    }
}
