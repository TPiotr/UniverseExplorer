package explorer.world.object.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

import java.util.concurrent.atomic.AtomicBoolean;

import explorer.game.Helper;
import explorer.game.framework.Game;
import explorer.network.NetworkClasses;
import explorer.world.World;
import explorer.world.lighting.CustomWorldObjectLight;
import explorer.world.lighting.SolidColorWorldObjectLight;
import explorer.world.object.StaticWorldObject;
import explorer.world.object.WorldObject;
import explorer.world.physics.shapes.RectanglePhysicsShape;

/**
 * Created by RYZEN on 11.01.2018.
 */

public class TreeObject extends WorldObject implements SolidColorWorldObjectLight {

    private static TextureRegion texture;
    private final float SCALE = .5f;

    private static final Color object_light_mask_color = new Color(Color.WHITE);

    private Vector2 render_position;

    //register packets
    static {
        //NetworkClasses.register();
        System.out.println("Registering tree packets!");
    }

    public TreeObject(Vector2 position, World world, final Game game) {
        super(position, world, game);

        synchronized (this) {
            texture = game.getAssetsManager().getTextureRegion("objects/tree_earth1");
            getWH().set(texture.getRegionWidth(), texture.getRegionHeight()).scl(SCALE);

            render_position = new Vector2(position);
            render_position.x -= (((int) (getWH().x / 2f) / World.BLOCK_SIZE) - 1) * World.BLOCK_SIZE;
        }
    }

    @Override
    public Color getSolidColorOfLightMask() {
        return object_light_mask_color;
    }

    @Override
    public void tick(float delta) {

    }

    @Override
    public void render(SpriteBatch batch) {
        batch.setColor(Color.WHITE);
        batch.draw(texture, render_position.x, render_position.y, getWH().x, getWH().y);
    }

    @Override
    public void dispose() {

    }
}
