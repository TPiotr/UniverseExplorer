package explorer.world.object.objects.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.object.HierarchicalWorldObject;

/**
 * Created by RYZEN on 19.10.2017.
 */

public class PlayerRendererOld {

    private Game game;
    private World world;

    private Player player;

    /** PLAYER BODY COMPONENTS **/
    public HierarchicalWorldObject body_root;

    private HierarchicalWorldObject head_object;
    private HierarchicalWorldObject torso_object;
    private HierarchicalWorldObject arm_object;
    private HierarchicalWorldObject legs_object;

    public PlayerRendererOld(Player player, World world, Game game) {
        this.player = player;
        this.game = game;
        this.world = world;

        //init all body parts
        TextureRegion head_region = new TextureRegion(game.getAssetsManager().getTextureRegion("player/base_head"));
        TextureRegion torso_region = new TextureRegion(game.getAssetsManager().getTextureRegion("player/base_torso"));
        TextureRegion arm_region = new TextureRegion(game.getAssetsManager().getTextureRegion("player/base_arm"));

        TextureRegion[][] legs_spritesheet = new TextureRegion(game.getAssetsManager().getTextureRegion("player/base_legs")).split(92, 106);
        TextureRegion legs_region = legs_spritesheet[0][1];

        float PLAYER_SCALE = .4f;

        float HEAD_HEIGHT = head_region.getRegionHeight() * PLAYER_SCALE;
        float HEAD_WIDTH = head_region.getRegionWidth() * PLAYER_SCALE;
        float HEAD_OFFSET = 11f * PLAYER_SCALE;

        float TORSO_HEIGHT = torso_region.getRegionHeight() * PLAYER_SCALE;
        float TORSO_OFFSET = 9f * PLAYER_SCALE;
        float TORSO_WIDTH = torso_region.getRegionWidth() * PLAYER_SCALE;

        float LEGS_HEIGHT = legs_region.getRegionHeight() * PLAYER_SCALE;
        float LEGS_WIDTH = legs_region.getRegionWidth() * PLAYER_SCALE;

        body_root = new HierarchicalWorldObject(new Vector2(0, 0), new Vector2(TORSO_WIDTH, LEGS_HEIGHT + TORSO_HEIGHT - TORSO_OFFSET - HEAD_OFFSET + HEAD_HEIGHT), new Vector2(.5f, .5f), 0, null, world, game);

        head_object = new PlayerTextureHierarchicalWorldObject(head_region, new Vector2(-HEAD_WIDTH / 4f, LEGS_HEIGHT + TORSO_HEIGHT - TORSO_OFFSET - HEAD_OFFSET), new Vector2(head_region.getRegionWidth(), head_region.getRegionHeight()), new Vector2(0, 0), 0, body_root, world, game);

        torso_object = new PlayerTextureHierarchicalWorldObject(torso_region, new Vector2(0, LEGS_HEIGHT - TORSO_OFFSET), new Vector2(torso_region.getRegionWidth(), torso_region.getRegionHeight()), new Vector2(0, 0), 0, body_root, world, game);

        legs_object = new PlayerTextureHierarchicalWorldObject(legs_region, new Vector2(-LEGS_WIDTH / 3f, 0), new Vector2(legs_region.getRegionWidth(), legs_region.getRegionHeight()), new Vector2(0, 0), 0, body_root, world, game);

        arm_object = new PlayerTextureHierarchicalWorldObject(arm_region, new Vector2(3, LEGS_HEIGHT), new Vector2(arm_region.getRegionWidth(), arm_region.getRegionHeight()), new Vector2(.2f, .9f), 0, body_root, world, game);
        arm_object.setLocalRotation(-45f);

        for(HierarchicalWorldObject hwo : body_root.getChildrens()) {
            hwo.getWH().scl(PLAYER_SCALE);
        }
    }

    private float rotation;
    public void tick(float delta) {
        rotation += delta * 200;
        //arm_object.setLocalRotation(rotation % 360);
    }

    public void render(SpriteBatch batch) {
        body_root.getLocalPosition().set(player.getPosition());

        body_root.render(batch);
    }

}
