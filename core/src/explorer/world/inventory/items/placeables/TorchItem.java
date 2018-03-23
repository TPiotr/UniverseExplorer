package explorer.world.inventory.items.placeables;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.minlog.Log;

import explorer.game.Helper;
import explorer.game.framework.Game;
import explorer.game.screen.screens.Screens;
import explorer.game.screen.screens.planet.PlanetScreen;
import explorer.world.World;
import explorer.world.inventory.Item;
import explorer.world.inventory.item_types.PlaceableItem;
import explorer.world.object.objects.TorchObject;
import explorer.world.object.objects.player.Player;

/**
 * Created by RYZEN on 26.02.2018.
 */

public class TorchItem extends PlaceableItem implements Item.InInventoryRenderer {


    public TorchItem(Game game) {
        super(game);

        ID = 10000;

        stackable = true;
        max_in_stack = 16;

        item_icon = game.getAssetsManager().getTextureRegion("white_texture");
        item_on_ground_texture = item_icon;
    }

    @Override
    public boolean place(Vector2 world_position, boolean foreground_placing_mode, World world, Game game) {
        if(canPlaceObject(world_position, 32, 32, world, game)) {
            TorchObject torch = new TorchObject(new Vector2(world_position), world, game);
            world.addObject(torch, true);
            return true;
        }

        return false;
    }

    @Override
    public void renderSilhouette(SpriteBatch batch, Vector2 world_cords) {


    }

    @Override
    public void renderInInventory(float x, float y, float w, float h, SpriteBatch batch) {
        batch.draw(item_icon, x, y, w, h);
    }
}
