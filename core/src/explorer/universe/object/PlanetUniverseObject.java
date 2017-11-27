package explorer.universe.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;
import explorer.game.framework.utils.FastNoise;
import explorer.universe.Universe;

/**
 * Created by RYZEN on 23.11.2017.
 */

public class PlanetUniverseObject extends UniverseObject {

    private final static FastNoise noise = new FastNoise(-9);

    private static TextureRegion planet_region;

    private float scale = 1f;
    private int rotation = 0;

    //radius/orbit
    private float radius;
    private float angle;
    private float full_orbit_time;

    private float time_offset;

    private StarUniverseObject parent_star;

    public PlanetUniverseObject(Vector2 position, Universe universe, StarUniverseObject parent_star, float radius, float full_orbit_time, Game game) {
        super(position, universe, game);

        this.radius = radius;
        this.full_orbit_time = full_orbit_time;

        this.parent_star = parent_star;

        wh = new Vector2(32, 32);

        time_offset = noise.GetNoise(position.x, position.y) * 100;

        if(planet_region == null)
            planet_region = game.getAssetsManager().getTextureRegion("white_texture");

    }

    @Override
    public void tick(float delta) {
        //calc pos
        float circle_x = (parent_star.getPosition().x + parent_star.getWH().x / 2f) + radius * MathUtils.cos(angle);
        float circle_y = (parent_star.getPosition().y + parent_star.getWH().y / 2f) + radius * MathUtils.sin(angle);

        getPosition().set(circle_x, circle_y);

        angle = (((universe.getUniverseTime() + time_offset) % full_orbit_time) / full_orbit_time) * 360f;
    }

    @Override
    public void render(SpriteBatch batch) {
        batch.setColor(Color.WHITE);
        batch.draw(planet_region, getPosition().x, getPosition().y, getWH().x * .5f, getWH().y * .5f, getWH().x, getWH().y, scale, scale, rotation);
    }

    @Override
    public void dispose() {

    }
}
