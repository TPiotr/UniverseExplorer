package explorer.game.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.esotericsoftware.minlog.Log;

import explorer.game.ExplorerGame;

public class DesktopLauncher {
	public static void main (String[] arg) {
		//pack all textures into atals
		TexturePacker.Settings settings = new TexturePacker.Settings();
		settings.maxWidth = 4096;
		settings.maxHeight = 4096;

		settings.filterMag = Texture.TextureFilter.Nearest;
		settings.filterMin = Texture.TextureFilter.Nearest;

		settings.bleed = true;
		settings.bleedIterations = 4;

		settings.limitMemory = false;
		settings.fast = true;

		settings.combineSubdirectories = true;

		TexturePacker.processIfModified(settings,"assets_to_pack", "atlas", "main_atlas");

		//run game
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();

		config.width = 1280;
		config.height = 720;
		config.foregroundFPS = 60;
		config.vSyncEnabled = false;

		new LwjglApplication(new ExplorerGame(), config);
	}
}
