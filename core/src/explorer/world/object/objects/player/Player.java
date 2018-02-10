package explorer.world.object.objects.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.minlog.Log;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import explorer.game.Helper;
import explorer.game.framework.Game;
import explorer.game.framework.utils.math.PositionInterpolator;
import explorer.game.framework.utils.math.RotationInterpolator;
import explorer.game.screen.gui.dialog.InfoDialog;
import explorer.game.screen.screens.Screens;
import explorer.game.screen.screens.menu.SelectPlayerScreen;
import explorer.game.screen.screens.planet.PlanetGUIScreen;
import explorer.game.screen.screens.planet.PlanetScreen;
import explorer.game.screen.screens.planet.PlayerInventoryScreen;
import explorer.network.NetworkClasses;
import explorer.network.NetworkHelper;
import explorer.network.client.ServerPlayer;
import explorer.network.server.GameServer;
import explorer.world.World;
import explorer.world.block.Block;
import explorer.world.chunk.TileHolder;
import explorer.world.chunk.WorldChunk;
import explorer.world.inventory.Item;
import explorer.world.inventory.ItemsContainer;
import explorer.world.inventory.item_types.BlockItem;
import explorer.world.inventory.item_types.BodyWearableItem;
import explorer.world.inventory.item_types.ToolItem;
import explorer.world.inventory.item_types.WearableItem;
import explorer.world.inventory.items.tools.TestPickaxeItem;
import explorer.world.inventory.items.wearables.BasicHeadItem;
import explorer.world.inventory.items.wearables.RedBandanaHeadItem;
import explorer.world.object.DynamicWorldObject;
import explorer.world.object.objects.LayingItemObject;
import explorer.world.object.objects.TorchObject;
import explorer.world.object.objects.player.gui.PlayerBlockSelectorGUIComponent;
import explorer.world.object.objects.player.gui.PlayerWearablesInventoryRenderer;
import explorer.world.physics.shapes.RectanglePhysicsShape;

/**
 * Created by RYZEN on 12.10.2017.
 */

public class Player extends DynamicWorldObject {

    private PlayerRenderer1 player_renderer;

    private boolean left, right, jump;
    private final float PLAYER_SPEED = 300f;

    //clone vars
    private boolean is_clone;
    private ServerPlayer representing_player;
    private Vector2 last_position_clone;

    //current center chunk pos
    private Vector2 current_center_chunk_pos;
    private float on_current_chunk_time;

    //interpolating
    private PositionInterpolator interpolator;
    private RotationInterpolator arm_interpolator;

    //player inventory container
    private ItemsContainer items_container;
    private ItemsContainer toolbar_items_container;
    public static final int INVENTORY_SLOTS_COUNT = 30;
    public static final int TOOLBAR_INVENTORY_SLOTS_COUNT = 6;

    //player data handler
    private PlayerData player_data;

    //finding parent chunk vars
    private Rectangle chunk_rect;

    //selected item
    private ItemsContainer.ItemsStack selected_items, last_selected_items;

    //wear items
    private WearableItem wear_head_item, last_wear_head_item;
    private BodyWearableItem wear_body_item, last_wear_body_item;
    private WearableItem wear_legs_item, last_wear_legs_item;

    //breaking, placing blocks & arm animation
    private boolean hitting_block, block_loot_spawned;
    private boolean placing_block;
    private int last_x, last_y, x, y;
    private float block_hardness, last_arm_angle;
    private boolean is_foreground_placing = true, last_is_foreground_placing = true;
    private WorldChunk chunk;
    public static final int ARM_RANGE = World.BLOCK_SIZE * 5;

    public static final float ARM_TOOL_POWER = 1f;

    //blocks/tools pointing system
    private boolean pointing;
    private int last_pointed_block_x, last_pointed_block_y;
    private WorldChunk last_pointed_chunk;

    private boolean pickaxe_button_pressed, placeblock_button_pressed;

    private InputAdapter input_adapter;

    /* PACKETS */
    public static class ArmAngleUpdatePacket extends NetworkClasses.PlayerBoundPacket {
        public float angle;
    }

    public static class UpdateWearItemPacket extends NetworkClasses.PlayerBoundPacket {
        public String item_class_name;
        public String item_property;

        /**
         * Slot index 0 = helmet, 1 = torso, 2 = legs; if -1 just skip
         */
        public int slot = -1;
    }

    public static class UpdateHoldingItemPacket extends NetworkClasses.PlayerBoundPacket {
        public String item_class_name;
        public String item_property;
    }

    /**
     * Empty packet acts like interface marker
     */
    public static class UpdateWearablesAndHoldingItemsRequest extends NetworkClasses.PlayerBoundPacket {}

    public Player(Vector2 position, World w, boolean clone, final Game game) {
        super(position, w, game);

        this.is_clone = clone;

        player_renderer = new PlayerRenderer1(this, w, game);

        getWH().set(player_renderer.getPlayerWH());
        this.physics_shape = new RectanglePhysicsShape(new Vector2(), new Vector2(getWH()), this);

        this.current_center_chunk_pos = new Vector2();

        chunk_rect = new Rectangle();
        last_position_clone = new Vector2();

        //create interpolators
        interpolator = new PositionInterpolator(this, new PositionInterpolator.InterpolatorPacketSender() {

            float last_center_chunk_x, last_center_chunk_y;

            @Override
            public void sendUpdatePacket() {
                NetworkClasses.PlayerPositionUpdatePacket position_update_packet = new NetworkClasses.PlayerPositionUpdatePacket();
                position_update_packet.player_connection_id = (Game.IS_HOST) ? GameServer.SERVER_CONNECTION_ID : game.getGameClient().getClient().getID();
                position_update_packet.x = getPosition().x;
                position_update_packet.y = getPosition().y;
                position_update_packet.direction = getPlayerRenderer().getDirection();
                position_update_packet.tcp = false;
                position_update_packet.on_current_chunk_time = on_current_chunk_time;

                if(!world.getWorldChunks()[1][1].isDirty()) {
                    position_update_packet.center_chunk_x = world.getWorldChunks()[1][1].getPosition().x;
                    position_update_packet.center_chunk_y = world.getWorldChunks()[1][1].getPosition().y;

                    last_center_chunk_x = position_update_packet.center_chunk_x;
                    last_center_chunk_y = position_update_packet.center_chunk_y;
                } else {
                    position_update_packet.center_chunk_x = last_center_chunk_x;
                    position_update_packet.center_chunk_y = last_center_chunk_y;
                }

                NetworkHelper.send(position_update_packet);
            }
        }, game);

        interpolator.setOnlyReceive(is_clone);
        interpolator.setOnlySend(!is_clone);

        arm_interpolator = new RotationInterpolator(new PositionInterpolator.InterpolatorPacketSender() {
            @Override
            public void sendUpdatePacket() {
                ArmAngleUpdatePacket update_packet = new ArmAngleUpdatePacket();
                update_packet.player_connection_id = (Game.IS_HOST) ? GameServer.SERVER_CONNECTION_ID : game.getGameClient().getClient().getID();
                update_packet.angle = getPlayerRenderer().getArmAngle();
                update_packet.tcp = false;

                NetworkHelper.send(update_packet);
            }
        }, game);

        arm_interpolator.setOnlyReceive(is_clone);
        arm_interpolator.setOnlySend(!is_clone);

        //is this instance is just clone don't calculate physics for it because we are just receiving position of player and physics for him is
        //calculated on other client side which sends packets about position update
        setPhysicsEnabled(!is_clone);

        if(is_clone) {
            return;
        }

        //create items container
        items_container = new ItemsContainer(INVENTORY_SLOTS_COUNT);
        toolbar_items_container = new ItemsContainer(TOOLBAR_INVENTORY_SLOTS_COUNT);

        //load inventory
        player_data = new PlayerData(SelectPlayerScreen.selected_username, this, game);
        player_data.loadData();

        //update wearables worn items
        PlayerWearablesInventoryRenderer wearables_inv_renderer = game.getScreen(Screens.PLAYER_INVENTORY_SCREEN, PlayerInventoryScreen.class).getWearablesInventoryRenderer();

        if(getWearHeadItem() != null) {
            wearables_inv_renderer.getItemsContainer().getItems().set(0, new ItemsContainer.ItemsStack(getWearHeadItem(), 1, wearables_inv_renderer.getItemsContainer()));
        }
        if(getWearBodyItem() != null) {
            wearables_inv_renderer.getItemsContainer().getItems().set(1, new ItemsContainer.ItemsStack(getWearBodyItem(), 1, wearables_inv_renderer.getItemsContainer()));
        }
        if(getWearLegsItem() != null) {
            wearables_inv_renderer.getItemsContainer().getItems().set(2, new ItemsContainer.ItemsStack(getWearLegsItem(), 1, wearables_inv_renderer.getItemsContainer()));
        }

        /* debug adding item */
        toolbar_items_container.clear();

        toolbar_items_container.addItem(new TestPickaxeItem(game));
        toolbar_items_container.addItem(new BlockItem(game).setBlock(world.getBlocks().GRASS_PLANT_BLOCK.getBlockID(), world), 64);

        //debug, add stack of every possible block to inventory
        items_container.clear();
        for(Object id : ((Map) world.getBlocks().getAllBlocksMap()).keySet()) {
            if((Integer) id == world.getBlocks().AIR.getBlockID())
                continue;

            items_container.addItem(new BlockItem(game).setBlock((Integer) id, world), 64);
        }

        //create blocks pointer listener
        makeBlockPointerListener();

        //because player is always on center of map
        setParentChunk(w.getWorldChunks()[1][1]);

        game.getMainCamera().position.set(position.x, position.y, 0);
        game.getMainCamera().update();

        //debug
        makeDebugInput();
    }

    private void makeBlockPointerListener() {
        PlayerBlockSelectorGUIComponent.SelectorListener listener = new PlayerBlockSelectorGUIComponent.SelectorListener() {
            @Override
            public void start() {
                pointing = true;

            }

            @Override
            public void stop() {
                pointing = false;
            }

            @Override
            public void changed(int new_block_x, int new_block_y, WorldChunk chunk, Vector2 pointer_world_pos) {
                calculateArmAngle(pointer_world_pos, true);

                last_pointed_block_x = new_block_x;
                last_pointed_block_y = new_block_y;
                last_pointed_chunk = chunk;
            }
        };
        game.getScreen(Screens.PLANET_GUI_SCREEN_NAME, PlanetGUIScreen.class).getBlockPointerGUIComponent().setSelectorListener(listener);
    }

    private void makeDebugInput() {
        input_adapter = new InputAdapter() {

            boolean c_button = false;

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                final Vector2 touch = new Vector2(screenX, screenY);
                game.getMainViewport().unproject(touch);

                if(!game.getScreen(Screens.PLANET_SCREEN_NAME).isVisible())
                    return false;

                if(button == 0 && !c_button) {
                    //transform touch to block cords
                    for (int i = 0; i < world.getWorldChunks().length; i++) {
                        for (int j = 0; j < world.getWorldChunks()[0].length; j++) {
                            WorldChunk chunk = world.getWorldChunks()[i][j];
                            Rectangle chunk_rect = new Rectangle(chunk.getPosition().x, chunk.getPosition().y, chunk.getWH().x, chunk.getWH().y);

                            //so we have proper chunk now just transform global cords to local blocks coords
                            if (chunk_rect.contains(touch)) {
                                int local_x = (int) (touch.x - chunk_rect.getX()) / World.BLOCK_SIZE;
                                int local_y = (int) (touch.y - chunk_rect.getY()) / World.BLOCK_SIZE;

                                setHittingBlock(true, local_x, local_y, chunk);

                                return true;
                            }
                        }
                    }

                } else if(button == 0 && c_button) {
                    getPosition().set(touch);
                    getVelocity().set(0, 0);
                } else if(button == 1) {
                    if(selected_items == null)
                        return false;

                    if(selected_items.getItem() instanceof BlockItem) {
                        for (int i = 0; i < world.getWorldChunks().length; i++) {
                            for (int j = 0; j < world.getWorldChunks()[0].length; j++) {
                                WorldChunk chunk = world.getWorldChunks()[i][j];
                                Rectangle chunk_rect = new Rectangle(chunk.getPosition().x, chunk.getPosition().y, chunk.getWH().x, chunk.getWH().y);

                                //so we have proper chunk now just transform global cords to local blocks coords
                                if (chunk_rect.contains(touch)) {
                                    int local_x = (int) (touch.x - chunk_rect.getX()) / World.BLOCK_SIZE;
                                    int local_y = (int) (touch.y - chunk_rect.getY()) / World.BLOCK_SIZE;

                                    //we have to notify network if game is using network(game checks it itself)
                                    boolean result = chunk.setBlockPlayerChecks(local_x, local_y, ((BlockItem) selected_items.getItem()).getRepresentingBlockID(), false, true);

                                    //if block was placed removed one from stack and check if stack don't ended
                                    if(result) {
                                        selected_items.removeOneFromStack();

                                        if (selected_items.getInStack() <= 0)
                                            selected_items = null;
                                    }
                                }
                            }
                        }
                    }

                } else if(button == 2) {
                    TorchObject torch_object = new TorchObject(touch, world, game);
                    world.addObject(torch_object, true);
                }

                return false;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {

                if(hitting_block) {
                    setHittingBlock(false, 0, 0, null);
                }

                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if(hitting_block) {
                    Vector2 touch = new Vector2(screenX, screenY);
                    game.getMainViewport().unproject(touch);

                    for (int i = 0; i < world.getWorldChunks().length; i++) {
                        for (int j = 0; j < world.getWorldChunks()[0].length; j++) {
                            WorldChunk chunk = world.getWorldChunks()[i][j];
                            Rectangle chunk_rect = new Rectangle(chunk.getPosition().x, chunk.getPosition().y, chunk.getWH().x, chunk.getWH().y);

                            //so we have proper chunk now just transform global cords to local blocks coords
                            if (chunk_rect.contains(touch)) {
                                int local_x = (int) (touch.x - chunk_rect.getX()) / World.BLOCK_SIZE;
                                int local_y = (int) (touch.y - chunk_rect.getY()) / World.BLOCK_SIZE;

                                setHittingBlock(true, local_x, local_y, chunk);

                                return true;
                            }
                        }
                    }
                }

                return false;
            }

            @Override
            public boolean scrolled(int amount) {
                game.getMainCamera().zoom += amount * .1f * game.getMainCamera().zoom;
                game.getMainCamera().update();
                Log.debug("(Player) zoom: "  + game.getMainCamera().zoom);

                return false;
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                //Vector2 cords = new Vector2(screenX, screenY);
                //getMainViewport().unproject(cords);

                //getMainCamera().position.add(Gdx.input.getDeltaX(), -Gdx.input.getDeltaY(), 0);
                //getMainCamera().update();

                return false;
            }

            public boolean keyDown(int keycode) {
                if(keycode == Input.Keys.A) {
                    left = true;
                } else if(keycode == Input.Keys.D) {
                    right = true;
                } else if(keycode == Input.Keys.C) {
                    c_button = true;
                }

                if(keycode == Input.Keys.SPACE) {
                    jump = true;
                }

                if(keycode == Input.Keys.F && Game.IS_CLIENT) {
                    game.getGameClient().disconnect();
                }

                return false;
            }

            @Override
            public boolean keyUp(int keycode) {
                if(keycode == Input.Keys.A) {
                    left = false;
                } else if(keycode == Input.Keys.D) {
                    right = false;
                } else if(keycode == Input.Keys.C) {
                    c_button = false;
                }

                if(keycode == Input.Keys.SPACE) {
                    jump = false;
                }

                return false;
            }
        };
        game.getInputEngine().addInputProcessor(input_adapter);
    }

    /**
     * This function is only called if some packet from network comes to player as normal player not a clone (so this method can be called only if player is not an clone)
     * @param packet packet
     */
    public void processToPlayerPacket(Object packet) {
        if(packet instanceof UpdateWearablesAndHoldingItemsRequest) {
            updateWearItemsNetwork(true);
            updateSelectedItemNetwork(true);
        }
    }

    /**
     * This function is only called if player is an clone
     * @param packet packet that come from network from client which this cloned player is normal player to update his state
     */
    public void processToClonePacket(Object packet) {
        if(packet instanceof NetworkClasses.PlayerPositionUpdatePacket) {
            NetworkClasses.PlayerPositionUpdatePacket pos_update = (NetworkClasses.PlayerPositionUpdatePacket) packet;

            final float teleport_after_distance = 200;
            if(Vector2.dst(pos_update.x, pos_update.y, getPosition().x, getPosition().y) > teleport_after_distance) {
                getPosition().set(pos_update.x, pos_update.y);
            }

            getPlayerRenderer().setDirection(pos_update.direction);
            current_center_chunk_pos.set(pos_update.center_chunk_x, pos_update.center_chunk_y);
            on_current_chunk_time = pos_update.on_current_chunk_time;

            interpolator.interpolate(pos_update.x, pos_update.y, System.currentTimeMillis());
        } else if(packet instanceof ArmAngleUpdatePacket) {
            ArmAngleUpdatePacket arm_update = (ArmAngleUpdatePacket) packet;

            arm_interpolator.interpolate(arm_update.angle, System.currentTimeMillis());
        } else if(packet instanceof UpdateWearItemPacket) {
            UpdateWearItemPacket wear_update = (UpdateWearItemPacket) packet;

            try {
                WearableItem new_item = null;

                if(!wear_update.item_class_name.equals("null")) {
                    new_item = (WearableItem) Helper.objectFromClassName(wear_update.item_class_name, new Object[]{game}, Game.class);
                    new_item.setItemProperty(wear_update.item_property);
                }

                //finally update worn item
                switch(wear_update.slot) {
                    case 0:
                        wear_head_item = new_item;
                        break;
                    case 1:
                        wear_body_item = (BodyWearableItem) new_item;
                        break;
                    case 2:
                        wear_legs_item = new_item;
                        break;
                }

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        } else if(packet instanceof UpdateHoldingItemPacket) {
            UpdateHoldingItemPacket holding_update = (UpdateHoldingItemPacket) packet;

            Item item = null;

            if(!holding_update.item_class_name.equals("null")) {
                Object[] arguments = new Object[]{game};
                try {
                    item = (Item) Helper.objectFromClassName(holding_update.item_class_name, arguments, Game.class);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                }
            }

            if(item != null) {
                item.setItemProperty(holding_update.item_property);

                //well here we don't really care how to make new items stack because if we are here we are 100% sure that this is an clone
                selected_items = new ItemsContainer.ItemsStack(item, 1, null);
            } else {
                selected_items = new ItemsContainer.ItemsStack(null, 1, null);
            }
        }
    }

    private void calculateArmAngle(Vector2 touch_pos, boolean y_axis_up) {
        Vector2 pos = new Vector2(getPosition()).add(getWH().x / 2f, getWH().y / 2f);

        float angle = MathUtils.radiansToDegrees * (MathUtils.atan2((pos.x - touch_pos.x), pos.y - touch_pos.y)) * -1f;

        player_renderer.setArmAngle(angle);

        //calc player dir
        if(touch_pos.x > getPosition().x + getWH().x / 2f) {
            getPlayerRenderer().setDirection(1);
        } else if(touch_pos.x < getPosition().x + getWH().x / 2f) {
            getPlayerRenderer().setDirection(-1);
        }
    }

    public void updateWearItemsNetwork(boolean dont_check) {
        if(!Game.IS_HOST && !Game.IS_CLIENT)
            return;

        if((last_wear_head_item != wear_head_item || dont_check)) {
            UpdateWearItemPacket packet = new UpdateWearItemPacket();
            packet.tcp = true;

            if(wear_head_item != null) {
                packet.item_class_name = wear_head_item.getClass().getName();
                packet.item_property = wear_head_item.getItemProperty();
            } else {
                packet.item_class_name = "null";
            }

            packet.slot = 0;
            packet.player_connection_id = NetworkHelper.getConnectionID(game);

            NetworkHelper.send(packet);
        }

        if((last_wear_body_item != wear_body_item || dont_check)) {
            UpdateWearItemPacket packet = new UpdateWearItemPacket();
            packet.tcp = true;

            if(wear_body_item != null) {
                packet.item_class_name = wear_body_item.getClass().getName();
                packet.item_property = wear_body_item.getItemProperty();
            } else {
                packet.item_class_name = "null";
            }

            packet.slot = 1;
            packet.player_connection_id = NetworkHelper.getConnectionID(game);

            NetworkHelper.send(packet);
        }

        if((last_wear_legs_item != wear_legs_item || dont_check)) {
            UpdateWearItemPacket packet = new UpdateWearItemPacket();
            packet.tcp = true;

            if(wear_legs_item != null) {
                packet.item_class_name = wear_legs_item.getClass().getName();
                packet.item_property = wear_legs_item.getItemProperty();
            } else {
                packet.item_class_name = "null";
            }

            packet.slot = 2;
            packet.player_connection_id = NetworkHelper.getConnectionID(game);

            NetworkHelper.send(packet);
        }

        last_wear_head_item = wear_head_item;
        last_wear_body_item = wear_body_item;
        last_wear_legs_item = wear_legs_item;
    }

    public void updateSelectedItemNetwork(boolean dont_check) {
        if(!Game.IS_HOST && !Game.IS_CLIENT)
            return;

        if((last_selected_items != selected_items || dont_check)) {
            UpdateHoldingItemPacket packet = new UpdateHoldingItemPacket();
            packet.tcp = true;

            if(selected_items != null && selected_items.getItem() != null) {
                packet.item_class_name = selected_items.getItem().getClass().getName();
                packet.item_property = selected_items.getItem().getItemProperty();
            } else {
                packet.item_class_name = "null";
            }

            packet.player_connection_id = NetworkHelper.getConnectionID(game);
            NetworkHelper.send(packet);
        }

        last_selected_items = selected_items;
    }

    @Override
    public void move(Vector2 move_vector) {
        super.move(move_vector);

        if(is_clone)
            return;

        game.getMainCamera().position.add(move_vector.x, move_vector.y, 0);
        game.getMainCamera().update();
        getPosition().add(move_vector.x, move_vector.y);
    }

    private float animation_time = 0;

    private WorldChunk last_parent_chunk;

    @Override
    public void tick(float delta) {
        animation_time += delta * 20f;

        if(!getParentChunk().isDirty())
            on_current_chunk_time += delta * 1000f;

        //find parent chunk (because chunk[1][1] is not always players parent chunk because of delayed loading chunks system)
        for(int i = 0; i < world.getWorldChunks().length; i++) {
            for(int j = 0; j < world.getWorldChunks()[0].length; j++) {
                WorldChunk chunk = world.getWorldChunks()[i][j];
                chunk_rect.set(chunk.getPosition().x, chunk.getPosition().y, chunk.getWH().x, chunk.getWH().y);

                if(chunk_rect.contains(getPosition().x + getWH().x / 2f, getPosition().y + getWH().y / 2f)) {
                    setParentChunk(chunk);

                    if(chunk == world.getWorldChunks()[1][1] && chunk != last_parent_chunk) {
                        on_current_chunk_time = 0;
                    }
                    last_parent_chunk = chunk;
                    break;
                }
            }
        }

        //render our player
        player_renderer.tick(delta);

        //update interpolators
        interpolator.update();
        arm_interpolator.update();

        if(is_clone) {
            //fake calculate velocity for renderer to play animation properly
            getVelocity().set(getPosition()).sub(interpolator.getLastTargetPosition());

            if(getVelocity().epsilonEquals(0, 0, 5))
                getVelocity().set(0, 0);

            last_position_clone.set(getPosition());

            //update clone arm angle
            getPlayerRenderer().setArmAngleUnchecked(arm_interpolator.getValue());

            return;
        }

        //calculate to which direction player is pointing
        if(getVelocity().x > 0)
            getPlayerRenderer().setDirection(1);
        else if(getVelocity().x < 0)
            getPlayerRenderer().setDirection(-1);

        //arm following cursor animation
        if(!pointing) {
            Vector2 mouse_pos = new Vector2(Gdx.input.getX(), Gdx.input.getY());
            game.getMainViewport().unproject(mouse_pos);
            calculateArmAngle(mouse_pos, false);
        }

        //read ui from planetGuiScreen
        if(pickaxe_button_pressed) {
            setHittingBlock(true, last_pointed_block_x, last_pointed_block_y, last_pointed_chunk);
        } else if(placeblock_button_pressed) {
            placing_block = true;
        }

        //hitting block system & animation
        if(hitting_block) {
            Block block;
            if (is_foreground_placing)
                block = chunk.getBlocks()[x][y].getForegroundBlock();
            else
                block = chunk.getBlocks()[x][y].getBackgroundBlock();

            if(last_x != x || last_y != y || is_foreground_placing != last_is_foreground_placing) {
                if(is_foreground_placing)
                    block_hardness = chunk.getBlocks()[x][y].getForegroundBlock().getHardness();
                else
                    block_hardness = chunk.getBlocks()[x][y].getBackgroundBlock().getHardness();

                last_arm_angle = getPlayerRenderer().getArmAngle();
                block_loot_spawned = false;
            }

            float strength = ARM_TOOL_POWER;
            if(selected_items != null && selected_items.getItem() != null && selected_items.getItem() instanceof ToolItem) {
                ToolItem tool = (ToolItem) selected_items.getItem();
                if(block.getProfferedToolType().equals(ToolItem.ToolType.ANY)) {
                    //if any just use holding tool power
                    strength = tool.getToolPower();
                } else {
                    //if block want some specific tool compare if holding one is proper
                    if(tool.getToolType().equals(block.getProfferedToolType())) {
                        strength = tool.getToolPower();
                    } else {
                        strength = tool.getToolPower() / 3f;

                        //if acc strength is less than arm power set it to arm power
                        if(strength < ARM_TOOL_POWER) {
                            strength = ARM_TOOL_POWER;
                        }
                    }
                }

            }

            block_hardness -= strength * delta;

            if(block_hardness <= 0f) {
                //so here block is broken and we have to remove it from world and optionally spawn item on ground that represents this item
                chunk.setBlock(x, y, world.getBlocks().AIR.getBlockID(), !is_foreground_placing, true);

                //create object that represents broken block
                if(!block_loot_spawned && block.getBlockID() != world.getBlocks().AIR.getBlockID()) {
                    if(block.isDropable()) {
                        LayingItemObject loot_object = new LayingItemObject(new Vector2(x * World.BLOCK_SIZE, y * World.BLOCK_SIZE).add(chunk.getPosition()), world, game);
                        loot_object.setItem(new BlockItem(game).setBlock(block.getBlockID(), world));
                        world.addObject(loot_object, true);
                    }
                    block_loot_spawned = true;
                }
            }

            //arm animation
            getPlayerRenderer().setArmAngleUnchecked(last_arm_angle + (MathUtils.cos(animation_time) * 5));

            last_x = x;
            last_y = y;
        } else if(placing_block) {
            int in_hand_block = -1;

            //first check if player is even holding some block
            if(getSelectedItems() != null && getSelectedItems().getItem() != null) {
                if(getSelectedItems().getItem() instanceof BlockItem) {
                    in_hand_block = ((BlockItem) getSelectedItems().getItem()).getRepresentingBlockID();
                }
            }

            if(in_hand_block != -1) {
                //try to place a block
                if(last_pointed_chunk != null) {
                    boolean placed = last_pointed_chunk.setBlockPlayerChecks(last_pointed_block_x, last_pointed_block_y, in_hand_block, !is_foreground_placing, true);

                    if(placed)
                        getSelectedItems().removeOneFromStack();
                }
            }

            placing_block = false;
        }

        //run this code only if this is not a clone
        if(left) {
            getVelocity().x = -PLAYER_SPEED;
        } else if(right) {
            getVelocity().x = PLAYER_SPEED;
        }

        if(jump) {
            if(getVelocity().y == 0f) {
                getVelocity().y = 700f;
            }
        }

        game.getMainCamera().position.lerp(new Vector3(getPosition().x + (getWH().x / 2f), getPosition().y + (getWH().y / 2f), 0), delta * 10f);
        game.getMainCamera().update();
    }

    @Override
    public void render(SpriteBatch batch) {
        player_renderer.render(batch);
    }

    @Override
    public void dispose() {
        if(!isClone()) {
            Log.info("(Player) Disposing player!");
            player_data.saveData();
            game.getInputEngine().remove(input_adapter);
        }
    }

    public void setWearHeadItem(WearableItem head_item) {
        this.wear_head_item = head_item;
        updateWearItemsNetwork(false);
    }

    public void setWearBodyItem(BodyWearableItem body_item) {
        this.wear_body_item = body_item;
        updateWearItemsNetwork(false);
    }

    public void setWearLegsItem(WearableItem legs_item) {
        this.wear_legs_item = legs_item;
        updateWearItemsNetwork(false);
    }

    public WearableItem getWearHeadItem() {
        return wear_head_item;
    }

    public BodyWearableItem getWearBodyItem() {
        return wear_body_item;
    }

    public WearableItem getWearLegsItem() {
        return wear_legs_item;
    }

    public void setLeft(boolean left) {
        this.left = left;
    }
    public void setRight(boolean right) {
        this.right = right;
    }
    public void setJump(boolean jump) {
        this.jump = jump;
    }

    public void setHittingBlock(boolean hitting_block, int x, int y, WorldChunk chunk) {
        this.hitting_block = hitting_block;

        this.x = x;
        this.y = y;
        this.chunk = chunk;
    }

    public void setSelectedItems(ItemsContainer.ItemsStack selected_items) {
        this.selected_items = selected_items;
        updateSelectedItemNetwork(false);
    }

    public ItemsContainer.ItemsStack getSelectedItems() {
        return selected_items;
    }

    public void setRepresentingPlayer(ServerPlayer player) {
        representing_player = player;

        //send request for updating what this clone on client side (where this clone is an normal player) is wearing and holding
        Runnable r = new Runnable() {
            @Override
            public void run() {
                UpdateWearablesAndHoldingItemsRequest update_request = new UpdateWearablesAndHoldingItemsRequest();
                update_request.player_connection_id = getRepresentingPlayer().connection_id;
                update_request.tcp = true;
                NetworkHelper.send(update_request);
            }
        };
        Gdx.app.postRunnable(r);
    }

    public void setForegroundPlacing(boolean foregroundPlacing) {
        this.is_foreground_placing = foregroundPlacing;
    }

    public void setPickaxeButtonPressed(boolean pressed) {
        pickaxe_button_pressed = pressed;
    }

    public void setPlaceBlockButtonPressed(boolean pressed) {
        placeblock_button_pressed = pressed;
    }

    public boolean isPointing() {
        return pointing;
    }

    public ServerPlayer getRepresentingPlayer() {
        return representing_player;
    }

    public boolean isClone() {
        return is_clone;
    }

    public ItemsContainer getItemsContainer() {
        return items_container;
    }

    public ItemsContainer getToolbarItemsContainer() {
        return toolbar_items_container;
    }

    public PlayerRenderer1 getPlayerRenderer() {
        return player_renderer;
    }

    /**
     * Float used on server side when deciding which player should send chunk data packet
     * @return how long this player wasn't changing current chunk to another (= how long ago player was loading some chunk)
     */
    public float getOnCurrentChunkTime() {
        return on_current_chunk_time;
    }

    /**
     * Vector used on server side when deciding which player should send chunk data packet
     * @return current center chunk position stored in vector instance
     */
    public Vector2 getCurrentCenterChunkPosition() {
        return current_center_chunk_pos;
    }
}
