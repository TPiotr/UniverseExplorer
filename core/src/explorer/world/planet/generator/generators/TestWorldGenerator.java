package explorer.world.planet.generator.generators;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;

import explorer.game.framework.Game;
import explorer.world.ChunkDataProvider;
import explorer.world.World;
import explorer.world.object.CustomDataWorldObject;
import explorer.world.object.WorldObject;
import explorer.world.object.objects.TestDynamicObject;
import explorer.world.object.objects.TestObject;
import explorer.world.planet.PlanetProperties;
import explorer.world.planet.generator.HeightsGenerator;
import explorer.world.planet.generator.WorldGenerator;

/**
 * Created by RYZEN on 13.10.2017.
 */

public class TestWorldGenerator extends WorldGenerator {

    private HeightsGenerator heights_generator;

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
    }

    @Override
    public synchronized ChunkDataProvider.ChunkData getChunkData(Vector2 chunk_position) {
        ChunkDataProvider.ChunkData out = new ChunkDataProvider.ChunkData();

        int chunk_pos_x = (int) chunk_position.x / World.CHUNK_WORLD_SIZE;
        int chunk_pos_y = (int) chunk_position.y / World.CHUNK_WORLD_SIZE;

        int planet_width = world.getPlanetProperties().PLANET_SIZE;
        chunk_pos_x %= planet_width;

        for (int i = 0; i < out.foreground_blocks.length; i++) {
            final int height_offset = 100;
            int y = (int) heights_generator.generateHeight(i + (chunk_pos_x * World.CHUNK_SIZE), 0) + height_offset;

            //last chunk interpolating to first mechanism

            //first check if acc generating chunk is last one
            if(chunk_pos_y * World.CHUNK_SIZE <= y && (chunk_pos_y + 1) * World.CHUNK_SIZE > y && chunk_pos_x == world.getPlanetProperties().PLANET_SIZE - 1) {
                //System.out.println("last");
                int first_y = (int) heights_generator.generateHeight(chunk_pos_x * World.CHUNK_SIZE, 0) + height_offset;
                int first_chunk_first_y = (int) heights_generator.generateHeight(0, 0) + height_offset;

                //using linear interpolation make nice connection effect
                float percentage = (float) i / (float) World.CHUNK_SIZE;
                y = (int) (first_y + ((first_chunk_first_y - first_y) * percentage)) + (int) ((first_y - y) * .5f);
            }

            for (int j = 0; j < out.foreground_blocks[0].length; j++) {
                if ((j + (chunk_pos_y * World.CHUNK_SIZE)) > y)
                    out.foreground_blocks[i][j] = world.getBlocks().AIR.getBlockID();
                else if ((j + (chunk_pos_y * World.CHUNK_SIZE)) == y)
                    out.foreground_blocks[i][j] = world.getBlocks().GRASS.getBlockID();
                else
                    out.foreground_blocks[i][j] = world.getBlocks().DIRT.getBlockID();

                out.background_blocks[i][j] = out.foreground_blocks[i][j];

                if ((j + (chunk_pos_y * World.CHUNK_SIZE)) == y) {
                    //use some noise func to check if can spawn never again use random!
                    float noise = heights_generator.getNoise(i + (chunk_pos_x * World.CHUNK_SIZE), j);

                    if(noise >= .9f) {
                        out.objects.add(new TestObject(new Vector2(chunk_position).add(i * World.BLOCK_SIZE, (j + 1) * World.BLOCK_SIZE), world, game));
                    } else if(noise >= .6f && noise <= .65f) {
                        out.objects.add(new TestDynamicObject(new Vector2(chunk_position).add(i * World.BLOCK_SIZE, (j + 1) * World.BLOCK_SIZE), world, game));
                    }
                }
            }
        }

        return out;
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
            data_output.writeInt(data.objects.size);

            for(WorldObject object : data.objects) {
                if(!object.isSaveable())
                    continue;

                String class_name = object.getClass().getName();
                data_output.writeUTF(class_name);

                //save position
                data_output.writeFloat(object.getPosition().x - chunk_position.x);
                data_output.writeFloat(object.getPosition().y - chunk_position.y);

                //if object implements CustomDataWorldObject use it
                if(object instanceof CustomDataWorldObject) {
                    ((CustomDataWorldObject) object).save(data_output);
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
}
