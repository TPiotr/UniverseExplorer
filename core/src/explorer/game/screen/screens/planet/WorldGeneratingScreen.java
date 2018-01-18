package explorer.game.screen.screens.planet;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.esotericsoftware.minlog.Log;

import explorer.game.framework.Game;
import explorer.game.screen.Screen;
import explorer.game.screen.screens.Screens;

/**
 * Screen for showing progress bar of world generation
 * Created by RYZEN on 29.10.2017.
 */

public class WorldGeneratingScreen extends Screen {

    /**
     * For now this class is just placeholder for something nice but main mechanism is implemented
     */
    private ShapeRenderer shape_renderer;
    private float progress;

    private float bar_width = 400;
    private float bar_heigth = 125;

    /**
     * Construct new screen instance
     * @param game game instance
     */
    public WorldGeneratingScreen(Game game) {
        super(game);

        NAME = Screens.WORLD_GENERATING_SCREEN_NAME;

        shape_renderer = new ShapeRenderer();
    }

    @Override
    public void tick(float delta) {

    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        Log.debug("(WorldGeneratingScreen) Setting visibility to: " + visible);
    }

    @Override
    public void render(SpriteBatch batch) {
        batch.end();

        shape_renderer.setProjectionMatrix(game.getGUICamera().combined);

        shape_renderer.begin(ShapeRenderer.ShapeType.Line);
        shape_renderer.rect(-bar_width / 2, -bar_heigth / 2, bar_width, bar_heigth);
        shape_renderer.end();

        shape_renderer.begin(ShapeRenderer.ShapeType.Filled);
        shape_renderer.rect(-bar_width / 2, -bar_heigth / 2, bar_width * progress, bar_heigth);
        shape_renderer.end();

        batch.begin();
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    @Override
    public void dispose() {

    }
}
