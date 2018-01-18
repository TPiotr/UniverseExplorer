package explorer.world.object.objects.player;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import explorer.game.framework.Game;
import explorer.game.framework.utils.MathHelper;
import explorer.network.NetworkClasses;
import explorer.network.NetworkHelper;
import explorer.network.client.ServerPlayer;
import explorer.network.server.GameServer;
import explorer.world.World;
import explorer.world.block.Block;
import explorer.world.chunk.WorldChunk;
import explorer.world.object.DynamicWorldObject;
import explorer.world.object.WorldObject;
import explorer.world.object.objects.TorchObject;
import explorer.world.physics.shapes.RectanglePhysicsShape;

/**
 * Created by RYZEN on 12.10.2017.
 */

public class Player extends DynamicWorldObject {

    private PlayerRenderer player_renderer;

    private boolean left, right, jump;
    private final float PLAYER_SPEED = 300f;

    //clone vars
    private boolean is_clone;
    private ServerPlayer representing_player;

    //interpolating vars
    private Vector2 recived_position, last_recived_position;
    private long recive_time, probably_next_recive_time;

    //finding parent chunk vars
    private Rectangle chunk_rect;

    private Block selected_block;
    private boolean background_placing;

    public Player(Vector2 position, World w, boolean clone, final Game game) {
        super(position, w, game);

        this.is_clone = clone;

        player_renderer = new PlayerRenderer(this, w, game);

        getWH().set(player_renderer.getPlayerWH());
        this.physics_shape = new RectanglePhysicsShape(new Vector2(), new Vector2(getWH()), this);

        chunk_rect = new Rectangle();

        recived_position = new Vector2();
        last_recived_position = new Vector2();

        //is this instance is just clone don't calculate physics for it because we are just receiving position of player and physics for him is
        //calculated on other client side which sends packets about position update
        setPhysicsEnabled(!is_clone);

        if(is_clone)
            return;

        //because player is always on center of in ram map
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

            last_recived_position.set(recived_position);
            recived_position.set(pos_update.x, pos_update.y);

            final float teleport_after_distance = 200;
            if(Vector2.dst(recived_position.x, recived_position.y, getPosition().x, getPosition().y) > teleport_after_distance) {
                getPosition().set(recived_position);
            }

            recive_time = System.currentTimeMillis();
            probably_next_recive_time = recive_time + time_step; //+ ((Game.IS_CLIENT) ? game.getGameClient().getClient().getReturnTripTime() : 0); //test it first because on local ping is near 0
        }
    }

    private void makeDebugInput() {
        InputAdapter input = new InputAdapter() {

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                final Vector2 touch = new Vector2(screenX, screenY);
                game.getMainViewport().unproject(touch);

                if(button == 0) {
                    getPosition().set(touch);
                    getVelocity().set(0, 0);
                } else if(button == 1) {
                    if(selected_block == null)
                        return false;

                    for(int i = 0; i < world.getWorldChunks().length; i++) {
                        for(int j = 0; j < world.getWorldChunks()[0].length; j++) {
                            WorldChunk chunk = world.getWorldChunks()[i][j];
                            Rectangle chunk_rect = new Rectangle(chunk.getPosition().x, chunk.getPosition().y, chunk.getWH().x, chunk.getWH().y);

                            //so we have proper chunk now just transform global cords to local blocks coords
                            if(chunk_rect.contains(touch)) {
                                int local_x = (int) (touch.x - chunk_rect.getX()) / World.BLOCK_SIZE;
                                int local_y = (int) (touch.y - chunk_rect.getY()) / World.BLOCK_SIZE;

                                //we have to notify network if game is using network(game checks it itself)
                                chunk.setBlock(local_x, local_y, selected_block.getBlockID(), background_placing, true);

                                /*for(int k = 0; k < chunk.getObjects().size; k++) {
                                    WorldObject o = chunk.getObjects().get(k);
                                    if(MathHelper.overlaps2Rectangles(o.getPosition().x, o.getPosition().y, o.getWH().x, o.getWH().y, touch.x, touch.y, 1, 1)) {
                                        world.removeObject(o, true);
                                    }
                                }*/

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

    @Override
    public void move(Vector2 move_vector) {
        super.move(move_vector);

        if(is_clone)
            return;

        game.getMainCamera().position.add(move_vector.x, move_vector.y, 0);
        game.getMainCamera().update();
        getPosition().add(move_vector.x, move_vector.y);
    }

    //pos update stuff
    private final float UPS = 10; // 10[Hz] times per second position info is send to server and then to clients
    private final long time_step = (long) ((1f / UPS) * 1000f); //transform to milis
    private long last_time_send;

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

        player_renderer.tick(delta);

        if(is_clone) {
            //interpolate position to achieve smooth movement effect
            float progress = 1.0f - (float) (probably_next_recive_time - System.currentTimeMillis()) / time_step;
            getPosition().set(last_recived_position).lerp(recived_position, progress);

            return;
        }

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

        //send player position update packet
        if((Game.IS_HOST || Game.IS_CLIENT) && System.currentTimeMillis() - last_time_send > time_step) {
            NetworkClasses.PlayerPositionUpdatePacket position_update_packet = new NetworkClasses.PlayerPositionUpdatePacket();
            position_update_packet.player_connection_id = (Game.IS_HOST) ? GameServer.SERVER_CONNECTION_ID : game.getGameClient().getClient().getID();
            position_update_packet.x = getPosition().x;
            position_update_packet.y = getPosition().y;

            NetworkHelper.send(position_update_packet);

            last_time_send = System.currentTimeMillis();
        }

        game.getMainCamera().position.lerp(new Vector3(getPosition().x + (getWH().x / 2f), getPosition().y + (getWH().y / 2f), 0), delta * 10f);
        game.getMainCamera().update();
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

    public void setSelectedBlock(Block block) {
        this.selected_block = block;
    }

    public void setBackgroundPlacing(boolean bool) {
        this.background_placing = bool;
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

    @Override
    public void render(SpriteBatch batch) {
        player_renderer.render(batch);
    }

    @Override
    public void dispose() {

    }
}
