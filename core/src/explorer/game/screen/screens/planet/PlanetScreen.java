package explorer.game.screen.screens.planet;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.profiling.GLProfiler;

import explorer.game.framework.AssetsManager;
import explorer.game.framework.Game;
import explorer.game.screen.Screen;
import explorer.game.screen.screens.Screens;
import explorer.world.World;

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

        world = new World(game, 122);

        font = AssetsManager.font;

        GLProfiler.enable();
    }

    public void createWorld(final int planet_seed) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                System.out.println("Creating world: " + planet_seed);

                world.dispose();

                world = new World(game, planet_seed);
                world.init();

                game.getMainCamera().zoom = .7f;
                game.getMainCamera().update();
            }
        };
        Gdx.app.postRunnable(r);
    }

    @Override
    public void screenSizeChanged(int new_w, int new_h) {
        super.screenSizeChanged(new_w, new_h);

        world.screenSizeChanged(new_w, new_h);
    }

    @Override
    public void tick(float delta) {
        if(world.isGenerating())
            setVisible(false);

        world.tick(delta);
    }

    @Override
    public void render(SpriteBatch batch) {
        if(world == null || !world.isInitializated())
            return;

        batch.setProjectionMatrix(game.getMainCamera().combined);

        world.render(batch);

        batch.setProjectionMatrix(game.getGUICamera().combined);

        font.draw(batch, "FPS: "+ Gdx.graphics.getFramesPerSecond(), -620, 350);
        font.draw(batch, "Drawn lights:" + world.getLightEngine().getDrawnLightsCount(), -620, 330);
        font.draw(batch, "Total ground lights:" + world.getLightEngine().getGroundLineRenderer().getPositions().size, -620, 310);
        font.draw(batch, "Current tasks: " + game.getThreadPool().getActuallyTasksRunningCount(), -620, 290);

        font.draw(batch, "Chunk x: " + ((world.getPlayer().getPosition().x / World.CHUNK_WORLD_SIZE) % world.getPlanetProperties().PLANET_SIZE), -620, 270);
        font.draw(batch, "Chunk x: " + ((world.getPlayer().getPosition().x / World.CHUNK_WORLD_SIZE)), -450, 270);

        font.draw(batch, "Phys. obj. count: " + world.getPhysicsEngine().getAllObjectsCount(), -620, 250);
        //font.draw(batch, "Chunks colliders count: " + world.getPhysicsEngine().getPhysicsEngineChunksHelper().getChunkCollidersCount(), -620, 230);

        font.draw(batch, "Draw calls: " + GLProfiler.drawCalls, -620, 210);
        font.draw(batch, "Vertices rendered: " + (int) GLProfiler.vertexCount.average, -620, 190);
        font.draw(batch, "Texture binds: " + GLProfiler.textureBindings, -620, 170);


        //batch.draw(world.getLightEngine().getLightMap().getColorBufferTexture(), 0, 0, 480, 320);

        GLProfiler.reset();
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
        game.getScreen(Screens.PLANET_GUI_SCREEN_NAME).setVisible(visible);
    }

    @Override
    public void dispose() {
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
