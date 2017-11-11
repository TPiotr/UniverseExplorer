package explorer.world.planet.planet_type.types;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;
import explorer.world.background.Background;
import explorer.world.background.BackgroundLayer;

/**
 * Created by RYZEN on 05.11.2017.
 */

public class TestPlanetBackground extends Background {

    public TestPlanetBackground(Vector2 position, Game game) {
        super(position);

        //center whole background on the screen
        position.set(-1280f / 2f, -720f / 2f);

        TextureRegion sky_texture = game.getAssetsManager().getTextureRegion("background/sky_day");

        BackgroundLayer static_sky = new BackgroundLayer(sky_texture, new Vector2(), new Vector2(), new Vector2(1280, 720), this);
        addLayer(static_sky);

    }
}
