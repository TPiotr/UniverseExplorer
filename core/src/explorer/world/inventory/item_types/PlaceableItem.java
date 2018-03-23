package explorer.world.inventory.item_types;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

import explorer.game.Helper;
import explorer.game.framework.Game;
import explorer.game.framework.utils.math.MathHelper;
import explorer.world.World;
import explorer.world.inventory.Item;
import explorer.world.object.DynamicWorldObject;
import explorer.world.object.StaticWorldObject;
import explorer.world.object.WorldObject;
import explorer.world.object.objects.player.Player;

/**
 * Another custom Item this item type represents some thing like world object, block
 * Created by RYZEN on 17.02.2018.
 */

public abstract class PlaceableItem extends Item {
    
    /**
     * Flag determining if object which this item represents can be placed over any other objects
     */
    protected boolean can_place_over_object = true;

    /**
     * Flag that is taking into account only if can_place_over_object == true, this flag determines if this object can be placed over collidable objects
     */
    protected boolean can_place_over_collidable_object;

    /**
     * Flat that is taking into account only if can_place_over_object == true , this flag determines if object can be placed over dynamic objects (players too)
     */
    protected boolean can_place_over_dynamic_object;

    /**
     * Flag that determines if object can be placed over foreground blocks (probably all time it will be false)
     */
    protected boolean can_place_over_foreground_blocks;

    /**
     * Flag that determines if object can be placed over background blocks (probably all time it will be true)
     */
    protected boolean can_place_over_background_blocks = true;

    public PlaceableItem(Game game) {
        super(game);
    }

    /**
     * Function that tries to place thing and returns boolean which means if placing process was successful or not
     * @return boolean which means if placing process was successful or not
     */
    public abstract boolean place(Vector2 world_position, boolean foreground_placing_mode, World world, Game game);

    /**
     * Helper function that returns boolean meaning if placing thing with given bound and position is possible, takes into account all PlaceableItem flags
     * @param world_cords world cords on which player want to place object
     * @param width width of thing that will be placed to properly check if it can be placed
     * @param height height of thing that will be placed to properly check if it can be placed
     * @param world world instance to access all needed variable to determine if object can be place and then place it
     * @param game game instance to access all needed variable to determine if object can be place and then place it
     * @return bool that determines if object was placed or not
     */
    public boolean canPlaceObject(Vector2 world_cords, float width, float height, World world, Game game) {
        //object part

        //check objects
        for(int i = 0; i < world.getWorldChunks().length; i++) {
            for(int j = 0; j < world.getWorldChunks()[0].length; j++) {
                for(int k = 0; k < world.getWorldChunks()[i][j].getObjects().size; k++) {
                    WorldObject object = world.getWorldChunks()[i][j].getObjects().get(k);

                    if(MathHelper.overlaps2Rectangles(object.getPosition().x, object.getPosition().y, object.getWH().x, object.getWH().y,
                            world_cords.x, world_cords.y, width, height)) {

                        if(!can_place_over_object) {
                            return false;
                        }

                        if(!can_place_over_collidable_object) {
                            if(object instanceof StaticWorldObject) {
                                return false;
                            }
                        } else if(!can_place_over_dynamic_object) {
                            if(object instanceof DynamicWorldObject) {
                                return false;
                            }
                        }
                    }
                }
            }
        }

        //check players
        if(can_place_over_object && !can_place_over_dynamic_object) {
            //first this player
            if(MathHelper.overlaps2Rectangles(world.getPlayer().getPosition().x, world.getPlayer().getPosition().y, world.getPlayer().getWH().x, world.getPlayer().getWH().y,
                    world_cords.x, world_cords.y, width, height)) {
                return false;
            }

            //server players
            for(int i = 0; i < world.getClonedPlayers().size; i++) {
                Player clone = world.getClonedPlayers().get(i);

                if(MathHelper.overlaps2Rectangles(clone.getPosition().x, clone.getPosition().y, clone.getWH().x, clone.getWH().y,
                        world_cords.x, world_cords.y, width, height)) {
                    return false;
                }
            }
        }


        //blocks part
        //TODO


        //if we are here that means that placing this thing is possible
        return true;
    }

    /**
     * Method to render object as silhouette to show player where he is placing that object, default method is empty so override
     * it if there is need for using it
     * @param batch sprite batch instance to render silhouette
     * @param world_cords world cords at which object will be placed when place button will be pressed
     */
    public void renderSilhouette(SpriteBatch batch, Vector2 world_cords) {

    }
}
