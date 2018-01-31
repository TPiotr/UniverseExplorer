package explorer.game.framework.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.esotericsoftware.minlog.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by RYZEN on 30.01.2018.
 */

public class ShaderFactory {

    private static HashMap<String, ShaderProgram> shaders = new HashMap<String, ShaderProgram>();

    /**
     *
     * @param vert_path string contains all vertex shader code
     * @param frag_path string containning all fragment shader code
     * @param shader_name name used in logging to determine in what shader something went wrong
     * @return fresh and new shader, remember to dispose it after using!
     */
    public static synchronized ShaderProgram createShaderProgram(String vert_path, String frag_path, String shader_name) {
        if(shaders.containsKey(shader_name))
            return shaders.get(shader_name);

        String vert = Gdx.files.internal(vert_path).readString();
        String frag = Gdx.files.internal(frag_path).readString();

        ShaderProgram shader = new ShaderProgram(vert, frag);

        shader.begin();
        Log.info("(ShaderFactory) Shader compiling log (" + shader_name + "): " + shader.getLog());
        shader.end();

        if(!shader.isCompiled()) {
            Log.error("(ShaderFactory) Shader not compiled! (some problem occured) closing app " + shader_name);
            Gdx.app.exit();
        }

        //register new shader
        shaders.put(shader_name, shader);

        return shader;
    }

    /**
     * Dispose all shader which were loaded during app life
     */
    public static void disposeAll() {
        for(String key : shaders.keySet())
            shaders.get(key).dispose();
    }

}
