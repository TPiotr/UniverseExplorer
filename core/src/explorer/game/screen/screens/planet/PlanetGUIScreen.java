package explorer.game.screen.screens.planet;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryonet.Server;

import org.omg.CORBA.ServerRequest;

import explorer.game.framework.AssetsManager;
import explorer.game.framework.Game;
import explorer.game.framework.utils.MathHelper;
import explorer.game.screen.Screen;
import explorer.game.screen.ScreenComponent;
import explorer.game.screen.gui.TextButton;
import explorer.game.screen.gui.TextureButton;
import explorer.game.screen.screens.Screens;
import explorer.game.screen.screens.planet.PlanetScreen;
import explorer.network.client.ServerPlayer;
import explorer.world.World;
import explorer.world.block.Block;
import explorer.world.block.CustomColorBlock;
import explorer.world.block.CustomRenderingBlock;

/**
 * Created by RYZEN on 05.11.2017.
 */

public class PlanetGUIScreen extends Screen {

    /**
     * Little GUI component that allows player to select what block to place
     * It reads data from Blocks database so it don't have to be updated
     */
    private class BlockPlacingSelector implements ScreenComponent {

        private Vector2 position;

        private final float block_render_size = 64;
        private final float spacing = 5;
        private final int max_in_column = 10;
        private int selected = 0;

        private PlanetScreen planet_screen;
        private boolean visible = true;

        private TextureRegion white_texture;

        private TextButton background_checkbox;
        private boolean background_placing = false;

        public BlockPlacingSelector(final Vector2 position, final PlanetScreen planet_screen) {
            this.position = position;

            //copy blocks references
            this.planet_screen = planet_screen;

            //placeholder texture for invisible blocks like air
            white_texture = game.getAssetsManager().getTextureRegion("white_texture");

            //create background checkbox
            background_checkbox = new TextButton(AssetsManager.font, "Foreground", new Vector2(position.x - 120, position.y + 20), game.getGUIViewport(), game);
            background_checkbox.setButtonListener(new TextureButton.ButtonListener() {
                @Override
                public void touched() {
                    background_placing = !background_placing;
                    background_checkbox.setText((background_placing) ? "Background" : "Foreground");

                    planet_screen.getWorld().getPlayer().setBackgroundPlacing(background_placing);
                }

                @Override
                public void released() {

                }
            });

            InputAdapter input = new InputAdapter() {
                @Override
                public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                    if(!visible)
                        return false;

                    Vector2 unproj = new Vector2(screenX, screenY);
                    game.getGUIViewport().unproject(unproj);

                    if(planet_screen.getWorld().getBlocks() == null)
                        return false;

                    int i = 0;
                    for(Block block : planet_screen.getWorld().getBlocks().getAllBlocksHashmap().values()) {
                        int column = i / max_in_column;
                        int row = i % max_in_column;

                        if(MathHelper.overlaps2Rectangles(position.x + (column * (block_render_size + spacing)), position.y - (row * (block_render_size + spacing)), block_render_size, block_render_size, unproj.x, unproj.y, 1, 1)) {
                            selected = i;
                            planet_screen.getWorld().getPlayer().setSelectedBlock(block);

                            return true;
                        }

                        i++;
                    }

                    return false;
                }
            };

            game.getInputEngine().addInputProcessor(input);
        }

        public void setVisible(boolean visible) {
            background_checkbox.setVisible(visible);
            this.visible = visible;
        }

        @Override
        public void tick(float delta) {
            background_checkbox.tick(delta);
        }

        @Override
        public void render(SpriteBatch batch) {
            int i = 0;

            if(planet_screen.getWorld().getBlocks() == null)
                return;

            for(Block block : planet_screen.getWorld().getBlocks().getAllBlocksHashmap().values()) {
                int column = i / max_in_column;
                int row = i % max_in_column;

                if(block.getTextureRegion(Block.COLLIDE_NONE) != null) {
                    if(block instanceof CustomColorBlock) {
                        batch.setColor(((CustomColorBlock) block).getBlockColor());
                        batch.draw(block.getTextureRegion(Block.COLLIDE_NONE), position.x + (column * (block_render_size + spacing)), position.y - (row * (block_render_size + spacing)), block_render_size, block_render_size);
                    } else if(block instanceof CustomRenderingBlock) {
                        CustomRenderingBlock cr_block = (CustomRenderingBlock) block;
                        cr_block.render(batch, Block.COLLIDE_NONE, position.x + (column * (block_render_size + spacing)), position.y - (row * (block_render_size + spacing)), block_render_size, block_render_size, false);
                    } else {
                        batch.draw(block.getTextureRegion(Block.COLLIDE_NONE), position.x + (column * (block_render_size + spacing)), position.y - (row * (block_render_size + spacing)), block_render_size, block_render_size);
                    }
                } else {
                    //just draw white texture as placeholder because it is probably air or other invisible block
                    batch.draw(white_texture, position.x + (column * (block_render_size + spacing)), position.y - (row * (block_render_size + spacing)), block_render_size, block_render_size);
                }

                if(i == selected) {
                    batch.setColor(.5f, .5f, .5f, .5f);
                    batch.draw(white_texture, position.x + (column * (block_render_size + spacing)), position.y - (row * (block_render_size + spacing)), block_render_size, block_render_size);
                }

                batch.setColor(Color.WHITE);

                i++;
            }

            background_checkbox.render(batch);
        }
    }

    private TextureButton left_button, right_button, jump_button;

    private BlockPlacingSelector blocks_selector;

    private BitmapFont debug_font;

    /**
     * Construct new screen instance
     *
     * @param game game instance
     */
    public PlanetGUIScreen(final PlanetScreen planet_screen, Game game) {
        super(game);

        debug_font = game.getAssetsManager().getFont("fonts/pixel_font.ttf", 15);

        NAME = Screens.PLANET_GUI_SCREEN_NAME;

        TextureRegion white_texture = game.getAssetsManager().getTextureRegion("white_texture");
        left_button = new TextureButton(new Vector2(-550, -300), new Vector2(128, 128), new TextureRegion(white_texture), game.getGUIViewport(), game);
        addScreenComponent(left_button);

        right_button = new TextureButton(new Vector2(-200, -300), new Vector2(128, 128), new TextureRegion(white_texture), game.getGUIViewport(), game);
        addScreenComponent(right_button);

        jump_button = new TextureButton(new Vector2(450, -300), new Vector2(128, 128), new TextureRegion(white_texture), game.getGUIViewport(), game);
        addScreenComponent(jump_button);

        //create listeners
        left_button.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched() {
                planet_screen.getWorld().getPlayer().setLeft(true);
            }

            @Override
            public void released() {
                planet_screen.getWorld().getPlayer().setLeft(false);
            }
        });

        right_button.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched() {
                planet_screen.getWorld().getPlayer().setRight(true);
            }

            @Override
            public void released() {
                planet_screen.getWorld().getPlayer().setRight(false);
            }
        });

        jump_button.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched() {
                planet_screen.getWorld().getPlayer().setJump(true);
            }

            @Override
            public void released() {
                planet_screen.getWorld().getPlayer().setJump(false);
            }
        });

        blocks_selector = new BlockPlacingSelector(new Vector2(500, 290), planet_screen);
        addScreenComponent(blocks_selector);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        jump_button.setVisible(visible);
        left_button.setVisible(visible);
        right_button.setVisible(visible);
        blocks_selector.setVisible(visible);
    }

    @Override
    public void tick(float delta) {
        if(!isVisible())
            return;

        tickComponents(delta);
    }

    @Override
    public void render(SpriteBatch batch) {
        if (!isVisible())
            return;

        batch.setProjectionMatrix(game.getGUICamera().combined);
        renderComponents(batch);

        if (Game.IS_HOST || Game.IS_CLIENT) {
            renderPlayers((Game.IS_HOST) ? game.getGameServer().getPlayers() : game.getGameClient().getPlayers(), batch);
        }
    }

    private void renderPlayers(Array<ServerPlayer> players, SpriteBatch batch) {
        debug_font.draw(batch, "Players list:", 180, 350);
        for (int i = 0; i < players.size; i++) {
            ServerPlayer player = players.get(i);
            debug_font.draw(batch, player.username + " (" + player.connection_id + (player.is_host ? ", host)" : ")"), 180, 320 - (i * 30));
        }
    }

    @Override
    public void dispose() {

    }
}
