package explorer.game.screen.screens.planet;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import explorer.game.framework.AssetsManager;
import explorer.game.framework.Game;
import explorer.game.framework.utils.math.MathHelper;
import explorer.game.screen.Screen;
import explorer.game.screen.ScreenComponent;
import explorer.game.screen.gui.GUIComponent;
import explorer.game.screen.gui.TextButton;
import explorer.game.screen.gui.TextureButton;
import explorer.game.screen.screens.Screens;
import explorer.network.client.ServerPlayer;
import explorer.world.World;
import explorer.world.block.Block;
import explorer.world.block.CustomColorBlock;
import explorer.world.block.CustomRenderingBlock;
import explorer.world.inventory.Item;
import explorer.world.inventory.item_types.BlockItem;
import explorer.world.inventory.item_types.ToolItem;
import explorer.world.object.WorldObject;
import explorer.world.object.objects.player.gui.BackgroundForegroundSwitch;
import explorer.world.object.objects.player.gui.PlayerBlockSelectorGUIComponent;
import explorer.world.object.objects.player.gui.PlayerInventoryRenderer;
import explorer.world.object.objects.player.gui.PlayerToolBarGUIComponent;

/**
 * Created by RYZEN on 05.11.2017.
 */

public class PlanetGUIScreen extends Screen {

    private TextureButton left_button, right_button, jump_button, inventory_button;

    private TextureButton pickaxe_button, placeblock_button;
    private BackgroundForegroundSwitch placing_switch;

    //private BlockPlacingSelector blocks_selector;

    private PlayerToolBarGUIComponent toolbar_renderer;
    private PlayerBlockSelectorGUIComponent block_pointer;

    private BitmapFont debug_font;

    private PlanetScreen planet_screen;

    /**
     * Construct new screen instance
     *
     * @param game game instance
     */
    public PlanetGUIScreen(final PlanetScreen planet_screen, final Game game) {
        super(game);

        this.planet_screen = planet_screen;

        debug_font = game.getAssetsManager().getFont("fonts/pixel_font.ttf", 15);

        NAME = Screens.PLANET_GUI_SCREEN_NAME;

        block_pointer = new PlayerBlockSelectorGUIComponent(game.getGUIViewport(), planet_screen, game);
        addScreenComponent(block_pointer);

        placing_switch = new BackgroundForegroundSwitch(new Vector2(450, -10), game.getGUIViewport(), planet_screen, game);
        addScreenComponent(placing_switch);

        toolbar_renderer = new PlayerToolBarGUIComponent(planet_screen, game.getGUIViewport(), game);
        addScreenComponent(toolbar_renderer);

        TextureRegion white_texture = game.getAssetsManager().getTextureRegion("gui/right_arrow");
        left_button = new TextureButton(new Vector2(-550, -300), new Vector2(128, 128), new TextureRegion(white_texture), game.getGUIViewport(), game);
        left_button.setRotation(180);
        addScreenComponent(left_button);

        right_button = new TextureButton(new Vector2(-300, -300), new Vector2(128, 128), new TextureRegion(white_texture), game.getGUIViewport(), game);
        addScreenComponent(right_button);

        jump_button = new TextureButton(new Vector2(450, -300), new Vector2(128, 128), new TextureRegion(white_texture), game.getGUIViewport(), game);
        jump_button.setRotation(90);
        addScreenComponent(jump_button);

        inventory_button = new TextureButton(new Vector2(450, 200), new Vector2(64, 64), new TextureRegion(white_texture), game.getGUIViewport(), game);
        addScreenComponent(inventory_button);

        pickaxe_button = new TextureButton(new Vector2(450, -150), new Vector2(128, 128), new TextureRegion(white_texture), game.getGUIViewport(), game);
        addScreenComponent(pickaxe_button);
        pickaxe_button.setVisible(false);

        placeblock_button = new TextureButton(new Vector2(450, -150), new Vector2(128, 128), new TextureRegion(white_texture), game.getGUIViewport(), game);
        addScreenComponent(placeblock_button);
        placeblock_button.setVisible(false);

        //create listeners
        left_button.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched(GUIComponent instance) {
                planet_screen.getWorld().getPlayer().setLeft(true);
            }

            @Override
            public void released(GUIComponent instance) {
                planet_screen.getWorld().getPlayer().setLeft(false);
            }
        });

        right_button.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched(GUIComponent instance) {
                planet_screen.getWorld().getPlayer().setRight(true);
            }

            @Override
            public void released(GUIComponent instance) {
                planet_screen.getWorld().getPlayer().setRight(false);
            }
        });

        jump_button.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched(GUIComponent instance) {
                planet_screen.getWorld().getPlayer().setJump(true);
            }

            @Override
            public void released(GUIComponent instance) {
                planet_screen.getWorld().getPlayer().setJump(false);
            }
        });

        inventory_button.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched(GUIComponent instance) {
                planet_screen.setVisible(false);
                game.getScreen(Screens.PLAYER_INVENTORY_SCREEN).setVisible(true);
            }

            @Override
            public void released(GUIComponent instance) {}
        });

        pickaxe_button.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched(GUIComponent instance) {
                if(planet_screen.getWorld() != null && planet_screen.getWorld().getPlayer() != null) {
                    planet_screen.getWorld().getPlayer().setPickaxeButtonPressed(true);
                }
            }

            @Override
            public void released(GUIComponent instance) {
                if(planet_screen.getWorld() != null && planet_screen.getWorld().getPlayer() != null) {
                    planet_screen.getWorld().getPlayer().setPickaxeButtonPressed(false);
                }
            }
        });

        placeblock_button.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched(GUIComponent instance) {
                if(planet_screen.getWorld() != null && planet_screen.getWorld().getPlayer() != null) {
                    planet_screen.getWorld().getPlayer().setPlaceBlockButtonPressed(true);
                }
            }

            @Override
            public void released(GUIComponent instance) {
                if(planet_screen.getWorld() != null && planet_screen.getWorld().getPlayer() != null) {
                    planet_screen.getWorld().getPlayer().setPlaceBlockButtonPressed(false);
                }
            }
        });

        //blocks_selector = new BlockPlacingSelector(new Vector2(500, 290), planet_screen);
        //addScreenComponent(blocks_selector);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
    }

    @Override
    public void tick(float delta) {
        if(!isVisible())
            return;

        tickComponents(delta);

        //check which buttons should be visible
        pickaxe_button.setVisible(false);
        placeblock_button.setVisible(false);
        placing_switch.setVisible(false);
        if(planet_screen.getWorld() != null && planet_screen.getWorld().getPlayer() != null
                && planet_screen.getWorld().getPlayer().isPointing()) {

            if(block_pointer.isMirror()) {
                int offset = 720;
                float offset2 = 720f * 4f/3f + 70;
                left_button.getPosition().set(-550 + offset, -300);
                right_button.getPosition().set(-300 + offset, -300);
                jump_button.getPosition().set(450 - offset2, -300);
                pickaxe_button.getPosition().set(450 - offset2, -150);
                placeblock_button.getPosition().set(450 - offset2, -150);
                placing_switch.getPosition().set(450 - offset2, -10);
            } else {
                left_button.getPosition().set(-550, -300);
                right_button.getPosition().set(-300, -300);
                jump_button.getPosition().set(450, -300);
                pickaxe_button.getPosition().set(450, -150);
                placeblock_button.getPosition().set(450, -150);
                placing_switch.getPosition().set(450, -10);
            }

            if(planet_screen.getWorld().getPlayer().getSelectedItems() != null){
                Item item = planet_screen.getWorld().getPlayer().getSelectedItems().getItem();
                if (item instanceof ToolItem) {
                    pickaxe_button.setVisible(true);
                } else if (item instanceof BlockItem) {
                    placeblock_button.setVisible(true);
                }

                placing_switch.setVisible(true);
            } else {
                pickaxe_button.setVisible(true);
                placing_switch.setVisible(true);
            }
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        if (!isVisible())
            return;

        batch.setProjectionMatrix(game.getGUICamera().combined);
        renderComponents(batch);

        /*if (Game.IS_HOST || Game.IS_CLIENT) {
            renderPlayers((Game.IS_HOST) ? game.getGameServer().getPlayers() : game.getGameClient().getPlayers(), batch);
        }

        //render debug stats
        if(planet_screen.getWorld() == null)
            return;

        World world = planet_screen.getWorld();
        batch.setProjectionMatrix(game.getGUICamera().combined);

        AssetsManager.font.draw(batch, "FPS: "+ Gdx.graphics.getFramesPerSecond(), -620, 350);
        AssetsManager.font.draw(batch, "Drawn lights:" + world.getLightEngine().getDrawnLightsCount(), -620, 330);
        AssetsManager.font.draw(batch, "Total ground lights:" + world.getLightEngine().getGroundLineRenderer().getPositions().size, -620, 310);
        AssetsManager.font.draw(batch, "Current tasks: " + game.getThreadPool().getActuallyTasksRunningCount(), -620, 290);

        AssetsManager.font.draw(batch, "Chunk x: " + ((world.getPlayer().getPosition().x / World.CHUNK_WORLD_SIZE) % world.getPlanetProperties().PLANET_SIZE), -620, 270);
        AssetsManager.font.draw(batch, "Chunk x: " + ((world.getPlayer().getPosition().x / World.CHUNK_WORLD_SIZE)), -450, 270);

        AssetsManager.font.draw(batch, "Phys. obj. count: " + world.getPhysicsEngine().getAllObjectsCount(), -620, 250);
        //font.draw(batch, "Chunks colliders count: " + world.getPhysicsEngine().getPhysicsEngineChunksHelper().getChunkCollidersCount(), -620, 230);

        AssetsManager.font.draw(batch, "Draw calls: " + GLProfiler.drawCalls, -620, 210);
        AssetsManager.font.draw(batch, "Vertices rendered: " + (int) GLProfiler.vertexCount.average, -620, 190);
        AssetsManager.font.draw(batch, "Texture binds: " + GLProfiler.textureBindings, -620, 170);
        AssetsManager.font.draw(batch, "ID assigner: " + WorldObject.IDAssigner.accValue(), -620, 150);
        AssetsManager.font.draw(batch, "Time: " + World.TIME, -620, 130);
        */
    }

    private void renderPlayers(Array<ServerPlayer> players, SpriteBatch batch) {
        debug_font.draw(batch, "Players list:", 180, 350);
        for (int i = 0; i < players.size; i++) {
            ServerPlayer player = players.get(i);
            debug_font.draw(batch, player.username + " (" + player.connection_id + (player.is_host ? ", host)" : ")"), 180, 320 - (i * 30));
        }
    }

    public PlayerToolBarGUIComponent getPlayerToolbarGUIComponent() {
        return toolbar_renderer;
    }

    public PlayerBlockSelectorGUIComponent getBlockPointerGUIComponent() {
        return block_pointer;
    }

    @Override
    public void dispose() {

    }
}
