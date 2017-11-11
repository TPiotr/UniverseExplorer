package explorer.game.screen.gui;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;

import explorer.game.framework.Game;

/**
 * Created by RYZEN on 04.11.2017.
 */

public class VerticalTabel extends GUIComponent {

    private float spacing = 10f; //default val of spacing between components

    private Array<GUIComponent> tabel_components;

    public VerticalTabel(Vector2 position, float spacing, Viewport component_game_viewport, Game game) {
        super(component_game_viewport, game);

        this.position = position;
        this.spacing = spacing;

        this.wh = new Vector2(spacing, spacing);

        tabel_components = new Array<GUIComponent>();
    }

    public VerticalTabel addComponent(GUIComponent component) {
        tabel_components.add(component);

        component.getPosition().add(getPosition());
        getWH().set(Math.max(getWH().x, component.getWH().x), getWH().y + component.getWH().y + spacing);

        return this;
    }

    @Override
    public void tick(float delta) {
        if(!isVisible())
            return;

        for(int i = 0; i < tabel_components.size; i++) {
            tabel_components.get(i).tick(delta);
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        if(!isVisible())
            return;

        for(int i = 0; i < tabel_components.size; i++) {
            tabel_components.get(i).render(batch);
        }
    }
}
