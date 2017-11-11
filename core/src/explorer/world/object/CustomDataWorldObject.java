package explorer.world.object;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * Interface that gives opportunity to object to save any data
 * Created by RYZEN on 07.10.2017.
 */

public interface CustomDataWorldObject {

    /**
     * Call it and give opened stream to save object data
     * @param output_stream opened binary data output stream
     */
    public void save(DataOutputStream output_stream);

    /**
     * Call it in proper order with others! (what order was while saving objects)
     * @param input_stream opened binary data input stream
     */
    public void load(DataInputStream input_stream);

}
