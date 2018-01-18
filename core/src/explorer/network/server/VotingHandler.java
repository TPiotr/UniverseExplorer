package explorer.network.server;

import java.lang.reflect.Array;
import java.util.concurrent.atomic.AtomicInteger;

import explorer.game.framework.Game;
import explorer.network.NetworkClasses;

/**
 * Created by RYZEN on 06.01.2018.
 */

public class VotingHandler {

    private Game game;

    private AtomicInteger voting_index;

    public VotingHandler(Game game) {
        this.game = game;

        voting_index = new AtomicInteger(0);
    }

    public void process(Object o) {
        if(o instanceof NetworkClasses.GoToPlanetRequestPacket) {
            NetworkClasses.GoToPlanetRequestPacket goto_planet_request = (NetworkClasses.GoToPlanetRequestPacket) o;

            NetworkClasses.VoteForGoingToPlanetPacket voting_packet = new NetworkClasses.VoteForGoingToPlanetPacket();
            voting_packet.planet_index = goto_planet_request.planet_index;
            voting_packet.voting_index = voting_index.incrementAndGet();
            game.getGameServer().getServer().sendToAllTCP(voting_packet);
        } else if(o instanceof NetworkClasses.VotingVotePacket) {
            NetworkClasses.VotingVotePacket vote_packet = (NetworkClasses.VotingVotePacket) o;

        }

    }
}
