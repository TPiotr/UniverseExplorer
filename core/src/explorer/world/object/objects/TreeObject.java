package explorer.world.object.objects;

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

public class TreeObject extends WorldObject {

    private static TextureRegion trunk, top;
    private Vector2 render_position;

    private static final float SCALE = 8f;

    private Color grass_color;

    /**
     * The most basic constructor that every object have to have!
     *
     * @param position position of new object
     * @param world    world instance
     * @param game     game instance
     */
    public TreeObject(Vector2 position, World world, Game game) {
        super(position, world, game);

        if(trunk == null) {
            trunk = game.getAssetsManager().getTextureRegion("objects/tree/tree_trunk2");
            top = game.getAssetsManager().getTextureRegion("objects/tree/tree_top2");
        }

        grass_color = world.getBlocks().GRASS.getGrassColor();

        getWH().set(trunk.getRegionWidth(), trunk.getRegionHeight()).scl(SCALE);

        render_position = new Vector2(position);
        render_position.x -= (((int) (getWH().x / 2f) / World.BLOCK_SIZE) - 1) * World.BLOCK_SIZE;
    }

    @Override
    public void tick(float delta) {

    }

    @Override
    public void render(SpriteBatch batch) {
        batch.setColor(Color.WHITE);
        batch.draw(trunk, render_position.x, render_position.y, getWH().x, getWH().y);

        //batch.setColor(grass_color);
       // batch.draw(top, render_position.x, render_position.y, getWH().x, getWH().y);

//        batch.setColor(Color.WHITE);
    }

    //@Override
    public Color getSolidColorOfLightMask() {
        return Color.WHITE;
    }

    @Override
    public void dispose() {

    }
}
