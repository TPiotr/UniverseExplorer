package explorer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Created by RYZEN on 07.10.2017.
 */

public class Helper {

    /**
     *
     * @param vert_path string contains all vertex shader code
     * @param frag_path string containning all fragment shader code
     * @param shader_name name used in logging to determine in what shader something went wrong
     * @return fresh and new shader
     */
    public static ShaderProgram createShaderProgram(String vert_path, String frag_path, String shader_name) {
        String vert = Gdx.files.internal(vert_path).readString();
        String frag = Gdx.files.internal(frag_path).readString();

        ShaderProgram shader = new ShaderProgram(vert, frag);

        shader.begin();
        System.out.println(shader.getLog());
        shader.end();

        if(!shader.isCompiled()) {
            System.err.println("Shader not compiled! (some problem occured) closing app " + shader_name);
            Gdx.app.exit();
        }
        return shader;
    }

    /**
     * Helper function saves time because we don't have to write these couple lines of code
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


}
