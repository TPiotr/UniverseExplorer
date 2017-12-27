package explorer.world.object.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.object.StaticWorldObject;
import explorer.world.physics.shapes.RectanglePhysicsShape;

/**
 * Created by RYZEN on 12.10.2017.
 */

public class TestObject extends StaticWorldObject {

    private static TextureRegion texture;

    public TestObject(Vector2 position, World world, Game game) {
        super(position, world, game);

        if(texture == null)
            texture = game.getAssetsManager().getTextureRegion("badlogic");

        getWH().set(128, 128);

        physics_shape = new RectanglePhysicsShape(new Vector2(0, 0), getWH(), this);
    }

    @Override
    public void tick(float delta) {

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
