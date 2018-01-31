package explorer.game.screen.screens.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.awt.Button;

import explorer.game.framework.AssetsManager;
import explorer.game.framework.Game;
import explorer.game.screen.Screen;
import explorer.game.screen.gui.ClickableGUIComponent;
import explorer.game.screen.gui.GUIComponent;
import explorer.game.screen.gui.TextButton;
import explorer.game.screen.gui.TextureButton;
import explorer.game.screen.screens.Screens;
import explorer.world.object.objects.player.PlayerData;

/**
 * Created by RYZEN on 27.01.2018.
 */

public class SelectPlayerScreen extends Screen {


    private Array<GUIComponent> players_components;

    public static String selected_username;

    private Screen next_screen;

    private static final float Y_OFFSET = 300;

    private static final int font_size = 24;
    private BitmapFont font;

    /**
     * Construct new screen instance
     *
     * @param game game instance
     */
    public SelectPlayerScreen(Game game) {
        super(game);

        this.NAME = Screens.SELECT_PLAYER_SCREEN;

        players_components = new Array<GUIComponent>();

        font = game.getAssetsManager().getFont("fonts/pixel_font.ttf", font_size);

        loadPlayers();
    }

    public SelectPlayerScreen next(Screen next_screen) {
        this.next_screen = next_screen;
        return this;
    }

    private void loadPlayers() {
        for(int i = 0; i < players_components.size; i++)
            ((ClickableGUIComponent) players_components.get(i)).dispose();

        players_components.clear();

        FileHandle players_dir = Gdx.files.local(PlayerData.PLAYER_DATA_DIR);
        FileHandle[] players_files = players_dir.list(PlayerData.PLAYER_SUFFIX);

        TextureButton.ButtonListener listener = new TextureButton.ButtonListener() {
            @Override
            public void touched(GUIComponent instance) {
                selected_username = ((TextButton) instance).getText();

                game.setUsername(selected_username);

                setVisible(false);
                next_screen.setVisible(true);
            }

            @Override
            public void released(GUIComponent instance) {}
        };

        int i = 0;
        for(FileHandle player_file : players_files) {
            createPlayerSection(player_file, i++, listener);
        }

        createNewPlayerSection(i);
    }

    private void createPlayerSection(FileHandle player_handle, int index, TextureButton.ButtonListener listener) {
        TextButton player_button = new TextButton(font, "" + player_handle.nameWithoutExtension(), new Vector2(0, Y_OFFSET - (index * (font_size + 5))), game.getGUIViewport(), game);
        players_components.add(player_button);
        player_button.setButtonListener(listener);

        center(player_button);
    }

    private void createNewPlayerSection(int index) {
        TextButton player_button = new TextButton(font, "New player", new Vector2(0, Y_OFFSET - (index * (font_size + 5))), game.getGUIViewport(), game);
        players_components.add(player_button);

        player_button.setButtonListener(new TextureButton.ButtonListener() {
            @Override
            public void touched(GUIComponent instance) {
                Gdx.input.getTextInput(new Input.TextInputListener() {
                    @Override
                    public void input(String text) {
                        if(text.length() > 0) {
                            new PlayerData(text, null, game).saveEmptyData();

                            //update players list
                            loadPlayers();
                        }
                    }

                    @Override
                    public void canceled() {

                    }
                }, "Write new player name", "", "name");
            }

            @Override
            public void released(GUIComponent instance) {}
        });

        center(player_button);
    }

    private void center(GUIComponent comp) {
        comp.getPosition().x = -comp.getWH().x / 2;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        for(int i = 0; i < players_components.size; i++) {
            players_components.get(i).setVisible(visible);
        }
    }

    @Override
    public void tick(float delta) {
        if(!isVisible())
            return;

        tickComponents(delta);

        for(int i = 0; i < players_components.size; i++)
            players_components.get(i).tick(delta);
    }

    @Override
    public void render(SpriteBatch batch) {
        if(!isVisible())
            return;

        batch.setProjectionMatrix(game.getGUICamera().combined);
        renderComponents(batch);

        for(int i = 0; i < players_components.size; i++)
            players_components.get(i).render(batch);
    }

    @Override
    public void dispose() {

    }
}
