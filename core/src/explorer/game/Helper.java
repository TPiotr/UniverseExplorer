package explorer.game;

import com.badlogic.gdx.Gdx;
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

}
