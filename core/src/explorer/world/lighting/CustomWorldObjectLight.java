package explorer.world.lighting;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import explorer.world.chunk.WorldChunk;
import explorer.world.object.WorldObject;

/**
 * Interface that allows world object to render custom bounding light for ambient lighting
 * When f.e. default circular light is not suited for particular situation
 * Created by RYZEN on 11.01.2018.
 */

public interface CustomWorldObjectLight {

    /**
     * Render custom light for world object, remember rendering in this method is to light map not normal screen
     * @param batch instance of sprite batch which will be used to render light texture
     */
    public void renderCustomLight(SpriteBatch batch);

}
