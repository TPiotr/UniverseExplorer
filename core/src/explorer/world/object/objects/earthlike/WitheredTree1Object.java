package explorer.world.object.objects.earthlike;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.lighting.SolidColorWorldObjectLight;
import explorer.world.object.WorldObject;

/**
 * Created by RYZEN on 01.02.2018.
 */

public class WitheredTree1Object extends WorldObject implements SolidColorWorldObjectLight {

    private static TextureRegion trunk;

    private static final float SCALE = 8f;
    public static final float CENTER_OFFSET = SCALE * 15;

    public WitheredTree1Object(Vector2 position, World world, Game game) {
        super(position, world, game);

        if(trunk == null) {
            trunk = game.getAssetsManager().getTextureRegion("objects/tree/withered_tree1");
        }

        getWH().set(trunk.getRegionWidth(), trunk.getRegionHeight()).scl(SCALE);
    }

    @Override
    public void tick(float delta) {

    }

    @Override
    public void render(SpriteBatch batch) {
        batch.setColor(Color.WHITE);
        batch.draw(trunk, getPosition().x, getPosition().y, getWH().x, getWH().y);
    }

    @Override
    public Color getSolidColorOfLightMask() {
        return Color.WHITE;
    }

    @Override
    public void dispose() {

    }
}
