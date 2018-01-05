package explorer.world.physics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import explorer.game.framework.Game;
import explorer.game.framework.utils.MathHelper;
import explorer.world.World;
import explorer.world.block.Block;
import explorer.world.chunk.TileHolder;
import explorer.world.chunk.WorldChunk;
import explorer.world.object.DynamicWorldObject;
import explorer.world.object.StaticWorldObject;
import explorer.world.object.WorldObject;
import explorer.world.physics.shapes.PhysicsShape;

import static explorer.world.chunk.TileHolderTools.inWorldBounds;

public class PhysicsEngine {

	private World world;
	private Game game;
	
	//world physics chunks 
	private Array<StaticWorldObject> reusable_static_objects_array;
	private ObjectMap<Integer, Array<StaticWorldObject>> static_objects_map;
	private float PHYSICS_CHUNK_SIZE_X = 32f;
	
	//lists that contains all different types of objects
	private Array<StaticWorldObject> static_objects;
	private Array<DynamicWorldObject> dynamic_objects;
	private Array<WorldObject> sensors;
	
	final private Vector<WorldObject> add_objects_bufor;
	final private Vector<WorldObject> remove_objects_bufor;
	
	//physics engine parameters
	private float GRAVITY = -1200f;
	
	//by this value we mean how far away dynamic objects can be to be affected by physics
	public static final float DYNAMIC_WORK_RANGE = 500f;
	
	//delta managment stuff to avoid bug when because of bug game will in fact teleport us in directon of last velocity
	private float[] deltas;
	private float avg_delta = 10f; 
	private float max_delta;
	private float delta_sum;
	private int delta_index;
	
	//value used to check sensors only once per 4 tick to save some performance
	private int tick_number;
	private final int sensor_ticks_div = 4;

	public PhysicsEngine(World world, Game game) {
		this.world = world;
		this.game = game;

		static_objects_map = new ObjectMap<Integer, Array<StaticWorldObject>>();
		reusable_static_objects_array = new Array<StaticWorldObject>();
		
		dynamic_objects = new Array<DynamicWorldObject>(50);
		static_objects = new Array<StaticWorldObject>(1000);
		sensors = new Array<WorldObject>(10);
		
		add_objects_bufor = new Vector<WorldObject>();//Collections.synchronizedList(new ArrayList<WorldObject>());//new Array<WorldObject>(100);
		remove_objects_bufor = new Vector<WorldObject>();//Collections.synchronizedList(new ArrayList<WorldObject>());

		deltas = new float[10];
	}

	public void tick(float in_delta) {
		/* DELTA MANAGEMENT STUFF */

		//why this? this code helps to avoid bug when some bug occur and our delta will be very big for example 10 (because game was lagged for f.e. 10 secs)
		//using directly that big delta will cause bug when every dynamic object with velocity > 0 will teleport probably far away compared to previous location
		//so calculate avg delta from last 10 deltas if in_delta is bigger than avg and some threshold it means that some lag occured
		//this problem resolve will not make any problems when entering f.e. laggy area because avg delta is calculating in real time

		tick_number++;
		float delta = in_delta;
		
		if(delta > max_delta)
			max_delta = delta;
		
		//put new delta into our array to calc avg delta in future
		delta_index++;
		
		if(delta_index >= deltas.length) {
			//calc avg delta
			for(int i = 0; i < deltas.length; i++)
				delta_sum += deltas[i];
			
			avg_delta = delta_sum / (float) deltas.length;
			delta_sum = 0;
			
			delta_index = 0;
		}
		
		//put delta only if is nearby avg 
		if(delta < avg_delta + .10f) {
			deltas[delta_index] = delta;
		} else {
			delta = avg_delta;
		}
		
		if(tick_number == sensor_ticks_div) {
			tick_number = 0;
		}

		/* EMPTY THE ADD/REMOVE BUFFERS */

		//add, remove object just before sim to avoid concurrent modification exception
		synchronized (add_objects_bufor) {
			if (add_objects_bufor.size() > 0) {
				for (Iterator<WorldObject> i = add_objects_bufor.iterator(); i.hasNext(); ) {
					WorldObject to_add_object = i.next();

					if (to_add_object instanceof StaticWorldObject) {
						static_objects.add((StaticWorldObject) to_add_object);

						addToStaticObjectsGrid((StaticWorldObject) to_add_object);
					} else if (to_add_object instanceof DynamicWorldObject) {
						dynamic_objects.add((DynamicWorldObject) to_add_object);
					}

					if (to_add_object instanceof SensorObject) {
						sensors.add(to_add_object);
					}
				}
				add_objects_bufor.clear();
			}
		}

		synchronized (remove_objects_bufor) {
			if (remove_objects_bufor.size() > 0) {
				for (Iterator<WorldObject> i = remove_objects_bufor.iterator(); i.hasNext(); ) {
					WorldObject to_remove_object = i.next();

					if (to_remove_object instanceof StaticWorldObject) {
						static_objects.removeValue((StaticWorldObject) to_remove_object, true);

						removeFromStaticObjectsGrid(to_remove_object);
					} else if (to_remove_object instanceof DynamicWorldObject) {
						dynamic_objects.removeValue((DynamicWorldObject) to_remove_object, true);
					}

					if (to_remove_object instanceof SensorObject) {
						sensors.removeValue(to_remove_object, true);
					}
				}
				remove_objects_bufor.clear();
			}
		}

		/* PROPER PHYSICS CALCULATIONS */

		physicsCalculations(delta);
	}

	/**
	 * Function that do the whole physics job in therms of resolving collisions etc.
	 * @param delta delta time
	 */
	private void physicsCalculations(float delta) {
		//with that implementation dynamic objects don't collide with other dynamics
		for(Iterator<DynamicWorldObject> i = dynamic_objects.iterator(); i.hasNext();) {
			DynamicWorldObject dynamic_object = i.next();
			reusable_static_objects_array.clear();

			//if physics calculations are disabled just skip this objects calculations
			if(!dynamic_object.isPhysicsEnabled()) {
				continue;
			}

			if(Vector2.dst(dynamic_object.getPosition().x, dynamic_object.getPosition().y,
					game.getMainCamera().position.x, game.getMainCamera().position.y) < DYNAMIC_WORK_RANGE) {

				/* RESOLVING STUCK PROBLEM */

				//here check if center of object overlaps with some block if so lift object up to resolve collision with block
				int block_x_object_center = (int) ((dynamic_object.getPosition().x + (dynamic_object.getWH().x / 2f)) - dynamic_object.getParentChunk().getPosition().x) / World.BLOCK_SIZE;
				int block_y_object_center = (int) ((dynamic_object.getPosition().y + (dynamic_object.getWH().y / 2f)) - dynamic_object.getParentChunk().getPosition().y) / World.BLOCK_SIZE;
				boolean resolved = false;

				if(WorldChunk.inChunkBounds(block_x_object_center, block_y_object_center)) {
					Block colliding_block = dynamic_object.getParentChunk().getBlocks()[block_x_object_center][block_y_object_center].getForegroundBlock();

					//if colliding block is collidable we have problem and we have to lift our object
					if(colliding_block.isCollidable()) {
						dynamic_object.getVelocity().set(0, 0);
						dynamic_object.getPosition().y += World.BLOCK_SIZE;

						resolved = true;
					}
				}

				/* COLLISION PART WITH BLOCKS ONLY */

				//vars used in whole process of physics calculation
				Vector2 object_velocity = dynamic_object.getVelocity();

				object_velocity.add(0, GRAVITY * delta);
				object_velocity.scl(delta);

				//how it works:
				//get blocks around dynamic object and iterate through them to check collision like with normal rectangulary static world objects

				//vars used in collision with chunk blocks
				int offset_add_x = (int) Math.max((dynamic_object.getWH().x / World.BLOCK_SIZE) * 1.5f, 4);
				int offset_add_y = (int) Math.max((dynamic_object.getWH().y / World.BLOCK_SIZE) * 1.5f, 2);

				int blocks_start_x = (int) (((dynamic_object.getPosition().x - (dynamic_object.getWH().x / 2)) - dynamic_object.getParentChunk().getPosition().x) / World.BLOCK_SIZE + 1) - (offset_add_x / 2);
				int blocks_start_y = (int) (((dynamic_object.getPosition().y) - dynamic_object.getParentChunk().getPosition().y) / World.BLOCK_SIZE + 1) - (offset_add_y / 2);

				int blocks_width = (int) Math.max((dynamic_object.getWH().x / World.BLOCK_SIZE), 1) + (offset_add_x);
				int blocks_height = (int) Math.max((dynamic_object.getWH().y / World.BLOCK_SIZE), 1) + (offset_add_y);

				for(int bx = 0; bx < blocks_width; bx++) {
					for(int by = 0; by < blocks_height; by++) {
						int block_x = blocks_start_x + bx;
						int block_y = blocks_start_y + by;

						Block block = getBlock(block_x, block_y, dynamic_object.getParentChunk());

						if(block == null || !block.isCollidable())
							continue;

						//x axis
						if(MathHelper.overlaps2Rectangles(dynamic_object.getPosition().x + dynamic_object.getVelocity().x, dynamic_object.getPosition().y, dynamic_object.getWH().x, dynamic_object.getWH().y,
								(block_x * World.BLOCK_SIZE) + dynamic_object.getParentChunk().getPosition().x, (block_y * World.BLOCK_SIZE) + dynamic_object.getParentChunk().getPosition().y, World.BLOCK_SIZE, World.BLOCK_SIZE)) {
							object_velocity.x = 0;
						}

						//y axis
						if(MathHelper.overlaps2Rectangles(dynamic_object.getPosition().x, dynamic_object.getPosition().y + dynamic_object.getVelocity().y, dynamic_object.getWH().x, dynamic_object.getWH().y,
								(block_x * World.BLOCK_SIZE) + dynamic_object.getParentChunk().getPosition().x, (block_y * World.BLOCK_SIZE) + dynamic_object.getParentChunk().getPosition().y, World.BLOCK_SIZE, World.BLOCK_SIZE)) {
							if(resolved) {
								//special case because we want to get dynamic object back to to the ground level so we resolve collision always on top of collider
								//pos of dynamic doesnt matter there
								dynamic_object.getPosition().y = (block_y * World.BLOCK_SIZE) + dynamic_object.getParentChunk().getPosition().y + World.BLOCK_SIZE;
							} else if(dynamic_object.getPosition().y > (block_y * World.BLOCK_SIZE) + dynamic_object.getParentChunk().getPosition().y + (World.BLOCK_SIZE / 2f)) {
								//dynamic object is under static
								dynamic_object.getPosition().y = (block_y * World.BLOCK_SIZE) + dynamic_object.getParentChunk().getPosition().y + World.BLOCK_SIZE;
							} else {
								//dynamic object is over static
								dynamic_object.getPosition().y = (block_y * World.BLOCK_SIZE) + dynamic_object.getParentChunk().getPosition().y - dynamic_object.getWH().y;
							}
							object_velocity.y = 0;
						}
					}
				}

				/* WORLD OBJECTS COLLISION STUFF */

				//get static objects from static objects map
				if(static_objects_map.get(getMapIndex(dynamic_object.getPosition().x)) != null) {
					reusable_static_objects_array.addAll(static_objects_map.get(getMapIndex(dynamic_object.getPosition().x)));
				}
				if(static_objects_map.get(getMapIndex(dynamic_object.getPosition().x) + 1) != null) {
					reusable_static_objects_array.addAll(static_objects_map.get(getMapIndex(dynamic_object.getPosition().x) + 1));
				}
				if(static_objects_map.get(getMapIndex(dynamic_object.getPosition().x) - 1) != null) {
					reusable_static_objects_array.addAll(static_objects_map.get(getMapIndex(dynamic_object.getPosition().x) - 1));
				}

				//check x axis
				for(Iterator<StaticWorldObject> ii = reusable_static_objects_array.iterator(); ii.hasNext();) {
					StaticWorldObject static_object = ii.next();
					//if rect is nearby check collision with it

					if(static_object.getPhysicsShape().overlaps(dynamic_object, dynamic_object.getPhysicsShape(), 0, 0, dynamic_object.getVelocity().x, 0)) {   //dynamic_temp_rect.overlaps(temp_rect2)) {
						object_velocity.x = 0;

						if(tick_number == 0) {
							//sensors stuff
							if(static_object instanceof SensorObject) {
								((SensorObject) static_object).collide(dynamic_object);
							}
							if(dynamic_object instanceof SensorObject) {
								((SensorObject) dynamic_object).collide(static_object);
							}
						}

						break;
					}
				}

				//check y axis
				for(Iterator<StaticWorldObject> ii = reusable_static_objects_array.iterator(); ii.hasNext();) {
					StaticWorldObject static_object = ii.next();

					if(dynamic_object.getPhysicsShape().overlaps(static_object, static_object.getPhysicsShape(), 0, dynamic_object.getVelocity().y, 0, 0)) {
						if(resolved) {
							//special case because we want to get dynamic object back to to the ground level so we resolve collision always on top of collider
							//pos od dynamic dont matter there
							dynamic_object.getPosition().y = static_object.getPosition().y + static_object.getWH().y;
						} else if(dynamic_object.getPosition().y + object_velocity.y > static_object.getPosition().y + (static_object.getWH().y / 2f)) {
							//dynamic object is under static
							dynamic_object.getPosition().y = static_object.getPosition().y + static_object.getWH().y;
						} else {
							//dynamic object is over static
							dynamic_object.getPosition().y = static_object.getPosition().y - dynamic_object.getWH().y;
						}
						object_velocity.y = 0;

						if(tick_number == 0) {
							//sensors stuff
							if(static_object instanceof SensorObject) {
								((SensorObject) static_object).collide(dynamic_object);
							}
							if(dynamic_object instanceof SensorObject) {
								((SensorObject) dynamic_object).collide(static_object);
							}
						}

						break;
					}
				}

				//try to collide with other dynamics if thi and other dynamic had proper flag
				if(dynamic_object.isCollidingWithOtherDynamicObjects()) {

					for(int k = 0; k < dynamic_objects.size; k++) {
						DynamicWorldObject other_dynamic = dynamic_objects.get(k);

						//if physics calculations are disabled to some other dynamic object just skip its collision calculations
						if(!other_dynamic.isPhysicsEnabled()) {
							continue;
						}

						//at first other dynamic had to have this flag set to true too
						if(other_dynamic.isCollidingWithOtherDynamicObjects() && !(other_dynamic == dynamic_object)) {
							//now distance cull

							float this_dynamic_radius = Math.max(dynamic_object.getWH().x, dynamic_object.getWH().y) / 2f;
							float other_dynamic_radius = Math.max(other_dynamic.getWH().x, other_dynamic.getWH().y) / 2f;

							float dst_x = Math.abs((dynamic_object.getPosition().x + this_dynamic_radius) - (other_dynamic.getPosition().x + other_dynamic_radius));
							float dst_y = Math.abs((dynamic_object.getPosition().y + this_dynamic_radius) - (other_dynamic.getPosition().y + other_dynamic_radius));

							//if(dst_x < (this_dynamic_radius + other_dynamic_radius) && dst_y < (this_dynamic_radius + other_dynamic_radius)) {
							//if we are here calculate collision

							//check x axis
							if(other_dynamic.getPhysicsShape().overlaps(dynamic_object, dynamic_object.getPhysicsShape(), 0, 0, dynamic_object.getVelocity().x, 0)) {   //dynamic_temp_rect.overlaps(temp_rect2)) {
								object_velocity.x = 0;

								if(tick_number == 0) {
									//sensors stuff
									if(other_dynamic instanceof SensorObject) {
										((SensorObject) other_dynamic).collide(dynamic_object);
									}
									if(dynamic_object instanceof SensorObject) {
										((SensorObject) dynamic_object).collide(other_dynamic);
									}
								}

								//break;
							}

							//check y axis
							if(dynamic_object.getPhysicsShape().overlaps(other_dynamic, other_dynamic.getPhysicsShape(), 0, dynamic_object.getVelocity().y, 0, 0)) {
								if (dynamic_object.getPosition().y + object_velocity.y > other_dynamic.getPosition().y + (other_dynamic.getWH().y / 2f)) {
									//dynamic object is under
									dynamic_object.getPosition().y = other_dynamic.getPosition().y + other_dynamic.getWH().y;
								} else {
									//dynamic object is over
									dynamic_object.getPosition().y = other_dynamic.getPosition().y - dynamic_object.getWH().y;
								}
								object_velocity.y = 0;

								if (tick_number == 0) {
									//sensors stuff
									if (other_dynamic instanceof SensorObject) {
										((SensorObject) other_dynamic).collide(dynamic_object);
									}
									if (dynamic_object instanceof SensorObject) {
										((SensorObject) dynamic_object).collide(other_dynamic);
									}
								}
							}
							//}
						}
					}
				}

				dynamic_object.getPosition().add(object_velocity);

				object_velocity.scl(1f / delta);

				//with that we can simulate smth. like friction
				object_velocity.x *= .1f;//*= (object_velocity.x - (object_velocity.x * PLAYER_DAMPING)) * delta;
			}
		}

		//check if sensors collide with dynamic objects
		if(tick_number == 0) {
			for(int ii = 0; ii < sensors.size; ii++) {
				WorldObject sensor = sensors.get(ii);

				PhysicsShape sensor_shape = null;
				if(sensor instanceof StaticWorldObject)
					sensor_shape = ((StaticWorldObject) sensor).getPhysicsShape();
				else if(sensor instanceof DynamicWorldObject)
					sensor_shape = ((DynamicWorldObject) sensor).getPhysicsShape();

				if(sensor_shape != null) {
					for(int i = 0; i < dynamic_objects.size; i++) {
						DynamicWorldObject object = dynamic_objects.get(i);

						if (sensor != object) {
							if(object.getPhysicsShape().overlaps(sensor, sensor_shape)) {
								((SensorObject) sensor).collide(object);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Get block from given coords at given world chunk
	 * This function holds situation when x,y < 0 or x,y > World.CHUNK_SIZE
	 * then it gets blocks data from given chunk neighbour if it exists
	 * @param x
	 * @param y
	 * @param parent_chunk
	 * @return
	 */
	private Block getBlock(int x, int y, WorldChunk parent_chunk) {
		//if true we have situation where we have to grab data from other chunk than given
		if(!WorldChunk.inChunkBounds(x, y)) {
			//first find from which chunk we have to get data

			//calc this chunk in array position
			int zero_chunk_pos_x = (int) world.getWorldChunks()[0][0].getPosition().x / World.CHUNK_WORLD_SIZE;
			int zero_chunk_pos_y = (int) world.getWorldChunks()[0][0].getPosition().y / World.CHUNK_WORLD_SIZE;

			int this_chunk_x = ((int) parent_chunk.getPosition().x / World.CHUNK_WORLD_SIZE) - zero_chunk_pos_x;
			int this_chunk_y = ((int) parent_chunk.getPosition().y / World.CHUNK_WORLD_SIZE) - zero_chunk_pos_y;

			if(x < 0) {
				x = World.CHUNK_SIZE + x;

				if(!inWorldBounds(this_chunk_x - 1, this_chunk_y, world))
					return null;

				if(!WorldChunk.inChunkBounds(x, y))
					return null;

				//left
				WorldChunk other_chunk = world.getWorldChunks()[this_chunk_x - 1][this_chunk_y];
				return other_chunk.getBlocks()[x][y].getForegroundBlock();
			} else if(x >= World.CHUNK_SIZE) {
				//right
				x -= World.CHUNK_SIZE;

				if (!inWorldBounds(this_chunk_x + 1, this_chunk_y, world)) {
					return null;
				}

				if(!WorldChunk.inChunkBounds(x, y))
					return null;

				WorldChunk other_chunk = world.getWorldChunks()[this_chunk_x + 1][this_chunk_y];
				return other_chunk.getBlocks()[x][y].getForegroundBlock();
			} else if(y < 0) {
				//down
				y = World.CHUNK_SIZE + y;

				if (!inWorldBounds(this_chunk_x, this_chunk_y - 1, world)) {
					return null;
				}

				if(!WorldChunk.inChunkBounds(x, y))
					return null;

				WorldChunk other_chunk = world.getWorldChunks()[this_chunk_x][this_chunk_y - 1];
				return other_chunk.getBlocks()[x][y].getForegroundBlock();
			} else if(y >= World.CHUNK_SIZE) {
				//up
				y -= World.CHUNK_SIZE;

				if (!inWorldBounds(this_chunk_x, this_chunk_y + 1, world)) {
					return null;
				}

				if(!WorldChunk.inChunkBounds(x, y))
					return null;

				WorldChunk other_chunk = world.getWorldChunks()[this_chunk_x][this_chunk_y + 1];
				return other_chunk.getBlocks()[x][y].getForegroundBlock();
			}
		}

		return parent_chunk.getBlocks()[x][y].getForegroundBlock();
	}

	/**
	 * Debug renderer function
	 * @param renderer shape renderer instance
	 */
	public void debugRender(ShapeRenderer renderer) {
		renderer.begin(ShapeRenderer.ShapeType.Line);

		renderer.setColor(Color.BLUE);
		/*for(int i = 0; i < static_objects.size; i++) {
			StaticWorldObject o = static_objects.get(i);
			renderer.rect(o.getPosition().x, o.getPosition().y, o.getWH().x, o.getWH().y);
		}*/

		for(DynamicWorldObject dynamic_object : dynamic_objects) {
			reusable_static_objects_array.clear();

			int index = getMapIndex(dynamic_object.getPosition().x);

			if (static_objects_map.get(index) != null)
				reusable_static_objects_array.addAll(static_objects_map.get(index));
			if (static_objects_map.get(index + 1) != null)
				reusable_static_objects_array.addAll(static_objects_map.get(index + 1));
			if (static_objects_map.get(index - 1) != null)
				reusable_static_objects_array.addAll(static_objects_map.get(index - 1));

			for (StaticWorldObject o : reusable_static_objects_array) {
				renderer.rect(o.getPosition().x, o.getPosition().y, o.getWH().x, o.getWH().y);
			}
		}

		renderer.setColor(Color.GREEN);
		for(int i = 0; i < dynamic_objects.size; i++) {
			DynamicWorldObject o = dynamic_objects.get(i);
			renderer.rect(o.getPosition().x, o.getPosition().y, o.getWH().x, o.getWH().y);
		}

		renderer.setColor(Color.CYAN);

		//render blocks collision bounding rects
		for(int i = 0; i < dynamic_objects.size; i++) {
			DynamicWorldObject dynamic_object = dynamic_objects.get(i);

			int offset_add_x = (int) Math.max((dynamic_object.getWH().x / World.BLOCK_SIZE) * 1.5f, 4);
			int offset_add_y = (int) Math.max((dynamic_object.getWH().y / World.BLOCK_SIZE) * 1.5f, 2);

			int blocks_start_x = (int) (((dynamic_object.getPosition().x - (dynamic_object.getWH().x / 2)) - dynamic_object.getParentChunk().getPosition().x) / World.BLOCK_SIZE + 1) - (offset_add_x / 2);
			int blocks_start_y = (int) (((dynamic_object.getPosition().y) - dynamic_object.getParentChunk().getPosition().y) / World.BLOCK_SIZE + 1) - (offset_add_y / 2);

			int blocks_width = (int) Math.max((dynamic_object.getWH().x / World.BLOCK_SIZE), 1) + (offset_add_x);
			int blocks_height = (int) Math.max((dynamic_object.getWH().y / World.BLOCK_SIZE), 1) + (offset_add_y);

			for (int bx = 0; bx < blocks_width; bx++) {
				for (int by = 0; by < blocks_height; by++) {
					int block_x = blocks_start_x + bx;
					int block_y = blocks_start_y + by;

					Block block = getBlock(block_x, block_y, dynamic_object.getParentChunk());

					if (block == null || !block.isCollidable())
						continue;

					//System.out.println("BX: "+block_x + " BY: " + block_y);
					renderer.rect((block_x * World.BLOCK_SIZE) + dynamic_object.getParentChunk().getPosition().x, (block_y * World.BLOCK_SIZE) + dynamic_object.getParentChunk().getPosition().y, World.BLOCK_SIZE, World.BLOCK_SIZE);
				}
			}
		}

		renderer.setColor(Color.WHITE);

		renderer.end();
	}

	/**
	 * Get index for object to access proper physics chunk
	 * @param x x coord in world coords system
	 * @return index of physics chunk which given object belongs to
	 */
	private synchronized int getMapIndex(float x) {
		//take care of that the planet is circle shaped
		int total_physics_chunks = (int) (World.CHUNK_WORLD_SIZE / PHYSICS_CHUNK_SIZE_X) * world.getPlanetProperties().PLANET_SIZE;

		int index = (int) (x / PHYSICS_CHUNK_SIZE_X);
		index %= total_physics_chunks;

		return index;
	}

	/**
	 * Add object to objects grid for faster physics calculations
	 * @param object objects to add
	 */
	private synchronized void addToStaticObjectsGrid(StaticWorldObject object) {
		//handle width objects
		float object_width = object.getWH().x;

		if(object_width > PHYSICS_CHUNK_SIZE_X) {
			final float GRID_CHUNK_X = PHYSICS_CHUNK_SIZE_X;

			//calc with how much this objects is colliding with grid cells
			int width_parts = (int) (object_width / GRID_CHUNK_X);
			//System.out.println("WP:" + width_parts);

			//add this objects to overlapping grid cells
			int last_map_index = -1;
			for (int i = 0; i < width_parts; i++) {
				int map_index = getMapIndex(object.getPosition().x + (i * GRID_CHUNK_X));
				//System.out.println("index: " + map_index + " x: " + (object.getPosition().x + (i * PHYSICS_CHUNK_SIZE_X)));

				if (last_map_index < map_index) {
					//System.out.println("adding index: " + map_index);

					//if map at given don't exists yet add new one
					if (static_objects_map.get(map_index) == null) {
						Array<StaticWorldObject> new_object = new Array<StaticWorldObject>(4);
						new_object.add(object);

						static_objects_map.put(map_index, new_object);
					} else {
						static_objects_map.get(map_index).add(object);
					}
				}

				last_map_index = map_index;
			}
		} else {
			int map_index = getMapIndex(object.getPosition().x);

			//if map at given index don't exists yet create new one
			if (static_objects_map.get(map_index) == null) {
				Array<StaticWorldObject> new_object = new Array<StaticWorldObject>(4);
				new_object.add(object);

				static_objects_map.put(map_index, new_object);
			} else {
				static_objects_map.get(map_index).add(object);
			}
		}
	}

	/**
	 * Remove object from objects grid
	 * @param object object to remove
	 */
	private synchronized void removeFromStaticObjectsGrid(WorldObject object) {
		//handle width objects
		float object_width = object.getWH().x;

		if(object_width > PHYSICS_CHUNK_SIZE_X) {
			final float GRID_CHUNK_X = PHYSICS_CHUNK_SIZE_X;

			//calc with how much this objects is colliding with grid cells
			int width_parts = (int) (object_width / GRID_CHUNK_X);
			//System.out.println("WP:" + width_parts);

			//add this objects to overlapping grid cells
			int last_map_index = -1;
			for (int i = 0; i < width_parts; i++) {
				int map_index = getMapIndex(object.getPosition().x + (i * GRID_CHUNK_X));
				//System.out.println("index: " + map_index + " x: " + (object.getPosition().x + (i * PHYSICS_CHUNK_SIZE_X)));

				if (last_map_index < map_index) {
					//System.out.println("removing index: " + map_index);

					//get map if not null remove from array
					Array<StaticWorldObject> map_objects = static_objects_map.get(map_index);

					if(map_objects != null) {
						map_objects.removeValue((StaticWorldObject) object, true);

						if(map_objects.size == 0) {
							static_objects_map.remove(map_index);
						}
					}
				}

				last_map_index = map_index;
			}
		} else {
			int map_index = getMapIndex(object.getPosition().x);

			//get array of objects and remove object from it if array exists
			Array<StaticWorldObject> map_objects = static_objects_map.get(map_index);
			if(map_objects != null) {
				map_objects.removeValue((StaticWorldObject) object, true);

				if(map_objects.size == 0) {
					static_objects_map.remove(map_index);
				}
			}
		}
	}

	/**
	 * Add given world object to simulation
	 * @param object world object that will be added to simulation
	 */
	public void addWorldObject(WorldObject object) {
		synchronized (add_objects_bufor) {
			add_objects_bufor.add(object);
		}
	}

	/**
	 * Add array of objects to simulation
	 * @param objects list of objects that will be added
	 */
	public void addWorldObjects(Array<WorldObject> objects) {
		synchronized (add_objects_bufor) {
			for (WorldObject o : objects)
				addWorldObject(o);
		}
	}

	public void addWorldObjects(List<WorldObject> objects) {
		synchronized (add_objects_bufor) {
			System.out.println("adding objects count: " + objects.size());
			for (WorldObject o : objects)
				addWorldObject(o);
		}
	}

	/**
	 * Remove one world object
	 * @param object object to remove
	 */
	public void removeWorldObject(WorldObject object) {
		synchronized (remove_objects_bufor) {
			remove_objects_bufor.add(object);
		}
	}

	/**
	 * Remove given objects in array from simulation
	 * @param objects list of objects that will be removed
	 */
	public void removeWorldObjects(Array<WorldObject> objects) {
		synchronized (remove_objects_bufor) {
			for (WorldObject o : objects)
				removeWorldObject(o);
		}
	}

	public void removeWorldObjects(List<WorldObject> objects) {
		synchronized (remove_objects_bufor) {
			for (WorldObject o : objects)
				removeWorldObject(o);
		}
	}

	/**
	 * Get array that contains all static objects
	 * @return array of all static objects
	 */
	public Array<StaticWorldObject> getStaticObjects() {
		return static_objects;
	}

	/**
	 * Get all dynamic objects array
	 * @return array with all dynamic objects
	 */
	public Array<DynamicWorldObject> getDynamicObjects() {
		return dynamic_objects;
	}

	/**
	 * Get all objects that implements SensorObjects interface
	 * @return array of objects that implements SensorObject interface
	 */
	public Array<WorldObject> getSensorsObjects() {
		return sensors;
	}

	/**
	 * Get total amount of objects that this engine contains
	 * @return total amount of objects
	 */
	public int getAllObjectsCount() {
		return static_objects.size + dynamic_objects.size;
	}
}
