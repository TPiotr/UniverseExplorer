package explorer.universe.object;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.minlog.Log;

import explorer.game.framework.Game;
import explorer.game.framework.utils.math.FastNoise;
import explorer.universe.Universe;
import explorer.world.planet.generator.HeightsGenerator;

/**
 * Created by RYZEN on 18.11.2017.
 */

public class StarUniverseObject extends UniverseObject {

    private static TextureRegion star_region;
    private static TextureRegion light_region;
    private static HeightsGenerator heights_generator;

    private static ShapeRenderer shape_renderer;

    private int planet_index;

    private float scale;
    private float rotation;

    private float min_scale_add;
    private float max_scale_add;

    private float speed;

    private Color color;

    //minimal value of main camera zoom when planets can be rendered
    private static final float MIN_ZOOM = 18f;

    //fast noise static instance for generating planets
    private static FastNoise noise = new FastNoise(3);

    //so because it is star it has planets that are orbiting around her
    private Array<PlanetUniverseObject> planets;

    public StarUniverseObject(int planet_index, Vector2 position, Universe universe, Game game) {
        super(position, universe, game);

        this.planet_index = planet_index;

        wh = new Vector2(64, 64);

        if(star_region == null) {
            star_region = game.getAssetsManager().getTextureRegion("star");
            light_region = game.getAssetsManager().getTextureRegion("blocks/light");

            heights_generator = new HeightsGenerator(75, 1, .5f, 1);

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    shape_renderer = new ShapeRenderer();
                }
            };
            Gdx.app.postRunnable(r);
        }

        rotation = MathUtils.random(0, 360);
        scale = 1f;

        min_scale_add = .1f;
        max_scale_add = MathUtils.random(.1f, 1f);

        speed = MathUtils.random(1f, 3f);

        int x = (int) position.x;
        int y = (int) position.y;
        color = new Color(heights_generator.getNoise01(x, y) / 1.5f, heights_generator.getNoise01(x, y) / 2f, heights_generator.getNoise01(x, y), 1f);

        //time to generate planets
        planets = new Array<PlanetUniverseObject>();

        int planets_num = (int) (((noise.GetNoise(position.x, position.y) + 1f) / 2f) * 5f);

        final float min_dst_between_planets = 50;
        final float min_radius_from_start = wh.x * (scale + max_scale_add);

        float last_radius = min_radius_from_start;
        for(int i = 0; i < planets_num; i++) {
            float radius_add = ((noise.GetNoise(position.x * i, position.y) + 1f) / 2f) * 100f;
            float radius = last_radius + min_dst_between_planets + radius_add;

            float full_time_orbit = ((noise.GetNoise(position.x - (i * 100), position.y) + 1f) / 2f) * 400f + 200;

            Vector2 planet_position = new Vector2(position.x + radius, position.y);

            PlanetUniverseObject planet = new PlanetUniverseObject(planet_position, universe, this, radius, full_time_orbit, game);
            planets.add(planet);

            last_radius = radius;
        }

    }

    @Override
    public void tick(float delta) {
        float scale_add = min_scale_add + (MathUtils.cos(universe.getUniverseTime() * speed) * max_scale_add);
        scale = 2f + scale_add;

        //if planets visible update them
        if(game.getMainCamera().zoom < MIN_ZOOM) {
            for(int i = 0; i < planets.size; i++) {
                planets.get(i).tick(delta);
            }
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        //at first render start
        float last_scale = scale;
        scale *= 1.5f;
        batch.setColor(color);
        //batch.setBlendFunction(GL20.GL_SRC_COLOR, GL20.GL_ONE);
        batch.draw(light_region, getPosition().x, getPosition().y, getWH().x * .5f, getWH().y * .5f, getWH().x, getWH().y, scale, scale, rotation);
        //batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        scale = last_scale;

        batch.setColor(Color.WHITE);
        batch.draw(star_region, getPosition().x, getPosition().y, getWH().x * .5f, getWH().y * .5f, getWH().x, getWH().y, scale, scale, rotation);

        //if planets not visible just return this function
        if(game.getMainCamera().zoom > MIN_ZOOM)
            return;

        //render planets orbits
        if(shape_renderer != null) {
            batch.end();
            shape_renderer.setProjectionMatrix(batch.getProjectionMatrix());
            shape_renderer.begin(ShapeRenderer.ShapeType.Line);

            final float max_intensity_at = .01f;
            final float min_intensity_at = 5f;
            float intensity = (game.getMainCamera().zoom - max_intensity_at) / min_intensity_at;
            intensity = MathUtils.clamp(intensity, 0f, 1f);
            intensity = 1f - intensity;

            for (int i = 0; i < planets.size; i++) {
                planets.get(i).renderOrbit(shape_renderer, intensity);
            }

            shape_renderer.end();
            batch.begin();
        }

        //render planets
        final float max_intensity_at = 5f;
        final float min_intensity_at = 18f;
        float intensity = (game.getMainCamera().zoom - max_intensity_at) / min_intensity_at;
        intensity = MathUtils.clamp(intensity, 0f, 1f);
        intensity = 1f - intensity;

        for(int i = 0; i < planets.size; i++) {
            planets.get(i).render(batch, intensity);
        }
    }

    @Override
    public void dispose() {

    }

    public int getPlanetIndex() {
        return planet_index;
    }

    public Array<PlanetUniverseObject> getPlanets() {
        return planets;
    }
}
