package explorer.world.planet.generator.generators;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;

import explorer.game.framework.Game;
import explorer.game.framework.utils.math.FastNoise;
import explorer.world.ChunkDataProvider;
import explorer.world.World;
import explorer.world.chunk.TileHolderTools;
import explorer.world.object.WorldObject;
import explorer.world.object.objects.TestDynamicObject;
import explorer.world.object.objects.TreeObject;
import explorer.world.object.objects.earthlike.WitheredTree1Object;
import explorer.world.planet.PlanetProperties;
import explorer.world.planet.generator.HeightsGenerator;
import explorer.world.planet.generator.WorldGenerator;

/**
 * Created by RYZEN on 13.10.2017.
 */

public class TestWorldGenerator extends WorldGenerator {

    private HeightsGenerator heights_generator;
    private FastNoise noise, cave_noise;

    private final float MAX_AMPLITUDE = 100f;
    private final float MIN_AMPLITUDE = 50f;

    private final int MAX_OCTAVES = 7;
    private final int MIN_OCTAVES = 4;

    private final float MAX_ROUGHNESS = .5f;
    private final float MIN_ROUGHNESS = .0f;

    private float AMPLITUDE;
    private int OCTAVES;
    private float ROUGHNESS;

    private PlanetProperties properties;

    public TestWorldGenerator(PlanetProperties properties, World world, Game game) {
        super(world, game);

        this.properties = properties;

        AMPLITUDE = (properties.random.nextFloat() * (MAX_AMPLITUDE - MIN_AMPLITUDE)) + MIN_AMPLITUDE;
        OCTAVES = (int) (properties.random.nextFloat() * (MAX_OCTAVES - MIN_OCTAVES)) + MIN_OCTAVES;
        ROUGHNESS = (properties.random.nextFloat() * (MAX_ROUGHNESS - MIN_ROUGHNESS)) + MIN_ROUGHNESS;

        heights_generator = new HeightsGenerator(AMPLITUDE, OCTAVES, ROUGHNESS, properties.PLANET_SEED);
        noise = new FastNoise(properties.PLANET_SEED);
        cave_noise = new FastNoise(properties.PLANET_SEED + 1);
    }

    private int getHeight(int x, int chunk_pos_x) {
        final int height_offset = 200;
        int y = (int) heights_generator.generateHeight(x + (chunk_pos_x * World.CHUNK_SIZE), 0) + height_offset;

        return y;
    }

    @Override
    public synchronized ChunkDataProvider.ChunkData getChunkData(Vector2 chunk_position) {
        ChunkDataProvider.ChunkData out = new ChunkDataProvider.ChunkData();

        int chunk_pos_x = (int) chunk_position.x / World.CHUNK_WORLD_SIZE;
        int chunk_pos_y = (int) chunk_position.y / World.CHUNK_WORLD_SIZE;

        int planet_width = world.getPlanetProperties().PLANET_SIZE;
        chunk_pos_x %= planet_width;

        final int AIR = world.getBlocks().AIR.getBlockID();

        for (int i = 0; i < out.foreground_blocks.length; i++) {
            int y = getHeight(i, chunk_pos_x);
            int last_y = getHeight(i - 1, chunk_pos_x);
            int next_y = getHeight(i + 1, chunk_pos_x);

            //last chunk interpolating to first mechanism

            //first check if acc generating chunk is last one
            if(chunk_pos_y * World.CHUNK_SIZE <= y && (chunk_pos_y + 1) * World.CHUNK_SIZE > y && chunk_pos_x == world.getPlanetProperties().PLANET_SIZE - 1) {
                //System.out.println("last");
                int first_y = getHeight(0, chunk_pos_x);//(int) heights_generator.generateHeight(chunk_pos_x * World.CHUNK_SIZE, 0) + height_offset;
                int first_chunk_first_y = getHeight(0, 0);//(int) heights_generator.generateHeight(0, 0) + height_offset;

                //using linear interpolation make nice connection effect
                float percentage = (float) i / (float) World.CHUNK_SIZE;
                y = (int) (first_y + ((first_chunk_first_y - first_y) * percentage)) + (int) ((first_y - y) * .5f);
            }

            for (int j = 0; j < out.foreground_blocks[0].length; j++) {
                int global_block_y = (j + (chunk_pos_y * World.CHUNK_SIZE));
                int global_block_x = (i + (chunk_pos_x * World.CHUNK_SIZE));

                //place ground blocks
                if (global_block_y > y) {
                    out.foreground_blocks[i][j] = world.getBlocks().AIR.getBlockID();
                    out.background_blocks[i][j] = AIR;
                } else if (global_block_y == y) {
                    out.foreground_blocks[i][j] = world.getBlocks().GRASS.getBlockID();
                    out.background_blocks[i][j] = world.getBlocks().DIRT.getBlockID();
                } else {
                    out.foreground_blocks[i][j] = world.getBlocks().DIRT.getBlockID();
                    out.background_blocks[i][j] = world.getBlocks().DIRT.getBlockID();

                    //this part of code makes that grass is everywhere on ground line
                    if((global_block_y > last_y || global_block_y > next_y) && global_block_y < y) {
                        out.foreground_blocks[i][j] = world.getBlocks().GRASS.getBlockID();
                    }
                }

                //place some grass plants on grass block
                if(global_block_y == y + 1 && (noise.GetNoise(global_block_x, global_block_y) + 1f) / 2f > .3f) {
                    out.foreground_blocks[i][j] = world.getBlocks().GRASS_PLANT_BLOCK.getBlockID();
                    out.background_blocks[i][j] = AIR;
                }

                //dig caves
                boolean is_cave = false;
                final float cave_scale = 1f;
                float cave = noise.GetPerlin(global_block_x * cave_scale, global_block_y * cave_scale) + 1f;
                if(cave > 1.1f && cave < 1.2f) {
                    out.foreground_blocks[i][j] = AIR;
                    is_cave = true;
                }

                if ((j + (chunk_pos_y * World.CHUNK_SIZE)) == y && !is_cave) {
                    generateOnGroundLevelObject(chunk_position.x + (i * World.BLOCK_SIZE), chunk_position.y + ((j + 1) * World.BLOCK_SIZE), i, j, out, chunk_position, chunk_pos_x, chunk_pos_y);
                }
            }
        }

        //assign ids to objects
        for(int i = 0; i < out.objects.size; i++) {
            out.objects.get(i).OBJECT_ID = WorldObject.IDAssigner.next();
        }

        return out;
    }


    private boolean havePlace(int i, int j, int space) {
        for(int k = -space / 2; k < space / 2; k++) {
            int offset = k;

        }
        return  false;
    }

    private void generateOnGroundLevelObject(float x, float y, int i, int j, ChunkDataProvider.ChunkData out, Vector2 chunk_position, int chunk_pos_x, int chunk_pos_y) {
        //use some noise func to check if can spawn never again use random!
        float noise = heights_generator.getNoise(i + (chunk_pos_x * World.CHUNK_SIZE), j);

        if(noise >= .9f) {
            out.objects.add(new WitheredTree1Object(new Vector2(x, y).add(-WitheredTree1Object.CENTER_OFFSET, 0), world, game));
        } else if(noise >= .6f && noise <= .65f) {
            out.objects.add(new TestDynamicObject(new Vector2(x, y), world, game));
        }
    }

    @Override
    public void generateAndSaveChunk(String chunk_path, Vector2 chunk_position) {
        ChunkDataProvider.ChunkData data = getChunkData(chunk_position);//properties.PLANET_TYPE.PLANET_GENERATOR.getChunkData(chunk_position);

        try {
            DeflaterOutputStream output = new DeflaterOutputStream(Gdx.files.local(chunk_path).write(false, 1024));
            DataOutputStream data_output = new DataOutputStream(output);

            //save chunk pos
            data_output.writeInt((int) chunk_position.x / World.CHUNK_WORLD_SIZE);
            data_output.writeInt((int) chunk_position.y / World.CHUNK_WORLD_SIZE);

            //save blocks
            for(int i = 0; i < data.foreground_blocks.length; i++) {
                for(int j = 0; j < data.foreground_blocks[0].length; j++) {
                    data_output.writeInt(data.foreground_blocks[i][j]);
                    data_output.writeInt(data.background_blocks[i][j]);
                }
            }

            //objects part
            int objects_count = 0;
            for(int i = 0; i < data.objects.size; i++)
                if(data.objects.get(i).isSaveable())
                    objects_count++;

            data_output.writeInt(objects_count);

            for(WorldObject object : data.objects) {
                if(!object.isSaveable())
                    continue;

                String class_name = object.getClass().getName();
                data_output.writeUTF(class_name);

                //save position
                data_output.writeFloat(object.getPosition().x);
                data_output.writeFloat(object.getPosition().y);

                //save object id
                data_output.writeInt(object.OBJECT_ID);

                //check if we have to save properties
                if(object.getObjectProperties() == null) {
                    //save info that this object does not contain any properties
                    data_output.writeBoolean(false);
                } else {
                    //save info that this object have some properties that were saved
                    data_output.writeBoolean(true);

                    //write info about amount of properties
                    data_output.writeInt(object.getObjectProperties().size());

                    for(String key : object.getObjectProperties().keySet()) {
                        String val = object.getObjectProperties().get(key);

                        //save key, val couple
                        data_output.writeUTF(key);
                        data_output.writeUTF(val);
                    }
                }
            }

            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getMaxHeight() {
        return 5;
    }

    @Override
    public Vector2 getPlayerSpawn() {
        Vector2 out = new Vector2();

        int chunk_x = 2;
        int block_x = 10;

        int y = getHeight(block_x, chunk_x);
        out.set(chunk_x * World.CHUNK_WORLD_SIZE + (block_x * World.BLOCK_SIZE), y * World.BLOCK_SIZE);

        return out;
    }
}
