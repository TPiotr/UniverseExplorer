package explorer.world.chunk;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import explorer.world.World;
import explorer.world.block.Block;

/**
 * Created by RYZEN on 07.10.2017.
 */

public class TileHolderTools {

    /**
     * True if you can place ground light on given position used in calculateGroundLight() function in chunk class
     * @param x local pos
     * @param y local pos
     * @param move_x local offset
     * @param move_y local offset
     * @param world world instance
     * @param tiles_data data of chunk
     * @return
     */
    public static synchronized boolean canPlaceLight(int x, int y, int move_x, int move_y, World world, TileHolder[][] tiles_data) {
        if(inChunkBounds(x + move_x, y + move_y)) {
            if(!tiles_data[x][y].getForegroundBlock().isBlockingGroundLight() && !tiles_data[x][y].getBackgroundBlock().isBlockingGroundLight()) {
                if (tiles_data[x + move_x][y + move_y].getForegroundBlock().isBlockingGroundLight() || tiles_data[x + move_x][y + move_y].getBackgroundBlock().isBlockingGroundLight()) {
                    return true;
                }
            }
            return false;
        }

        return false;
    }

    public static synchronized short getTileTextureID(TileHolder[][] tiles_data,
                                                    Block this_block, int i, int j, boolean background, WorldChunk chunk, World world) {
        if(this_block == null)
            return 0;

        // if group is to connect with nothing then we have only one option
        if (this_block.getBlockGroup().equals(Block.BlockGroup.CONNECT_WITH_NOTHING)) {
            return Block.COLLIDE_NONE;
        }

        // check if blocks collide on all sides
        else if (tileCollide(tiles_data, this_block, -1 + i, 0 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 1 + i, 0 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 0 + i, 1 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 0 + i, -1 + j,
                background, chunk, world)) {

            return Block.COLLIDE_ALL_SIDES;
        }

        // check if block collide on left right down
        else if (tileCollide(tiles_data, this_block, -1 + i, 0 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 1 + i, 0 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 0 + i, -1 + j,
                background, chunk, world)) {

            return Block.COLLIDE_LEFT_DOWN_RIGHT;
        }

        // check if block collide on left right up
        else if (tileCollide(tiles_data, this_block, -1 + i, 0 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 1 + i, 0 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 0 + i, 1 + j, background, chunk, world)) {

           return Block.COLLIDE_LEFT_UP_RIGHT;
        }

        // check if block collide on up right down
        else if (tileCollide(tiles_data, this_block, 0 + i, 1 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 1 + i, 0 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 0 + i, -1 + j,
                background, chunk, world)) {

            return Block.COLLIDE_UP_RIGHT_DOWN;
        }

        // check if block collide on up left down
        else if (tileCollide(tiles_data, this_block, 0 + i, 1 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, -1 + i, 0 + j,
                background, chunk, world)
                && tileCollide(tiles_data, this_block, 0 + i, -1 + j,
                background, chunk, world)) {

            return Block.COLLIDE_LEFT_UP_DOWN;
        }

        // check if block collide on left right
        else if (tileCollide(tiles_data, this_block, -1 + i, 0 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 1 + i, 0 + j, background, chunk, world)) {

            return Block.COLLIDE_LEFT_RIGHT;
        }

        // check if block collide on up down
        else if (tileCollide(tiles_data, this_block, 0 + i, 1 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 0 + i, -1 + j,
                background, chunk, world)) {

            return Block.COLLIDE_UP_DOWN;
        }

        // check if block collide on left up
        else if (tileCollide(tiles_data, this_block, 0 + i, 1 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, -1 + i, 0 + j,
                background, chunk, world)) {

            return Block.COLLIDE_LEFT_UP;
        }

        // check if block collide on up right
        else if (tileCollide(tiles_data, this_block, 1 + i, 0 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 0 + i, 1 + j, background, chunk, world)) {

            return Block.COLLIDE_UP_RIGHT;
        }

        // check if block collide on right down
        else if (tileCollide(tiles_data, this_block, 1 + i, 0 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 0 + i, -1 + j,
                background, chunk, world)) {

            return Block.COLLIDE_RIGHT_DOWN;
        }

        // check if block collide on left down
        else if (tileCollide(tiles_data, this_block, -1 + i, 0 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 0 + i, -1 + j,
                background, chunk, world)) {

            return Block.COLLIDE_LEFT_DOWN;
        }

        // check if block collide on left only
        else if (tileCollide(tiles_data, this_block, -1 + i, 0 + j, background, chunk, world)) {

            return Block.COLLIDE_LEFT;
        }

        // check if block collide on right only
        else if (tileCollide(tiles_data, this_block, 1 + i, 0 + j, background, chunk, world)) {
            return Block.COLLIDE_RIGHT;
        }

        // check if block collide on down
        else if (tileCollide(tiles_data, this_block, 0 + i, -1 + j, background, chunk, world)) {
            return Block.COLLIDE_DOWN;
        }

        // check if block collide on up
        else if (tileCollide(tiles_data, this_block, 0 + i, 1 + j, background, chunk, world)) {
            return Block.COLLIDE_UP;
        }

        else {
            return Block.COLLIDE_NONE;
        }
    }

    public static synchronized TextureRegion getTileTexture(TileHolder[][] tiles_data,
                                        Block this_block, int i, int j, boolean background, WorldChunk chunk, World world) {

        TextureRegion out = this_block.getTextureRegion(getTileTextureID(tiles_data, this_block, i, j, background, chunk, world));
        return out;

        /*TextureRegion final_region = null;

        // if group is to connect with nothing then we have only one option
        if (this_block.getBlockGroup().equals(Block.BlockGroup.CONNECT_WITH_NOTHING)) {
            final_region = this_block.getTextureRegion(Block.COLLIDE_NONE);
        }

        // check if blocks collide on all sides
        else if (tileCollide(tiles_data, this_block, -1 + i, 0 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 1 + i, 0 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 0 + i, 1 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 0 + i, -1 + j,
                background, chunk, world)) {
            final_region = this_block.getTextureRegion(Block.COLLIDE_ALL_SIDES);
        }

        // check if block collide on left right down
        else if (tileCollide(tiles_data, this_block, -1 + i, 0 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 1 + i, 0 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 0 + i, -1 + j,
                background, chunk, world)) {
            final_region = this_block
                    .getTextureRegion(Block.COLLIDE_LEFT_DOWN_RIGHT);
        }

        // check if block collide on left right up
        else if (tileCollide(tiles_data, this_block, -1 + i, 0 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 1 + i, 0 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 0 + i, 1 + j, background, chunk, world)) {
            final_region = this_block
                    .getTextureRegion(Block.COLLIDE_LEFT_UP_RIGHT);
        }

        // check if block collide on up right down
        else if (tileCollide(tiles_data, this_block, 0 + i, 1 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 1 + i, 0 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 0 + i, -1 + j,
                background, chunk, world)) {
            final_region = this_block
                    .getTextureRegion(Block.COLLIDE_UP_RIGHT_DOWN);
        }

        // check if block collide on up left down
        else if (tileCollide(tiles_data, this_block, 0 + i, 1 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, -1 + i, 0 + j,
                background, chunk, world)
                && tileCollide(tiles_data, this_block, 0 + i, -1 + j,
                background, chunk, world)) {
            final_region = this_block
                    .getTextureRegion(Block.COLLIDE_LEFT_UP_DOWN);
        }

        // check if block collide on left right
        else if (tileCollide(tiles_data, this_block, -1 + i, 0 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 1 + i, 0 + j, background, chunk, world)) {
            final_region = this_block.getTextureRegion(Block.COLLIDE_LEFT_RIGHT);
        }

        // check if block collide on up down
        else if (tileCollide(tiles_data, this_block, 0 + i, 1 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 0 + i, -1 + j,
                background, chunk, world)) {
            final_region = this_block.getTextureRegion(Block.COLLIDE_UP_DOWN);
        }

        // check if block collide on left up
        else if (tileCollide(tiles_data, this_block, 0 + i, 1 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, -1 + i, 0 + j,
                background, chunk, world)) {
            final_region = this_block.getTextureRegion(Block.COLLIDE_LEFT_UP);
        }

        // check if block collide on up right
        else if (tileCollide(tiles_data, this_block, 1 + i, 0 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 0 + i, 1 + j, background, chunk, world)) {
            final_region = this_block.getTextureRegion(Block.COLLIDE_UP_RIGHT);
        }

        // check if block collide on right down
        else if (tileCollide(tiles_data, this_block, 1 + i, 0 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 0 + i, -1 + j,
                background, chunk, world)) {
            final_region = this_block.getTextureRegion(Block.COLLIDE_RIGHT_DOWN);
        }

        // check if block collide on left down
        else if (tileCollide(tiles_data, this_block, -1 + i, 0 + j, background, chunk, world)
                && tileCollide(tiles_data, this_block, 0 + i, -1 + j,
                background, chunk, world)) {
            final_region = this_block.getTextureRegion(Block.COLLIDE_LEFT_DOWN);
        }

        // check if block collide on left only
        else if (tileCollide(tiles_data, this_block, -1 + i, 0 + j, background, chunk, world)) {
            final_region = this_block.getTextureRegion(Block.COLLIDE_LEFT);
        }

        // check if block collide on right only
        else if (tileCollide(tiles_data, this_block, 1 + i, 0 + j, background, chunk, world)) {
            final_region = this_block.getTextureRegion(Block.COLLIDE_RIGHT);
        }

        // check if block collide on down
        else if (tileCollide(tiles_data, this_block, 0 + i, -1 + j, background, chunk, world)) {
            final_region = this_block.getTextureRegion(Block.COLLIDE_DOWN);
        }

        // check if block collide on up
        else if (tileCollide(tiles_data, this_block, 0 + i, 1 + j, background, chunk, world)) {
            final_region = this_block.getTextureRegion(Block.COLLIDE_UP);
        }

        else {
            final_region = this_block.getTextureRegion(Block.COLLIDE_NONE);
        }

        // finally
        return final_region;*/
    }

    // func used to avoid exceptions
    public static synchronized boolean inChunkBounds(int x, int y) {
        if(World.CHUNK_SIZE - 1 < x || 0 > x) {
            return false;
        } else if (World.CHUNK_SIZE - 1 < y || 0 > y) {
            return false;
        }
        return true;
    }

    /**
     * Returns if given chunk pos is in world bounds (pos is local chunk array position)
     * @param chunk_x
     * @param chunk_y
     * @param world
     * @return
     */
    public static synchronized boolean inWorldBounds(int chunk_x, int chunk_y, World world) {
        if(world.getWorldChunks().length <= chunk_x || chunk_x < 0) {
            return false;
        }

        if(world.getWorldChunks()[0].length <= chunk_y || chunk_y < 0) {
            return false;
        }

        return true;
    }

    // func return if given tile collide with tile on xy uses short two dim
    // array
    private static synchronized boolean tileCollide(TileHolder[][] tiles_data, Block this_block,
                                int x, int y, boolean background, WorldChunk chunk, World world) {

        if(!inChunkBounds(x, y)) {
            //first find from which chunk we have to get data

            //calc this chunk in array position
            int zero_chunk_pos_x = (int) world.getWorldChunks()[0][0].getPosition().x / World.CHUNK_WORLD_SIZE;
            int zero_chunk_pos_y = (int) world.getWorldChunks()[0][0].getPosition().y / World.CHUNK_WORLD_SIZE;

            int this_chunk_x = ((int) chunk.getPosition().x / World.CHUNK_WORLD_SIZE) - zero_chunk_pos_x;
            int this_chunk_y = ((int) chunk.getPosition().y / World.CHUNK_WORLD_SIZE) - zero_chunk_pos_y;

            if(x < 0) {
                x = World.CHUNK_SIZE + x;

                if(!inWorldBounds(this_chunk_x - 1, this_chunk_y, world))
                    return false;

                //left
                WorldChunk other_chunk = world.getWorldChunks()[this_chunk_x - 1][this_chunk_y];
                if(background) {
                    if (other_chunk.getBlocks()[x][y].getBackgroundBlock().getBlockID() != world.getBlocks().AIR.getBlockID()) {
                        if (other_chunk.getBlocks()[x][y].getBackgroundBlock().getBlockGroup().equals(this_block.getBlockGroup())) {
                            return true;
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else {
                    if (other_chunk.getBlocks()[x][y].getForegroundBlock().getBlockID() != world.getBlocks().AIR.getBlockID()) {
                        if(other_chunk.getBlocks()[x][y].getForegroundBlock().getBlockGroup().equals(this_block.getBlockGroup())) {
                            return true;
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            } else if(x >= World.CHUNK_SIZE) {
                //right
                x -= World.CHUNK_SIZE;

                if (!inWorldBounds(this_chunk_x + 1, this_chunk_y, world)) {
                    return false;
                }

                WorldChunk other_chunk = world.getWorldChunks()[this_chunk_x + 1][this_chunk_y];
                if(background) {
                    if (other_chunk.getBlocks()[x][y].getBackgroundBlock().getBlockID() != world.getBlocks().AIR.getBlockID()) {
                        if (other_chunk.getBlocks()[x][y].getBackgroundBlock().getBlockGroup().equals(this_block.getBlockGroup())) {
                            return true;
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else {
                    if (other_chunk.getBlocks()[x][y].getForegroundBlock().getBlockID() != world.getBlocks().AIR.getBlockID()) {
                        if(other_chunk.getBlocks()[x][y].getForegroundBlock().getBlockGroup().equals(this_block.getBlockGroup())) {
                            return true;
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            } else if(y < 0) {
                //down
                y = World.CHUNK_SIZE + y;

                if (!inWorldBounds(this_chunk_x, this_chunk_y - 1, world)) {
                    return false;
                }

                WorldChunk other_chunk = world.getWorldChunks()[this_chunk_x][this_chunk_y - 1];
                if(background) {
                    if (other_chunk.getBlocks()[x][y].getBackgroundBlock().getBlockID() != world.getBlocks().AIR.getBlockID()) {
                        if (other_chunk.getBlocks()[x][y].getBackgroundBlock().getBlockGroup().equals(this_block.getBlockGroup())) {
                            return true;
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else {
                    if (other_chunk.getBlocks()[x][y].getForegroundBlock().getBlockID() != world.getBlocks().AIR.getBlockID()) {
                        if(other_chunk.getBlocks()[x][y].getForegroundBlock().getBlockGroup().equals(this_block.getBlockGroup())) {
                            return true;
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            } else if(y >= World.CHUNK_SIZE) {
                //up
                y -= World.CHUNK_SIZE;

                if (!inWorldBounds(this_chunk_x, this_chunk_y + 1, world)) {
                    return false;
                }

                WorldChunk other_chunk = world.getWorldChunks()[this_chunk_x][this_chunk_y + 1];
                if(background) {
                    if (other_chunk.getBlocks()[x][y].getBackgroundBlock().getBlockID() != world.getBlocks().AIR.getBlockID()) {
                        if (other_chunk.getBlocks()[x][y].getBackgroundBlock().getBlockGroup().equals(this_block.getBlockGroup())) {
                            return true;
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else {
                    if (other_chunk.getBlocks()[x][y].getForegroundBlock().getBlockID() != world.getBlocks().AIR.getBlockID()) {
                        if(other_chunk.getBlocks()[x][y].getForegroundBlock().getBlockGroup().equals(this_block.getBlockGroup())) {
                            return true;
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            }

            return false;
        } else {
            if (!background) {
                if (tiles_data[x][y].getForegroundBlock().getBlockID() != world.getBlocks().AIR.getBlockID()) {
                    if (tiles_data[x][y].getForegroundBlock().getBlockGroup().equals(this_block.getBlockGroup())) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                if (tiles_data[x][y].getBackgroundBlock().getBlockID() != world.getBlocks().AIR.getBlockID()) {
                    if (tiles_data[x][y].getBackgroundBlock().getBlockGroup().equals(this_block.getBlockGroup())) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
    }
}

