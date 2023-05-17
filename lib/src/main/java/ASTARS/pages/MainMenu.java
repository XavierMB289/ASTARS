package ASTARS.pages;

import java.awt.Font;
import java.awt.Graphics2D;

import ASTARS.backend.AppData;
import ASTARS.backend.StockDataHandler;
import ASTARS.c.AppColors;
import ASTARS.c.AppKeys;
import backend.obj.GameScreen;
import engine.GameEngine;
import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.account.Account;
import net.jacobpeterson.alpaca.model.properties.DataAPIType;
import net.jacobpeterson.alpaca.model.properties.EndpointAPIType;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;

public class MainMenu extends GameScreen {
	
	AlpacaAPI alpaca = null;
	
	Account myAccount = null;
	
	StockDataHandler sdh = null;

	public MainMenu(GameEngine e) {
		super(e);
	}

	@Override
	public void init() {
		String keyID = (String)engine.getGlobalVars().getGlobal(AppKeys.KEY_ID);
		String secretID = (String)engine.getGlobalVars().getGlobal(AppKeys.SECRET_KEY);
		alpaca = new AlpacaAPI(keyID, secretID, EndpointAPIType.PAPER, DataAPIType.IEX);
		//Get a starting point for the account on hand
		myAccount = updateAccount();
		//Setting up SDH
		sdh = new StockDataHandler(alpaca);
	}

	@Override
	public void paint(Graphics2D g) {
		g.setColor(AppColors.WHITE);
		g.fillRect(0, 0, 1280, 720);
		//Left Panel
			fillPanel(g, 10, 10, 315, 700);
			g.setColor(AppColors.BLACK);
			g.setFont(new Font("Calibri", Font.PLAIN, 16));
			g.drawString(myAccount.getAccountNumber(), 30, 30);
			g.drawString("$"+myAccount.getCash(), 30, 50);
			//Looping through ticks
			String[] ticks = sdh.getTickers();
			for(int i = 0; i < ticks.length; i++) {
				g.drawString(ticks[i], 40, 100+(i*30));
			}
		//Mid Panel
			fillPanel(g, 335, 10, 935, 500);
			//Looping through data for the prices
			AppData[] data = sdh.getCurrentData();
			for(int i = 1; i < data.length; i++) {
				AppData prevData = data[i-1];
				AppData currData = data[i];
				g.drawLine((i-1), (int)prevData.getClose(), i, (int)currData.getClose());
			}
		//Bottom Panel
			fillPanel(g, 335, 520, 935, 190);
	}
	
	/**
	 * Creates the Background Panels
	 * @param g a Graphics2D Object
	 * @param x X Coord of the top left of the panel
	 * @param y Y Coord of the top left of the panel
	 * @param w the width of the panel
	 * @param h the height of the panel
	 */
	private void fillPanel(Graphics2D g, int x, int y, int w, int h) {
		g.setColor(AppColors.SPACE_CADET);
		g.fillRect(x, y, w, h);
		g.setColor(AppColors.COLUMBIA_BLUE);
		g.fillRect(x+5, y+5, w-10, h-10);
	}
	
	boolean testing = false;
	
	@Override
	public void update(double delta) {
		//TODO: Run setupData every half hour
	}
	
	@Override
	public void onEngineStop() {
		alpaca.getOkHttpClient().dispatcher().executorService().shutdown();
		alpaca.getOkHttpClient().connectionPool().evictAll();
	}
	
	/**
	 * Updates the Account data from Alpaca
	 * @return the Alpaca Account
	 */
	private Account updateAccount() {
		try {
			Account account = alpaca.account().get();
			return account;
		} catch (AlpacaClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}