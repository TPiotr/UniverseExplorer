package explorer.world.object.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.AssetsManager;
import explorer.game.framework.Game;
import explorer.network.NetworkClasses;
import explorer.network.server.GameServer;
import explorer.network.world.CanReceivePacketWorldObject;
import explorer.world.World;
import explorer.world.object.DynamicWorldObject;
import explorer.world.physics.PhysicsEngine;
import explorer.world.physics.shapes.RectanglePhysicsShape;

/**
 * Created by RYZEN on 27.10.2017.
 */

public class TestDynamicObject extends DynamicWorldObject implements CanReceivePacketWorldObject {

    private static TextureRegion texture;

    //interpolating vars
    private Vector2 recived_position, last_recived_position, predicted_server_position;
    private long recive_time, probably_next_recive_time;

    public TestDynamicObject(Vector2 position, World world, Game game) {
        super(position, world, game);

        if(texture == null)
            texture = game.getAssetsManager().getTextureRegion("badlogic");

        getWH().set(World.BLOCK_SIZE, World.BLOCK_SIZE);

        physics_shape = new RectanglePhysicsShape(new Vector2(0, 0), getWH(), this);

        //init interpolating vars
        recived_position = new Vector2();
        last_recived_position = new Vector2();
        predicted_server_position = new Vector2();
    }

    @Override
    public void receivedPacket(Object packet) {
        if(packet instanceof NetworkClasses.ObjectPositionUpdatePacket) {
            NetworkClasses.ObjectPositionUpdatePacket pos_update = (NetworkClasses.ObjectPositionUpdatePacket) packet;

            last_recived_position.set(recived_position);
            recived_position.set(pos_update.x, pos_update.y);

            final float teleport_after_distance = 200;
            if(Vector2.dst(recived_position.x, recived_position.y, getPosition().x, getPosition().y) > teleport_after_distance) {
                getPosition().set(recived_position);
            }

            recive_time = System.currentTimeMillis();
            probably_next_recive_time = recive_time + time_step; //+ ((Game.IS_CLIENT) ? game.getGameClient().getClient().getReturnTripTime() : 0); //test it first because on local ping is near 0
        }
    }

    //pos update stuff
    private final float UPS = 10; // 10[Hz] times per second position info is send to server and then to clients
    private final long time_step = (long) ((1f / UPS) * 1000f); //transform to milis
    private long last_time_send;

    @Override
    public void tick(float delta) {
        //normal logic
        if (getVelocity().y == 0) {
            getVelocity().y = 500f;
        }

        //network stuff
        if(World.SIMULATE_LOGIC) {
            //position update packet
            if (System.currentTimeMillis() - last_time_send > time_step) {
                NetworkClasses.ObjectPositionUpdatePacket position_update_packet = new NetworkClasses.ObjectPositionUpdatePacket();
                position_update_packet.object_id = OBJECT_ID;
                position_update_packet.x = getPosition().x;
                position_update_packet.y = getPosition().y;

                if (Game.IS_HOST) {
                    game.getGameServer().getServer().sendToAllTCP(position_update_packet);
                } else {
                    game.getGameClient().getClient().sendTCP(position_update_packet);
                }

                last_time_send = System.currentTimeMillis();
            }
        } else {
            //interpolate position to achieve smooth movement effect
            float progress = 1.0f - (float) (probably_next_recive_time - System.currentTimeMillis()) / time_step;
            predicted_server_position.set(last_recived_position).lerp(recived_position, progress);

            //check if predicted position vs obj pos is not to different
            if(!getPosition().epsilonEquals(predicted_server_position, getWH().x * 2f)) {
                getPosition().set(predicted_server_position);
            }
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        batch.setColor(Color.WHITE);
        batch.draw(texture, getPosition().x, getPosition().y, getWH().x, getWH().y);

        AssetsManager.font.draw(batch, "" + OBJECT_ID + " " + World.SIMULATE_LOGIC + "" + getPosition(), getPosition().x, getPosition().y + 50);
    }

    @Override
    public void dispose() {

    }
}
