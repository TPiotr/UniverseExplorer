package explorer.world.object.objects.player.gui;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.HashMap;

import explorer.game.framework.Game;
import explorer.game.screen.gui.GUIComponent;
import explorer.game.screen.screens.planet.PlanetScreen;
import explorer.world.inventory.Item;
import explorer.world.inventory.ItemsContainer;
import explorer.world.inventory.item_types.BodyWearableItem;
import explorer.world.inventory.item_types.WearableItem;
import explorer.world.object.objects.player.Player;

/**
 * Created by RYZEN on 22.01.2018.
 */

public class PlayerWearablesInventoryRenderer extends GUIComponent {

    private PlanetScreen planet_screen;

    private ItemsContainer wearables_container;
    private Array<PlayerInventoryRenderer.ItemRenderer> items_renderers;

    //hashmap that contains info about what slot which type of items can accept
    private HashMap<Integer, WearableItem.WearableType> required_item_types;

    public static final Vector2 OFFSET = new Vector2(380, 255);

    public PlayerWearablesInventoryRenderer(Viewport component_game_viewport, PlanetScreen planet_screen, Game game) {
        super(component_game_viewport, game);

        this.planet_screen = planet_screen;

        items_renderers = new Array<PlayerInventoryRenderer.ItemRenderer>();
        wearables_container = new ItemsContainer(3);

        //fill up item types
        required_item_types = new HashMap<Integer, WearableItem.WearableType>();
        required_item_types.put(0, WearableItem.WearableType.HEAD_ARMOR);
        required_item_types.put(1, WearableItem.WearableType.TORSO_ARMOR);
        required_item_types.put(2, WearableItem.WearableType.LEGS_ARMOR);

        //create renderers
        for(int i = 0; i < wearables_container.getItems().size; i++) {
            float x = 0;
            float y = i * -(PlayerInventoryRenderer.RENDERER_HEIGHT + PlayerInventoryRenderer.RENDERERS_SPACING);

            PlayerInventoryRenderer.ItemRenderer renderer = new PlayerInventoryRenderer.ItemRenderer(new Vector2(x, y), new Vector2(PlayerInventoryRenderer.RENDERER_WIDTH, PlayerInventoryRenderer.RENDERER_HEIGHT), i, wearables_container, game.getAssetsManager().getTextureRegion("inventory/slot_background"), game);
            renderer.setItemsStack(wearables_container.getItems().get(i), wearables_container);
            items_renderers.add(renderer);
        }

    }

    @Override
    public void tick(float delta) {
        for(int i = 0; i < items_renderers.size; i++) {
            items_renderers.get(i).setItemsStack(wearables_container.getItems().get(i), wearables_container);
            items_renderers.get(i).tick(delta);
        }

        //update players wear items
        if(planet_screen.getWorld() != null && planet_screen.getWorld().getPlayer() != null) {
            Item head = (getItemsContainer().getItems().get(0) != null) ? getItemsContainer().getItems().get(0).getItem() : null;
            if(head != null) {
                planet_screen.getWorld().getPlayer().getPlayerRenderer().setWearHeadItem((WearableItem) head);
            } else {
                planet_screen.getWorld().getPlayer().getPlayerRenderer().setWearHeadItem(null);
            }

            Item body = (getItemsContainer().getItems().get(1) != null) ? getItemsContainer().getItems().get(0).getItem() : null;
            if(body != null) {
                planet_screen.getWorld().getPlayer().getPlayerRenderer().setWearBodyItem((BodyWearableItem) body);
            } else {
                planet_screen.getWorld().getPlayer().getPlayerRenderer().setWearBodyItem(null);
            }

            Item legs = (getItemsContainer().getItems().get(2) != null) ? getItemsContainer().getItems().get(0).getItem() : null;
            if(legs != null) {
                planet_screen.getWorld().getPlayer().getPlayerRenderer().setWearLegsItem((WearableItem) legs);
                planet_screen.getWorld().getPlayer().getPlayerRenderer().updateLegs();
            } else {
                planet_screen.getWorld().getPlayer().getPlayerRenderer().setWearLegsItem(null);
            }
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        for(int i = 0; i < items_renderers.size; i++) {
            items_renderers.get(i).render(batch, OFFSET);
        }
    }

    public boolean isAcceptingItem(Item item, int what_slot) {
        if(!(item instanceof WearableItem))
            return false;

        WearableItem.WearableType type = required_item_types.get(what_slot);
        return type.equals(((WearableItem) item).getWearableType());
    }

    public Array<PlayerInventoryRenderer.ItemRenderer> getRenderers() {
        return items_renderers;
    }

    public ItemsContainer getItemsContainer() {
        return wearables_container;
    }
}
