package explorer.world.physics;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import explorer.game.framework.Game;
import explorer.world.World;
import explorer.world.block.Block;
import explorer.world.chunk.TileHolderTools;
import explorer.world.chunk.WorldChunk;
import explorer.world.object.StaticWorldObject;
import explorer.world.object.WorldObject;
import explorer.world.physics.shapes.RectanglePhysicsShape;

/**
 * Created by RYZEN on 11.11.2017.
 */

public class PhysicsBodyChunkHelper {

    private Game game;
    private World world;
    private WorldChunk chunk;

    private boolean has_body;

    /**
     * Simple class that represent chunk collidable tile
     */
    public static class ChunkTileCollider extends StaticWorldObject implements Pool.Poolable {

        public ChunkTileCollider(Vector2 position, World world, Game game) {
            super(position, world, game);

            this.wh.set(World.BLOCK_SIZE, World.BLOCK_SIZE);
            this.physics_shape = new RectanglePhysicsShape(new Vector2(0, 0), wh, this);
        }

        @Override
        public void reset() {
            this.position.set(0, 0);
        }

        @Override
        public void tick(float delta) {}

        @Override
        public void render(SpriteBatch batch) {}

        @Override
        public void dispose() {}
    }

    private Array<WorldObject> chunk_colliders;

    public PhysicsBodyChunkHelper(WorldChunk chunk, World world, Game game) {
        this.chunk = chunk;
        this.world = world;
        this.game = game;

        chunk_colliders = new Array<WorldObject>();
    }

    public void createBody() {
        destroyBody();

        synchronized (chunk_colliders) {
            for (int i = 0; i < World.CHUNK_SIZE; i++) {
                for (int j = 0; j < World.CHUNK_SIZE; j++) {
                    Block b = world.getBlocks().getBlock(chunk.getBlocks()[i][j].getForegroundBlock());
                    int AIR_ID = world.getBlocks().AIR.getBlockID();

                    boolean add = false;
                    if (b.isCollidable()) {
                        add = isAir(i - 1, j, AIR_ID, chunk) || isAir(i + 1, j, AIR_ID, chunk) || isAir(i, j - 1, AIR_ID, chunk) || isAir(i, j + 1, AIR_ID, chunk);
                    }

                    //calculate height of collider
                    float block_height = World.BLOCK_SIZE;

                    //max height of chunk tile collider
                    final int max_height = 6; //1 = 1 block

                    for (int block_offset = 1; block_offset < max_height; block_offset++) {
                        //first check if block with offset is in chunk bounds
                        if (TileHolderTools.inChunkBounds(i, j - block_offset)) {
                            //next check if block is not air and collidable
                            if (chunk.getBlocks()[i][j - block_offset].getForegroundBlock() != AIR_ID) {
                                Block block = world.getBlocks().getBlock(chunk.getBlocks()[i][j - block_offset].getForegroundBlock());

                                if (block.isCollidable()) {
                                    //if true our collider can be higher
                                    block_height += World.BLOCK_SIZE;
                                    continue;
                                }
                            }
                        }

                        //if we are here that means out block was air or not collidable so we reached max height of our new collider and have to stop
                        break;
                    }

                    if (add) {
                        ChunkTileCollider collider = new ChunkTileCollider(new Vector2(), world, game); //colliders_pool.obtain(); //

                        if (collider != null) {
                            collider.getPosition().set(chunk.getPosition().x + (i * World.BLOCK_SIZE), chunk.getPosition().y + (j * World.BLOCK_SIZE));

                            //put proper block changes due to height calculations
                            collider.getWH().y = block_height;
                            collider.getPosition().y -= block_height - World.BLOCK_SIZE;

                            chunk_colliders.add(collider);
                        }
                    }

                    if (WorldChunk.YIELD)
                        Thread.yield();
                }
            }

            world.getPhysicsEngine().addWorldObjects(chunk_colliders);
            has_body = true;
        }
    }

    public void destroyBody() {
        synchronized (chunk_colliders) {
            world.getPhysicsEngine().removeWorldObjects(chunk_colliders);
            chunk_colliders.clear();
            has_body = false;
        }
    }

    public void copyBody(PhysicsBodyChunkHelper other) {
        synchronized (chunk_colliders) {
            chunk_colliders.clear();
            chunk_colliders.addAll(other.chunk_colliders);

            other.chunk_colliders.clear();
            other.has_body = false;

            has_body = true;
        }
    }

    private boolean isAir(int x, int y, int AIR_ID, WorldChunk chunk) {
        if(TileHolderTools.inChunkBounds(x, y)) {
            return chunk.getBlocks()[x][y].getForegroundBlock() == AIR_ID;
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
                return other_chunk.getBlocks()[x][y].getForegroundBlock() == AIR_ID;
            } else if(x >= World.CHUNK_SIZE) {
                //right
                x -= World.CHUNK_SIZE;

                if (!TileHolderTools.inWorldBounds(this_chunk_x + 1, this_chunk_y, world)) {
                    return false;
                }

                WorldChunk other_chunk = world.getWorldChunks()[this_chunk_x + 1][this_chunk_y];
                return other_chunk.getBlocks()[x][y].getForegroundBlock() == AIR_ID;
            } else if(y < 0) {
                //down
                y = World.CHUNK_SIZE + y;

                if (!TileHolderTools.inWorldBounds(this_chunk_x, this_chunk_y - 1, world)) {
                    return false;
                }

                WorldChunk other_chunk = world.getWorldChunks()[this_chunk_x][this_chunk_y - 1];
                return other_chunk.getBlocks()[x][y].getForegroundBlock() == AIR_ID;
            } else if(y >= World.CHUNK_SIZE) {
                //up
                y -= World.CHUNK_SIZE;

                if (!TileHolderTools.inWorldBounds(this_chunk_x, this_chunk_y + 1, world)) {
                    return false;
                }

                WorldChunk other_chunk = world.getWorldChunks()[this_chunk_x][this_chunk_y + 1];
                return other_chunk.getBlocks()[x][y].getForegroundBlock() == AIR_ID;
            }

            return false;
        }
    }

    public boolean hasBody() {
        return has_body;
    }
}
