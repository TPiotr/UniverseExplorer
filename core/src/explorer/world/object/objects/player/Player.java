package explorer.world.object.objects.player;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.block.Block;
import explorer.world.chunk.WorldChunk;
import explorer.world.object.DynamicWorldObject;
import explorer.world.object.objects.TestDynamicObject;
import explorer.world.physics.shapes.RectanglePhysicsShape;

/**
 * Created by RYZEN on 12.10.2017.
 */

public class Player extends DynamicWorldObject {

    private PlayerRenderer player_renderer;

    private boolean left, right, jump;
    private final float PLAYER_SPEED = 300f;

    //finding parent chunk vars
    private Rectangle chunk_rect;

    private Block selected_block;
    private boolean background_placing;

    public Player(Vector2 position, World w, final Game game) {
        super(position, w, game);

        game.getMainCamera().position.set(position.x, position.y, 0);
        game.getMainCamera().update();

        player_renderer = new PlayerRenderer(this, w, game);

        getWH().set(player_renderer.body_root.getWH());
        this.physics_shape = new RectanglePhysicsShape(new Vector2(), new Vector2(getWH()), this);

        chunk_rect = new Rectangle();

        //because player is always on center of in ram map
        setParentChunk(w.getWorldChunks()[1][1]);

        //debug
        makeDebugInput();
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

                                //chunk.setBlock(local_x, local_y, selected_block.getBlockID(), false);
                                chunk.setBlock(local_x, local_y, selected_block.getBlockID(), background_placing);
                            }
                        }
                    }

                } else if(button == 2) {
                    for(int i = 0; i < world.getWorldChunks().length; i++) {
                        for(int j = 0; j < world.getWorldChunks()[0].length; j++) {
                            WorldChunk chunk = world.getWorldChunks()[i][j];
                            Rectangle chunk_rect = new Rectangle(chunk.getPosition().x, chunk.getPosition().y, chunk.getWH().x, chunk.getWH().y);

                            //so we have proper chunk now just transform global cords to local blocks coords
                            if(chunk_rect.contains(touch)) {
                                world.getLightEngine().getGroundLineRenderer().removeChunkBoundLights(chunk);
                            }
                        }
                    }
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

        game.getMainCamera().position.lerp(new Vector3(getPosition().x, getPosition().y + (getWH().y / 2f), 0), delta * 10f);
        game.getMainCamera().update();

        player_renderer.tick(delta);
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

    @Override
    public void render(SpriteBatch batch) {
        player_renderer.render(batch);
    }

    @Override
    public void dispose() {

    }
}
