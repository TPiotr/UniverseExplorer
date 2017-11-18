package explorer.universe.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;
import explorer.universe.Universe;

/**
 * Created by RYZEN on 18.11.2017.
 */

public class PlanetUniverseObject extends UniverseObject {

    private static TextureRegion planet_region;

    private int planet_index;

    public PlanetUniverseObject(int planet_index, Vector2 position, Universe universe, Game game) {
        super(position, universe, game);

        wh = new Vector2(64, 64);

        if(planet_region == null) {
            planet_region = game.getAssetsManager().getTextureRegion("white_texture");
        }
    }

    @Override
    public void tick(float delta) {

    }

    @Override
    public void render(SpriteBatch batch) {
        batch.setColor(Color.WHITE);
        batch.draw(planet_region, getPosition().x, getPosition().y, getWH().x, getWH().y);
    }

    @Override
    public void dispose() {

    }

    public int getPlanetIndex() {
        return planet_index;
    }
}
