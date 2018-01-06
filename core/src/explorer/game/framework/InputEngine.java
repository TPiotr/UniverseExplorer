package explorer.game.framework;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;

public class InputEngine {

	private InputMultiplexer multi_input;
	
	public InputEngine() {
		multi_input = new InputMultiplexer();
		Gdx.input.setInputProcessor(multi_input);
	}
	
	public void addInputProcessor(InputProcessor input) {
		multi_input.addProcessor(input);
	}

	public void addInputProcessor(InputProcessor input, int index) {
		multi_input.addProcessor(index, input);
	}

	public void remove(InputProcessor inputProcessor) {
		multi_input.removeProcessor(inputProcessor);
	}
	
	public InputMultiplexer getInputMultiplexer() {
		return multi_input;
	}
}
