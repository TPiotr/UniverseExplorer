package explorer.world.object.objects.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.object.HierarchicalWorldObject;

/**
 * Created by RYZEN on 19.10.2017.
 */

public class PlayerTextureHierarchicalWorldObject extends HierarchicalWorldObject {

    private TextureRegion texture_region;

    public PlayerTextureHierarchicalWorldObject(TextureRegion region, Vector2 position, Vector2 wh, Vector2 origin, float rotation, HierarchicalWorldObject parent, World world, Game game) {
        super(position, wh, origin, rotation, parent, world, game);

        this.texture_region = region;

        //temp_scale = new Vector2();
        //temp_pos = new Vector2();
    }

    public void render(SpriteBatch batch) {
        super.render(batch);

        batch.draw(texture_region, getPosition().x, getPosition().y, getWH().x * getOrigin().x, getWH().y * getOrigin().y, getWH().x, getWH().y, 1, 1, getRotation());
    }
}
