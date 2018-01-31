package explorer.world.object.objects.player.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

import explorer.game.framework.Game;
import explorer.game.framework.utils.math.MathHelper;
import explorer.game.screen.gui.GUIComponent;
import explorer.game.screen.screens.planet.PlanetScreen;
import explorer.world.World;
import explorer.world.chunk.WorldChunk;
import explorer.world.object.objects.player.Player;

/**
 * Component used to point with touch screen to some block preciously
 *
 * Created by RYZEN on 29.01.2018.
 */

public class PlayerBlockSelectorGUIComponent extends GUIComponent {

    /**
     * Selector listener
     */
    public interface SelectorListener {
        /**
         * Called when selecting process started
         */
        void start();

        /**
         * Called when selecting process stopped
         */
        void stop();

        /**
         * Called when selected block changed to a new one
         * @param new_block_x new selected block local chunk cords x
         * @param new_block_y new selected block local chunk cords y
         * @param chunk chunk instance on which block is selected (so to get block just use chunk.getBlock(new_block_x, new_block_y))
         * @param pointer_world_pos position of pointer in world cords system
         */
        void changed(int new_block_x, int new_block_y, WorldChunk chunk, Vector2 pointer_world_pos);
    }

    private SelectorListener selector_listener;

    private PlanetScreen planet_screen;
    private World world;
    private Player player;

    //pointer stuff
    private boolean pointing;
    private Vector2 pointer_gui_cords;
    private Vector2 pointer_world_cords;

    private TextureRegion pointer_texture, pointer_texture1;

    public PlayerBlockSelectorGUIComponent(Viewport component_game_viewport, final PlanetScreen planet_screen, final Game game) {
        super(component_game_viewport, game);

        this.planet_screen = planet_screen;

        this.pointer_gui_cords = new Vector2();
        this.pointer_world_cords = new Vector2();

        this.pointer_texture = game.getAssetsManager().getTextureRegion("gui/blocks_pointer");
        this.pointer_texture1 = game.getAssetsManager().getTextureRegion("gui/blocks_pointer_block");

        InputAdapter input = new InputAdapter() {
            int p = -1;

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if(!isVisible() || player == null)
                    return false;

                Vector2 world_touch = new Vector2(screenX, screenY);
                game.getMainViewport().unproject(world_touch);

                if(p == -1) {
                    if(MathHelper.overlaps2Rectangles(world_touch.x, world_touch.y, 1, 1,
                            player.getPosition().x, player.getPosition().y, player.getWH().x, player.getWH().y)) {

                        calculatePointerPositions(screenX, screenY);

                        //call listener about changes
                        if(selector_listener != null) {
                            selector_listener.start();

                            //calc block x, y and find proper chunk
                            if(world != null) {
                                loop:
                                for (int i = 0; i < world.getWorldChunks().length; i++) {
                                    for (int j = 0; j < world.getWorldChunks()[0].length; j++) {
                                        WorldChunk chunk = world.getWorldChunks()[i][j];
                                        Rectangle chunk_rect = new Rectangle(chunk.getPosition().x, chunk.getPosition().y, chunk.getWH().x, chunk.getWH().y);

                                        //so we have proper chunk now just transform global cords to local blocks coords
                                        if (chunk_rect.contains(pointer_world_cords)) {
                                            int local_x = (int) (pointer_world_cords.x - chunk_rect.getX()) / World.BLOCK_SIZE;
                                            int local_y = (int) (pointer_world_cords.y - chunk_rect.getY()) / World.BLOCK_SIZE;

                                            selector_listener.changed(local_x, local_y, chunk, pointer_world_cords);
                                            break loop;
                                        }
                                    }
                                }
                            }
                        }

                        pointing = true;
                        p = pointer;
                        return true;
                    }
                }

                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if(!isVisible() || player == null)
                    return false;

                if(p == pointer) {
                    //calc touch pos
                    calculatePointerPositions(screenX, screenY);

                    for (int i = 0; i < world.getWorldChunks().length; i++) {
                        for (int j = 0; j < world.getWorldChunks()[0].length; j++) {
                            WorldChunk chunk = world.getWorldChunks()[i][j];
                            Rectangle chunk_rect = new Rectangle(chunk.getPosition().x, chunk.getPosition().y, chunk.getWH().x, chunk.getWH().y);

                            //so we have proper chunk now just transform global cords to local blocks coords
                            if (chunk_rect.contains(pointer_world_cords)) {
                                int local_x = (int) (pointer_world_cords.x - chunk_rect.getX()) / World.BLOCK_SIZE;
                                int local_y = (int) (pointer_world_cords.y - chunk_rect.getY()) / World.BLOCK_SIZE;

                                //call listener
                                selector_listener.changed(local_x, local_y, chunk, pointer_world_cords);
                                return true;
                            }
                        }
                    }
                }

                return false;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if(!isVisible() || player == null)
                    return false;

                if(p == pointer) {
                    if(selector_listener != null)
                        selector_listener.stop();

                    pointing = false;
                    p = -1;
                }

                return false;
            }

            Vector2 touch = new Vector2();
            Vector2 player_center = new Vector2();
            private void calculatePointerPositions(int screenX, int screenY) {
                //calculate player center in gui cords system
                player_center.set(player.getPosition().x + player.getWH().x / 2f, player.getPosition().y + player.getWH().y / 2f);
                game.getMainViewport().project(player_center);
                game.getGUIViewport().unproject(player_center);

                //calculate touch in gui cords
                touch.set(screenX, screenY);
                game.getGUIViewport().unproject(touch);

                //calculate pointer position in gui cords system
                pointer_gui_cords.x = (2f * player_center.x) - touch.x;
                pointer_gui_cords.y = (2f * player_center.y) - touch.y;

                //also calc pointer in world cords system
                pointer_world_cords.set(pointer_gui_cords);
                pointer_world_cords = game.getGUIViewport().project(pointer_world_cords);
                pointer_world_cords.y = Gdx.graphics.getHeight() - pointer_world_cords.y;
                pointer_world_cords = game.getMainViewport().unproject(pointer_world_cords);
            }
        };
        game.getInputEngine().addInputProcessor(input);
    }

    @Override
    public void tick(float delta) {
        //try to get player instance
        if(planet_screen.getWorld() == null)
            return;


        //update player & world reference
        player = planet_screen.getWorld().getPlayer();
        world = planet_screen.getWorld();

        //if player is still null skip logic
        if(player == null)
            return;
    }

    @Override
    public void render(SpriteBatch batch) {
        //render pointer in gui cords system
        if(isPointing()) {
            float size = World.BLOCK_SIZE / game.getMainCamera().zoom;
            batch.draw(pointer_texture, pointer_gui_cords.x - size / 2f, pointer_gui_cords.y - size / 2f, size, size);
        }
    }

    public void setSelectorListener(SelectorListener listener) {
        this.selector_listener = listener;
    }

    public boolean isPointing() {
        return pointing;
    }
}
