package explorer.world.object.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import explorer.game.Helper;
import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.lighting.lights.PointLight;
import explorer.world.object.StaticWorldObject;
import explorer.world.object.WorldObject;

/**
 * Created by RYZEN on 23.12.2017.
 */

public class TorchObject extends WorldObject {

    private PointLight light;

    private static Animation<TextureRegion> torch_animation;

    private float time;

    public TorchObject(Vector2 position, World world, Game game) {
        super(position, world, game);

        final float scale = 3f;
        wh.set(6, 14).scl(scale);

        if (torch_animation == null) {
            torch_animation = Helper.makeAnimation(6, 14, .4f, game.getAssetsManager().getTextureRegion("objects/torch_texture"));
        }

        light = new PointLight(new Vector2(getPosition().x + getWH().x / 2f, getPosition().y + getWH().y / 2f), new Vector3(1, 1, 1), 50 * scale, game);
        world.getLightEngine().addLight(light);
    }

    @Override
    public void tick(float delta) {
        time += delta;
    }

    @Override
    public void render(SpriteBatch batch) {
        batch.draw(torch_animation.getKeyFrame(time), getPosition().x, getPosition().y, getWH().x, getWH().y);
    }

    @Override
    public void dispose() {
        world.getLightEngine().removeLight(light);
    }
}
