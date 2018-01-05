package explorer.network.world;

/**
 * Type of object that can receive packet from network packets are addressed for one instance of object not whole type
 * Created by RYZEN on 29.12.2017.
 */

public interface CanReceivePacketWorldObject {

    /**
     * Called when this object instance will receive some packet from network f.e. with info about position change etc.
     * @param packet packet that just come in
     */
    public void receivedPacket(Object packet);

}
