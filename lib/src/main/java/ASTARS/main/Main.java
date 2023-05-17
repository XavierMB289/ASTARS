package ASTARS.main;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import ASTARS.c.AppKeys;
import engine.GameConfigKeys;
import engine.GameEngine;

public class Main {
	
	public Main(String[] args) {
		GameEngine ge = new GameEngine();
		ge.preInit();
		Map<String, Object> config = new HashMap<>();
		config.put(GameConfigKeys.TITLE, "ASTARS");
		config.put(GameConfigKeys.EXIT_KEY, KeyEvent.VK_ESCAPE);
		config.put(GameConfigKeys.FILEPATH, "ASTARS/");
		ge.addConfig(config);
		ge.getGlobalVars().setGlobal(AppKeys.KEY_ID, getArg("-key=", args));
		ge.getGlobalVars().setGlobal(AppKeys.SECRET_KEY, getArg("-secret=", args));
		ge.setFullscreen();
		ge.init();
		ge.postInit();
	}
	
	public static void main(String[] args) {
		new Main(args);
	}
	
	private String getArg(String key, String[] args) {
		for(String s : args) {
			if(s.contains(key)) {
				return s.split("=")[1];
			}
		}
		return null;
	}
	
}
