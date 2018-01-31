package explorer.game.screen.screens.planet;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.esotericsoftware.minlog.Log;

import explorer.game.framework.AssetsManager;
import explorer.game.framework.Game;
import explorer.game.screen.Screen;
import explorer.game.screen.screens.Screens;
import explorer.game.screen.screens.universe.UniverseScreen;
import explorer.world.World;
import explorer.world.object.WorldObject;

/**
 * Created by RYZEN on 26.10.2017.
 */

public class PlanetScreen extends Screen {

    private World world;
    private BitmapFont font;

    /**
     * Construct new screen instance
     * @param game game instance
     */
    public PlanetScreen(Game game) {
        super(game);

        NAME = Screens.PLANET_SCREEN_NAME;

        font = AssetsManager.font;

        GLProfiler.enable();
    }

    public void createWorld(final int planet_seed) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Log.info("Creating world: " + planet_seed);

                if(world != null)
                    world.dispose();

                world = new World(game, planet_seed);
                world.init();

                game.getMainCamera().zoom = .7f;
                game.getMainCamera().update();
            }
        };
        Gdx.app.postRunnable(r);
    }

    /**
     * Function that cleans up after disconnection (disposes world and restores back game multiplayer flags)
     * Also returns to main menu screen
     */
    public void cleanUpAfterDisconnection() {
        world.dispose();

        Game.IS_HOST = false;
        Game.IS_CONNECTED = false;
        Game.IS_CLIENT = false;

        game.getScreen(Screens.UNIVERSE_SCREEN_NAME, UniverseScreen.class).reset();
        game.setAllScreensInvisible();
        game.getScreen(Screens.MAIN_MENU_SCREEN_NAME).setVisible(true);

        Log.info("(PlanetScreen) Cleaned up after disconnection!");
    }

    @Override
    public void screenSizeChanged(int new_w, int new_h) {
        super.screenSizeChanged(new_w, new_h);

        if(world != null)
            world.screenSizeChanged(new_w, new_h);
    }

    @Override
    public void tick(float delta) {
        //if(world.isGenerating())
        //    setVisible(false);

        if(world != null)
            world.tick(delta);
    }

    @Override
    public void render(SpriteBatch batch) {
        if(world == null || !world.isInitializated())
            return;

        batch.setProjectionMatrix(game.getMainCamera().combined);

        world.render(batch);

        //batch.draw(world.getLightEngine().getLightMap().getColorBufferTexture(), 0, 0, 480, 320);

        GLProfiler.reset();
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
        game.getScreen(Screens.PLANET_GUI_SCREEN_NAME).setVisible(visible);

        Log.debug("Setting planet screen visibility to: " + visible);
    }

    @Override
    public void dispose() {
        if(world != null)
            world.dispose();
    }

    /**
     * Get world instance
     * @return world instance
     */
    public World getWorld() {
        return world;
    }

}
