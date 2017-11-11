package explorer.game.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;

import explorer.game.ExplorerGame;
import explorer.game.framework.Game;
import explorer.game.screen.screens.GameScreen;

public class DesktopLauncher {
	public static void main (String[] arg) {
		//pack textures into atlas
		TexturePacker.Settings settings = new TexturePacker.Settings();
		settings.maxWidth = 1024;
		settings.maxHeight = 1024;

		settings.limitMemory = false;
		settings.fast = true;

		settings.combineSubdirectories = true;

		TexturePacker.process(settings, "assets_to_pack", "atlas", "main_atlas");

		//run game like normally
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();

		config.width = 1280;
		config.height = 720;
		config.foregroundFPS = 60;
		config.vSyncEnabled = true;

		new LwjglApplication(new ExplorerGame(), config);
	}
}
