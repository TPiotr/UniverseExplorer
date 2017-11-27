package explorer.game.framework;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.CpuSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.HashMap;

import explorer.game.framework.utils.RenderCallsCounterSpriteBatch;
import explorer.game.screen.Screen;

public abstract class Game extends ApplicationAdapter {

	//BASIC ENGINE STUFF
	private OrthographicCamera main_camera, gui_camera;
	private Viewport main_viewport, gui_viewport;

	private SpriteBatch batch;

	private InputEngine input_engine;
	private AssetsManager assets_manager;

	private explorer.game.framework.utils.ThreadPool thread_pool;

	//array that holds all screens
	private HashMap<String, Screen> screens;

	@Override
	public void create () {
		//init cameras and viewports
		//base init
		Vector2 res = new Vector2(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		int virtual_res_x = 1280;
		int virtual_res_y = 720;

		main_camera = new OrthographicCamera(res.x, res.y);
		main_camera.position.set(0, 0, 0);
		main_camera.zoom = .6f;
		main_camera.update();

		main_viewport = new StretchViewport(virtual_res_x, virtual_res_y, main_camera);

		gui_camera = new OrthographicCamera(res.x, res.y);
		gui_camera.position.set(0, 0, 0);
		gui_camera.update();

		gui_viewport = new StretchViewport(virtual_res_x, virtual_res_y, gui_camera);

		batch = new explorer.game.framework.utils.RenderCallsCounterSpriteBatch();

		thread_pool = new explorer.game.framework.utils.ThreadPool();

		//
		input_engine = new InputEngine();
		assets_manager = new AssetsManager();

		//init screens array
		screens = new HashMap<String, Screen>();

		//at last init game
		initGame();
	}

	/**
	 * Init game function called after basic engine stuff is initializated
	 */
	protected abstract void initGame();

	@Override
	public void resize(int width, int height) {
		gui_viewport.update(width, height);
		main_viewport.update(width, height);

		//update screens sizes
		for(Screen s : screens.values()) {
			s.screenSizeChanged(width, height);
		}
	}

	public void tick(float delta) {
		for(Screen screen : screens.values()) {
			if(screen.isVisible())
				screen.tick(delta);
		}
	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		float delta = Gdx.graphics.getDeltaTime();
		tick(delta);

		batch.begin();

		for(Screen s : screens.values()) {
			if(s.isVisible())
				s.render(batch);
		}

		batch.end();

		Gdx.graphics.setTitle("FPS: " + Gdx.graphics.getFramesPerSecond() + " Render calls: " + ((explorer.game.framework.utils.RenderCallsCounterSpriteBatch) batch).getRenderCalls() + " render method calls: " + ((explorer.game.framework.utils.RenderCallsCounterSpriteBatch) batch).getRenderMethodCalls());

		((RenderCallsCounterSpriteBatch) batch).resetCounter();
	}
	
	@Override
	public void dispose() {
		for(Screen s : screens.values())
			s.dispose();

		getThreadPool().dispose();
		getAssetsManager().dispose();
	}

	/**
	 * Get screen casted to proper class using this function
	 * @param screen_name screen name that we want (use ScreenClass.NAME variable)
	 * @param s_class screen class to get proper casting
	 * @param <T> class of screen
	 * @return screen instance
	 */
	public <T> T getScreen(String screen_name, Class<T> s_class) {
		return (T) screens.get(screen_name);
	}

	/**
	 * Get screen instance by its name
	 * @param screen_name name of screen that we want (use ScreenClass.NAME variable)
	 * @return screen instance
	 */
	public Screen getScreen(String screen_name) {
		return screens.get(screen_name);
	}

	/**
	 * Add new screen to game
	 * @param screen new screen instance, if exists already this one will replace previous one
	 */
	public void addScreen(Screen screen) {
		screens.put(screen.NAME, screen);
	}

	/**
	 * Get all screens that game contains stored in hashmap
	 * @return instance of hashmap that contains all game screens instances
	 */
	public HashMap<String, Screen> getScreens() {
		return screens;
	}

	/**
	 * Get instance of game thread pool
	 * @return instance of thread pool (for running some runnables in controlled way)
	 */
	public explorer.game.framework.utils.ThreadPool getThreadPool() {
		return thread_pool;
	}

	/**
	 * Get main world camera
	 * @return world camera
	 */
	public OrthographicCamera getMainCamera() {
		return main_camera;
	}

	/**
	 * Get main world camera viewport
	 * @return world camera viewport
	 */
	public Viewport getMainViewport() {
		return main_viewport;
	}

	/**
	 * Get gui camera
	 * @return gui camera
	 */
	public OrthographicCamera getGUICamera() {
		return gui_camera;
	}

	/**
	 * Get gui camera viewport
	 * @return gui camera viewport
	 */
	public Viewport getGUIViewport() {
		return gui_viewport;
	}

	/**
	 * Get assets manager instance
	 * @return assets manager instance
	 */
	public AssetsManager getAssetsManager() {
		return assets_manager;
	}

	/**
	 * Get input engine instance
	 * @return input engine instance
	 */
	public InputEngine getInputEngine() {
		return input_engine;
	}
}
