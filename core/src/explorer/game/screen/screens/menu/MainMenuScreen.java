package explorer.game.screen.screens.menu;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;
import explorer.game.screen.Screen;
import explorer.game.screen.gui.GUIComponent;
import explorer.game.screen.gui.TextButton;
import explorer.game.screen.gui.TextureButton;
import explorer.game.screen.screens.Screens;
import explorer.game.screen.screens.universe.UniverseScreen;
import explorer.network.client.GameClient;
import explorer.network.server.GameServer;

/**
 * Created by RYZEN on 27.12.2017.
 */

public class MainMenuScreen extends Screen {

    private TextButton start_single;
    private TextButton start_host;
    private TextButton start_client;

    /**
     * Construct new screen instance
     *
     * @param game game instance
     */
    public MainMenuScreen(final Game game) {
        super(game);

        NAME = Screens.MAIN_MENU_SCREEN_NAME;

        BitmapFont font = game.getAssetsManager().getFont("fonts/pixel_font.ttf", 15);

        start_single = new TextButton(font, "Start singleplayer", new Vector2(0, 200), game.getGUIViewport(), game);
        center(start_single);
        addScreenComponent(start_single);

        start_single.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched(GUIComponent instance) {
                if(!isVisible())
                    return;

                game.getScreen(Screens.SELECT_PLAYER_SCREEN, SelectPlayerScreen.class).next(game.getScreen(Screens.UNIVERSE_SCREEN_NAME)).setVisible(true);
                setVisible(false);
            }

            @Override
            public void released(GUIComponent instance) {}
        });

        start_host = new TextButton(font, "Start local server and play", new Vector2(0, 100), game.getGUIViewport(), game);
        center(start_host);
        addScreenComponent(start_host);

        start_host.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched(GUIComponent instance) {
                if(!isVisible())
                    return;

                game.hostServer(new GameServer.GameServerCreatedCallback() {
                    @Override
                    public void created() {
                        game.getScreen(Screens.SELECT_PLAYER_SCREEN, SelectPlayerScreen.class).next(game.getScreen(Screens.UNIVERSE_SCREEN_NAME)).setVisible(true);
                        setVisible(false);
                    }

                    @Override
                    public void failed() {

                    }
                });
            }

            @Override
            public void released(GUIComponent instance) {}
        });

        start_client = new TextButton(font, "Connect to server", new Vector2(0, 0), game.getGUIViewport(), game);
        center(start_client);
        addScreenComponent(start_client);

        start_client.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched(GUIComponent instance) {
                if(!isVisible())
                    return;

                setVisible(false);
                game.getScreen(Screens.SELECT_PLAYER_SCREEN, SelectPlayerScreen.class).next(game.getScreen(Screens.MAIN_MENU_CLIENT_SERVER_CHOOSE_SCREEN_NAME)).setVisible(true);
            }

            @Override
            public void released(GUIComponent instance) {}
        });
    }

    private void center(GUIComponent comp) {
        comp.getPosition().x = -comp.getWH().x / 2;
    }

    @Override
    public void tick(float delta) {
        if(!isVisible())
            return;

        tickComponents(delta);
    }

    @Override
    public void render(SpriteBatch batch) {
        if(!isVisible())
            return;

        batch.setProjectionMatrix(game.getGUICamera().combined);
        renderComponents(batch);
    }

    @Override
    public void dispose() {

    }
}
