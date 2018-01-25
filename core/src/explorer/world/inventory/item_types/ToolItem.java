package explorer.world.inventory.item_types;

import explorer.game.framework.Game;
import explorer.world.inventory.Item;

/**
 * Class representing item type tool, used to do some things like breaking blocks etc.
 * Created by RYZEN on 24.01.2018.
 */

public class ToolItem extends Item {

    public enum ToolType {
        PICKAXE, AXE
    }

    protected ToolType tool_type = ToolType.PICKAXE;

    protected float tool_power = 1f;

    public ToolItem(Game game) {
        super(game);
    }

    public ToolType getToolType() {
        return tool_type;
    }

    public float getToolPower() {
        return tool_power;
    }
}
