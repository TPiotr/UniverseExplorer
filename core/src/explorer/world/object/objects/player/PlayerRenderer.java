package explorer.world.object.objects.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.block.PlanetBoundBlock;

/**
 * Created by RYZEN on 11.01.2018.
 */

public class PlayerRenderer {

    /**
     * Instances from which we can access to in fact everything in engine
     */
    private Game game;
    private World world;
    private Player player;

    /**
     * Scale of player
     */
    private static final float PLAYER_SCALE = 10f;

    /**
     * Vector that stores width & height of player
     */
    private Vector2 wh;

    //old_assets.textures instances

    //to jest ta twoja jedna cała tekstura jak stoi i nic nie robi, puki co dałem tam biała teksture jako placeholdera
    private TextureRegion player_standing_base_texture;

    public PlayerRenderer(Player player, World world, Game game) {
        this.game = game;
        this.world = world;
        this.player = player;

        this.wh = new Vector2();

        load();
    }

    private void load() {
        player_standing_base_texture = game.getAssetsManager().getTextureRegion("objects/player/player_standing_base_texture");

        //calculate wh
        wh.set(player_standing_base_texture.getRegionWidth(), player_standing_base_texture.getRegionHeight()).scl(PLAYER_SCALE);
    }

    public void tick(float delta) {

    }

    public void render(SpriteBatch batch) {
        batch.setColor(Color.WHITE);

        batch.draw(player_standing_base_texture, player.getPosition().x, player.getPosition().y, getPlayerWH().x, getPlayerWH().y);
    }

    /**
     * Getter for player width & height vector
     * @return vector containing info about player width & height
     */
    public Vector2 getPlayerWH() {
        return wh;
    }
}
