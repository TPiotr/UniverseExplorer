package explorer.game;

import explorer.game.screen.screens.PlanetGUIScreen;
import explorer.game.screen.screens.PlanetScreen;
import explorer.game.screen.screens.UniverseScreen;
import explorer.game.screen.screens.WorldGeneratingScreen;
import explorer.game.screen.screens.WorldLoadingScreen;

/**
 * Created by RYZEN on 26.10.2017.
 */

public class ExplorerGame extends explorer.game.framework.Game {

    private PlanetScreen game_screen;
    private PlanetGUIScreen game_gui_screen;

    @Override
    protected void initGame() {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        UniverseScreen universe_screen = new UniverseScreen(this);
        universe_screen.setVisible(true);
        addScreen(universe_screen);

        game_screen = new PlanetScreen(this);
        addScreen(game_screen);

        game_gui_screen = new PlanetGUIScreen(game_screen.getWorld(), this);
        addScreen(game_gui_screen);
        game_screen.setVisible(false);

        WorldLoadingScreen world_loading_screen = new WorldLoadingScreen(this);
        world_loading_screen.setVisible(false);
        addScreen(world_loading_screen);

        WorldGeneratingScreen world_generating_screen = new WorldGeneratingScreen(this);
        world_generating_screen.setVisible(false);
        addScreen(world_generating_screen);

        //call init of world after all screens init stuff
        //game_screen.getWorld().init();
    }


}
