package explorer.game.screen.screens.planet;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.concurrent.atomic.AtomicInteger;

import explorer.game.framework.Game;
import explorer.game.framework.utils.math.MathHelper;
import explorer.game.screen.Screen;
import explorer.game.screen.gui.TextButton;
import explorer.game.screen.gui.TextureButton;
import explorer.game.screen.screens.Screens;
import explorer.world.inventory.Item;
import explorer.world.inventory.ItemsContainer;
import explorer.world.object.objects.player.Player;
import explorer.world.object.objects.player.gui.PlayerInventoryRenderer;
import explorer.world.object.objects.player.gui.PlayerToolBarGUIComponent;
import explorer.world.object.objects.player.gui.PlayerWearablesInventoryRenderer;

/**
 * Created by RYZEN on 21.01.2018.
 */

public class PlayerInventoryScreen extends Screen {

    private PlayerInventoryRenderer inventory_renderer;
    private PlayerToolBarGUIComponent toolbar_renderer;
    private PlayerWearablesInventoryRenderer wearables_renderer;

    private TextButton back_button;

    private Texture pattern_texture;

    //dragging
    private PlayerInventoryRenderer.ItemRenderer dragging_renderer;
    private Vector2 dragging_position;

    /**
     * Construct new screen instance
     *
     * @param game game instance
     */
    public PlayerInventoryScreen(PlanetScreen planet_screen, final Game game) {
        super(game);

        this.NAME = Screens.PLAYER_INVENTORY_SCREEN;

        dragging_position = new Vector2();

        pattern_texture = game.getAssetsManager().getTexturee("assets/textures/pattern.png", Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pattern_texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        inventory_renderer = new PlayerInventoryRenderer(planet_screen, game.getGUIViewport(), game);
        addScreenComponent(inventory_renderer);

        toolbar_renderer = game.getScreen(Screens.PLANET_GUI_SCREEN_NAME, PlanetGUIScreen.class).getPlayerToolbarGUIComponent();
        addScreenComponent(toolbar_renderer);

        wearables_renderer = new PlayerWearablesInventoryRenderer(game.getGUIViewport(), planet_screen, game);
        addScreenComponent(wearables_renderer);

        back_button = new TextButton(game.getAssetsManager().getFont("fonts/pixel_font.ttf", 16), "Back", new Vector2(-620, -340), game.getGUIViewport(), game);
        addScreenComponent(back_button);

        back_button.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched() {
                setVisible(false);
                game.getScreen(Screens.PLANET_SCREEN_NAME).setVisible(true);
            }

            @Override
            public void released() {}
        });

        InputAdapter input = new InputAdapter() {

            int pointer = -1;

            @Override
            public boolean touchDown(int screenX, int screenY, int p, int button) {
                if(pointer != -1 || !isVisible())
                    return false;

                pointer = p;

                Vector2 touch = new Vector2(screenX, screenY);
                game.getGUIViewport().unproject(touch);

                AtomicInteger component_clicked = new AtomicInteger(-1);
                PlayerInventoryRenderer.ItemRenderer renderer = getOverlappedItemRenderer(touch, component_clicked);

                if(renderer != null && renderer.getItemsStack() != null) {
                    if(component_clicked.get() == 0) {
                        //inventory
                        dragging_renderer = renderer;
                        dragging_position.set(touch).sub(renderer.getWH().x / 2f, renderer.getWH().y / 2f);

                        return true;
                    } else if(component_clicked.get() == 1) {
                        //toolbar
                        dragging_renderer = renderer;
                        dragging_position.set(touch).sub(renderer.getWH().x / 2f, renderer.getWH().y / 2f);

                        return true;
                    }
                }

                return false;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int p, int button) {
                if(!isVisible())
                    return false;

                //check if this is proper touch index, must be same as pressed one some time ago (multitouch support thing)
                if(pointer == p) {
                    pointer = -1;

                    if (dragging_renderer != null && dragging_renderer.getItemsStack() != null) {
                        //get touch up screen pos
                        Vector2 dropdown_cords = new Vector2(screenX, screenY);
                        game.getGUIViewport().unproject(dropdown_cords);

                        //get item renderer on which dragging one was dropped
                        PlayerInventoryRenderer.ItemRenderer dropdown_renderer = getOverlappedItemRenderer(dropdown_cords, null);

                        if (dropdown_renderer != null) {
                            //if player picked up item and dropped it without moving just skip blocks connection/switching operations
                            if(dropdown_renderer.getItemParent() == dragging_renderer.getItemParent()
                                    && dropdown_renderer.getItemsContainerIndex() == dragging_renderer.getItemsContainerIndex()) {

                                //reset dragging renderer
                                dragging_renderer = null;
                                return false;
                            }

                            boolean just_replace = false;

                            //if items are null, not stackable and not the same we just to have replace them with their places
                            if(dropdown_renderer.getItemsStack() == null || !dropdown_renderer.getItemsStack().getItem().isStackable() || !dragging_renderer.getItemsStack().getItem().isStackable()
                                    || !(dropdown_renderer.getItemsStack().getItem().getItemID() == dragging_renderer.getItemsStack().getItem().getItemID())) {

                                just_replace = true;
                            }

                            //if both of items are stackable and have same ID's we know we have to stack them up instead of replacing them
                            else if(dropdown_renderer.getItemsStack().getItem().getItemID() == dragging_renderer.getItemsStack().getItem().getItemID()
                                    && dropdown_renderer.getItemsStack().getItem().isStackable() && dragging_renderer.getItemsStack().getItem().isStackable()) {

                                //try to add items to stack etc.

                                Item item = dropdown_renderer.getItemsStack().getItem();
                                int added_count = dropdown_renderer.getItemsStack().getInStack() + dragging_renderer.getItemsStack().getInStack();

                                if(added_count > item.getMaxInStack()) {
                                    //set dropdown target items stack items in stack count to max possible
                                    //and dragging renderer items in stack count to diff: added_count - max_in_stack
                                    int diff = added_count - item.getMaxInStack();

                                    dragging_renderer.getItemsStack().setInStack(diff);
                                    dropdown_renderer.getItemsStack().setInStack(item.getMaxInStack());
                                } else {
                                    //just set dragging items stack to null and dropdown items stack in stack count to added value
                                    dragging_renderer.getItemParent().getItems().set(dragging_renderer.getItemsContainerIndex(), null);
                                    dropdown_renderer.getItemsStack().setInStack(added_count);
                                }

                            }

                            //this piece of code just replaces two renderers contents
                            if(just_replace) {
                                ItemsContainer.ItemsStack holder = (dropdown_renderer.getItemsStack() != null) ? new ItemsContainer.ItemsStack(dropdown_renderer.getItemsStack()) : null;
                                ItemsContainer.ItemsStack dragging_copy = new ItemsContainer.ItemsStack(dragging_renderer.getItemsStack());

                                //wearables checking part
                                if(dropdown_renderer.getItemParent() == wearables_renderer.getItemsContainer()) {
                                    //check if dragged item is accepted is yes do nothing if not stop replacing process
                                    if(!wearables_renderer.isAcceptingItem(dragging_renderer.getItemsStack().getItem(), dropdown_renderer.getItemsContainerIndex())) {
                                        //reset dragging item and return
                                        dragging_renderer = null;
                                        return false;
                                    }
                                }

                                //copy dragging items to dropdown target
                                ItemsContainer dropdown_parent = dropdown_renderer.getItemParent();
                                if (dropdown_renderer.getItemsStack() != null) {
                                    dropdown_renderer.getItemsStack().set(dragging_renderer.getItemsStack());
                                    dropdown_renderer.getItemsStack().setParent(dropdown_parent);
                                } else {
                                    //if current dropdown target is null we have to change its items stack to totally new one
                                    ItemsContainer.ItemsStack new_value = new ItemsContainer.ItemsStack(dragging_renderer.getItemsStack());
                                    new_value.setParent(dropdown_parent);
                                    dropdown_renderer.getItemParent().getItems().set(dropdown_renderer.getItemsContainerIndex(), new_value);
                                }

                                //check if dropdown target is null
                                if (holder != null) {
                                    dragging_renderer.getItemsStack().set(holder);
                                    dragging_renderer.getItemsStack().setParent(dragging_copy.getParent());
                                } else {
                                    //if dropdown target was null we have to set dragging items stack to null too
                                    dragging_renderer.getItemParent().getItems().set(dragging_renderer.getItemsContainerIndex(), null);
                                }
                            }
                        }

                        //reset dragging renderer
                        dragging_renderer = null;
                    }
                }

                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int p) {
                if(!isVisible())
                    return false;

                if(pointer == p && dragging_renderer != null) {
                    dragging_position.set(screenX, screenY);
                    game.getGUIViewport().unproject(dragging_position);
                    dragging_position.sub(dragging_renderer.getWH().x / 2f, dragging_renderer.getWH().y / 2f);
                }

                return false;
            }
        };
        game.getInputEngine().addInputProcessor(input);
    }

    private PlayerInventoryRenderer.ItemRenderer getOverlappedItemRenderer(Vector2 position, AtomicInteger component_clicked) {
        //first check if some item renderer in inventory is pressed
        for(int i = 0; i < inventory_renderer.getRenderers().size; i++) {
            PlayerInventoryRenderer.ItemRenderer renderer = inventory_renderer.getRenderers().get(i);

            if(MathHelper.overlaps2Rectangles(renderer.getLocalPosition().x + PlayerInventoryRenderer.OFFSET.x, renderer.getLocalPosition().y + PlayerInventoryRenderer.OFFSET.y, renderer.getWH().x, renderer.getWH().y,
                    position.x, position.y, 1, 1)) {

                if(component_clicked != null)
                    component_clicked.set(0);

                return renderer;
            }
        }

        //second toolbar
        for(int i = 0; i < toolbar_renderer.getRenderers().size; i++) {
            PlayerInventoryRenderer.ItemRenderer renderer = toolbar_renderer.getRenderers().get(i);

            if(MathHelper.overlaps2Rectangles(renderer.getLocalPosition().x + PlayerToolBarGUIComponent.OFFSET.x, renderer.getLocalPosition().y + PlayerToolBarGUIComponent.OFFSET.y, renderer.getWH().x, renderer.getWH().y,
                    position.x, position.y, 1, 1)) {

                if(component_clicked != null)
                    component_clicked.set(1);

                return renderer;
            }
        }

        //third wearables
        for(int i = 0; i < wearables_renderer.getRenderers().size; i++) {
            PlayerInventoryRenderer.ItemRenderer renderer = wearables_renderer.getRenderers().get(i);

            if(MathHelper.overlaps2Rectangles(renderer.getLocalPosition().x + PlayerWearablesInventoryRenderer.OFFSET.x, renderer.getLocalPosition().y + PlayerWearablesInventoryRenderer.OFFSET.y, renderer.getWH().x, renderer.getWH().y,
                    position.x, position.y, 1, 1)) {

                if(component_clicked != null)
                    component_clicked.set(1);

                return renderer;
            }
        }

        return null;
    }

    @Override
    public void tick(float delta) {
        if (!isVisible())
            return;

        super.tickComponents(delta);
    }

    @Override
    public void render(SpriteBatch batch) {
        if (!isVisible())
            return;

        game.getScreen(Screens.PLANET_SCREEN_NAME).render(batch);

        batch.setProjectionMatrix(game.getGUICamera().combined);
        batch.setColor(.6f, .6f, .6f, .6f);
        batch.draw(pattern_texture, -1000, -1000,0, 0, 2000, 2000);

        batch.setColor(Color.WHITE);
        super.renderComponents(batch);

        if(dragging_renderer != null) {
            dragging_renderer.render(batch, Vector2.Zero, dragging_position, false, Color.WHITE);
        }
    }

    @Override
    public void dispose() {

    }
}
