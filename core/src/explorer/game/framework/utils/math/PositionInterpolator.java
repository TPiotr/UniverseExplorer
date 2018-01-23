package explorer.game.framework.utils.math;

import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.minlog.Log;

import explorer.game.framework.Game;
import explorer.network.NetworkClasses;
import explorer.network.NetworkHelper;
import explorer.network.server.GameServer;
import explorer.world.World;
import explorer.world.object.WorldObject;

/**
 * Class used in interpolating position of object which is received from network with not high rate like 60Hz (most of the time freq of new position if like 10Hz)
 * so we have to interpolate position to achieve nice looking movement, not jagged one
 * This class handles all stuff to send/receive position of object
 * Created by RYZEN on 19.01.2018.
 */

public class PositionInterpolator {

    /**
     * Variable that stores time in milis which represents last time when new position packet or whatever was received
     */
    private long last_time_received;

    /**
     * Variable that stores last time when position update packet was send
     */
    private long last_time_send;

    /**
     * Variable that represents probably time in which new packet or whatever containing position will be received
     */
    private long probably_next_time_packet;

    /**
     * Position updates per second, when we are sending position (and used in probably_next_time_packet)
     */
    private int UPS;

    /**
     * Difference in receive time between packet and next one (just 1/frequency) in milis
     */
    private long time_step;

    /**
     * Used in interpolating, this is position which we want to reach
     */
    private Vector2 target_position;

    /**
     * Used in interpolating, just last received target position
     */
    private Vector2 last_target_position;

    /**
     * World object to which we are making changes in position
     */
    private WorldObject object;

    /**
     * Game instance (for sending packets stuff)
     */
    private Game game;

    /**
     * Booleans (default all false) represents 2 options in which PositionInterpolator can work:
     * -only_send - interpolator will only send packets about new position independently if this client has to calculate work, will never interpolate position
     * -only_receive - interpolator will only wait for new packet with new position and interpolate it to achieve smooth movement, will never send any packets
     */
    private boolean only_send, only_receive;

    public PositionInterpolator(WorldObject object, Game game) {
        this.object = object;
        this.game = game;

        this.UPS = 10; //10Hz
        this.target_position = new Vector2();
        this.last_target_position = new Vector2();

        this.time_step = (long) ((1f / UPS) * 1000f);
    }

    public void interpolate(float x, float y, long time_received) {
        last_target_position.set(target_position);
        target_position.set(x, y);

        last_time_received = time_received;
        probably_next_time_packet = time_received + time_step;
    }

    public void update() {
        //check if we are in multiplayer game mode
        if(Game.IS_CONNECTED) {
            //check which operation we have to do
            boolean send = !World.SIMULATE_LOGIC;

            if(only_receive)
                send = false;
            if(only_send)
                send = true;

            if(send) {
                //here we have to send to other clients about new position
                if(System.currentTimeMillis() - last_time_send > time_step) {
                    NetworkClasses.PlayerPositionUpdatePacket position_update_packet = new NetworkClasses.PlayerPositionUpdatePacket();
                    position_update_packet.player_connection_id = (Game.IS_HOST) ? GameServer.SERVER_CONNECTION_ID : game.getGameClient().getClient().getID();
                    position_update_packet.x = object.getPosition().x;
                    position_update_packet.y = object.getPosition().y;
                    position_update_packet.tcp = false;

                    NetworkHelper.send(position_update_packet);

                    last_time_send = System.currentTimeMillis();
                }
            } else {
                //here we have to interpolate position to received one
                float progress = 1.0f - (float) (probably_next_time_packet - System.currentTimeMillis()) / time_step;
                object.getPosition().set(last_target_position).lerp(target_position, progress);
            }
        }
    }

    /**
     * True of interpolator is working in only send mode
     * @return represents if interpolator is working in only send mode
     */
    public boolean isOnlySend() {
        return only_send;
    }

    /**
     * Set to only send interpolator mode
     * @param only_send if true interpolator will work in only send mode
     */
    public void setOnlySend(boolean only_send) {
        this.only_send = only_send;

        if(only_send)
            setOnlyReceive(false);
    }

    /**
     * True of interpolator is working in only receive mode
     * @return represents if interpolator is working in only receive mode
     */
    public boolean isOnlyReceive() {
        return only_receive;
    }

    /**
     * Set to only_receive send interpolator mode
     * @param only_receive if true interpolator will work in only receive mode
     */
    public void setOnlyReceive(boolean only_receive) {
        this.only_receive = only_receive;

        if(only_receive)
            setOnlySend(false);
    }

    /**
     * Frequency of sending update position packets
     * @return sending update position packets frequency
     */
    public int getUPS() {
        return UPS;
    }

    /**
     * Set frequency of sending update position packets
     * @param ups new frequency of sending update position packets
     */
    public void setUPS(int ups) {
        this.UPS = ups;

        //recalculate vars
        this.time_step = (long) ((1f / UPS) * 1000f);
        this.probably_next_time_packet = last_time_received + time_step;
    }

}
