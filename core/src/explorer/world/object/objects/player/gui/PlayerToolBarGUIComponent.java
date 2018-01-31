package explorer.world.object.objects.player.gui;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;

import explorer.game.framework.Game;
import explorer.game.framework.utils.math.MathHelper;
import explorer.game.screen.gui.GUIComponent;
import explorer.game.screen.screens.Screens;
import explorer.game.screen.screens.planet.PlanetScreen;
import explorer.world.inventory.Item;
import explorer.world.inventory.ItemsContainer;
import explorer.world.inventory.item_types.BlockItem;
import explorer.world.object.objects.player.Player;

/**
 * Class similar to PlayerInventoryRenderer but responsible for rendering tool bar, see that class to get comments on code
 * Created by RYZEN on 21.01.2018.
 */

public class PlayerToolBarGUIComponent extends GUIComponent {

    private Player player;
    private PlanetScreen planet_screen;

    private Array<PlayerInventoryRenderer.ItemRenderer> items_renderers;
    private PlayerInventoryRenderer.ItemRenderer selected_renderer;

    private boolean initialized = false;

    private static final float RENDERER_WIDTH = 72;
    private static final float RENDERER_HEIGHT = 72;
    private static final float RENDERERS_SPACING = 25;
    public static final Vector2 OFFSET = new Vector2(-((RENDERER_WIDTH + RENDERERS_SPACING) * (Player.TOOLBAR_INVENTORY_SLOTS_COUNT)) / 2f, 250);

    private static final Color SELECTED_COLOR = new Color(.8f, .8f, .8f, 1f);

    /**
     * Custom ItemRenderer which makes items floating, if some object is selected if floats faster to indicate that this item is selected
     */
    private static class FloatingItemRenderer extends PlayerInventoryRenderer.ItemRenderer {

        private float time;
        public float time_mul = 1f;

        public FloatingItemRenderer(Vector2 position, Vector2 wh, int items_stack_index, ItemsContainer item_parent, TextureRegion slot_background, Game game) {
            super(position, wh, items_stack_index, item_parent, slot_background, game);
            time = getLocalPosition().x;
        }

        @Override
        public void tick(float delta) {
            time += delta * time_mul;
        }

        public void render(SpriteBatch batch, Vector2 offset, Vector2 position, boolean background, Color item_color) {
            if(background) {
                batch.setColor(.8f, .8f, .8f, .5f);
                batch.draw(slot_background, position.x + offset.x - slot_background_size / 2f, position.y + offset.y - slot_background_size / 2f, wh.x + slot_background_size, wh.y + slot_background_size);
            }

            batch.setColor(item_color);

            if(item != null && item.getItem() != null) {
                if(!(item.getItem() instanceof Item.InInventoryRenderer)) {
                    batch.draw(item.getItem().getItemIcon(), position.x + offset.x, position.y + offset.y + (MathUtils.cos(time) * 3), wh.x, wh.y);
                } else {
                    ((Item.InInventoryRenderer) item.getItem()).renderInInventory(position.x + offset.x, position.y + offset.y + (MathUtils.cos(time) * 3), wh.x, wh.y, batch);
                }

                if(item.getItem().isStackable()) {
                    item_count_font.draw(batch, "x" + item.getInStack(), position.x + offset.x + 15, position.y + offset.y + 25);
                }
            }
        }
    }

    public PlayerToolBarGUIComponent(PlanetScreen planet_screen, final Viewport viewport, Game game) {
        super(viewport, game);

        this.planet_screen = planet_screen;
        this.items_renderers = new Array<PlayerInventoryRenderer.ItemRenderer>(Player.INVENTORY_SLOTS_COUNT);

        //create input adapter for selecting items
        InputAdapter input = new InputAdapter() {

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if(!isVisible() || !isPlanetGUIScreenVisible() || player == null)
                    return false;

                Vector2 touch_pos = new Vector2(screenX, screenY);
                viewport.unproject(touch_pos);

                return check(touch_pos);
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if(!isVisible() || !isPlanetGUIScreenVisible())
                    return false;

                Vector2 touch_pos = new Vector2(screenX, screenY);
                viewport.unproject(touch_pos);

                return check(touch_pos);
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if(!isVisible() || !isPlanetGUIScreenVisible())
                    return false;

                return false;
            }

            private boolean check(Vector2 touch_pos) {
                for(int i = 0; i < items_renderers.size; i++) {
                    PlayerInventoryRenderer.ItemRenderer renderer = items_renderers.get(i);

                    if(MathHelper.overlaps2Rectangles(touch_pos.x, touch_pos.y, 1, 1,
                            renderer.getLocalPosition().x + OFFSET.x, renderer.getLocalPosition().y + OFFSET.y, renderer.getWH().x, renderer.getWH().y)) {

                        //if previous renderer wasn't null set it's time multipler to normal one
                        if(selected_renderer != null)
                            ((FloatingItemRenderer) selected_renderer).time_mul = 1f;

                        //set new selected renderer and update player
                        selected_renderer = renderer;
                        player.setSelectedItems(selected_renderer.getItemsStack());

                        //to indicate that some renderer is selected make it floating effect faster
                        ((FloatingItemRenderer) selected_renderer).time_mul = 3f;

                        return true;
                    }
                }
                return false;
            }
        };
        game.getInputEngine().addInputProcessor(input);
    }

    private boolean isPlanetGUIScreenVisible() {
        return game.getScreen(Screens.PLANET_GUI_SCREEN_NAME).isVisible();
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
            TextureRegion background_texture = game.getAssetsManager().getTextureRegion("inventory/slot_background");

            //create items renderers
            for(int i = 0; i < Player.TOOLBAR_INVENTORY_SLOTS_COUNT; i++) {
                float x = i * (RENDERER_WIDTH + RENDERERS_SPACING);

                FloatingItemRenderer renderer = new FloatingItemRenderer(new Vector2(x, 0), new Vector2(RENDERER_WIDTH, RENDERER_HEIGHT), i, player.getToolbarItemsContainer(), background_texture, game);
                renderer.setItemsStack(player.getToolbarItemsContainer().getItems().get(i), player.getToolbarItemsContainer());
                items_renderers.add(renderer);
            }

            initialized = true;
        }

        for(int i = 0; i < items_renderers.size; i++) {
            items_renderers.get(i).setItemsStack(player.getToolbarItemsContainer().getItems().get(i), player.getToolbarItemsContainer());
            items_renderers.get(i).tick(delta);
        }

        //update player current holding item
        if(selected_renderer != null) {
            player.setSelectedItems(selected_renderer.getItemsStack());
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        if(player == null)
            return;

        for(int i = 0; i < items_renderers.size; i++) {
            if(items_renderers.get(i) == selected_renderer) {
                items_renderers.get(i).render(batch, OFFSET, items_renderers.get(i).getLocalPosition(), true, SELECTED_COLOR);
            } else {
                items_renderers.get(i).render(batch, OFFSET);
            }
        }
        batch.setColor(Color.WHITE);
    }

    public PlayerInventoryRenderer.ItemRenderer getSelectedRenderer() {
        return selected_renderer;
    }

    public Array<PlayerInventoryRenderer.ItemRenderer> getRenderers() {
        return items_renderers;
    }
}
