package explorer.world.object.objects.player.gui;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

import explorer.game.framework.Game;
import explorer.game.screen.gui.ClickableGUIComponent;
import explorer.game.screen.gui.GUIComponent;
import explorer.game.screen.screens.planet.PlanetScreen;

/**
 * Created by RYZEN on 29.01.2018.
 */

public class BackgroundForegroundSwitch extends ClickableGUIComponent {

    private PlanetScreen planet_screen;

    private TextureRegion foreground_selected, background_selected;
    private boolean foreground_placing = true;

    public BackgroundForegroundSwitch(Vector2 position, Viewport component_game_viewport, PlanetScreen planet_screen, Game game) {
        super(component_game_viewport, game);

        this.planet_screen = planet_screen;

        this.foreground_selected = game.getAssetsManager().getTextureRegion("gui/foreground_selected_switch");
        this.background_selected = game.getAssetsManager().getTextureRegion("gui/background_selected_switch");

        this.position = position;
        this.wh = new Vector2(foreground_selected.getRegionWidth(), foreground_selected.getRegionHeight()).scl(4f);

        createClickableInputAdapter(true);
    }

    @Override
    public void touched(GUIComponent instance) {
        if(!isVisible())
            return;

        if(planet_screen.getWorld() != null && planet_screen.getWorld().getPlayer() != null) {
            foreground_placing = !foreground_placing;
            planet_screen.getWorld().getPlayer().setForegroundPlacing(foreground_placing);
        }
    }

    @Override
    public void released(GUIComponent instance) {

    }

    @Override
    public void tick(float delta) {
        if(!isVisible())
            return;
    }

    @Override
    public void render(SpriteBatch batch) {
        if(!isVisible())
            return;

        batch.draw((foreground_placing) ? foreground_selected : background_selected, getPosition().x, getPosition().y, getWH().x, getWH().y);
    }
}
