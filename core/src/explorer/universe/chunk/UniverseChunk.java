package explorer.universe.chunk;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;

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

                    //copy objects
                    getObjects().clear();
                    for(int i = 0; i < data.objects.size; i++) {
                        UniverseObject o = data.objects.get(i);
                        o.setParentChunk(UniverseChunk.this);

                        getObjects().add(o);
                    }

                    if(Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    //loading completed so change dirty flag
                    is_dirty = false;

                    long end_time = System.nanoTime();
                    System.out.println("(Universe) Total loading time: " + TimeUtils.nanosToMillis(end_time - loading_start) + " milis");
                } catch(Exception e) {
                    System.out.println("(Universe) Loading exception: " + e.getClass().getSimpleName());
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
            System.out.println("Aborting loading task!");
            generating_future.cancel(true);
        }

        //first update this chunk position
        getPosition().add(chunks_x * Universe.UNIVERSE_CHUNK_SIZE, chunks_y * Universe.UNIVERSE_CHUNK_SIZE);

        //mark as dirty
        is_dirty = true;

        //copy objects
        getObjects().clear();
        for(int i = 0; i < copy_from.getObjects().size; i++) {
            UniverseObject o = copy_from.getObjects().get(i);
            o.setParentChunk(this);

            getObjects().add(o);
        }

        //copying completed so not dirty anymore
        is_dirty = false;
    }

    public void tick(float delta) {
        //just iterate through objects array and call tick()
        for(int i = 0; i < objects.size; i++) {
            UniverseObject o = objects.get(i);

            if(o == null)
                continue;

            o.tick(delta);
        }
    }

    public void render(SpriteBatch batch) {
        //update culling rectangle
        screen_bounding_rectangle.set(game.getMainCamera().position.x - (game.getMainViewport().getWorldWidth() * game.getMainCamera().zoom) / 2, game.getMainCamera().position.y - (game.getMainViewport().getWorldHeight() * game.getMainCamera().zoom) / 2, game.getMainViewport().getWorldWidth() * game.getMainCamera().zoom, game.getMainViewport().getWorldHeight() * game.getMainCamera().zoom);

        //render chunk objects

        for(int i = 0; i < objects.size; i++) {
            UniverseObject o = objects.get(i);

            if(o == null)
                continue;

            //cull
            object_bounding_rectangle.set(o.getPosition().x, o.getPosition().y, o.getWH().x, o.getWH().y);
            if(screen_bounding_rectangle.overlaps(object_bounding_rectangle))
                o.render(batch);
        }
    }

    /**
     * Called when chunks moved so all objects have to be disposed or when just closing up game window
     */
    public void dispose() {
        for(int i = 0; i < objects.size; i++)
            objects.get(i).dispose();
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
