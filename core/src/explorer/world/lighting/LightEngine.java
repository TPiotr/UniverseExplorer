package explorer.world.lighting;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import explorer.game.framework.Game;
import explorer.game.Helper;
import explorer.game.framework.utils.ShaderFactory;
import explorer.game.framework.utils.math.MathHelper;
import explorer.world.World;
import explorer.world.chunk.WorldChunk;
import explorer.world.lighting.lights.Light;
import explorer.world.lighting.lights.PointLight;
import explorer.world.object.WorldObject;

/**
 * Created by RYZEN on 07.10.2017.
 */

public class LightEngine {


    /**
     * Game instance for assets management
     */
    private Game game;

    /**
     * World instance for accessing chunks
     */
    private World world;

    /**
     * Frame buffer to storing light map
     */
    private FrameBuffer screen_space_light_map;

    /**
     * Ambient color
     */
    private Vector3 ambient_color;

    /**
     * Array that contains all lights (except ground lighting)
     */
    private Array<Light> lights;

    /**
     * Class that holds points for ground lighting
     */
    private GroundLineRenderer ground_line_renderer;

    /**
     * Custom shader for rendering light maps
     */
    private ShaderProgram lights_shader;

    /**
     * Rectangle used for culling lights
     */
    private Rectangle screen_bounds;

    /**
     * How many lights were drawn in frame
     */
    private int drawn_lights_count;

    /**
     * Frames skipping system light engine will render all lights to light map every skip_frames_count frames
     * With value 2 and FPS on 60 level it looks almost same as with value 1
     */
    private int skip_frames_count = 1;
    /**
     * frame index variable used in frame skipping system
     */
    private int frame_index;

    /**
     * Shader used to render objects that implements SolidColorWorldObjectLight
     */
    private ShaderProgram solid_color_mask_shader;

    public LightEngine(World world, Game game) {
        this.game = game;
        this.world = world;

        //init frame buffer
        screen_space_light_map = new FrameBuffer(Pixmap.Format.RGB888, 1280, 720, false);

        //init ambient color
        ambient_color = new Vector3(.1f, .1f, .1f);

        //init lights array
        lights = new Array<Light>();

        //init ground line lights holder
        ground_line_renderer = new GroundLineRenderer(world, game);

        //init rect for culling
        screen_bounds = new Rectangle();

        //load assets.shaders
        lights_shader = ShaderFactory.createShaderProgram("shaders/basic_vertex_shader.vs", "shaders/light_shader.fs", "LIGHT SHADER");
        solid_color_mask_shader = ShaderFactory.createShaderProgram("shaders/basic_vertex_shader.vs", "shaders/solid_light_shader.fs", "SOLID COLOR LIGHT SHADER");
    }

    /**
     * Render all lights to light map
     * @param batch
     */
    public void render(SpriteBatch batch) {
        //frames skipping system
        if(frame_index++ % skip_frames_count != 0)
            return;

        //restart drawn lights counter
        drawn_lights_count = 0;

        //flush batch and start rendering to light map buffer
        batch.flush();

        screen_space_light_map.begin();

        //clear light map
        Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);

        batch.setShader(lights_shader);

        //set proper blending function to achieve proper light maps "connecting"
        batch.setBlendFunction(GL20.GL_SRC_COLOR, GL20.GL_ONE);

        //update culling bounds
        screen_bounds.set(0, 0, game.getMainViewport().getWorldWidth() * game.getMainCamera().zoom, game.getMainViewport().getWorldHeight() * game.getMainCamera().zoom);
        screen_bounds.x += game.getMainCamera().position.x - screen_bounds.getWidth() / 2;
        screen_bounds.y += game.getMainCamera().position.y - screen_bounds.getHeight() / 2;

        //first ground lights
        batch.setColor(getGroundLineRenderer().getPointLight().getLightColor().x, getGroundLineRenderer().getPointLight().getLightColor().y, getGroundLineRenderer().getPointLight().getLightColor().z, 1f);
        for(int i = 0; i < getGroundLineRenderer().getPositions().size; i++) {
            Vector2 pos = getGroundLineRenderer().getPositions().get(i);
            if(pos == null)
                continue;

            PointLight light = getGroundLineRenderer().getPointLight();
            light.getLightPosition().set(pos);

            Rectangle light_bounds = light.getLightBounds();

            if(screen_bounds.overlaps(light_bounds)) {
                batch.draw(light.getAlphaMask(), light_bounds.x, light_bounds.y, light_bounds.width, light_bounds.height);
                drawn_lights_count++;
            }
        }

        //second custom lights
        for(Light light : lights) {
            //if light is not visible don't render it lol
            if(!light.isVisible())
                continue;

            Rectangle light_bounds = light.getLightBounds();

            if(screen_bounds.overlaps(light_bounds)) {
                batch.draw(light.getAlphaMask(), light_bounds.x, light_bounds.y, light_bounds.width, light_bounds.height);
                drawn_lights_count++;
            }
        }

        //third render objects point light fitting their size
        float ground_light_radius_before = getGroundLineRenderer().getPointLight().getRadius();

        for(int i = 0; i < world.getWorldChunks().length; i++) {
            for(int j = 0; j < world.getWorldChunks()[0].length; j++) {
                WorldChunk chunk = world.getWorldChunks()[i][j];

                for(int ii = 0; ii < chunk.getObjects().size; ii++) {
                    WorldObject chunk_object = chunk.getObjects().get(ii);
                    if(chunk_object == null || !chunk_object.isCastingSelfLight()) continue;

                    if(!(chunk_object instanceof CustomWorldObjectLight) && !(chunk_object instanceof SolidColorWorldObjectLight)) {
                        if(renderObjectLight(chunk_object, chunk, batch)) {
                            renderObjectPointLight(chunk_object, chunk, batch);
                        }
                    }
                }
            }
        }

        //render objects that implements SolidColorWorldObjectLight
        batch.setShader(solid_color_mask_shader);
        for(int i = 0; i < world.getWorldChunks().length; i++) {
            for(int j = 0; j < world.getWorldChunks()[0].length; j++) {
                WorldChunk chunk = world.getWorldChunks()[i][j];

                for(int ii = 0; ii < chunk.getObjects().size; ii++) {
                    WorldObject chunk_object = chunk.getObjects().get(ii);
                    if(chunk_object == null || !chunk_object.isCastingSelfLight()) continue;

                    if((chunk_object instanceof SolidColorWorldObjectLight) && !(chunk_object instanceof CustomWorldObjectLight)) {
                        if(renderObjectLight(chunk_object, chunk, batch)) {
                            renderObjectSilhouetteLight(chunk_object, batch);
                        }
                    }
                }
            }
        }

        batch.setShader(lights_shader);
        //copy loop because custom rendering of light shape could make some texture binds etc. so firstly render objects that we know will batch and them custom ones
        for(int i = 0; i < world.getWorldChunks().length; i++) {
            for(int j = 0; j < world.getWorldChunks()[0].length; j++) {
                WorldChunk chunk = world.getWorldChunks()[i][j];

                for(int ii = 0; ii < chunk.getObjects().size; ii++) {
                    WorldObject chunk_object = chunk.getObjects().get(ii);
                    if(chunk_object == null || !chunk_object.isCastingSelfLight()) continue;

                    if(chunk_object instanceof CustomWorldObjectLight && !(chunk_object instanceof SolidColorWorldObjectLight)) {
                        if(renderObjectLight(chunk_object, chunk, batch)) {
                            ((CustomWorldObjectLight) chunk_object).renderCustomLight(batch);
                        }
                    }
                }
            }
        }

        //fourth render bounding point light for player
        renderObjectPointLight(world.getPlayer(), world.getPlayer().getParentChunk(), batch);

        getGroundLineRenderer().getPointLight().setRadius(ground_light_radius_before);

        //set back blending mode to "normal" one
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        //set shader to default
        batch.setShader(null);

        //flush batch and end rendering to light map
        batch.flush();
        screen_space_light_map.end();
    }

    /**
     * Checks if there is need to render bounding point light for an object
     * @param chunk_object world object
     * @param chunk parent chunk
     * @param batch sprite batch instance
     * @return false if object light is not going to render because is outside of screen bounding rectangle or is covered by blocks
     */
    private boolean renderObjectLight(WorldObject chunk_object, WorldChunk chunk, SpriteBatch batch) {
        int block_x = (int) ((chunk_object.getPosition().x + chunk_object.getWH().x / 2) - chunk.getPosition().x) / World.BLOCK_SIZE;
        int block_y = (int) ((chunk_object.getPosition().y + chunk_object.getWH().y / 2) - chunk.getPosition().y) / World.BLOCK_SIZE;

        if (WorldChunk.inChunkBounds(block_x, block_y)) {
            if (chunk.getBlocks()[block_x][block_y].getForegroundBlock().getBlockID() != world.getBlocks().AIR.getBlockID()
                    || chunk.getBlocks()[block_x][block_y].getBackgroundBlock().getBlockID() != world.getBlocks().AIR.getBlockID()) {
                //because our object overlaps with some block we will not render any light
                return false;
            }
        }
        return true;
    }

    /**
     * Function used to render bounding point light for world object
     * @param chunk_object world object
     * @param chunk parent chunk
     * @param batch sprite batch instance
     */
    private void renderObjectPointLight(WorldObject chunk_object, WorldChunk chunk, SpriteBatch batch) {
        int radius = (int) Math.max(chunk_object.getWH().x, chunk_object.getWH().y);
        radius *= 1.41f; //mul by sqrt2 (because diagonal from border is border * sqrt2)

        //finally render
        PointLight light = getGroundLineRenderer().getPointLight();

        light.getLightPosition().set(chunk_object.getPosition());
        light.getLightPosition().add(chunk_object.getWH().x / 2f, chunk_object.getWH().y / 2f);
        light.setRadius(radius);

        Rectangle light_bounds = light.getLightBounds();

        //check if light is visible on screen and render it
        if(screen_bounds.overlaps(light_bounds)) {
            batch.draw(light.getAlphaMask(), light_bounds.x, light_bounds.y, light_bounds.width, light_bounds.height);
            drawn_lights_count++;
        }
    }

    /**
     * Render silhouette of object using proper shader (solid_color_mask_shader) used by world objects that implements SolidColorWorldObjectLight interface
     * @param object world object
     * @param batch sprite batch
     */
    private void renderObjectSilhouetteLight(WorldObject object, SpriteBatch batch) {
        //here I know batch shader is solid_color_mask_shader

        //cull out invisible for screen camera objects
        if(MathHelper.overlaps2Rectangles(screen_bounds.x, screen_bounds.y, screen_bounds.width, screen_bounds.height,
                object.getPosition().x, object.getPosition().y, object.getWH().x, object.getWH().y)) {

            batch.setColor(((SolidColorWorldObjectLight) object).getSolidColorOfLightMask());
            object.render(batch);
        }
    }

    /**
     * Add new light to engine
     * @param light new light
     */
    public synchronized void addLight(Light light) {
        lights.add(light);
    }

    /**
     * Remove light from light engine
     * @param light light to remove
     */
    public synchronized void removeLight(Light light) {
        lights.removeValue(light, true);
    }

    /**
     * Getter for ambient color, you can change it directly from this method
     * @return ambient color
     */
    public Vector3 getAmbientColor() {
        return ambient_color;
    }

    /**
     * Get array that contains all lights except ground lighting
     * @return array with lights
     */
    public Array<Light> getLights() {
        return lights;
    }

    /**
     * Class that handles ground lighting (from sun)
     * @return instance of class that takes care of ground lighting
     */
    public GroundLineRenderer getGroundLineRenderer() {
        return ground_line_renderer;
    }

    /**
     * @return light map frame buffer
     */
    public FrameBuffer getLightMap() {
        return screen_space_light_map;
    }

    /**
     * @return amount of light rendered in this frame, also includes bounding lights for world objects, so in fact total amount of rendered light masks
     */
    public int getDrawnLightsCount() {
        return drawn_lights_count;
    }

    /**
     * Dispose assets.shaders
     */
    public void dispose() {
        screen_space_light_map.dispose();
    }
}
