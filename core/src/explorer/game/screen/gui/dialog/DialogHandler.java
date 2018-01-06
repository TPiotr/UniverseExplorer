package explorer.game.screen.gui.dialog;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;

import explorer.game.framework.Game;
import explorer.game.screen.gui.GUIComponent;

/**
 * Class responsible for handling dialogs rendering, updating etc.
 * Dialog is popup window with text, some options etc depends on its type
 * Usefull f.e. for making votes for going to some planet, showing changelog
 *
 * Created by RYZEN on 06.01.2018.
 */

public class DialogHandler extends GUIComponent {

    /**
     * Dialog abstract class contains basic functions so we can build over it custom dialog with options, messages etc.
     */
    public abstract static class Dialog extends GUIComponent {

        public Dialog(Viewport component_game_viewport, Game game) {
            super(component_game_viewport, game);
        }

        /**
         * Dispose for this dialog (remove input listeners, free up memory etc)
         */
        public abstract void dispose();

        public void destroySelf() {
            dispose();
            dialogs.removeValue(this, true);
        }
    }

    private static Array<Dialog> dialogs;

    public DialogHandler(Viewport component_game_viewport, Game game) {
        super(component_game_viewport, game);

        dialogs = new Array<Dialog>();
    }

    public void showDialog(Dialog dialog) {
        dialogs.add(dialog);
    }

    public void setVisible(boolean vis) {
        super.setVisible(vis);

        for(int i = 0; i < dialogs.size; i++)
            dialogs.get(i).setVisible(vis);
    }

    @Override
    public void tick(float delta) {
        if(!isVisible())
            return;

        for(int i = 0; i < dialogs.size; i++)
            dialogs.get(i).tick(delta);
    }

    @Override
    public void render(SpriteBatch batch) {
        if(!isVisible())
            return;

        for(int i = 0; i < dialogs.size; i++)
            dialogs.get(i).render(batch);
    }
}
