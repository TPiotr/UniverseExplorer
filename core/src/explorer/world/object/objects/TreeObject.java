package explorer.world.object.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.lighting.CustomWorldObjectLight;
import explorer.world.object.StaticWorldObject;
import explorer.world.object.WorldObject;
import explorer.world.physics.shapes.RectanglePhysicsShape;

/**
 * Created by RYZEN on 11.01.2018.
 */

public class TreeObject extends WorldObject implements CustomWorldObjectLight {

    private static TextureRegion texture, white_texture;
    private boolean first_tick = true;

    private final float SCALE = .5f;

    public TreeObject(Vector2 position, World world, Game game) {
        super(position, world, game);

        synchronized (this) {
            if (texture == null) {
                texture = game.getAssetsManager().getTextureRegion("objects/tree_earth1");
                white_texture = game.getAssetsManager().getTextureRegion("white_texture");
            }

            getWH().set(texture.getRegionWidth(), texture.getRegionHeight()).scl(SCALE);
        }
    }

    @Override
    public void tick(float delta) {
        //center tree on block at first tick of world
        if(first_tick) {
            getPosition().x -= (((int) (getWH().x / 2f) / World.BLOCK_SIZE) - 1) * World.BLOCK_SIZE;
            first_tick = false;
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        batch.setColor(Color.WHITE);
        batch.draw(texture, getPosition().x, getPosition().y, getWH().x, getWH().y);
    }

    @Override
    public void renderCustomLight(SpriteBatch batch) {
        batch.draw(white_texture, getPosition().x, getPosition().y, getWH().x, getWH().y);
    }

    @Override
    public void dispose() {

    }

}
