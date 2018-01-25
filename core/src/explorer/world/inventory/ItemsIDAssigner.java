package explorer.world.inventory;

import java.util.HashMap;

/**
 * Created by RYZEN on 13.07.2017.
 */

public class ItemsIDAssigner {

    /**
     * Class for automated giving items id to prevent from situation where 2 items have same id
     */

    private static HashMap<Class<?>, Integer> ids = new HashMap<Class<?>, Integer>();
    private static int acc_id = 1;

    public static synchronized int getID(Class<?> item_class) {
        if(!ids.containsKey(item_class)) {
            ids.put(item_class, acc_id);
            acc_id++;
            return acc_id - 1;
        }

        return ids.get(item_class);
    }

    public static HashMap<Class<?>, Integer> getIds() {
        return ids;
    }
}
