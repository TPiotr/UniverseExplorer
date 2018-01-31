package explorer.game.screen.gui;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

import explorer.game.framework.Game;

/**
 * GUIComponent which have ability to be touched
 * Created by RYZEN on 04.11.2017.
 */

public abstract class ClickableGUIComponent extends GUIComponent {

    protected InputAdapter clickable_adapter;

    public ClickableGUIComponent(Viewport component_game_viewport, Game game) {
        super(component_game_viewport, game);
    }

    protected void createClickableInputAdapter(final boolean block_input_when_pressed_relased) {
        clickable_adapter = new InputAdapter() {

            int p = -1;
            Rectangle temp_rect = new Rectangle();

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if(!isVisible())
                    return false;

                if(p == -1) {
                    //transform screen space touch coords to local viewport and check if component bounding rectangle contains them
                    Vector2 touch = new Vector2(screenX, screenY);
                    component_game_viewport.unproject(touch);

                    temp_rect.set(getPosition().x, getPosition().y, getWH().x, getWH().y);
                    if(temp_rect.contains(touch)) {
                        //store acc pointer
                        p = pointer;

                        //call touched event
                        touched(ClickableGUIComponent.this);

                        return block_input_when_pressed_relased;
                    }
                }

                return false;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if(pointer == p) {
                    released(ClickableGUIComponent.this);
                    p = -1;
                }

                return false;
            }
        };

        game.getInputEngine().addInputProcessor(clickable_adapter);
    }

    /**
     * Event fired when user touched component bounding rectangle
     */
    public abstract void touched(GUIComponent instance);

    /**
     * Event fired at the moment when player release component
     */
    public abstract void released(GUIComponent instance);

    /**
     * Dispose component
     */
    public void dispose() {
        if(clickable_adapter != null)
            game.getInputEngine().remove(clickable_adapter);
    }

    /**
     * Getter for input adapter instance from ClickableGUIComponent
     * @return input adapter clickableGuiComponent instance
     */
    public InputAdapter getClickableInputAdapter() {
        return clickable_adapter;
    }
}
