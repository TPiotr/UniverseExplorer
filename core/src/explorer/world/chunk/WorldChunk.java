package explorer.world.chunk;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.concurrent.Future;

import explorer.game.framework.Game;
import explorer.world.ChunkDataProvider;
import explorer.world.World;
import explorer.world.block.Block;
import explorer.world.block.CustomColorBlock;
import explorer.world.block.CustomRenderingBlock;
import explorer.world.object.DynamicWorldObject;
import explorer.world.object.StaticWorldObject;
import explorer.world.object.WorldObject;

/**
 * Created by RYZEN on 07.10.2017.
 */

public class WorldChunk extends StaticWorldObject {

    public static final boolean YIELD = true;

    /**
     * Offset of color when block is rendering as background (use it like normal_color.minus(BACKGROUND_COLOR_OFFSET))
     */
    public static final Color BACKGROUND_COLOR_OFFSET = new Color(.2f, .2f, .2f, 0f);

    /**
     * Temp color instance used in rendering background to not make multipe instance every frame
     */
    private static Color temp_color = new Color();

    /**
     * World instance
     */
    private World world;

    /**
     * All blocks of this chunk
     */
    private TileHolder[][] blocks;

    /**
     * All objects on that chunk
     */
    private Array<WorldObject> objects;

    /**
     * Flag that determines if chunk after going unloaded needs to be saved to file again
     */
    private boolean need_save;

    /**
     * Rectangles used in tick method when checking dynamic objects
     */
    private Rectangle chunk_bounds_rectangle;
    private Rectangle object_bounds_rectangle;

    /**
     * Rectangles used to cull objects when rendering
     */
    private Rectangle screen_bounding_rectangle;
    private Rectangle object_bounding_rectangle;

    /**
     * Flag that determines if this chunk is dirty (if loading process is in progress)
     */
    private boolean is_dirty;

    /**
     * Future instance used to abort loading task when we have to
     */
    private Future<?> loading_future;

    public WorldChunk(Vector2 position, World world, Game game) {
        super(position, world, game);

        this.world = world;

        getWH().set(World.CHUNK_WORLD_SIZE, World.CHUNK_WORLD_SIZE);

        //init rects
        chunk_bounds_rectangle = new Rectangle();
        object_bounds_rectangle = new Rectangle();

        screen_bounding_rectangle = new Rectangle();
        object_bounding_rectangle = new Rectangle();

        //init blocks
        blocks = new TileHolder[World.CHUNK_SIZE][World.CHUNK_SIZE];
        for(int i = 0; i < World.CHUNK_SIZE; i++) {
            for(int j = 0; j < World.CHUNK_SIZE; j++) {
                blocks[i][j] = new TileHolder(world.getBlocks().AIR, world.getBlocks().AIR);
            }
        }

        objects = new Array<WorldObject>();
    }

    /**
     * Add object to this chunk
     * @param object object that we want to add
     * @return if object was added
     */
    public synchronized boolean addObject(WorldObject object) {
        synchronized (objects) {
            objects.add(object);

            world.getPhysicsEngine().addWorldObject(object);
            object.setParentChunk(this);

            //because new object appeared this chunk have to be saved in a file
            need_save = true;

            return true;
        }
    }

    /**
     * Move chunk in some direction determined by given parameters next load data
     * @param chunks_x one unit of this equals one World.CHUNK_WORLD_SIZE
     * @param chunks_y one unit of this equals one World.CHUNK_WORLD_SIZE
     */
    public void move(int chunks_x, int chunks_y) {
        //abort old task if have  to
        if(loading_future != null && !loading_future.isDone()) {
            System.out.println("Aborting loading task!");
            loading_future.cancel(true);
        }

        final long loading_start = System.nanoTime();

        //first update this chunk position
        getPosition().add(chunks_x * World.CHUNK_WORLD_SIZE, chunks_y * World.CHUNK_WORLD_SIZE);

        //mark as dirty
        is_dirty = true;

        //next get data for "new chunk"
        ChunkDataProvider provider = world.getChunksDataProvider();

        loading_future = provider.getChunkData(new ChunkDataProvider.DataLoaded() {
            @Override
            public void loaded(ChunkDataProvider.ChunkData data) {

                boolean started_creating_physics_body = false;

                try {
                    //first compare positions of chunk if not equal stop loading data,
                    //because it means that player moved fast and this result is some old random one
                    //and loading it like normal would cause serious bug where chunk would be totally broken (old & new data would mix and make total mess)
                    Vector2 loaded_position = data.chunk_loaded_position;

                    Vector2 this_pos = new Vector2(getPosition());
                    this_pos.x %= world.getPlanetProperties().PLANET_SIZE * World.CHUNK_WORLD_SIZE;

                    if(!loaded_position.equals(this_pos)) {
                        System.err.println("Loaded wrong chunk file (position check failed)!" + "(acc pos: " + this_pos + " loaded pos: " + loaded_position + ")");

                        //call chunk to load proper chunk now
                        move(0, 0);

                        throw new InterruptedException();
                    }

                    //reset save flag
                    need_save = false;

                    //copy new data
                    for (int i = 0; i < blocks.length; i++) {
                        for (int j = 0; j < blocks[0].length; j++) {
                            blocks[i][j].setForegroundBlock(world.getBlocks().getBlock(data.foreground_blocks[i][j]));
                            blocks[i][j].setBackgroundBlock(world.getBlocks().getBlock(data.background_blocks[i][j]));

                            if(Thread.interrupted()) {
                                throw new InterruptedException();
                            }
                        }
                    }

                    if(Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    //copy objects
                    objects.clear();
                    for (int i = 0; i < data.objects.size; i++) {
                        if(Thread.interrupted()) {
                            throw new InterruptedException();
                        }

                        WorldObject o = data.objects.get(i);

                        //because dynamic world object will move or smth there is almost 100% possibility that this chunk have to be saved after
                        if (!need_save)
                            if (o instanceof DynamicWorldObject)
                                need_save = true;

                        o.setParentChunk(WorldChunk.this);
                        objects.add(o);
                    }

                    if(Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    //calculate blocks textures
                    updateBlocksTextures();

                    if(Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    //calculate ground point lights
                    calculateGroundLight();

                    if(Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    //update center chunk border blocks textures
                    world.getWorldChunks()[1][1].updateTexturesOnChunkBorders();

                    if(Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    //add chunk objects to physics engine
                    world.getPhysicsEngine().addWorldObjects(objects);

                    //set creating physics stuff flag
                    started_creating_physics_body = true;

                    if(Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    if (WorldChunk.YIELD) {
                        Thread.yield();
                    }

                    //loading completed so change dirty flag
                    is_dirty = false;

                    long end_time = System.nanoTime();
                    System.out.println("Total loading time: " + TimeUtils.nanosToMillis(end_time - loading_start) + " milis");
                } catch(Exception e) {
                    System.out.println("Loading exception: " + e.getClass().getSimpleName());

                    //if creating physics stuff flag is true and task was aborted we have to clean loaded mess up
                    if(started_creating_physics_body) {
                        world.getPhysicsEngine().removeWorldObjects(objects);
                    }
                }
            }
        }, new Vector2(getPosition()), world, game);
    }

    /**
     * Instead of loading from data provider just copy all data from given chunk
     * @param chunks_x one unit of this equals one World.CHUNK_WORLD_SIZE
     * @param chunks_y one unit of this equals one World.CHUNK_WORLD_SIZE
     * @param copy_from copying from
     */
    public void moveAndCopy(int chunks_x, int chunks_y, WorldChunk copy_from) {
        if(loading_future != null && !loading_future.isDone()) {
            System.out.println("Aborting loading task!");
            loading_future.cancel(true);
        }

        //first update this chunk position
        getPosition().add(chunks_x * World.CHUNK_WORLD_SIZE, chunks_y * World.CHUNK_WORLD_SIZE);

        //mark as dirty
        is_dirty = true;

        //copy blocks data
        for(int i = 0; i < blocks.length; i++) {
            for(int j = 0; j < blocks[0].length; j++) {
                blocks[i][j].set(copy_from.blocks[i][j]);
            }
        }

        //copy objects
        objects.clear();
        for(int i = 0; i < copy_from.getObjects().size; i++) {
            WorldObject o = copy_from.getObjects().get(i);

            o.setParentChunk(this);
            objects.add(o);
        }

        need_save = copy_from.isSaveRequest();

        //copying completed so not dirty anymore
        is_dirty = false;
    }

    /**
     * Set block to new one
     * @param x x in local cords
     * @param y y in local cords
     * @param new_id new block id
     * @param background if true background block will be set to new one
     */
    public void setBlock(int x, int y, int new_id, boolean background) {
        if(!background) {
            blocks[x][y].setForegroundBlock(world.getBlocks().getBlock(new_id));
        } else {
            blocks[x][y].setBackgroundBlock(world.getBlocks().getBlock(new_id));
        }

        //update blocks textures in area where block was set
        int area_size = 4;
        boolean update_other_border = false;
        for(int i = x - (area_size / 2); i < x + (area_size / 2); i++) {
            for(int j = y - (area_size / 2); j < y + (area_size / 2); j++) {
                if(!inChunkBounds(i, j)) {
                    //update other chunks on borders to achieve nice blocks connecting effect
                    update_other_border = true;
                    continue;
                }

                Block this_block_background = blocks[i][j].getBackgroundBlock();
                Block this_block_foreground = blocks[i][j].getForegroundBlock();

                if(background) {
                    short texture_id = TileHolderTools.getTileTextureID(blocks, this_block_background, i, j, true, this, world);

                    blocks[i][j].setBackgroundBlockTextureID(texture_id);
                    blocks[i][j].setBackgroundTextureRegion(this_block_background.getTextureRegion(texture_id));
                } else {
                    short texture_id = TileHolderTools.getTileTextureID(blocks, this_block_foreground, i, j, false, this, world);

                    blocks[i][j].setForegroundBlockTextureID(texture_id);
                    blocks[i][j].setForegroundTextureRegion(this_block_foreground.getTextureRegion(texture_id));
                }

                if(YIELD)
                    Thread.yield();
            }
        }

        //update other chunks borders if there is need for it
        if(update_other_border) {
            for(int chunk_x = 0; chunk_x < world.getWorldChunks().length; chunk_x++){
                for(int chunk_y = 0; chunk_y < world.getWorldChunks()[0].length; chunk_y++) {

                    if(world.getWorldChunks()[chunk_x][chunk_y] != this)
                        world.getWorldChunks()[chunk_x][chunk_y].updateTexturesOnChunkBorders();
                }
            }
        }

        //update ground lights
        world.getLightEngine().getGroundLineRenderer().removeChunkBoundLights(this);
        calculateGroundLight();

        //because chunk was changed we need to save it to a file again
        need_save = true;
    }

    /**
     * Function for smoothing blocks (nice connections effect)
     */
    private void updateBlocksTextures() {
        for(int i = 0; i < World.CHUNK_SIZE; i++) {
            for(int j = 0; j < World.CHUNK_SIZE; j++) {
                Block this_block_background = blocks[i][j].getBackgroundBlock();
                Block this_block_foreground = blocks[i][j].getForegroundBlock();

                if(this_block_foreground == null || this_block_background == null)
                    continue;

                short background_block_texture_id = TileHolderTools.getTileTextureID(blocks, this_block_background, i, j, true, this, world);
                short foreground_block_texture_id = TileHolderTools.getTileTextureID(blocks, this_block_foreground, i, j, false, this, world);

                blocks[i][j].setBackgroundBlockTextureID(background_block_texture_id);
                blocks[i][j].setForegroundBlockTextureID(foreground_block_texture_id);

                blocks[i][j].setBackgroundTextureRegion(this_block_background.getTextureRegion(background_block_texture_id));//TileHolderTools.getTileTexture(blocks, this_block_background, i, j, true, this, world));
                blocks[i][j].setForegroundTextureRegion(this_block_foreground.getTextureRegion(foreground_block_texture_id));

                if(YIELD)
                    Thread.yield();
            }
        }
    }

    /**
     * Update blocks textures only on chunk borders
     * used by other chunks when block is set on their border so textures of blocks on nearby chunk must be recalculated too
     */
    public void updateTexturesOnChunkBorders() {
        for(int i = 0; i < World.CHUNK_SIZE; i++) {
            for (int j = 0; j < World.CHUNK_SIZE; j++) {
                if(i == 0 || j == 0 || i == World.CHUNK_SIZE - 1 || j == World.CHUNK_SIZE - 1) {
                    Block this_block_background = blocks[i][j].getBackgroundBlock();
                    Block this_block_foreground = blocks[i][j].getForegroundBlock();

                    if(this_block_foreground == null || this_block_background == null)
                        continue;

                    short background_block_texture_id = TileHolderTools.getTileTextureID(blocks, this_block_background, i, j, true, this, world);
                    short foreground_block_texture_id = TileHolderTools.getTileTextureID(blocks, this_block_foreground, i, j, false, this, world);

                    blocks[i][j].setBackgroundBlockTextureID(background_block_texture_id);
                    blocks[i][j].setForegroundBlockTextureID(foreground_block_texture_id);

                    blocks[i][j].setBackgroundTextureRegion(this_block_background.getTextureRegion(background_block_texture_id));//TileHolderTools.getTileTexture(blocks, this_block_background, i, j, true, this, world));
                    blocks[i][j].setForegroundTextureRegion(this_block_foreground.getTextureRegion(foreground_block_texture_id));//TileHolderTools.getTileTexture(blocks, this_block_foreground, i, j, false, this, world));

                    if(YIELD)
                        Thread.yield();
                }
            }
        }
    }

    /**
     * Function for calculating point lights on ground
     */
    private void calculateGroundLight() {
        for(int i = 0; i < World.CHUNK_SIZE; i++) {
            for (int j = 0; j < World.CHUNK_SIZE; j++) {
                int foreground_id = blocks[i][j].getForegroundBlock().getBlockID();
                int background_id = blocks[i][j].getBackgroundBlock().getBlockID();

                //if block is clear air
                if(foreground_id == world.getBlocks().AIR.getBlockID() && background_id == world.getBlocks().AIR.getBlockID()) {
                    //and if somewhere nearby is other foreground/background block

                    float half_block = World.BLOCK_SIZE / 2;

                    //
                    if(TileHolderTools.canPlaceLight(i, j, 0, -1, world, blocks)) {
                        world.getLightEngine().getGroundLineRenderer().addPosition(new Vector2(getPosition()).add(i * World.BLOCK_SIZE + half_block, j * World.BLOCK_SIZE + half_block));
                    } else if(TileHolderTools.canPlaceLight(i, j, 0, 1, world, blocks)) {
                        world.getLightEngine().getGroundLineRenderer().addPosition(new Vector2(getPosition()).add(i * World.BLOCK_SIZE + half_block, j * World.BLOCK_SIZE + half_block));
                    } else if(TileHolderTools.canPlaceLight(i, j, 1, 0, world, blocks)) {
                        world.getLightEngine().getGroundLineRenderer().addPosition(new Vector2(getPosition()).add(i * World.BLOCK_SIZE + half_block, j * World.BLOCK_SIZE + half_block));
                    } else if(TileHolderTools.canPlaceLight(i, j, -1, 0, world, blocks)) {
                        world.getLightEngine().getGroundLineRenderer().addPosition(new Vector2(getPosition()).add(i * World.BLOCK_SIZE + half_block, j * World.BLOCK_SIZE + half_block));
                    }
                }

                if(YIELD)
                    Thread.yield();
            }
        }
    }

    /**
     * Very useful function to protect from exceptions
     * @param x local x (0 - CHUNK_SIZE)
     * @param y local y (0 - CHUNK_SIZE)
     * @return if is in chunk bounds
     */
    public static boolean inChunkBounds(int x, int y) {
        if(World.CHUNK_SIZE - 1 < x || 0 > x) {
            return false;
        } else if (World.CHUNK_SIZE - 1 < y || 0 > y) {
            return false;
        }
        return true;
    }

    @Override
    public void tick(float delta) {
        //take care of dynamic objects because they can move out from this chunk
        chunk_bounds_rectangle.set(getPosition().x, getPosition().y, getWH().x, getWH().y);

        for(int i = 0; i < getObjects().size; i++) {
            if(getObjects().get(i) instanceof DynamicWorldObject) {
                DynamicWorldObject o = (DynamicWorldObject) getObjects().get(i);
                object_bounds_rectangle.set(o.getPosition().x, o.getPosition().y, o.getWH().x, o.getWH().y);
                chunk_bounds_rectangle.set(getPosition().x, getPosition().y, getWH().x, getWH().y);

                object_transfer_loop:
                if(!chunk_bounds_rectangle.overlaps(object_bounds_rectangle)) {
                    //transfer this object to other chunk

                    //first find proper chunk
                    for(int x = 0; x < world.getWorldChunks().length; x++) {
                        for(int y = 0; y < world.getWorldChunks()[0].length; y++) {
                            WorldChunk chunk = world.getWorldChunks()[x][y];
                            chunk_bounds_rectangle.set(chunk.getPosition().x, chunk.getPosition().y, chunk.getWH().x, chunk.getWH().y);

                            if(chunk_bounds_rectangle.overlaps(object_bounds_rectangle)) {
                                //here we found proper chunk

                                //so remove object from this chunk and assign to proper new one
                                objects.removeValue(o, true);

                                chunk.objects.add(o);
                                o.setParentChunk(chunk);

                                need_save = true;

                                //because work is done break object transfer loop
                                break object_transfer_loop;
                            }
                        }
                    }

                    //if we are there that means our object felt out from the world so we have to dispose it
                    getObjects().removeIndex(i);
                    world.getPhysicsEngine().removeWorldObject(o);
                    o.dispose();
                }
            }
        }

        //and finally just call tick() for every object
        synchronized (objects) {
            for (int i = 0; i < objects.size; i++) {
                WorldObject o = objects.get(i);

                if (o != null)
                    o.tick(delta);
            }
        }
    }

    public int blocks_rendered;

    @Override
    public void render(SpriteBatch batch) {
        blocks_rendered = 0;

        int chunk_x_camera = (int) (game.getMainCamera().position.x - getPosition().x) / World.BLOCK_SIZE;
        int chunk_y_camera = (int) (game.getMainCamera().position.y - getPosition().y) / World.BLOCK_SIZE;

        int screen_width_blocks = (int) Math.ceil(game.getMainViewport().getScreenWidth() * game.getMainCamera().zoom / World.BLOCK_SIZE) + 1;
        int screen_height_blocks = (int) Math.ceil(game.getMainViewport().getScreenHeight() * game.getMainCamera().zoom / World.BLOCK_SIZE) + 1;

        //update culling rectangle
        screen_bounding_rectangle.set(game.getMainCamera().position.x - (game.getMainViewport().getWorldWidth() * game.getMainCamera().zoom) / 2, game.getMainCamera().position.y - (game.getMainViewport().getWorldHeight() * game.getMainCamera().zoom) / 2, game.getMainViewport().getWorldWidth() * game.getMainCamera().zoom, game.getMainViewport().getWorldHeight() * game.getMainCamera().zoom);

        //store air id
        int AIR_ID = world.getBlocks().AIR.getBlockID();

        if(chunk_x_camera + (screen_width_blocks / 2) < 0 || chunk_y_camera + (screen_height_blocks / 2) < 0) {
            return;

        }
        if(chunk_x_camera - (screen_width_blocks / 2) > World.CHUNK_SIZE || chunk_y_camera - (screen_height_blocks / 2) > World.CHUNK_SIZE) {
            return;
        }

        //background
        for(int i = -screen_width_blocks / 2; i < (screen_width_blocks / 2) + 1; i++) {
            for(int j = -screen_height_blocks / 2; j < (screen_height_blocks / 2) + 1; j++) {
                if(!inChunkBounds(i + chunk_x_camera, j + chunk_y_camera))
                    continue;

                TileHolder holder = blocks[i + chunk_x_camera][j + chunk_y_camera];

                if(holder.getBackgroundBlock().getBlockID() == AIR_ID)
                    continue;

                //first check if this background block don't have flag set to true that forces its rendering independently on others blocks
                if(!holder.getBackgroundBlock().needBackgroundBlockRendered()) {

                    //in this moment we know that this background tile is not an air block
                    if (holder.getForegroundBlock().needBackgroundBlockRenderedIfNotFullySurrounded() && holder.getForegroundBlock().getBlockID() != AIR_ID) {
                        boolean render = false;

                        //in this piece of code check if holder needs background block rendered if is not fully surrounded by other blocks with what it can connect
                        //check if block over or under or on left or on right don't have foreground block (have but AIR) and background block is not air
                        //if true render this background block to prevent empty pixels on screen where should be block texture (it happends if some pixel on block texture alpha is 0)

                        //check for block on right
                        if (inChunkBounds(i + chunk_x_camera + 1, j + chunk_y_camera)) {
                            TileHolder holder_right = blocks[i + chunk_x_camera + 1][j + chunk_y_camera];

                            if (holder_right.getForegroundBlock().getBlockID() == AIR_ID && holder_right.getBackgroundBlock().getBlockID() != AIR_ID) {
                                render = true;
                            }
                        }

                        //if still don't want to render check block on left
                        if (!render) {
                            if (inChunkBounds(i + chunk_x_camera - 1, j + chunk_y_camera)) {
                                TileHolder holder_left = blocks[i + chunk_x_camera - 1][j + chunk_y_camera];

                                if (holder_left.getForegroundBlock().getBlockID() == AIR_ID && holder_left.getBackgroundBlock().getBlockID() != AIR_ID) {
                                    render = true;
                                }
                            }
                        }

                        //if still don't want to render check block over
                        if (!render) {
                            if (inChunkBounds(i + chunk_x_camera, j + chunk_y_camera + 1)) {
                                TileHolder holder_up = blocks[i + chunk_x_camera][j + chunk_y_camera + 1];

                                if (holder_up.getForegroundBlock().getBlockID() == AIR_ID && holder_up.getBackgroundBlock().getBlockID() != AIR_ID) {
                                    render = true;
                                }
                            }
                        }

                        //if still don't want to render check block under
                        if (!render) {
                            if (inChunkBounds(i + chunk_x_camera, j + chunk_y_camera - 1)) {
                                TileHolder holder_down = blocks[i + chunk_x_camera][j + chunk_y_camera - 1];

                                if (holder_down.getForegroundBlock().getBlockID() == AIR_ID && holder_down.getBackgroundBlock().getBlockID() != AIR_ID) {
                                    render = true;
                                }
                            }
                        }

                        //if don't have to render skip this background block
                        if (!render)
                            continue;

                    }
                    //here if block don't have needBackgroundBlockIfNotFullySurrounded flag set to true and foreground block is not air we are sure that we don't have to render this background block
                    else if (!holder.getForegroundBlock().needBackgroundBlockRenderedIfNotFullySurrounded() && holder.getForegroundBlock().getBlockID() != AIR_ID) {
                        continue;
                    }
                }

                Block block = holder.getBackgroundBlock();
                if(block instanceof CustomColorBlock) {
                    CustomColorBlock cblock = (CustomColorBlock) block;
                    batch.setColor(temp_color.set(cblock.getBlockColor()).sub(BACKGROUND_COLOR_OFFSET));
                }

                if(!(block instanceof CustomRenderingBlock)) {
                    TextureRegion block_region = holder.getBackgroundTextureRegion();
                    if (block_region != null) {
                        batch.draw(block_region, getPosition().x + World.BLOCK_SIZE * (i + chunk_x_camera), getPosition().y + World.BLOCK_SIZE * (j + chunk_y_camera), World.BLOCK_SIZE, World.BLOCK_SIZE);
                    }
                } else {
                    ((CustomRenderingBlock) block).render(batch, holder.getBackgroundBlockTextureID(), getPosition().x + World.BLOCK_SIZE * (i + chunk_x_camera), getPosition().y + World.BLOCK_SIZE * (j + chunk_y_camera), World.BLOCK_SIZE, World.BLOCK_SIZE, true);
                }

                blocks_rendered++;

                batch.setColor(temp_color.set(1, 1, 1, 1).sub(BACKGROUND_COLOR_OFFSET));
            }
        }

        //in between render chunk objects
        for(int i = 0; i < objects.size; i++) {
            WorldObject o = objects.get(i);

            //cull
            object_bounding_rectangle.set(o.getPosition().x, o.getPosition().y, o.getWH().x, o.getWH().y);
            if(screen_bounding_rectangle.overlaps(object_bounding_rectangle))
                o.render(batch);
        }

        //next render foreground tiles
        for(int i = -screen_width_blocks / 2; i < (screen_width_blocks / 2) + 1; i++) {
            for(int j = -screen_height_blocks / 2; j < (screen_height_blocks / 2) + 1; j++) {
                if(!inChunkBounds(i + chunk_x_camera, j + chunk_y_camera))
                    continue;

                TileHolder holder = blocks[i + chunk_x_camera][j + chunk_y_camera];

                if(holder.getForegroundBlock().getBlockID() == AIR_ID)
                    continue;

                Block block = holder.getForegroundBlock();
                if(block instanceof CustomColorBlock) {
                    CustomColorBlock cblock = (CustomColorBlock) block;
                    batch.setColor(cblock.getBlockColor().r, cblock.getBlockColor().g, cblock.getBlockColor().b, cblock.getBlockColor().a);
                }

                if(!(block instanceof CustomRenderingBlock)) {
                    TextureRegion block_region = holder.getForegroundTextureRegion();
                    if (block_region != null) {
                        batch.draw(block_region, getPosition().x + World.BLOCK_SIZE * (i + chunk_x_camera), getPosition().y + World.BLOCK_SIZE * (j + chunk_y_camera), World.BLOCK_SIZE, World.BLOCK_SIZE);
                    }
                } else {
                    ((CustomRenderingBlock) block).render(batch, holder.getForegroundBlockTextureID(), getPosition().x + World.BLOCK_SIZE * (i + chunk_x_camera), getPosition().y + World.BLOCK_SIZE * (j + chunk_y_camera), World.BLOCK_SIZE, World.BLOCK_SIZE, false);
                }

                blocks_rendered++;
                batch.setColor(1f, 1f, 1f, 1f);
            }
        }

        //System.out.println("Blocks rendered count: " + blocks_rendered);
    }

    /**
     * Move chunk into some direction determined by given parameters
     * @param chunks_x x axis move 1 unit = CHUNK_WORLD_SIZE
     * @param chunks_y y axis move 1 unit = CHUNK_WORLD_SIZE
     */
    public void moveToPosition(int chunks_x, int chunks_y) {
        Vector2 move_vector = new Vector2(chunks_x * World.CHUNK_WORLD_SIZE, chunks_y * World.CHUNK_WORLD_SIZE);
        for(WorldObject object : objects) {
            object.move(move_vector);
        }

        getPosition().add(move_vector);

        calculateGroundLight();
    }

    /**
     * Called when chunks moved so all objects have to be disposed or when just closing up game window
     */
    public void dispose() {
        //remove objects from physics engine
        for(int i = 0; i < objects.size; i++) {
            WorldObject object = objects.get(i);

            if(object instanceof StaticWorldObject || object instanceof DynamicWorldObject)
                world.getPhysicsEngine().removeWorldObject(object);
        }

        for(int i = 0; i < objects.size; i++)
            objects.get(i).dispose();
    }

    /**
     * Get instance of this chunk loading future to get info about this process (is done, in progress, never started etc), may be null
     * @return loading future instance
     */
    public Future<?> getLoadingFuture() {
        return loading_future;
    }

    /**
     * Get array that contains all objects on that chunk
     * @return array of all objects
     */
    public Array<WorldObject> getObjects() {
        return objects;
    }

    /**
     * @return array of blocks holders
     */
    public TileHolder[][] getBlocks() {
        return blocks;
    }

    /**
     * Flag that determines if chunks is dirty (if loading process is in progress)
     * @return dirty flag
     */
    public boolean isDirty() {
        return is_dirty;
    }

    /**
     * True if chunk want to be saved when have to be unloaded
     * @return save request
     */
    public boolean isSaveRequest() {
        return need_save;
    }
}
