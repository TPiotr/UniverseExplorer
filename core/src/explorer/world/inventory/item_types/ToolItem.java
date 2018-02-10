package explorer.world.inventory.item_types;

import explorer.game.framework.Game;
import explorer.world.inventory.Item;

/**
 * Class representing item type tool, used to do some things like breaking blocks etc.
 * Created by RYZEN on 24.01.2018.
 */

public class ToolItem extends Item {

    /**
     * Enum that tells us what type this tool is (so which blocks can be destroyed faster with it)
     */
    public enum ToolType {
        PICKAXE, AXE, ANY
    }

    /**
     * ToolType of this ToolItem
     */
    protected ToolType tool_type = ToolType.PICKAXE;

    /**
     * Power of this tool (bigger power = smaller time of destroying things)
     */
    protected float tool_power = 1f;

    public ToolItem(Game game) {
        super(game);
    }

    /**
     * Getter fot this tool item tool type
     * @return tool type
     */
    public ToolType getToolType() {
        return tool_type;
    }

    /**
     * Getter for this tool item tool power variable
     * @return power of this tool
     */
    public float getToolPower() {
        return tool_power;
    }
}
