package explorer.game.screen.screens.universe;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import explorer.game.framework.Game;
import explorer.game.screen.Screen;
import explorer.game.screen.screens.Screens;

/**
 * Screen that is showing progress bar when universe camera teleports or universe want to load lot of universe chunks
 * we have to stop game because of possibility of bugs and to now show how universe is loading on the eyes of user
 * Created by RYZEN on 18.11.2017.
 */

public class UniverseLoadingScreen extends Screen {

    /**
     * For now this class is just placeholder for something nice but main mechanism is implemented
     */
    private ShapeRenderer shape_renderer;
    private float progress;

    private float bar_width = 400;
    private float bar_heigth = 125;

    /**
     * Construct new screen instance
     *
     * @param game game instance
     */
    public UniverseLoadingScreen(Game game) {
        super(game);

        NAME = Screens.UNIVERSE_LOADING_SCREEN_NAME;

        shape_renderer = new ShapeRenderer();
    }

    @Override
    public void tick(float delta) {
        //calc progress
        UniverseScreen universe_screen = game.getScreen(Screens.UNIVERSE_SCREEN_NAME, UniverseScreen.class);

        int not_dirty_count = 0;
        for(int i = 0; i < universe_screen.getUniverse().getUniverseChunks().length; i++) {
            for(int j = 0; j < universe_screen.getUniverse().getUniverseChunks()[0].length; j++) {
                if(!universe_screen.getUniverse().getUniverseChunks()[i][j].isDirty())
                    not_dirty_count++;
            }
        }

        progress = (float) not_dirty_count / (float) (universe_screen.getUniverse().getUniverseChunks().length * universe_screen.getUniverse().getUniverseChunks().length);

        //>= to avoid some bugs if something other is broken this will not stop whole game
        if(progress >= 1f) {
            universe_screen.setVisible(true);
            setVisible(false);

            progress = 0f;
        }
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

    @Override
    public void dispose() {

    }
}
