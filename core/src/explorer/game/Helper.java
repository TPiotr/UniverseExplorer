package explorer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.esotericsoftware.minlog.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import explorer.game.framework.Game;
import explorer.network.server.GameServer;

/**
 * Created by RYZEN on 07.10.2017.
 */

public class Helper {

    /**
     * Helper function saves time because we don't have to write these couple lines of code every time
     * @param tile_x width of one animation frame
     * @param tile_y height of one animation frame
     * @param speed time between frames in seconds
     * @param sprite_sheet texture region that contains all frames for animation
     * @return new animation instance made from given parameters with PlayMode.LOOP as default
     */
    public static Animation<TextureRegion> makeAnimation(int tile_x, int tile_y, float speed, TextureRegion sprite_sheet) {
        TextureRegion[][] frames_spritesheet = sprite_sheet.split(tile_x, tile_y);
        TextureRegion[] frames = new TextureRegion[frames_spritesheet[0].length];

        for(int i = 0; i < frames_spritesheet[0].length; i++)
            frames[i] = frames_spritesheet[0][i];

        Animation<TextureRegion> out = new Animation<TextureRegion>(speed, frames);
        out.setPlayMode(Animation.PlayMode.LOOP);
        return out;
    }


    /**
     * Creates new texture with every color as white if in source was in that place was some color, so it generates some kind of silhouette
     * @param source
     * @return
     */
    public static Texture createWhiteTexture(TextureRegion source) {
        source.getTexture().getTextureData().prepare();
        while(!source.getTexture().getTextureData().isPrepared());

        Pixmap pixmap = source.getTexture().getTextureData().consumePixmap();

        Pixmap new_pixmap = new Pixmap(source.getRegionWidth(), source.getRegionHeight(), pixmap.getFormat());
        Color c = new Color();

        int white = new Color(1,1,1,1).toIntBits();
        int none = new Color(0, 0, 0, 0).toIntBits();

        for(int i = 0; i < source.getRegionWidth(); i++) {
            for(int j = 0; j < source.getRegionHeight(); j++) {
                int x = i + source.getRegionX();
                int y = j + source.getRegionY();

                c.set(pixmap.getPixel(x, y));
                new_pixmap.drawPixel(x, y, (c.a > .1f) ? white : none);
            }
        }

        pixmap.dispose();

        Texture out = new Texture(new_pixmap);
        new_pixmap.dispose();

        return out;
    }

    /**
     * Construct instance from class name, find constructor by in_constructor_arguments array (proper order here!) and as these arguments pass object from constructor_arguments array
     * @param class_name name of the class
     * @param constructor_arguments arguments instances to push to class instance
     * @param in_constructor_types types in proper order from constructor, used to find proper constructor
     * @return new object
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
    public static Object objectFromClassName(String class_name, Object[] constructor_arguments, Class<?>... in_constructor_types) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class<?> clazz = Class.forName(class_name);
        Constructor<?> ctor = clazz.getConstructor(in_constructor_types);
        Object object = ctor.newInstance(constructor_arguments);

        return object;
    }
}
