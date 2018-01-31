package explorer.game.screen.screens.menu;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private final String trying_to_find_text = "Trying to find local server";
    private final String server_not_found_text = "Server not found :(";
    private final String search_for_server_text = "Search for server";
    private final String failed_to_connect_text = "Failed to connect";

    /**
     * Construct new screen instance
     *
     * @param game game instance
     */
    public ClientServerChooseScreen(final Game game) {
        super(game);

        NAME = Screens.MAIN_MENU_CLIENT_SERVER_CHOOSE_SCREEN_NAME;

        final BitmapFont font = game.getAssetsManager().getFont("fonts/pixel_font.ttf", 15);

        found_lan_server_button = new TextButton(font, search_for_server_text, new Vector2(0, 0), game.getGUIViewport(), game);
        center(found_lan_server_button);
        addScreenComponent(found_lan_server_button);

        found_lan_server_button.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched(GUIComponent instance) {
                if(isVisible()) {
                    //if server wasn't found or searching wasn't started yet, start searching for local servers
                    if(found_lan_server_button.getText().equals(server_not_found_text) || found_lan_server_button.getText().equals(search_for_server_text) || found_lan_server_button.getText().equals(failed_to_connect_text)) {
                        runLocalHostServerDiscovery();
                    }

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
                                found_lan_server_button.setText(failed_to_connect_text);
                                center(found_lan_server_button);
                            }
                        });
                    }
                }
            }

            @Override
            public void released(GUIComponent instance) {}
        });

        //button to connect directly to localhost ip
        TextButton localhost_button = new TextButton(font, "localhost", new Vector2(0, 200), game.getGUIViewport(), game);
        addScreenComponent(localhost_button);
        center(localhost_button);

        localhost_button.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched(GUIComponent instance) {
                try {
                    InetAddress address = InetAddress.getByName("localhost");

                    found_lan_server_button.setText("Connecting...");
                    center(found_lan_server_button);

                    game.connectToServer(address, new GameClient.ConnectedToServerCallback() {
                        @Override
                        public void connected() {
                            setVisible(false);
                            game.getScreen(Screens.UNIVERSE_SCREEN_NAME).setVisible(true);
                        }

                        @Override
                        public void failed() {
                            found_lan_server_button.setText(failed_to_connect_text);
                            center(found_lan_server_button);
                        }
                    });
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void released(GUIComponent instance) {}
        });

        //back button
        TextButton back_button = new TextButton(font, "Back", new Vector2(0, -300), game.getGUIViewport(), game);
        center(back_button);
        addScreenComponent(back_button);

        back_button.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched(GUIComponent instance) {
                setVisible(false);
                game.getScreen(Screens.MAIN_MENU_SCREEN_NAME).setVisible(true);
            }

            @Override
            public void released(GUIComponent instance) {

            }
        });
    }

    private void runLocalHostServerDiscovery() {
        found_lan_server_button.setText(trying_to_find_text);
        center(found_lan_server_button);

        //try to discover local server, on worker thread to not block whole game
        Runnable search_runnable = new Runnable() {
            @Override
            public void run() {
                final AtomicBoolean running = new AtomicBoolean(true);
                Runnable text_animation_run = new Runnable() {
                    @Override
                    public void run() {
                        while (running.get()) {
                            found_lan_server_button.setText(found_lan_server_button.getText() + ".");
                            center(found_lan_server_button);
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {}
                        }
                    }
                };
                Thread animation_thread = new Thread(text_animation_run);
                animation_thread.start();

                found_address = game.getGameClient().getClient().discoverHost(GameServer.UDP_PORT, 5000);
                running.set(false);

                if(found_address == null) {
                    found_lan_server_button.setText(server_not_found_text);
                } else {
                    found_lan_server_button.setText(found_address.getHostAddress());
                }
                center(found_lan_server_button);
            }
        };
        new Thread(search_runnable).start();
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
