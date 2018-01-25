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
 * Class responsible for rendering inventory slots (only inventory not tools inventory or wearables one) in PlayerInventoryScreen
 * Created by RYZEN on 21.01.2018.
 */

public class PlayerInventoryRenderer extends GUIComponent {

    /**
     * Simple renderer class for ItemsStack instances
     */
    public static class ItemRenderer {

        /**
         * Items stack that this renderer represents
         */
        protected ItemsContainer.ItemsStack item;

        /**
         * Parent of this items stack slot
         */
        protected ItemsContainer item_parent;
        /**
         * Index of this items stack in parents ItemsContainer
         */
        protected int items_stack_index;

        /**
         * Rendering variables, position vector and width&height vector
         */
        protected Vector2 position, wh;

        /**
         * Font used to render items in stack count
         */
        protected BitmapFont item_count_font;

        /**
         * Background texture region of this inventory slot
         */
        protected TextureRegion slot_background;

        /**
         * Size of background texture (background texture f.e. width will be WH.x + slot_background_size)
         */
        protected final static float slot_background_size = 10;

        /**
         * Construct new items stack renderer
         * @param position local positions of new renderer
         * @param wh wh of new renderer
         * @param items_stack_index index in item_parent container where this stack is stored
         * @param item_parent container in which this items stack is stored
         * @param slot_background background texture region
         * @param game game instance for assets loading
         */
        public ItemRenderer(Vector2 position, Vector2 wh, int items_stack_index, ItemsContainer item_parent, TextureRegion slot_background, Game game) {
            this.position = position;
            this.wh = wh;

            this.items_stack_index = items_stack_index;
            this.item_parent = item_parent;

            this.slot_background = slot_background;

            item_count_font = game.getAssetsManager().getFont("fonts/pixel_font.ttf", 10);
        }

        /**
         * Copy method
         * @param item items stack from which informations will be copied
         * @param item_parent new parent of this stack (this info is not copied from item instance)
         */
        public void setItemsStack(ItemsContainer.ItemsStack item, ItemsContainer item_parent) {
            this.item = item;
            this.item_parent = item_parent;
        }

        /**
         * Update
         * @param delta delta time
         */
        public void tick(float delta) {}

        /**
         * Render method
         * @param batch sprite batch instance
         * @param offset offset at which this item will be rendered (final pos on screen = local_position_of_renderer + offset)
         */
        public void render(SpriteBatch batch, Vector2 offset) {
            render(batch, offset, getLocalPosition(), true, Color.WHITE);
        }

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

        /**
         * Getter for local position of renderer
         * @return local renderer position
         */
        public Vector2 getLocalPosition() {
            return position;
        }

        /**
         * Getter for width & height vector (x = width, y = height )
         * @return width & height vector
         */
        public Vector2 getWH() {
            return wh;
        }

        /**
         * Getter for in items container slot index which this renderer represents
         * @return index in items container slot which this renderer represents
         */
        public int getItemsContainerIndex() {
            return items_stack_index;
        }

        /**
         * Getter for parent container of this renderer (this items stack)
         * @return parent container of this renderer (this items stack)
         */
        public ItemsContainer getItemParent() {
            return item_parent;
        }

        /**
         * Getter for Items Stack which this renderer represents
         * @return Items Stack which this renderer represents
         */
        public ItemsContainer.ItemsStack getItemsStack() {
            return item;
        }

    }

    /**
     * Player reference
     */
    private Player player;

    /**
     * planet screen reference
     */
    private PlanetScreen planet_screen;

    /**
     * Array that hold all item renderers
     */
    private Array<ItemRenderer> items_renderers;

    /**
     * Boolean which tells if items renderers were initializated
     */
    private boolean initialized = false;

    /**
     * Rendering properties, offsets
     */
    public static final float RENDERER_WIDTH = 64;
    public static final float RENDERER_HEIGHT = 64;
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

    /**
     * Getter for array that contains all ItemRenderers
     * @return
     */
    public Array<PlayerInventoryRenderer.ItemRenderer> getRenderers() {
        return items_renderers;
    }
}
