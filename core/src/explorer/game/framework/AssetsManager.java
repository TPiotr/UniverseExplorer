package explorer.game.framework;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.minlog.Log;

import java.awt.TextField;
import java.util.HashMap;
import java.util.Iterator;

public class AssetsManager {
	
	private TextureFilter default_min_filter;
	private TextureFilter default_mag_filter;
	
	//old_assets.textures
	private HashMap<String, Texture> textures;

	//atlases
	private HashMap<String, TextureAtlas> atlases;

	/**
	 * Helpful for debug bitmap font with default font;
	 */
	public static BitmapFont font;

	//create custom class fontkey for assets.fonts loading process
	private class FontKey {
		public String font_path;
		public int font_size;
		
		public FontKey(String font_path, int font_size) {
			this.font_path = font_path;
			this.font_size = font_size;
		}
	}
	
	//assets.fonts
	private HashMap<FontKey, BitmapFont> fonts;
	
	//in most cases for animations
	private HashMap<Texture, TextureRegion[][]> sprite_sheets;
	
	//callback class to handle assets loading on other threads
	public static interface AssetCallback {
		//objects because we will use it for assets.fonts, old_assets.textures, textureregions, textureregions arrays etc. (user will know what he is loading)
		public void loaded(Object asset);
	}
	
	//class for loading texture assets.atlas with animations frames on multiple threads
	private class TextureAtlasKey {
		public int tile_x, tile_y;
		public String path;
		
		public TextureAtlasKey(int tile_x, int tile_y, String path) {
			this.tile_x = tile_x;
			this.tile_y = tile_y;
			this.path = path;
		}
	}
	
	//helper class to loading old_assets.textures on other thread
	private class TextureRequest {
		public String path;
		public AssetCallback callback;
		
		public TextureRequest(String path, AssetCallback callback) {
			this.path = path;
			this.callback = callback;
		}
	}
	
	//for multithreading stuff
	private Array<TextureRequest> textures_to_load;
	private HashMap<FontKey, AssetCallback> fonts_to_load;
	private HashMap<TextureAtlasKey, AssetCallback> texture_atlases_to_load;

	private TextureAtlas main_atlas;

	public AssetsManager() {
		//init base stuff
		textures = new HashMap<String, Texture>();
		fonts = new HashMap<FontKey, BitmapFont>();
		sprite_sheets = new HashMap<Texture, TextureRegion[][]>();
		atlases = new HashMap<String, TextureAtlas>();

		textures_to_load = new Array<TextureRequest>();
		fonts_to_load = new HashMap<FontKey, AssetCallback>();
		texture_atlases_to_load = new HashMap<TextureAtlasKey, AssetCallback>();

		//load main assets.atlas
		main_atlas = getTextureAtlas("atlas/main_atlas.atlas");

		default_min_filter = TextureFilter.Nearest;
		default_mag_filter = TextureFilter.Nearest;

		font = getFont("fonts/pixel_font.ttf", 10, TextureFilter.Nearest, TextureFilter.Nearest, Color.WHITE);

	}
	
	//func called in opengl thread to load some old_assets.textures and pass them to another thread
	public synchronized void update() {
		//handle old_assets.textures loading
		if(textures_to_load.size > 0) {
			for(int i = 0; i < textures_to_load.size; i++) {
				TextureRequest request = textures_to_load.get(i);
				
				AssetCallback callback = request.callback;
				Texture texture = getTexturee(request.path);
				callback.loaded(texture);
			}
			textures_to_load.clear();
		}
		
		//handle font loading
		if(fonts_to_load.size() > 0) {
			for(FontKey key : fonts_to_load.keySet()) {
				BitmapFont font = getFont(key.font_path, key.font_size);
				AssetCallback callback = fonts_to_load.get(key);
				callback.loaded(font);
			}
			fonts_to_load.clear();
		}
		
		if(texture_atlases_to_load.size() > 0) {
			Iterator<TextureAtlasKey> iterator = texture_atlases_to_load.keySet().iterator();
			while(iterator.hasNext()) {
				TextureAtlasKey key = iterator.next();
				TextureAtlas atlas = getTextureAtlasWithSplitedTextureRegion(key.tile_x, key.tile_y, key.path);
				AssetCallback callback = texture_atlases_to_load.get(key);
				callback.loaded(atlas);
			}
			texture_atlases_to_load.clear();
		}
	}
	
	//base get texture function
	public Texture getTexturee(String path) {
		if(textures.containsKey(path)) {
			return textures.get(path);
		} else {
			//load texture
			Texture texture = new Texture(Gdx.files.internal(path));
			textures.put(path, texture);
			return texture;
		}
	}
	
	public Texture getTexturee(String path, TextureFilter min_filter, TextureFilter mag_filter) {
		Texture texture = getTexturee(path);
		texture.setFilter(min_filter, mag_filter);
		return texture;
	}

	/**
	 * Returns texture region from main game texture assets.atlas
	 * @param name name of texture region
	 * @return texture region
	 */
	public TextureRegion getTextureRegion(String name) {
		TextureRegion region = main_atlas.findRegion(name);

		if(region == null) {
			Log.error("Couldn't found texture region with name: " + name + "!");

			//grab basic white texture to not crash whole engine
			region = main_atlas.findRegion("white_texture");
		}

		return region;
	}

	public TextureAtlas getTextureAtlas(String path) {
		if(atlases.containsKey(path)) {
			return atlases.get(path);
		} else {
			//load assets.atlas
			TextureAtlas atlas = new TextureAtlas(Gdx.files.internal(path));
			atlases.put(path, atlas);
			return atlas;
		}
	}
	
	//func to get some texture if you are not in opengl thread(you need to wait for callback) 
	public synchronized void getTextureOnOtherThread(String path, AssetCallback callback) {
		if(textures.containsKey(path)) {
			callback.loaded(textures.get(path));
		} else {
			textures_to_load.add(new TextureRequest(path, callback));
		}
	}
	
	//base get font function
	public BitmapFont getFont(String path, int size) {
		FontKey key = new FontKey(path, size);
		if(fonts.containsKey(key)) {
			return fonts.get(key);
		} else {
			FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(path));
			FreeTypeFontParameter parameter = new FreeTypeFontParameter();
			
			parameter.size = size;
			parameter.genMipMaps = false;
			parameter.magFilter = default_mag_filter;
			parameter.minFilter = default_min_filter;
			
			BitmapFont font = generator.generateFont(parameter);
			generator.dispose();
			
			fonts.put(key, font);
			
			return font;
		}
	} 
	
	public BitmapFont getFont(String path, int size, TextureFilter min_filter, TextureFilter mag_filter, Color font_color) {
		FontKey key = new FontKey(path, size);
		if(fonts.containsKey(key)) {
			return fonts.get(key);
		} else {
			FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(path));
			FreeTypeFontParameter parameter = new FreeTypeFontParameter();
			
			parameter.size = size;
			parameter.genMipMaps = false;
			parameter.magFilter = mag_filter;
			parameter.minFilter = min_filter;
			parameter.color = font_color;
			
			BitmapFont font = generator.generateFont(parameter);
			generator.dispose();
			
			fonts.put(key, font);
			
			return font;
		}
	} 
	
	//func to get font on other thread than opengl
	public synchronized void getFont(String path, int size, AssetCallback callback) {
		FontKey key = new FontKey(path, size);
		if(fonts.containsKey(key)) {
			callback.loaded(fonts.get(key));
		} else {
			fonts_to_load.put(key, callback);
		}
	}
		
	//spliting 
	public TextureRegion getTileTextureFromSpritesheet(String sheet_path, int x, int y, int tx, int ty) {
		return getTilesFromSpriteSheet(sheet_path, tx, ty)[x][y];
	}
	
	public TextureRegion[][] getTilesFromSpriteSheet(String sheet_path, int tx, int ty, TextureFilter filter_min, TextureFilter filter_mag) {
		Texture spritesheet = getTexturee(sheet_path, filter_min, filter_mag);
		if(!sprite_sheets.containsKey(spritesheet)) {
			TextureRegion[][] tiles_array = TextureRegion.split(spritesheet, tx, ty);
	
			sprite_sheets.put(spritesheet, tiles_array);
			return tiles_array;
		} else {
			return sprite_sheets.get(spritesheet);
		}
		
	}
	public TextureRegion[][] getTilesFromSpriteSheet(String sheet_path, int tx, int ty) {
		Texture spritesheet = getTexturee(sheet_path);
		if(!sprite_sheets.containsKey(spritesheet)) {
			TextureRegion[][] tiles_array = TextureRegion.split(spritesheet, tx, ty);
	
			sprite_sheets.put(spritesheet, tiles_array);
			return tiles_array;
		} else {
			return sprite_sheets.get(spritesheet);
		}
		
	}
	
	public TextureAtlas getTextureAtlasWithSplitedTextureRegion(int tile_x, int tile_y, String path) {
		TextureRegion[][] right_anim_keys = getTilesFromSpriteSheet(path, tile_x, tile_y);
		
		TextureAtlas atlas = new TextureAtlas();
		for(int i = 0; i < right_anim_keys[0].length; i++) 
			atlas.addRegion(""+i, right_anim_keys[0][i]);
		
		return atlas;
	}
	public TextureAtlas getTextureAtlasWithSplitedTextureRegion(int tile_x, int tile_y, String path, TextureFilter min_filter, TextureFilter mag_filter) {
		TextureRegion[][] right_anim_keys = getTilesFromSpriteSheet(path, tile_x, tile_y, min_filter, mag_filter);
		
		TextureAtlas atlas = new TextureAtlas();
		for(int i = 0; i < right_anim_keys[0].length; i++) 
			atlas.addRegion(""+i, right_anim_keys[0][i]);
		
		return atlas;
	}
	
	public synchronized void getTextureAtlasWithSplitedTextureRegionOnOtherThread(int tile_x, int tile_y, String path, AssetCallback callback) {
		//if(!old_assets.textures.containsKey(path)) {
			texture_atlases_to_load.put(new TextureAtlasKey(tile_x, tile_y, path), callback);
		//} else {
		//	callback.loaded(getTextureAtlasWithSplitedTextureRegion(tile_x, tile_y, path));
		//}
	}
	
	//and dispose function
	public void dispose() {
		for(Texture texture : textures.values()) {
			texture.dispose();
		}
		
		for(BitmapFont font : fonts.values()) {
			font.dispose();
		}

		for(TextureAtlas atlas : atlases.values()) {
			atlas.dispose();
		}
	}
	
	//get and set default filters
	public TextureFilter getDefaultMinFilter() {
		return default_min_filter;
	}
	public TextureFilter getDefaultMagFilter() {
		return default_mag_filter;
	}

	public void setDefaultMinFilter(TextureFilter filter) {
		default_min_filter = filter;
	} 
	public void setDefaultMagFilter(TextureFilter filter) {
		default_mag_filter = filter;
	}
}
