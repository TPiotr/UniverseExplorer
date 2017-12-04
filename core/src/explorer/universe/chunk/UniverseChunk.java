package explorer.universe.chunk;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import explorer.game.framework.Game;
import explorer.universe.Universe;
import explorer.universe.generator.UniverseChunkDataProvider;
import explorer.universe.object.UniverseObject;

/**
 * Created by RYZEN on 14.11.2017.
 */

public class UniverseChunk {

    /**
     * Game instance
     */
    private Game game;

    /**
     * Vectors to store chunk position and wh
     */
    private Vector2 position, wh;
    
    /**
     * Universe instance
     */
    private Universe universe;

    /**
     * Rectangles used to cull objects when rendering
     */
    private Rectangle screen_bounding_rectangle;
    private Rectangle object_bounding_rectangle;

    /**
     * Chunk objects array
     */
    private Array<UniverseObject> objects;

    /**
     * Flag that determines if this chunk is dirty (if loading process is in progress)
     */
    private boolean is_dirty;

    /**
     * Future instance used to abort loading task when we have to
     */
    private Future<?> generating_future;

    /**
     * Width & height of render chunk
     */
    public static final int RENDER_CHUNK_SIZE = Universe.UNIVERSE_CHUNK_SIZE / 8;

    /**
     * Array that contains all render chunks instances of this chunk
     */
    private Array<RenderChunk> render_chunks;

    public UniverseChunk(Vector2 position, Universe universe, Game game) {
        this.universe = universe;
        this.position = position;
        this.game = game;

        this.wh = new Vector2(Universe.UNIVERSE_CHUNK_SIZE, Universe.UNIVERSE_CHUNK_SIZE);
        
        wh.set(Universe.UNIVERSE_CHUNK_SIZE, Universe.UNIVERSE_CHUNK_SIZE);
        
        //init rects
        screen_bounding_rectangle = new Rectangle();
        object_bounding_rectangle = new Rectangle();

        //init objects array
        objects = new Array<UniverseObject>();

        //create render chunks
        int chunks_count = Universe.UNIVERSE_CHUNK_SIZE / RENDER_CHUNK_SIZE;
        render_chunks = new Array<RenderChunk>(true, chunks_count * chunks_count);

        for(int i = 0; i < chunks_count; i++) {
            for(int j = 0; j < chunks_count; j++) {
                RenderChunk render_chunk = new RenderChunk(new Vector2(i * RENDER_CHUNK_SIZE, j * RENDER_CHUNK_SIZE), this);
                render_chunks.add(render_chunk);
            }
        }
    }

    /**
     * Get index of proper render chunk instance (to get instance use this index to get object from render_chunks array)
     * Calculation based on given object position
     * @param object object to which we want to get render chunk instance
     * @return index of render chunk where given object is
     */
    private synchronized int getRenderChunkIndex(UniverseObject object) {
        int x = (int) ((object.getPosition().x - getPosition().x) / RENDER_CHUNK_SIZE);
        int y = (int) ((object.getPosition().y - getPosition().y) / RENDER_CHUNK_SIZE);

        final int in_row = (Universe.UNIVERSE_CHUNK_SIZE / RENDER_CHUNK_SIZE);

        return y + (x * in_row);
    }

    /**
     * Move chunk in some direction determined by given parameters next load data
     * @param chunks_x one unit of this equals one World.CHUNK_WORLD_SIZE
     * @param chunks_y one unit of this equals one World.CHUNK_WORLD_SIZE
     */
    public void move(int chunks_x, int chunks_y) {
        //abort old task if have  to
        if(generating_future != null && !generating_future.isDone()) {
            System.out.println("(Universe) Aborting loading task!");
            generating_future.cancel(true);

            try {
                generating_future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch(CancellationException e) {
                //normal thing because when .cancel() before started to work calling .get() will throw this exception so its okay 
                //e.printStackTrace();
            }
        }

        final long loading_start = System.nanoTime();

        //first update this chunk position
        getPosition().add(chunks_x * Universe.UNIVERSE_CHUNK_SIZE, chunks_y * Universe.UNIVERSE_CHUNK_SIZE);

        //mark as dirty
        is_dirty = true;

        //next get data for "new chunk"
        UniverseChunkDataProvider provider = universe.getChunksDataProvider();

        generating_future = provider.getUniverseChunkData(new UniverseChunkDataProvider.UniverseChunkDataLoaded() {
            @Override
            public void loaded(UniverseChunkDataProvider.UniverseChunkData data) {
                try {
                    if(Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    //clear main objects array and render chunks arrays
                    getObjects().clear();

                    for(int i = 0; i < render_chunks.size; i++)
                        render_chunks.get(i).clear();

                    if(Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    //copy objects
                    for(int i = 0; i < data.objects.size; i++) {
                        UniverseObject o = data.objects.get(i);
                        o.setParentChunk(UniverseChunk.this);

                        getObjects().add(o);

                        //add object to proper render chunk
                        int index = getRenderChunkIndex(o);
                        if(render_chunks.size > index) {
                            render_chunks.get(getRenderChunkIndex(o)).addObject(o);
                        }
                    }

                    if(Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    //loading completed so change dirty flag
                    is_dirty = false;

                    long end_time = System.nanoTime();
                    System.out.println("(Universe) Total loading time: " + TimeUtils.nanosToMillis(end_time - loading_start) + " milis");
                } catch(InterruptedException e) {
                    System.out.println("(Universe) Loading chunk stopped by interruption");
                } catch(Exception e) {
                    System.out.println("(Universe) Loading exception: " + e.getClass().getSimpleName());
                    e.printStackTrace();
                }
            }
        }, new Vector2(getPosition()), universe, game);
    }

    /**
     * Instead of loading from data provider just copy all data from given chunk
     * @param chunks_x one unit of this equals one World.CHUNK_WORLD_SIZE
     * @param chunks_y one unit of this equals one World.CHUNK_WORLD_SIZE
     * @param copy_from copying from
     */
    public void moveAndCopy(int chunks_x, int chunks_y, UniverseChunk copy_from) {
        if(generating_future != null && !generating_future.isDone()) {
            System.out.println("(Universe) Aborting loading task because chunk is going to be copied!");
            generating_future.cancel(true);
        }

        //first update this chunk position
        getPosition().add(chunks_x * Universe.UNIVERSE_CHUNK_SIZE, chunks_y * Universe.UNIVERSE_CHUNK_SIZE);

        //mark as dirty
        is_dirty = true;

        //copy objects
        getObjects().clear();

        for(int i = 0; i < render_chunks.size; i++)
            render_chunks.get(i).copyObjects(copy_from.render_chunks.get(i));

        for(int i = 0; i < copy_from.getObjects().size; i++) {
            UniverseObject o = copy_from.getObjects().get(i);
            if(o == null)
                continue;

            o.setParentChunk(this);

            getObjects().add(o);
        }

        //copying completed so not dirty anymore
        is_dirty = false;
    }

    public void tick(float delta) {
        screen_bounding_rectangle.set(game.getMainCamera().position.x - (game.getMainViewport().getWorldWidth() * game.getMainCamera().zoom) / 2, game.getMainCamera().position.y - (game.getMainViewport().getWorldHeight() * game.getMainCamera().zoom) / 2, game.getMainViewport().getWorldWidth() * game.getMainCamera().zoom, game.getMainViewport().getWorldHeight() * game.getMainCamera().zoom);

        //tick chunks objects
        for(int i = 0; i < render_chunks.size; i++) {
            RenderChunk render_chunk = render_chunks.get(i);
            object_bounding_rectangle.set(render_chunk.getLocalPosition().x + getPosition().x, render_chunk.getLocalPosition().y + getPosition().y, render_chunk.getWH().x, render_chunk.getWH().y);

            if (screen_bounding_rectangle.overlaps(object_bounding_rectangle))
                render_chunk.tick(delta);
        }
    }

    public void render(SpriteBatch batch) {
        //update culling rectangle
        screen_bounding_rectangle.set(game.getMainCamera().position.x - (game.getMainViewport().getWorldWidth() * game.getMainCamera().zoom) / 2, game.getMainCamera().position.y - (game.getMainViewport().getWorldHeight() * game.getMainCamera().zoom) / 2, game.getMainViewport().getWorldWidth() * game.getMainCamera().zoom, game.getMainViewport().getWorldHeight() * game.getMainCamera().zoom);

        //render chunk objects
        for(int i = 0; i < render_chunks.size; i++) {
            RenderChunk render_chunk = render_chunks.get(i);
            object_bounding_rectangle.set(render_chunk.getLocalPosition().x + getPosition().x, render_chunk.getLocalPosition().y + getPosition().y, render_chunk.getWH().x, render_chunk.getWH().y);

            if (screen_bounding_rectangle.overlaps(object_bounding_rectangle))
                render_chunk.render(batch, screen_bounding_rectangle);
        }
    }

    /**
     * Called when chunks moved so all objects have to be disposed or when just closing up game window
     */
    public void dispose() {
        synchronized (objects) {
            for (int i = 0; i < objects.size; i++) {
                if(i > objects.size - 1)
                    break;

                UniverseObject o = objects.get(i);
                if(o == null)
                    continue;

                o.dispose();
            }
        }
    }

    /**
     * Returns generating future
     * @return generating future instance
     */
    public Future<?> getGeneratingFuture() {
        return generating_future;
    }

    /**
     * Get position vector instance
     * @return position vector instance
     */
    public Vector2 getPosition() {
        return position;
    }

    /**
     * Get width&height vector instance
     * @return width&height vector instance
     */
    public Vector2 getWH() {
        return wh;
    }

    /**
     * Get array that contains all chunk objects
     * @return array with chunk objects
     */
    public Array<UniverseObject> getObjects() {
        return objects;
    }

    /**
     * Flag that determines if chunks is dirty (if loading process is in progress)
     * @return dirty flag
     */
    public boolean isDirty() {
        return is_dirty;
    }
}
