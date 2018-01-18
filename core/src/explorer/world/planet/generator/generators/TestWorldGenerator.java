package explorer.world.planet.generator.generators;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;

import explorer.game.framework.Game;
import explorer.world.ChunkDataProvider;
import explorer.world.World;
import explorer.world.object.WorldObject;
import explorer.world.object.objects.TestDynamicObject;
import explorer.world.object.objects.TreeObject;
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

    private int getHeight(int x, int chunk_pos_x) {
        final int height_offset = 100;
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
                int this_y = (j + (chunk_pos_y * World.CHUNK_SIZE));

                if (this_y > y) {
                    out.foreground_blocks[i][j] = world.getBlocks().AIR.getBlockID();
                } else if (this_y == y) {
                    out.foreground_blocks[i][j] = world.getBlocks().GRASS.getBlockID();
                } else {
                    out.foreground_blocks[i][j] = world.getBlocks().DIRT.getBlockID();

                    //this part of code makes that grass is everywhere on ground line
                    if((this_y > last_y || this_y > next_y) && this_y < y) {
                        out.foreground_blocks[i][j] = world.getBlocks().GRASS.getBlockID();
                    }
                }

                out.background_blocks[i][j] = out.foreground_blocks[i][j];

                if ((j + (chunk_pos_y * World.CHUNK_SIZE)) == y) {
                    //use some noise func to check if can spawn never again use random!
                    float noise = heights_generator.getNoise(i + (chunk_pos_x * World.CHUNK_SIZE), j);

                    if(noise >= .9f) {
                        out.objects.add(new TreeObject(new Vector2(chunk_position).add(i * World.BLOCK_SIZE, (j + 1) * World.BLOCK_SIZE), world, game));
                    } else if(noise >= .6f && noise <= .65f) {
                        out.objects.add(new TestDynamicObject(new Vector2(chunk_position).add(i * World.BLOCK_SIZE, (j + 1) * World.BLOCK_SIZE), world, game));
                    }
                }
            }
        }

        //assign ids to objects
        for(int i = 0; i < out.objects.size; i++) {
            out.objects.get(i).OBJECT_ID = WorldObject.IDAssigner.next();
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
