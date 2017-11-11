package explorer.game.screen.screens;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import explorer.game.framework.Game;
import explorer.game.screen.Screen;

/**
 * Screen that is showing progress bar when player teleports and engine have to load whole >=75% of chunks,
 * we have to stop game because of possibility of bugs (like falling down from the world because world wasn't loaded yet)
 * Created by RYZEN on 26.10.2017.
 */

public class WorldLoadingScreen extends Screen {

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
    public WorldLoadingScreen(Game game) {
        super(game);

        NAME = Screens.WORLD_LOADING_SCREEN_NAME;

        shape_renderer = new ShapeRenderer();
    }

    @Override
    public void tick(float delta) {
        //calc progress
        GameScreen game_screen = game.getScreen(Screens.GAME_SCREEN_NAME, GameScreen.class);

        int not_dirty_count = 0;
        for(int i = 0; i < game_screen.getWorld().getWorldChunks().length; i++) {
            for(int j = 0; j < game_screen.getWorld().getWorldChunks()[0].length; j++) {
                if(!game_screen.getWorld().getWorldChunks()[i][j].isDirty())
                    not_dirty_count++;
            }
        }

        progress = (float) not_dirty_count / (float) (game_screen.getWorld().getWorldChunks().length * game_screen.getWorld().getWorldChunks().length);

        //>= to avoid some bugs if something other is broken this will not stop whole game
        if(progress >= 1f) {
            game_screen.setVisible(true);
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
