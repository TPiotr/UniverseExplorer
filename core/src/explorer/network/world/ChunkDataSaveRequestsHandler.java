package explorer.network.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.minlog.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import explorer.game.framework.Game;
import explorer.network.NetworkClasses;
import explorer.world.FileChunkDataProvider;
import explorer.world.World;

/**
 * Class that saves received chunk data in byte array to disk, use different thread to not block everything
 * Created by RYZEN on 31.01.2018.
 */

public class ChunkDataSaveRequestsHandler {

    /**
     * Game & world instance
     */
    private Game game;
    private World world;

    /**
     * Array that contains all pending save requests
     */
    private Array<NetworkClasses.ChunkDataSaveRequestPacket> save_requests;

    /**
     * Worker service to save all data to disk in background
     */
    private ExecutorService worker_pool;

    public ChunkDataSaveRequestsHandler(World world, Game game) {
        this.world = world;
        this.game = game;

        save_requests = new Array<NetworkClasses.ChunkDataSaveRequestPacket>();

        createWorkerPool();
    }

    private void createWorkerPool() {
        final ThreadFactory thread_factory = new ThreadFactory() {

            AtomicInteger index = new AtomicInteger();

            @Override
            public Thread newThread(Runnable runnable) {
                Thread out = new Thread(runnable);
                out.setDaemon(true);
                out.setName("Server-ChunkDataSaveRequestsHandler-" + index.getAndIncrement());

                return out;
            }
        };

        worker_pool = Executors.newSingleThreadExecutor(thread_factory);
    }

    /**
     * Method that saved received bytes into disk to store data
     * @param packet packet with new data
     */
    private void saveToDisk(NetworkClasses.ChunkDataSaveRequestPacket packet) {
        int x = (int) packet.position.x / World.CHUNK_WORLD_SIZE;
        int y = (int) packet.position.y / World.CHUNK_WORLD_SIZE;

        String world_dir = World.getWorldDirectory(world.getPlanetProperties().PLANET_SEED);
        final String path = FileChunkDataProvider.getPathToChunkFile(world_dir, new Vector2(x * World.CHUNK_WORLD_SIZE, y * World.CHUNK_WORLD_SIZE));
        FileHandle handle = Gdx.files.local(path);

        handle.writeBytes(packet.chunk_data, false);
    }

    /**
     * Method responsible for handling ChunkDataSaveRequestPacket packet when server receives it
     * @param packet new packet instance
     */
    public synchronized void handleRequest(final NetworkClasses.ChunkDataSaveRequestPacket packet) {
        synchronized (save_requests) {
            Log.debug("(ChunkDataSaveRequestsHandler) New save request! From con_id: " + packet.connection_id);
            save_requests.add(packet);

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    saveToDisk(packet);
                    save_requests.removeValue(packet, true);
                    Log.debug("(ChunkDataSaveRequestsHandler) Save request done! From con_id: " + packet.connection_id);
                }
            };
            worker_pool.submit(r);
        }
    }

    /**
     * Method used in HostNetworkChunkDataProvider & ChunkDataRequestsHandler useful when some client want data that wasn't saved to disk yet so we can grab data from there
     * @param chunk_x local chunk x
     * @param chunk_y local chunk y
     * @return chunk data in byte array, could be null!
     */
    public synchronized byte[] getPendingData(int chunk_x, int chunk_y) {
        synchronized (save_requests) {
            for(int i = 0; i < save_requests.size; i++) {
                NetworkClasses.ChunkDataSaveRequestPacket packet = save_requests.get(i);

                if(packet == null)
                    continue;

                int x = (int) packet.position.x / World.CHUNK_WORLD_SIZE;
                int y = (int) packet.position.y / World.CHUNK_WORLD_SIZE;

                if(x == chunk_x && y == chunk_y)
                    return packet.chunk_data;
            }
        }
        return null;
    }

    /**
     * Dispose (= save all pending save requests and shutdown thread pool)
     */
    public void dispose() {
        Log.info("(ChunkDataSaveRequestsHandler) Stopping ChunkDataSaveRequestsHandler worker");
        List<Runnable> pending_tasks = worker_pool.shutdownNow();
        if(pending_tasks.size() > 0) {
            Log.info("(ChunkDataSaveRequestsHandler) Saving pending chunks (size: " + pending_tasks.size() + ")");

            for(Runnable r : pending_tasks)
                r.run();
        }
        Log.info("(ChunkDataSaveRequestsHandler) Stopped!");
    }
}
