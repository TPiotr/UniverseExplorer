package explorer.world.physics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
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
import explorer.world.World;
import explorer.world.block.Block;
import explorer.world.chunk.TileHolder;
import explorer.world.chunk.WorldChunk;
import explorer.world.object.DynamicWorldObject;
import explorer.world.object.StaticWorldObject;
import explorer.world.object.WorldObject;
import explorer.world.physics.shapes.PhysicsShape;

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
	
	//delta managment stuff
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
		/* DELTA MANAGAMENT STUFF */

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

		//with that implementation dynamic objects don't collide with other dynamics
		for(Iterator<DynamicWorldObject> i = dynamic_objects.iterator(); i.hasNext();) {
			DynamicWorldObject dynamic_object = i.next();
			reusable_static_objects_array.clear();
			
			if(Vector2.dst(dynamic_object.getPosition().x, dynamic_object.getPosition().y,
					game.getMainCamera().position.x, game.getMainCamera().position.y) < DYNAMIC_WORK_RANGE) {
				
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

				Vector2 object_velocity = dynamic_object.getVelocity();
				
				object_velocity.add(0, GRAVITY * delta);
				object_velocity.scl(delta);

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
				
				dynamic_object.getPosition().add(object_velocity);
				
				object_velocity.scl(1f / delta);
				
				//with that we can simulate smth. like friction
				object_velocity.x *= .1f;//*= (object_velocity.x - (object_velocity.x * PLAYER_DAMPING)) * delta;
			}
		}

		//stuck situations resolving
		/*for(Iterator<WorldObject> i = dynamic_objects.iterator(); i.hasNext();) {
			WorldObject dynamic_object = i.next();

			if(Vector2.dst(dynamic_object.getPosition().x, dynamic_object.getPosition().y,
					world.getParentScreen().getScreenCamera().position.x, world.getParentScreen().getScreenCamera().position.y) < DYNAMIC_WORK_RANGE) {
				reusable_static_objects_array.clear();

				//get static objects from static objects map
				if (static_objects_map.get(getMapIndex(dynamic_object)) != null) {
					reusable_static_objects_array.addAll(static_objects_map.get(getMapIndex(dynamic_object)));
				}
				if (static_objects_map.get(getMapIndex(dynamic_object) + 1) != null) {
					reusable_static_objects_array.addAll(static_objects_map.get(getMapIndex(dynamic_object) + 1));
				}
				if (static_objects_map.get(getMapIndex(dynamic_object) - 1) != null) {
					reusable_static_objects_array.addAll(static_objects_map.get(getMapIndex(dynamic_object) - 1));
				}

				for(int j = 0; j < reusable_static_objects_array.size; j++) {
					WorldObject static_object = reusable_static_objects_array.get(j);

					if(static_object.getPhysicsBody().overlaps(dynamic_object.getPhysicsBody())) {
						System.out.println("(PhysicsEngine) resolving stuck!");
						dynamic_object.getVelocity().y = 0;
						dynamic_object.getPosition().y += static_object.getPhysicsBody().height + 1;
					}
				}

			}
		}*/
		
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
