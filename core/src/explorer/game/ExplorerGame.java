package explorer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.minlog.Log;

import explorer.game.screen.screens.menu.ClientServerChooseScreen;
import explorer.game.screen.screens.menu.MainMenuScreen;
import explorer.game.screen.screens.planet.PlanetGUIScreen;
import explorer.game.screen.screens.planet.PlanetScreen;
import explorer.game.screen.screens.planet.PlayerInventoryScreen;
import explorer.game.screen.screens.universe.UniverseLoadingScreen;
import explorer.game.screen.screens.universe.UniverseScreen;
import explorer.game.screen.screens.planet.WorldGeneratingScreen;
import explorer.game.screen.screens.planet.WorldLoadingScreen;
import explorer.world.World;

/**
 * Created by RYZEN on 26.10.2017.
 */

public class ExplorerGame extends explorer.game.framework.Game {

    private PlanetScreen game_screen;
    private PlanetGUIScreen game_gui_screen;

    @Override
    protected void initGame() {
        Log.set(Log.LEVEL_DEBUG);

        //make game render thread as one with max priority to avoid lags from threads working in background
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        //menu
        MainMenuScreen main_menu_screen = new MainMenuScreen(this);
        main_menu_screen.setVisible(true);
        addScreen(main_menu_screen);

        ClientServerChooseScreen main_menu_client_server_choose_screen = new ClientServerChooseScreen(this);
        main_menu_client_server_choose_screen.setVisible(false);
        addScreen(main_menu_client_server_choose_screen);

        //game screens

        UniverseScreen universe_screen = new UniverseScreen(this);
        universe_screen.setVisible(false);
        addScreen(universe_screen);

        UniverseLoadingScreen universe_loading_screen = new UniverseLoadingScreen(this);
        universe_loading_screen.setVisible(false);
        addScreen(universe_loading_screen);

        game_screen = new PlanetScreen(this);
        addScreen(game_screen);

        game_gui_screen = new PlanetGUIScreen(game_screen, this);
        addScreen(game_gui_screen);
        game_screen.setVisible(false);

        PlayerInventoryScreen inventory_screen = new PlayerInventoryScreen(game_screen, this);
        addScreen(inventory_screen);
        inventory_screen.setVisible(false);

        WorldLoadingScreen world_loading_screen = new WorldLoadingScreen(this);
        world_loading_screen.setVisible(false);
        addScreen(world_loading_screen);

        WorldGeneratingScreen world_generating_screen = new WorldGeneratingScreen(this);
        world_generating_screen.setVisible(false);
        addScreen(world_generating_screen);

        //go directly to some planet
        //game_screen.setVisible(true);
        //universe_screen.setVisible(false);
        //game_screen.createWorld(11);

        //debug
        InputAdapter input = new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                Vector2 gui_touch = new Vector2(screenX, screenY);
                getGUIViewport().unproject(gui_touch);

                Log.debug("GUI COORDS: " + gui_touch);
                return false;
            }
        };
        getInputEngine().addInputProcessor(input);
    }

    @Override
    public void tick(float delta) {
        //update world time
        World.TIME += Gdx.graphics.getRawDeltaTime();

        super.tick(delta);
    }
}
