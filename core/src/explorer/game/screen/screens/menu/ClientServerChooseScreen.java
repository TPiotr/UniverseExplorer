package explorer.game.screen.screens.menu;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import explorer.game.framework.Game;
import explorer.game.screen.Screen;
import explorer.game.screen.gui.GUIComponent;
import explorer.game.screen.gui.TextButton;
import explorer.game.screen.gui.TextureButton;
import explorer.game.screen.screens.Screens;
import explorer.network.client.GameClient;
import explorer.network.server.GameServer;

/**
 * Created by RYZEN on 28.12.2017.
 */

public class ClientServerChooseScreen extends Screen {


    private TextButton found_lan_server_button;

    private InetAddress found_address;

    /**
     * Construct new screen instance
     *
     * @param game game instance
     */
    public ClientServerChooseScreen(final Game game) {
        super(game);

        NAME = Screens.MAIN_MENU_CLIENT_SERVER_CHOOSE_SCREEN_NAME;

        final BitmapFont font = game.getAssetsManager().getFont("fonts/pixel_font.ttf", 15);

        final String trying_to_find_text = "Trying to find local server...";
        final String server_not_found_text = "Server not found :(";

        found_lan_server_button = new TextButton(font, trying_to_find_text, new Vector2(0, 0), game.getGUIViewport(), game);
        center(found_lan_server_button);
        addScreenComponent(found_lan_server_button);

        found_lan_server_button.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched() {
                if(isVisible()) {
                    if(found_address != null) {
                        found_lan_server_button.setText("Connecting...");
                        center(found_lan_server_button);

                        game.connectToServer(found_address, new GameClient.ConnectedToServerCallback() {
                            @Override
                            public void connected() {
                                setVisible(false);
                                game.getScreen(Screens.UNIVERSE_SCREEN_NAME).setVisible(true);
                            }

                            @Override
                            public void failed() {
                                found_lan_server_button.setText("Failed to connect");
                                center(found_lan_server_button);
                            }
                        });
                    }
                }
            }

            @Override
            public void released() {}
        });

        //try to discover local server, on worker thread to not block whole game
        Runnable search_runnable = new Runnable() {
            @Override
            public void run() {
                found_address = game.getGameClient().getClient().discoverHost(GameServer.UDP_PORT, GameServer.TCP_PORT);
                if(found_address == null) {
                    found_lan_server_button.setText(server_not_found_text);
                } else {
                    found_lan_server_button.setText(found_address.getHostAddress());
                }
                center(found_lan_server_button);
            }
        };
        game.getThreadPool().runTask(search_runnable);

        //button to connect direcltly to localhost
        TextButton localhost_button = new TextButton(font, "localhost", new Vector2(0, 200), game.getGUIViewport(), game);
        addScreenComponent(localhost_button);
        center(localhost_button);

        localhost_button.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched() {
                try {
                    InetAddress address = InetAddress.getByName("localhost");

                    game.connectToServer(address, new GameClient.ConnectedToServerCallback() {
                        @Override
                        public void connected() {
                            setVisible(false);
                            game.getScreen(Screens.UNIVERSE_SCREEN_NAME).setVisible(true);
                        }

                        @Override
                        public void failed() {
                            found_lan_server_button.setText("Failed to connect");
                            center(found_lan_server_button);
                        }
                    });
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void released() {}
        });
    }

    private void center(GUIComponent comp) {
        comp.getPosition().x = -comp.getWH().x / 2;
    }

    @Override
    public void tick(float delta) {
        if(!isVisible())
            return;

        super.tickComponents(delta);
    }

    @Override
    public void render(SpriteBatch batch) {
        if(!isVisible())
            return;

        batch.setProjectionMatrix(game.getGUICamera().combined);
        super.renderComponents(batch);
    }

    @Override
    public void dispose() {

    }
}
