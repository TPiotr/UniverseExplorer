package explorer.game.framework.utils.math;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import explorer.game.framework.Game;
import explorer.network.NetworkClasses;
import explorer.network.NetworkHelper;
import explorer.network.server.GameServer;
import explorer.world.World;
import explorer.world.object.WorldObject;

/**
 * Class used in interpolating rotation over network
 * Created by RYZEN on 24.01.2018.
 */

public class RotationInterpolator {

    /**
     * Variable that stores time in milis which represents last time when new rotation packet or whatever was received
     */
    private long last_time_received;

    /**
     * Variable that stores last time when rotation update packet was send
     */
    private long last_time_send;

    /**
     * Variable that represents probably time in which new packet or whatever containing rotation will be received
     */
    private long probably_next_time_packet;

    /**
     * rotation updates per second
     */
    private int UPS;

    /**
     * Difference in receive time between packet and next one (just 1/frequency) in milis
     */
    private long time_step;

    /**
     * Used in interpolating, this is rotation which we want to reach
     */
    private float target_value;

    /**
     * Used in interpolating, just last received target rotation
     */
    private float last_target_value;

    /**
     * Product of interpolation
     */
    private float value;

    /**
     * Game instance (for sending packets stuff)
     */
    private Game game;

    /**
     * Update packets sender
     */
    private PositionInterpolator.InterpolatorPacketSender sender;

    /**
     * Booleans (default all false) represents 2 options in which RotationInterpolator can work:
     * -only_send - interpolator will only send packets about new rotation independently if this client has to calculate work, will never interpolate rotation
     * -only_receive - interpolator will only wait for new packet with new rotation and interpolate it to achieve smooth movement, will never send any packets
     */
    private boolean only_send, only_receive;

    public RotationInterpolator(PositionInterpolator.InterpolatorPacketSender sender, Game game) {
        this.game = game;
        this.sender = sender;

        this.UPS = 10; //10Hz

        this.time_step = (long) ((1f / UPS) * 1000f);
    }

    public void interpolate(float value, long time_received) {
        last_target_value = target_value;
        target_value = value;

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
                    sender.sendUpdatePacket();

                    last_time_send = System.currentTimeMillis();
                }
            } else {
                //here we have to interpolate position to received one
                float progress = 1.0f - (float) (probably_next_time_packet - System.currentTimeMillis()) / time_step;
                value = MathUtils.lerpAngleDeg(last_target_value, target_value, progress);
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
     * Frequency of sending update packets
     * @return sending update packets frequency
     */
    public int getUPS() {
        return UPS;
    }

    /**
     * Set frequency of sending update packets
     * @param ups new frequency of sending update packets
     */
    public void setUPS(int ups) {
        this.UPS = ups;

        //recalculate vars
        this.time_step = (long) ((1f / UPS) * 1000f);
        this.probably_next_time_packet = last_time_received + time_step;
    }

    /**
     * Getter for interpolated value of angle, int one word final product
     * @return interpolated value of angle, world final product
     */
    public float getValue() {
        return value;
    }
}
