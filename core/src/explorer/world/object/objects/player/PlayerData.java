package explorer.world.object.objects.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.esotericsoftware.minlog.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import explorer.game.Helper;
import explorer.game.framework.Game;
import explorer.world.inventory.Item;
import explorer.world.inventory.ItemsContainer;
import explorer.world.inventory.item_types.BlockItem;
import explorer.world.inventory.item_types.BodyWearableItem;
import explorer.world.inventory.item_types.WearableItem;

/**
 * Class that contains method to load/save player data like inventory, wearing items etc.
 * Created by RYZEN on 27.01.2018.
 */

public class PlayerData {

    private String player_name;

    private Player player;
    private Game game;

    public static final String PLAYER_DATA_DIR = "game_data/players/";
    public static final String PLAYER_SUFFIX = ".player";

    public PlayerData(String player_name, Player player, Game game) {
        this.player_name = player_name;
        this.game = game;
        this.player = player;
    }

    public void loadData() {
        Log.debug("(PlayerData) Loading player data! (" + player_name + ")");

        FileHandle handle = Gdx.files.local(PLAYER_DATA_DIR + player_name + PLAYER_SUFFIX);
        DataInputStream input = new DataInputStream(handle.read(2048));

        try {
            //load basic inventory
            for(int i = 0; i < Player.INVENTORY_SLOTS_COUNT; i++) {
                ItemsContainer.ItemsStack stack = loadItemsStack(input, player.getItemsContainer());
                player.getItemsContainer().getItems().set(i, stack);
            }

            //load toolbar items
            for(int i = 0; i < Player.TOOLBAR_INVENTORY_SLOTS_COUNT; i++) {
                ItemsContainer.ItemsStack stack = loadItemsStack(input, player.getToolbarItemsContainer());
                player.getToolbarItemsContainer().getItems().set(i, stack);
            }

            //load wearables
            WearableItem head_wearable = (WearableItem) loadItem(input);
            player.setWearHeadItem(head_wearable);

            BodyWearableItem body_wearable = (BodyWearableItem) loadItem(input);
            player.setWearBodyItem(body_wearable);

            WearableItem legs_wearable = (WearableItem) loadItem(input);
            player.setWearLegsItem(legs_wearable);

            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.debug("(PlayerData) Loading player data done! (" + player_name + ")");
    }

    public void saveData() {
        Log.debug("(PlayerData) Saving player data! (" + player_name + ")");

        FileHandle handle = Gdx.files.local(PLAYER_DATA_DIR + player_name + PLAYER_SUFFIX);
        DataOutputStream output = new DataOutputStream(handle.write(false, 1024));

        try {
            //first basic inventory
            for(int i = 0; i < Player.INVENTORY_SLOTS_COUNT; i++) {
                ItemsContainer.ItemsStack stack = player.getItemsContainer().getItems().get(i);
                writeItemsStack(output, stack);
            }

            //toolbar
            for(int i = 0; i < Player.TOOLBAR_INVENTORY_SLOTS_COUNT; i++) {
                ItemsContainer.ItemsStack stack = player.getToolbarItemsContainer().getItems().get(i);
                writeItemsStack(output, stack);
            }

            //write items which player is wearing
            writeItem(output, player.getWearHeadItem());
            writeItem(output, player.getWearBodyItem());
            writeItem(output, player.getWearLegsItem());

            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.debug("(PlayerData) Saving player data done!");
    }

    public void saveEmptyData() {
        Log.debug("(PlayerData) Saving empty player data! (" + player_name + ")");

        FileHandle handle = Gdx.files.local(PLAYER_DATA_DIR + player_name + PLAYER_SUFFIX);
        DataOutputStream output = new DataOutputStream(handle.write(false, 1024));

        try {
            //first basic inventory
            for(int i = 0; i < Player.INVENTORY_SLOTS_COUNT; i++) {
                writeItemsStack(output, new ItemsContainer.ItemsStack(null, 0, null));
            }

            //toolbar
            for(int i = 0; i < Player.TOOLBAR_INVENTORY_SLOTS_COUNT; i++) {
                writeItemsStack(output, new ItemsContainer.ItemsStack(null, 0, null));
            }

            //write items which player is wearing
            writeItem(output, null);
            writeItem(output, null);
            writeItem(output, null);

            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.debug("(PlayerData) Saving empty player data done!");
    }

    private void writeItemsStack(DataOutputStream output, ItemsContainer.ItemsStack stack) throws IOException {
        Item item = (stack == null) ? null : stack.getItem();

        if(writeItem(output, item)) {
            output.writeInt(stack.getInStack());
        }
    }

    private boolean writeItem(DataOutputStream output, Item item) throws IOException {
        if(item == null) {
            output.writeBoolean(false);
            return false;
        }

        output.writeBoolean(true);
        output.writeUTF(item.getClass().getName());
        output.writeUTF(item.getItemProperty());
        return true;
    }

    private ItemsContainer.ItemsStack loadItemsStack(DataInputStream input_stream, ItemsContainer parent) throws IOException {
        Item item = loadItem(input_stream);

        if(item != null) {
            int in_stack_count = input_stream.readInt();
            return new ItemsContainer.ItemsStack(item, in_stack_count, parent);
        }

        return null;
    }

    private Item loadItem(DataInputStream input_stream) throws IOException {
        boolean not_null = input_stream.readBoolean();

        if(not_null) {
            String class_name = input_stream.readUTF();
            String properties = input_stream.readUTF();

            Item item = null;
            try {
                item = (Item) Helper.objectFromClassName(class_name, new Object[] { game }, Game.class);
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

            if(item != null)
                item.setItemProperty(properties);

            return item;
        }
        return null;
    }
}
