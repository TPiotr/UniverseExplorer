package explorer.game;

import com.badlogic.gdx.graphics.g2d.BitmapFont;

import explorer.game.framework.*;
import explorer.game.screen.screens.GameGUIScreen;
import explorer.game.screen.screens.GameScreen;
import explorer.game.screen.screens.WorldGeneratingScreen;
import explorer.game.screen.screens.WorldLoadingScreen;
import explorer.world.World;

/**
 * Created by RYZEN on 26.10.2017.
 */

public class ExplorerGame extends explorer.game.framework.Game {

    private GameScreen game_screen;
    private GameGUIScreen game_gui_screen;

    @Override
    protected void initGame() {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        game_screen = new GameScreen(this);
        addScreen(game_screen);

        game_gui_screen = new GameGUIScreen(game_screen.getWorld(), this);
        addScreen(game_gui_screen);

        WorldLoadingScreen world_loading_screen = new WorldLoadingScreen(this);
        world_loading_screen.setVisible(false);
        addScreen(world_loading_screen);

        WorldGeneratingScreen world_generating_screen = new WorldGeneratingScreen(this);
        world_generating_screen.setVisible(false);
        addScreen(world_generating_screen);

        //call init of world after all screens init stuff
        game_screen.getWorld().init();
    }


}
