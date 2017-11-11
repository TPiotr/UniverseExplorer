package explorer.world.physics;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.block.Block;
import explorer.world.chunk.TileHolderTools;
import explorer.world.chunk.WorldChunk;
import explorer.world.object.StaticWorldObject;
import explorer.world.object.WorldObject;
import explorer.world.physics.shapes.RectanglePhysicsShape;
import explorer.world.physics.PhysicsBodyChunkHelper.ChunkTileCollider;

/**
 * Helper class that takes care of chunks physics bodies
 * Created by RYZEN on 23.10.2017.
 */

public class PhysicsEngineChunksHelper1 {

    private PhysicsEngine engine;
    private World world;
    private Game game;

    //store all chunks colliders
    private HashMap<WorldChunk, List<WorldObject>> chunks_colliders;

    public PhysicsEngineChunksHelper1(PhysicsEngine engine, final World world, final Game game) {
        this.engine = engine;
        this.world = world;
        this.game = game;

        chunks_colliders = new HashMap<WorldChunk, List<WorldObject>>();
    }

    /**
     * Create chunks physics body
     * @param chunk chunk for which physics body will be created
     */
    public synchronized void createChunkPhysicsBody(WorldChunk chunk) {
        //remove old colliders if is old ones still exist
        destroyChunkPhysicsBody(chunk);

        for(int i = 0; i < World.CHUNK_SIZE; i++) {
            for(int j = 0; j < World.CHUNK_SIZE; j++) {
                Block b = chunk.getBlocks()[i][j].getForegroundBlock();
                int AIR_ID = world.getBlocks().AIR.getBlockID();

                boolean add = false;
                if(b.isCollidable()) {
                    add = isAir(i - 1, j, AIR_ID, chunk) || isAir(i + 1, j, AIR_ID, chunk) || isAir(i, j - 1, AIR_ID, chunk) || isAir(i, j + 1, AIR_ID, chunk);
                }

                //calculate height of collider
                float block_height = World.BLOCK_SIZE;

                //max height of chunk tile collider
                final int max_height = 6; //1 = 1 block

                for(int block_offset = 1; block_offset < max_height; block_offset++) {
                    //first check if block with offset is in chunk bounds
                    if(TileHolderTools.inChunkBounds(i, j - block_offset)) {
                        //next check if block is not air and collidable
                        if(chunk.getBlocks()[i][j - block_offset].getForegroundBlock().getBlockID() != AIR_ID) {
                            Block block = chunk.getBlocks()[i][j - block_offset].getForegroundBlock();

                            if(block.isCollidable()) {
                                //if true our collider can be higher
                                block_height += World.BLOCK_SIZE;
                                continue;
                            }
                        }
                    }

                    //if we are here that means out block was air or not collidable so we reached max height of our new collider and have to stop
                    break;
                }

                if(add) {
                    List<WorldObject> chunk_colliders = chunks_colliders.get(chunk);
                    if(chunk_colliders == null) {
                        chunk_colliders = Collections.synchronizedList(new ArrayList<WorldObject>());
                        chunks_colliders.put(chunk, chunk_colliders);
                    }

                    ChunkTileCollider collider = new ChunkTileCollider(new Vector2(), world, game); //colliders_pool.obtain(); //

                    if(collider != null) {
                        collider.getPosition().set(chunk.getPosition().x + (i * World.BLOCK_SIZE), chunk.getPosition().y + (j * World.BLOCK_SIZE));

                        //put proper block changes due to height calculations
                        collider.getWH().y = block_height;
                        collider.getPosition().y -= block_height - World.BLOCK_SIZE;

                        chunk_colliders.add(collider);
                    }
                }

                if(WorldChunk.YIELD)
                    Thread.yield();
            }
        }

        List<WorldObject> chunk_colliders = chunks_colliders.get(chunk);
        if(chunk_colliders == null) {
            chunk_colliders = Collections.synchronizedList(new ArrayList<WorldObject>());
            chunks_colliders.put(chunk, chunk_colliders);
        }

        //after all add colliders to physics engine
        engine.addWorldObjects(chunk_colliders);
    }

    /**
     * Destroy given chunk physics body (remove all chunk colliders from physics engine)
     * @param chunk
     */
    public synchronized void destroyChunkPhysicsBody(WorldChunk chunk) {
        final List<WorldObject> chunk_objects = chunks_colliders.get(chunk);
        if(chunk_objects != null) {
            engine.removeWorldObjects(chunk_objects);

            //free up all colliders that were removed
            //for(WorldObject o : chunk_objects) {
            //    colliders_pool.freeInstance((ChunkTileCollider) o);
            //}

            chunk_objects.clear();
        }
    }

    /**
     * Copy given parent world chunk tile colliders to new_parent world chunk so just reassign them
     * @param parent current parent of colliders
     * @param new_parent new parent of colliders
     */
    public void reassignColliders(WorldChunk parent, WorldChunk new_parent) {
        List<WorldObject> parent_objects = chunks_colliders.get(parent);
        chunks_colliders.remove(parent);

        chunks_colliders.put(new_parent, parent_objects);
    }

    public synchronized int getChunkCollidersCount() {
        int count = 0;

        for(int i = 0; i < world.getWorldChunks().length; i++) {
            for(int j = 0; j < world.getWorldChunks()[0].length; j++) {
                List<WorldObject> parent_objects = chunks_colliders.get(world.getWorldChunks()[i][j]);

                if(parent_objects != null)
                    count += parent_objects.size();
            }
        }

        return count;
    }

    private boolean isAir(int x, int y, int AIR_ID, WorldChunk chunk) {
        if(TileHolderTools.inChunkBounds(x, y)) {
            return chunk.getBlocks()[x][y].getForegroundBlock().getBlockID() == AIR_ID;
        } else {
            //first find from which chunk we have to get data

            //calc this chunk in array position
            int zero_chunk_pos_x = (int) world.getWorldChunks()[0][0].getPosition().x / World.CHUNK_WORLD_SIZE;
            int zero_chunk_pos_y = (int) world.getWorldChunks()[0][0].getPosition().y / World.CHUNK_WORLD_SIZE;

            int this_chunk_x = ((int) chunk.getPosition().x / World.CHUNK_WORLD_SIZE) - zero_chunk_pos_x;
            int this_chunk_y = ((int) chunk.getPosition().y / World.CHUNK_WORLD_SIZE) - zero_chunk_pos_y;

            if(x < 0) {
                x = World.CHUNK_SIZE + x;

                if(!TileHolderTools.inWorldBounds(this_chunk_x - 1, this_chunk_y, world))
                    return false;

                //left
                WorldChunk other_chunk = world.getWorldChunks()[this_chunk_x - 1][this_chunk_y];
                return other_chunk.getBlocks()[x][y].getForegroundBlock().getBlockID() == AIR_ID;
            } else if(x >= World.CHUNK_SIZE) {
                //right
                x -= World.CHUNK_SIZE;

                if (!TileHolderTools.inWorldBounds(this_chunk_x + 1, this_chunk_y, world)) {
                    return false;
                }

                WorldChunk other_chunk = world.getWorldChunks()[this_chunk_x + 1][this_chunk_y];
                return other_chunk.getBlocks()[x][y].getForegroundBlock().getBlockID() == AIR_ID;
            } else if(y < 0) {
                //down
                y = World.CHUNK_SIZE + y;

                if (!TileHolderTools.inWorldBounds(this_chunk_x, this_chunk_y - 1, world)) {
                    return false;
                }

                WorldChunk other_chunk = world.getWorldChunks()[this_chunk_x][this_chunk_y - 1];
                return other_chunk.getBlocks()[x][y].getForegroundBlock().getBlockID() == AIR_ID;
            } else if(y >= World.CHUNK_SIZE) {
                //up
                y -= World.CHUNK_SIZE;

                if (!TileHolderTools.inWorldBounds(this_chunk_x, this_chunk_y + 1, world)) {
                    return false;
                }

                WorldChunk other_chunk = world.getWorldChunks()[this_chunk_x][this_chunk_y + 1];
                return other_chunk.getBlocks()[x][y].getForegroundBlock().getBlockID() == AIR_ID;
            }

            return false;
        }
    }
}
