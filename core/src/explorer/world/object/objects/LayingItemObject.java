package explorer.world.object.objects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import explorer.game.Helper;
import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.inventory.Item;
import explorer.world.inventory.item_types.BlockItem;
import explorer.world.object.DynamicWorldObject;
import explorer.world.object.WorldObject;
import explorer.world.object.objects.player.Player;
import explorer.world.physics.SensorObject;
import explorer.world.physics.shapes.RectanglePhysicsShape;

/**
 * Object that represents some item laying on ground, responsible for detect collision with player and add this to his inventory
 * Created by RYZEN on 24.01.2018.
 */

public class LayingItemObject extends DynamicWorldObject implements SensorObject {

    private Item item;

    /**
     * The most basic constructor that every saveable object have to have!
     *
     * @param position position of new object
     * @param world    world instance
     * @param game     game instance
     */
    public LayingItemObject(Vector2 position, World world, Game game) {
        super(position, world, game);

        saveable = false;

        wh = new Vector2(24, 24);
        physics_shape = new RectanglePhysicsShape(new Vector2(0, 0), getWH(), this);
    }

    @Override
    public void setObjectProperties(HashMap<String, String> properties) {
        super.setObjectProperties(properties);

        //read properties
        if(properties.size() > 0) {
            String item_class_name = properties.get("item");

            if(item_class_name.equals(BlockItem.class.getName())) {
                int block_id = Integer.parseInt(properties.get("block_id"));

                //create new item instance
                Object item = null;
                try {
                    item = Helper.objectFromClassName(item_class_name, new Object[] { game, block_id, world }, Game.class, int.class, World.class);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                }

                if(item != null) {
                    this.item = (BlockItem) item;
                }
            } else {
                Object item = null;
                try {
                    item = Helper.objectFromClassName(item_class_name, new Object[] { game }, Game.class);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                }

                if(item != null) {
                    this.item = (Item) item;
                }
            }
        }
    }

    @Override
    public HashMap<String, String> getObjectProperties() {
        /**
         * So why this item non saveable but still override this method and store data and it work?
         * Because this object contains extra info(storen item type) and in multiplayer it has to be replicated on all clients
         * So engine send class name and properties to client, then client make new instance of this object and sets properties then this objects
         * reads them and reconstruct itself and everything work nice
         */

        if(object_properties == null)
            object_properties = new HashMap<String, String>();

        //save properties;
        object_properties.clear();
        object_properties.put("item", "" + item.getClass().getName());

        if(item instanceof BlockItem)
            object_properties.put("block_id", "" + ((BlockItem) item).getRepresentingBlockID());

        return object_properties;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    @Override
    public void tick(float delta) {

    }

    @Override
    public void render(SpriteBatch batch) {
        if(item != null && item.getItemOnGroundTexture() != null) {
            batch.draw(item.getItemOnGroundTexture(), getPosition().x, getPosition().y, getWH().x, getWH().y);
        }
    }

    @Override
    public void dispose() {

    }

    private boolean removed = false;
    @Override
    public void collide(WorldObject other) {
        if(item == null && !removed) {
            world.removeObject(this, false);
            removed = true;
            return;
        }

        if(other instanceof Player && !removed) {
            Player player = (Player) other;
            if(!player.isClone()) {
                player.getItemsContainer().addItem(item, 1);
                world.removeObject(this, true);
                removed = true;
            }
        }
    }
}
