package explorer.world.object.objects.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.minlog.Log;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.inventory.Item;
import explorer.world.inventory.item_types.BodyWearableItem;
import explorer.world.inventory.item_types.WearableItem;
import explorer.world.inventory.items.wearables.BasicBodyItem;
import explorer.world.inventory.items.wearables.BasicHeadItem;
import explorer.world.inventory.items.wearables.BasicLegsItem;

/**
 * Created by RYZEN on 20.01.2018.
 */

public class PlayerRenderer1 {

    private Player player;
    private World world;
    private Game game;

    private Vector2 player_wh;
    private final float SCALE = 3f;

    private WearableItem basic_head_item;
    private BodyWearableItem basic_body_item;
    private WearableItem basic_legs_item;

    //legs texture regions from splitting process used in rendering
    private TextureRegion basic_idle_legs, basic_falling_legs;
    private Animation<TextureRegion> basic_run_animation;

    private TextureRegion wear_idle_legs, wear_falling_legs;
    private Animation<TextureRegion> wear_run_animation;

    private float arm_angle = 10;
    private float ARM_SCALE = 2f;

    public PlayerRenderer1(Player player, World world, Game game) {
        this.player = player;
        this.world = world;
        this.game = game;

        this.player_wh = new Vector2(26, 34).scl(SCALE);

        //debug
        basic_head_item = new BasicHeadItem(game);
        basic_body_item = new BasicBodyItem(game);
        basic_legs_item = new BasicLegsItem(game);

        loadBasicLegs();
    }

    private void loadBasicLegs() {
        //load legs animation
        TextureRegion[][] legs_splited = basic_legs_item.getClothTexture().split(26, 34);

        int offset = 1;
        int frames_count = legs_splited[0].length - offset;
        TextureRegion[] legs_animation = new TextureRegion[frames_count];
        for(int i = 0; i < frames_count; i++) {
            legs_animation[i] = legs_splited[0][i + offset];
        }

        basic_idle_legs = legs_splited[0][0];
        basic_falling_legs = legs_splited[0][8];

        //create run animation
        basic_run_animation = new Animation<TextureRegion>(.1f, legs_animation);
        basic_run_animation.setPlayMode(Animation.PlayMode.LOOP);
    }

    private void loadWearLegs() {
        //load legs animation
        TextureRegion[][] legs_splited = player.getWearLegsItem().getClothTexture().split(26, 34);

        int offset = 1;
        int frames_count = legs_splited[0].length - offset;
        TextureRegion[] legs_animation = new TextureRegion[frames_count];
        for(int i = 0; i < frames_count; i++) {
            legs_animation[i] = legs_splited[0][i + offset];
        }

        wear_idle_legs = legs_splited[0][0];
        wear_falling_legs = legs_splited[0][8];

        //create run animation
        wear_run_animation = new Animation<TextureRegion>(.1f, legs_animation);
        wear_run_animation.setPlayMode(Animation.PlayMode.LOOP);
    }

    private float time;
    private int dir = 1;

    public void tick(float delta) {
        time += delta;
    }

    private WearableItem last_legs = null;
    public void updateLegs() {
        if(last_legs != player.getWearLegsItem()) {
            loadWearLegs();
        }

        last_legs = player.getWearLegsItem();
    }

    public void render(SpriteBatch batch) {
        //render second background arm
        final float background_arm_brightness = .85f;
        batch.setColor(background_arm_brightness, background_arm_brightness, background_arm_brightness, 1f);

        if(player.getWearBodyItem() == null || (player.getWearBodyItem().renderBasic()))
            renderBackgroundArm(batch, basic_body_item.getArmRegion(), basic_body_item);

        if(player.getWearBodyItem() != null)
            renderBackgroundArm(batch, player.getWearBodyItem().getArmRegion(), player.getWearBodyItem());

        //body
        batch.setColor(Color.WHITE);

        if(basic_body_item == null || (basic_body_item.renderBasic()))
            renderTextureRegion(basic_body_item.getClothTexture(), player.getPosition(), player.getWH(), dir, batch);

        if(player.getWearBodyItem() != null) {
            if(player.getWearBodyItem() instanceof WearableItem.CustomWearableRenderer) {
                ((WearableItem.CustomWearableRenderer) player.getWearBodyItem()).render(player.getPosition().x, player.getPosition().y, player.getWH().x, player.getWH().y, player.getWearBodyItem().getClothTexture(), dir, batch);
            } else {
                renderTextureRegion(player.getWearBodyItem().getClothTexture(), player.getPosition(), player.getWH(), dir, batch);
            }
        }

        //head
        if(player.getWearHeadItem() == null || (player.getWearHeadItem().renderBasic()))
            renderTextureRegion(basic_head_item.getClothTexture(), player.getPosition(), player.getWH(), dir, batch);

        if(player.getWearHeadItem() != null) {
            if(player.getWearHeadItem() instanceof WearableItem.CustomWearableRenderer) {
                ((WearableItem.CustomWearableRenderer) player.getWearHeadItem()).render(player.getPosition().x, player.getPosition().y, player.getWH().x, player.getWH().y, player.getWearHeadItem().getClothTexture(), dir, batch);
            } else {
                renderTextureRegion(player.getWearHeadItem().getClothTexture(), player.getPosition(), player.getWH(), dir, batch);
            }
        }

        //legs anim
        if(player.getWearLegsItem() == null || player.getWearLegsItem().renderBasic()) {
            if (Math.abs(player.getVelocity().y) > 1) {
                renderTextureRegion(basic_falling_legs, player.getPosition(), player.getWH(), dir, batch);
            } else if (Math.abs(player.getVelocity().x) < 5) {
                renderTextureRegion(basic_idle_legs, player.getPosition(), player.getWH(), dir, batch);
            } else {
                renderTextureRegion(basic_run_animation.getKeyFrame(time), player.getPosition(), player.getWH(), dir, batch);
            }
        }

        if(player.getWearLegsItem() != null) {
            if(Math.abs(player.getVelocity().y) > 1) {
                if(player.getWearLegsItem() instanceof WearableItem.CustomWearableRenderer) {
                    ((WearableItem.CustomWearableRenderer) player.getWearLegsItem()).render(player.getPosition().x, player.getPosition().y, player.getWH().x, player.getWH().y, wear_falling_legs, dir, batch);
                } else {
                    renderTextureRegion(wear_falling_legs, player.getPosition(), player.getWH(), dir, batch);
                }
            } else if(Math.abs(player.getVelocity().x) < 5) {
                if(player.getWearLegsItem() instanceof WearableItem.CustomWearableRenderer) {
                    ((WearableItem.CustomWearableRenderer) player.getWearLegsItem()).render(player.getPosition().x, player.getPosition().y, player.getWH().x, player.getWH().y, wear_idle_legs, dir, batch);
                } else {
                    renderTextureRegion(wear_idle_legs, player.getPosition(), player.getWH(), dir, batch);
                }
            } else {
                if(player.getWearLegsItem() instanceof WearableItem.CustomWearableRenderer) {
                    ((WearableItem.CustomWearableRenderer) player.getWearLegsItem()).render(player.getPosition().x, player.getPosition().y, player.getWH().x, player.getWH().y, wear_run_animation.getKeyFrame(time), dir, batch);
                } else {
                    renderTextureRegion(wear_run_animation.getKeyFrame(time), player.getPosition(), player.getWH(), dir, batch);
                }
            }
        }

        //arm
        if(player.getWearBodyItem() == null || (player.getWearBodyItem().renderBasic()))
            renderArm(batch, basic_body_item.getArmRegion(), basic_body_item);

        if(player.getWearBodyItem() != null)
            renderArm(batch, player.getWearBodyItem().getArmRegion(), player.getWearBodyItem());
    }

    private Affine2 transform = new Affine2();
    private void renderArm(SpriteBatch batch, TextureRegion arm_region, BodyWearableItem item) {
        ARM_SCALE = SCALE / 2f;

        //cords of socket for arm on body texture (in pixels) (y axis up!!!)
        float body_arm_socket_x = item.getForegroundBodyArmSocket().x;
        float body_arm_socket_y = item.getForegroundBodyArmSocket().y;

        float body_x = player.getPosition().x + (body_arm_socket_x * SCALE);
        float body_y = player.getPosition().y + (body_arm_socket_y * SCALE);

        //cords of origin in pixels (y axis up)
        float local_origin_x = item.getLocalArmOrigin().x;
        float local_origin_y = item.getLocalArmOrigin().y;

        float arm_origin_x = local_origin_x * ARM_SCALE;
        float arm_origin_y = local_origin_y * ARM_SCALE;

        float arm_x = body_x - arm_origin_x;
        float arm_y = body_y - arm_origin_y;

        float arm_width = arm_region.getRegionWidth() * ARM_SCALE;
        float arm_height = arm_region.getRegionHeight() * ARM_SCALE;

        float arm_rotation = (dir == -1) ? (360 - arm_angle) : arm_angle;

        if(player.getSelectedItems() != null && player.getSelectedItems().getItem() != null) {
            if (player.getSelectedItems().getItem() instanceof Item.InHandItemRenderer) {
                if (!((Item.InHandItemRenderer) player.getSelectedItems().getItem()).firstArmThenTool()) {
                    renderInHandItem(local_origin_x, local_origin_y, arm_x, arm_y, arm_origin_x, arm_origin_y, arm_rotation, arm_width, item, batch);
                }
            }
        }

        batch.draw(arm_region, arm_x + ((dir == -1) ? (arm_width + arm_origin_x) : 0), arm_y, arm_origin_x, arm_origin_y, arm_width, arm_height, dir, 1, arm_rotation);

        if(player.getSelectedItems() != null && player.getSelectedItems().getItem() != null) {
            if (player.getSelectedItems().getItem() instanceof Item.InHandItemRenderer) {
                if(((Item.InHandItemRenderer) player.getSelectedItems().getItem()).firstArmThenTool()) {
                    renderInHandItem(local_origin_x, local_origin_y, arm_x, arm_y, arm_origin_x, arm_origin_y, arm_rotation, arm_width, item, batch);
                }
            }
        }
    }

    private void renderInHandItem(float local_origin_x, float local_origin_y, float arm_x, float arm_y, float arm_origin_x, float arm_origin_y, float arm_rotation, float arm_width, BodyWearableItem item, SpriteBatch batch) {
        //try to render holding in arm item
        if(player.getSelectedItems() != null && player.getSelectedItems().getItem() != null) {
            if(player.getSelectedItems().getItem() instanceof Item.InHandItemRenderer) {
                //calc item in hand position
                float radius = (Vector2.dst(local_origin_x, local_origin_y, item.getArmToolSocket().x, item.getArmToolSocket().y)) * ARM_SCALE;

                arm_rotation -= 90;
                float x = radius * MathUtils.cosDeg(arm_rotation);
                float y = radius * MathUtils.sinDeg(arm_rotation);

                x += arm_x + arm_origin_x + ((dir == -1) ? (arm_width + arm_origin_x) : 0);
                y += arm_y + arm_origin_y;

                //calc transform
                transform.idt();
                transform.translate(x, y);
                transform.rotate(arm_rotation);

                //rotate and scale if player is turned to left
                if(dir == -1) {
                    transform.scale(-1, 1);
                    transform.rotate(180);
                }

                ((Item.InHandItemRenderer) player.getSelectedItems().getItem()).render(x, y, arm_rotation, dir, player, transform, batch);
            }
        }
    }

    private void renderBackgroundArm(SpriteBatch batch, TextureRegion arm_region, BodyWearableItem item) {
        //cords of socket for arm on body texture (in pixels) (y axis up!!!)
        float body_arm_socket_x = item.getBackgroundBodyArmSocket().x;
        float body_arm_socket_y = item.getBackgroundBodyArmSocket().y;

        float body_x = player.getPosition().x + (body_arm_socket_x * SCALE);
        float body_y = player.getPosition().y + (body_arm_socket_y * SCALE);

        //cords of origin in pixels (y axis up)
        float local_origin_x = item.getLocalArmOrigin().x;
        float local_origin_y = item.getLocalArmOrigin().y;

        float arm_origin_x = local_origin_x * ARM_SCALE;
        float arm_origin_y = local_origin_y * ARM_SCALE;

        float arm_x = body_x - arm_origin_x;
        float arm_y = body_y - arm_origin_y;

        float arm_width = arm_region.getRegionWidth() * ARM_SCALE;
        float arm_height = arm_region.getRegionHeight() * ARM_SCALE;

        float arm_rotation = (dir == -1) ? (360 - 10) : 10;

        //batch.draw(arm_region, arm_x, arm_y, arm_region.getRegionWidth() * ARM_SCALE, arm_region.getRegionHeight() * ARM_SCALE);
        batch.draw(arm_region, arm_x + ((dir == -1) ? (-arm_width) : 0), arm_y, arm_origin_x, arm_origin_y, arm_width, arm_height, dir, 1, arm_rotation);
    }

    private void renderTextureRegion(TextureRegion region, Vector2 pos, Vector2 wh, int dir, SpriteBatch batch) {
        batch.draw(region, pos.x + ((dir == -1) ? wh.x : 0), pos.y, 0, 0, wh.x, wh.y, dir, 1, 0);
    }

    public Vector2 getPlayerWH() {
        return player_wh;
    }

    public float getArmAngle() {
        return arm_angle;
    }

    public void setArmAngle(float angle) {
        this.arm_angle = angle;

        final float max_angle = 120;
        final float min_angle = 10;

        if(dir == -1) {
            arm_angle = 360 - angle;
            angle += max_angle;

            //clamp
            if (angle > max_angle && 250 > angle) {
                arm_angle = min_angle;
            } else if (angle < min_angle || angle > 250) {
                arm_angle = max_angle;
            }
        } else {
            //clamp
            if (angle > max_angle && 250 > angle) {
                arm_angle = max_angle;
            } else if (angle < min_angle || angle > 250) {
                arm_angle = min_angle;
            }
        }
    }

    public void setArmAngleUnchecked(float angle) {
        this.arm_angle = angle;
    }

    public int getDirection() {
        return dir;
    }

    public void setDirection(int dir) {
        this.dir = dir;
    }
}
