package explorer.game;

import explorer.game.screen.screens.planet.PlanetGUIScreen;
import explorer.game.screen.screens.planet.PlanetScreen;
import explorer.game.screen.screens.universe.UniverseLoadingScreen;
import explorer.game.screen.screens.universe.UniverseScreen;
import explorer.game.screen.screens.planet.WorldGeneratingScreen;
import explorer.game.screen.screens.planet.WorldLoadingScreen;

/**
 * Created by RYZEN on 26.10.2017.
 */

public class ExplorerGame extends explorer.game.framework.Game {

    private PlanetScreen game_screen;
    private PlanetGUIScreen game_gui_screen;

    @Override
    protected void initGame() {
        //make game render thread as one with max priority to avoid lags from threads working in background
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        UniverseScreen universe_screen = new UniverseScreen(this);
        universe_screen.setVisible(true);
        addScreen(universe_screen);

        UniverseLoadingScreen universe_loading_screen = new UniverseLoadingScreen(this);
        universe_loading_screen.setVisible(false);
        addScreen(universe_loading_screen);

        game_screen = new PlanetScreen(this);
        addScreen(game_screen);

        game_gui_screen = new PlanetGUIScreen(game_screen, this);
        addScreen(game_gui_screen);
        game_screen.setVisible(false);

        WorldLoadingScreen world_loading_screen = new WorldLoadingScreen(this);
        world_loading_screen.setVisible(false);
        addScreen(world_loading_screen);

        WorldGeneratingScreen world_generating_screen = new WorldGeneratingScreen(this);
        world_generating_screen.setVisible(false);
        addScreen(world_generating_screen);

        //go directly to some planet
        game_screen.setVisible(true);
        universe_screen.setVisible(false);
        game_screen.createWorld(11);
    }


}
