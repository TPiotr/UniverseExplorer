package explorer.world.object.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.minlog.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JEditorPane;

import explorer.game.Helper;
import explorer.game.framework.Game;
import explorer.game.framework.utils.math.FastNoise;
import explorer.network.NetworkClasses;
import explorer.world.World;
import explorer.world.lighting.CustomWorldObjectLight;
import explorer.world.lighting.SolidColorWorldObjectLight;
import explorer.world.object.StaticWorldObject;
import explorer.world.object.WorldObject;
import explorer.world.physics.shapes.RectanglePhysicsShape;

/**
 * Created by RYZEN on 11.01.2018.
 */

public class ProceduralTreeObject extends WorldObject implements SolidColorWorldObjectLight {

    private static class TreeBranch {

        private Vector2 local_branch_position;
        private boolean left;

        public static final Vector2 ORIGIN = new Vector2(29, 2); //25 16 //44, 17
        private Affine2 offset;
        float rot, scale;

        private Vector2 tree_position;

        public TreeBranch(Vector2 local_branch_position, float scale, Vector2 tree_position, boolean left) {
            this.local_branch_position = local_branch_position;
            this.left = left;
            this.tree_position = tree_position;

            this.scale = scale;

            if(left) {
                offset = new Affine2().idt().translate(local_branch_position).translate(-ORIGIN.x * scale, ORIGIN.y * scale);
            } else {
                offset = new Affine2().idt().translate(local_branch_position).translate(ORIGIN.x * scale, ORIGIN.y * scale).scale(-1, 1);
            }
        }

        public void tick(float delta) {
            rot = MathUtils.cos(World.TIME + (local_branch_position.y * 100) + tree_position.x) * 2;
        }

        public void render(SpriteBatch batch, Affine2 tree_transform) {
            temp_affine.set(tree_transform);
            temp_affine.mul(offset);

            //batch.draw(branch_texture, branch_texture.getRegionWidth(), branch_texture.getRegionHeight(), temp_affine);

            batch.setTransformMatrix(temp_matrix.set(temp_affine));
            batch.draw(branch_texture, 0, 0, ORIGIN.x * scale, ORIGIN.y * scale, branch_texture.getRegionWidth() * scale, branch_texture.getRegionHeight() * scale, 1, 1, rot);
        }
    }

    private static TextureRegion trunk_base_texture, trunk_texture, branch_texture, tree_top_texture;

    //4 = 4 pieces of trunk_texture
    private static FastNoise noise;
    private int HEIGHT = 4;
    private static final float SCALE = 3f;

    private static final Color object_light_mask_color = new Color(Color.WHITE);
    private Vector2 render_position;

    private Affine2 tree_transform;
    private static Affine2 temp_affine = new Affine2();
    private static Matrix4 temp_matrix = new Matrix4();

    private Array<TreeBranch> left_branches;
    private Array<TreeBranch> right_branches;

    public ProceduralTreeObject(Vector2 position, World world, final Game game) {
        super(position, world, game);

        if(trunk_base_texture == null) {
            trunk_base_texture = game.getAssetsManager().getTextureRegion("objects/tree/tree_base1");
            trunk_texture = game.getAssetsManager().getTextureRegion("objects/tree/tree_base2");
            branch_texture = game.getAssetsManager().getTextureRegion("objects/tree/tree_branch3");
            tree_top_texture = game.getAssetsManager().getTextureRegion("objects/tree/tree_top1");

            noise = new FastNoise(world.getPlanetProperties().PLANET_SEED);
        }

        can_place_block_over = true;

        tree_transform = new Affine2();

        //generate branches
        left_branches = new Array<TreeBranch>();
        right_branches = new Array<TreeBranch>();

        HEIGHT = (int) ((1f + noise.GetNoise(getPosition().x, 0)) * 5f);
        if(HEIGHT < 3)
            HEIGHT = 3;

        float scale_scale = (float) HEIGHT / 4f;

        for(int i = 0; i < HEIGHT; i++) {
            float progress = ((float) i / (float) HEIGHT);
            float y = progress * (HEIGHT * trunk_texture.getRegionHeight()) + ((1f + noise.GetNoise(getPosition().x + i, 0)) * 5f);

            progress = 1.0f - progress;

            //float min = .5f * scale_scale;
            // float max_diff = .9f * scale_scale; //so max_scale = min + max_diff
            //float scale = min + (progress * max_diff);
            float scale = 1.5f;

            if(y + (TreeBranch.ORIGIN.y * 2) > HEIGHT * trunk_texture.getRegionHeight())
                continue;

            //TreeBranch branch = new TreeBranch(new Vector2(9, y), scale, getPosition(), true);
            //left_branches.add(branch);

            //TreeBranch branch_right = new TreeBranch(new Vector2(16, y), scale, getPosition(), false);
            //right_branches.add(branch_right);
        }

        //calc wh
        getWH().set((trunk_base_texture.getRegionWidth() + (2 * branch_texture.getRegionWidth())) * SCALE, trunk_base_texture.getRegionHeight() * SCALE * HEIGHT);

        render_position = new Vector2(position);
        render_position.x -= (((int) (getWH().x / 2f) / World.BLOCK_SIZE) - 1) * World.BLOCK_SIZE;
    }

    @Override
    public Color getSolidColorOfLightMask() {
        return object_light_mask_color;
    }

    @Override
    public void tick(float delta) {

        for(int i = 0; i < left_branches.size; i++) {
            left_branches.get(i).tick(delta);
        }
        for(int i = 0; i < right_branches.size; i++) {
            right_branches.get(i).tick(delta);
        }

        //float diff = 3;
        //rotation = -diff / 2f + MathUtils.cos(World.TIME + (getPosition().x * 1000)) * diff;
    }

    @Override
    public void render(SpriteBatch batch) {
        batch.setColor(Color.WHITE);

        //prepare tree transform
        tree_transform.idt();
        tree_transform.setToTrnRotScl(render_position.x, render_position.y, getRotation(), SCALE, SCALE);

        //first left branches
        for(int i = 0; i < left_branches.size; i++) {
            left_branches.get(i).render(batch, tree_transform);
        }

        batch.setTransformMatrix(Helper.IDT_MAT);
        //render tree base
        for(int i = 0; i < HEIGHT; i++) {
            if(i == 0) {
                batch.draw(trunk_base_texture, render_position.x, render_position.y, trunk_base_texture.getRegionWidth() * SCALE, trunk_base_texture.getRegionHeight() * SCALE);
            } else {
                batch.draw(trunk_texture, render_position.x, render_position.y + (i * (trunk_texture.getRegionHeight() * SCALE)), trunk_texture.getRegionWidth() * SCALE, trunk_texture.getRegionHeight() * SCALE);
            }
        }

        //render top
        batch.draw(tree_top_texture, render_position.x - (tree_top_texture.getRegionWidth() * SCALE / 2f) + (12 * SCALE), render_position.y + ((HEIGHT) * (trunk_texture.getRegionHeight() * SCALE)), tree_top_texture.getRegionWidth() * SCALE, tree_top_texture.getRegionHeight() * SCALE);

        //right branches
        for(int i = 0; i < right_branches.size; i++) {
            right_branches.get(i).render(batch, tree_transform);
        }

        batch.setTransformMatrix(Helper.IDT_MAT);
    }

    @Override
    public void dispose() {

    }
}
