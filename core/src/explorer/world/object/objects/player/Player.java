package explorer.world.object.objects.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import explorer.game.framework.Game;
import explorer.game.framework.utils.math.PositionInterpolator;
import explorer.game.screen.screens.Screens;
import explorer.network.NetworkClasses;
import explorer.network.NetworkHelper;
import explorer.network.client.ServerPlayer;
import explorer.network.server.GameServer;
import explorer.world.World;
import explorer.world.block.Block;
import explorer.world.chunk.WorldChunk;
import explorer.world.inventory.ItemsContainer;
import explorer.world.inventory.item_types.BlockItem;
import explorer.world.inventory.items.wearables.BasicHeadItem;
import explorer.world.inventory.items.wearables.RedBandanaHeadItem;
import explorer.world.object.DynamicWorldObject;
import explorer.world.object.objects.TorchObject;
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

    //interpolating
    private PositionInterpolator interpolator;

    //player inventory container
    private ItemsContainer items_container;
    private ItemsContainer toolbar_items_container;
    public static final int INVENTORY_SLOTS_COUNT = 30;
    public static final int TOOLBAR_INVENTORY_SLOTS_COUNT = 6;

    //finding parent chunk vars
    private Rectangle chunk_rect;

    //selected item
    private ItemsContainer.ItemsStack selected_items;

    public Player(Vector2 position, World w, boolean clone, final Game game) {
        super(position, w, game);

        this.is_clone = clone;

        player_renderer = new PlayerRenderer1(this, w, game);

        getWH().set(player_renderer.getPlayerWH());
        this.physics_shape = new RectanglePhysicsShape(new Vector2(), new Vector2(getWH()), this);

        chunk_rect = new Rectangle();

        last_position_clone = new Vector2();

        //create interpolator
        interpolator = new PositionInterpolator(this, game);
        interpolator.setOnlyReceive(is_clone);
        interpolator.setOnlySend(!is_clone);

        //is this instance is just clone don't calculate physics for it because we are just receiving position of player and physics for him is
        //calculated on other client side which sends packets about position update
        setPhysicsEnabled(!is_clone);

        if(is_clone) {
            return;
        }

        //create items container
        items_container = new ItemsContainer(INVENTORY_SLOTS_COUNT);
        toolbar_items_container = new ItemsContainer(TOOLBAR_INVENTORY_SLOTS_COUNT);

        for(int i = 0; i < INVENTORY_SLOTS_COUNT - 1; i++) {
            items_container.getItems().set(i, new ItemsContainer.ItemsStack(new BlockItem(game, world.getBlocks().DIRT.getBlockID(), world), MathUtils.random(1, 64), items_container));
        }
        items_container.addItem(new RedBandanaHeadItem(game), 1);

        for(int i = 0; i < TOOLBAR_INVENTORY_SLOTS_COUNT - 1; i++) {
            toolbar_items_container.getItems().set(i, new ItemsContainer.ItemsStack(new BlockItem(game, world.getBlocks().STONE.getBlockID(), world), MathUtils.random(1, 64), toolbar_items_container));
        }

        //because player is always on center of map
        setParentChunk(w.getWorldChunks()[1][1]);

        game.getMainCamera().position.set(position.x, position.y, 0);
        game.getMainCamera().update();

        //debug
        makeDebugInput();
    }

    /**
     * This function is only called if player is an clone
     * @param packet packet that come from network from client which this cloned player is normal player to update his state
     */
    public void processPacket(Object packet) {
        if(packet instanceof NetworkClasses.PlayerPositionUpdatePacket) {
            NetworkClasses.PlayerPositionUpdatePacket pos_update = (NetworkClasses.PlayerPositionUpdatePacket) packet;

            final float teleport_after_distance = 200;
            if(Vector2.dst(pos_update.x, pos_update.y, getPosition().x, getPosition().y) > teleport_after_distance) {
                getPosition().set(pos_update.x, pos_update.y);
            }

            interpolator.interpolate(pos_update.x, pos_update.y, System.currentTimeMillis());
        }
    }

    private void makeDebugInput() {
        InputAdapter input = new InputAdapter() {

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                final Vector2 touch = new Vector2(screenX, screenY);
                game.getMainViewport().unproject(touch);

                if(!game.getScreen(Screens.PLANET_SCREEN_NAME).isVisible())
                    return false;

                if(button == 0) {
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

                                    //set block only if current one is an air
                                    if(chunk.getBlocks()[local_x][local_y].getForegroundBlock().getBlockID() == world.getBlocks().AIR.getBlockID()) {
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
                    }

                } else if(button == 2) {
                    TorchObject torch_object = new TorchObject(touch, world, game);
                    world.addObject(torch_object, true);
                }

                return false;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {

                return false;
            }

            @Override
            public boolean scrolled(int amount) {
                game.getMainCamera().zoom += amount * .1f * game.getMainCamera().zoom;
                game.getMainCamera().update();
                System.out.println(game.getMainCamera().zoom);

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
                }

                if(keycode == Input.Keys.SPACE) {
                    jump = true;
                }

                return false;
            }

            @Override
            public boolean keyUp(int keycode) {
                if(keycode == Input.Keys.A) {
                    left = false;
                } else if(keycode == Input.Keys.D) {
                    right = false;
                }

                if(keycode == Input.Keys.SPACE) {
                    jump = false;
                }

                return false;
            }
        };
        game.getInputEngine().addInputProcessor(input);
    }

    private void calculateArmAngle(Vector2 touch_pos) {
        Vector2 pos = new Vector2(getPosition()).add(getWH().x / 2f, getWH().y / 2f);
        float angle = MathUtils.radiansToDegrees * (MathUtils.atan2((touch_pos.x - pos.x), touch_pos.y - pos.y));

        player_renderer.setArmAngle(angle);
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

    @Override
    public void tick(float delta) {
        //find parent chunk (because chunk[1][1] is now always players parent chunk because of delayed loading chunks system)
        for(int i = 0; i < world.getWorldChunks().length; i++) {
            for(int j = 0; j < world.getWorldChunks()[0].length; j++) {
                WorldChunk chunk = world.getWorldChunks()[i][j];
                chunk_rect.set(chunk.getPosition().x, chunk.getPosition().y, chunk.getWH().x, chunk.getWH().y);

                if(chunk_rect.contains(getPosition().x + getWH().x / 2f, getPosition().y + getWH().y / 2f)) {
                    setParentChunk(chunk);
                    break;
                }
            }
        }

        //render our player
        player_renderer.tick(delta);

        //update interpolator he will take care of sending or receiving packets about new position
        interpolator.update();

        if(is_clone) {
            //fake calculate velocity for renderer to play animation properly
            getVelocity().set(getPosition()).sub(last_position_clone);

            if(getVelocity().epsilonEquals(0, 0, 5))
                getVelocity().set(0, 0);

            last_position_clone.set(getPosition());

            return;
        }

        //arm animation
        Vector2 mouse_pos = new Vector2(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY());
        game.getMainViewport().unproject(mouse_pos);

        calculateArmAngle(mouse_pos);

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

    public void setSelectedItems(ItemsContainer.ItemsStack selected_items) {
        this.selected_items = selected_items;
    }

    public ItemsContainer.ItemsStack getSelectedItems() {
        return selected_items;
    }

    public void setRepresentingPlayer(ServerPlayer player) {
        representing_player = player;
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
}
