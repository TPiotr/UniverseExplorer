package explorer.world.object.objects.player.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.esotericsoftware.minlog.Log;

import explorer.game.framework.Game;
import explorer.game.screen.gui.GUIComponent;
import explorer.game.screen.screens.planet.PlanetScreen;
import explorer.world.inventory.Item;
import explorer.world.inventory.ItemsContainer;
import explorer.world.object.objects.player.Player;

/**
 * Created by RYZEN on 21.01.2018.
 */

public class PlayerInventoryRenderer extends GUIComponent {

    public static class ItemRenderer {

        protected ItemsContainer.ItemsStack item;
        protected ItemsContainer item_parent;
        protected int items_stack_index;

        protected Vector2 position, wh;

        protected BitmapFont item_count_font;

        protected TextureRegion slot_background;

        public ItemRenderer(Vector2 position, Vector2 wh, int items_stack_index, ItemsContainer item_parent, TextureRegion slot_background, Game game) {
            this.position = position;
            this.wh = wh;

            this.items_stack_index = items_stack_index;
            this.item_parent = item_parent;

            this.slot_background = slot_background;

            item_count_font = game.getAssetsManager().getFont("fonts/pixel_font.ttf", 10);
        }

        public void setItemsStack(ItemsContainer.ItemsStack item, ItemsContainer item_parent) {
            this.item = item;
            this.item_parent = item_parent;
        }

        public void tick(float delta) {}

        public void render(SpriteBatch batch, Vector2 offset) {
            render(batch, offset, getLocalPosition(), true, Color.WHITE);
        }

        protected final static float slot_background_size = 10;

        public void render(SpriteBatch batch, Vector2 offset, Vector2 position, boolean background, Color item_color) {
            if(background) {
                batch.setColor(.8f, .8f, .8f, .5f);
                batch.draw(slot_background, position.x + offset.x - slot_background_size / 2f, position.y + offset.y - slot_background_size / 2f, wh.x + slot_background_size, wh.y + slot_background_size);
            }

            batch.setColor(item_color);

            if(item != null) {
                batch.draw(item.getItem().getItemIcon(), position.x + offset.x, position.y + offset.y, wh.x, wh.y);

                if(item.getItem().isStackable()) {
                    item_count_font.draw(batch, "x" + item.getInStack(), position.x + offset.x + 15, position.y + offset.y + 25);
                }
            }
        }

        public Vector2 getLocalPosition() {
            return position;
        }

        public Vector2 getWH() {
            return wh;
        }

        public int getItemsContainerIndex() {
            return items_stack_index;
        }

        public ItemsContainer getItemParent() {
            return item_parent;
        }

        public ItemsContainer.ItemsStack getItemsStack() {
            return item;
        }

    }

    private Player player;
    private PlanetScreen planet_screen;

    private Array<ItemRenderer> items_renderers;

    private boolean initialized = false;

    public static final float RENDERER_WIDTH = 64;
    public static  final float RENDERER_HEIGHT = 64;
    public static final float RENDERERS_SPACING = 25;
    public static final int COLUMNS_COUNT = 6;
    public static final Vector2 OFFSET = new Vector2(-((RENDERER_WIDTH + RENDERERS_SPACING) * (COLUMNS_COUNT)) / 2f, -250);

    public PlayerInventoryRenderer(PlanetScreen planet_screen, Viewport component_game_viewport, Game game) {
        super(component_game_viewport, game);

        this.planet_screen = planet_screen;
        this.items_renderers = new Array<ItemRenderer>(Player.INVENTORY_SLOTS_COUNT);
    }

    @Override
    public void tick(float delta) {
        if(planet_screen.getWorld() == null)
            return;

        //update player reference
        player = planet_screen.getWorld().getPlayer();


        //if player is still null skip logic
        if(player == null)
            return;

        if(!initialized) {
            //create items renderers
            for(int i = 0; i < Player.INVENTORY_SLOTS_COUNT; i++) {
                int column = i / COLUMNS_COUNT;
                int row = i % COLUMNS_COUNT;

                float x = row * (RENDERER_WIDTH + RENDERERS_SPACING);
                float y = column * (RENDERER_HEIGHT + RENDERERS_SPACING);

                ItemRenderer renderer = new ItemRenderer(new Vector2(x, y), new Vector2(RENDERER_WIDTH, RENDERER_HEIGHT), i, player.getItemsContainer(), game.getAssetsManager().getTextureRegion("inventory/slot_background"), game);
                renderer.setItemsStack(player.getItemsContainer().getItems().get(i), player.getItemsContainer());
                items_renderers.add(renderer);
            }

            initialized = true;
        }

        for(int i = 0; i < items_renderers.size; i++) {
            items_renderers.get(i).setItemsStack(player.getItemsContainer().getItems().get(i), player.getItemsContainer());
            items_renderers.get(i).tick(delta);
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        if(player == null)
            return;

        for(int i = 0; i < items_renderers.size; i++) {
            items_renderers.get(i).render(batch, OFFSET);
        }
    }

    public Array<PlayerInventoryRenderer.ItemRenderer> getRenderers() {
        return items_renderers;
    }
}
