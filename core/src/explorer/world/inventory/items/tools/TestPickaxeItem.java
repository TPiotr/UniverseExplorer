package explorer.world.inventory.items.tools;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;

import explorer.game.framework.Game;
import explorer.world.inventory.Item;
import explorer.world.inventory.item_types.ToolItem;
import explorer.world.object.objects.player.Player;

/**
 * Created by RYZEN on 24.01.2018.
 */

public class TestPickaxeItem extends ToolItem implements Item.InHandItemRenderer {

    private TextureRegion pickaxe_region;

    private Affine2 offset;
    private float w, h;

    public TestPickaxeItem(Game game) {
        super(game);

        this.item_icon = game.getAssetsManager().getTextureRegion("inventory/items/test_pickaxe_item_icon");
        this.item_on_ground_texture = item_icon;

        this.dropable = true;
        this.stackable = false;

        this.pickaxe_region = game.getAssetsManager().getTextureRegion("inventory/items/test_pickaxe_item");

        this.tool_power = 10f;

        float SCALE = 2f;
        w = pickaxe_region.getRegionWidth() * SCALE;
        h = pickaxe_region.getRegionHeight() * SCALE;

        offset = new Affine2().idt().rotate(70).translate(-w / 4f, -h / 2f);
    }

    @Override
    public void render(float x, float y, float angle, int direction, Player player_instance, Affine2 transform, SpriteBatch batch) {
        transform.mul(offset);
        batch.draw(pickaxe_region, w, h, transform);
    }

    @Override
    public boolean firstArmThenTool() {
        return false;
    }
}
