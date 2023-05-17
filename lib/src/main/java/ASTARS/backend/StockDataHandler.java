package ASTARS.backend;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.assets.Asset;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.historical.bar.enums.BarTimePeriod;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.StockBar;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.StockBarsResponse;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.enums.BarAdjustment;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.enums.BarFeed;
import net.jacobpeterson.alpaca.model.endpoint.watchlist.Watchlist;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;

public class StockDataHandler {
	
	private final ZoneId EST = ZoneId.of("America/Indiana/Indianapolis");
	private final int STOCK_DAYS = 3; //Number of days to collect stock data
	
	AlpacaAPI alpaca;
	Watchlist watchlist;
	
	int pointer;
	String[] tickers;
	
	Map<String, AppData[]> stockData;
	
	public StockDataHandler(AlpacaAPI alpaca) {
		//setup AlpacaAPI var
		this.alpaca = alpaca;
		//Setup SDH vars
		pointer = 0;
		tickers = new String[0];
		stockData = new HashMap<String, AppData[]>();
		//Get the current watchlist
		watchlist = getWatchlist();
		//removeFromWatchlist("TSLA");
		//addToWatchlist("DIS");
		//Setup Data on App Start
		setupData();
	}
	
	/**
	 * Use Directly
	 * Setup the SDH
	 */
	public void setupData() {
		//Get current date
		Date date = new Date();
		//update watchlist
		watchlist = getWatchlist();
		//loop through watchlist and setup SDH
		for(Asset asset : watchlist.getAssets()) {
			StockBarsResponse bars = getBars(date, asset.getSymbol());
			if(bars.getBars() != null) {
				addStock(ZonedDateTime.ofInstant(date.toInstant(), EST).minusDays(STOCK_DAYS), asset.getSymbol(), bars.getBars());
			}
		}
	}
	
	/**
	 * Sets the needed data for the appropriate ticker
	 * @param date ZonedDateTime referencing to (date - STOCK_DAYS)
	 * @param ticker Ticker for the data collected
	 * @param bars the RAW data collected
	 */
	private void addStock(ZonedDateTime date, String ticker, ArrayList<StockBar> bars) {
		AppData[] data = new AppData[STOCK_DAYS*24];
		StockBar[] newBars = new StockBar[data.length]; //so you can use the "correct" bars
		//place bars in correct order
		int prevIndex = 0;
		for(int i = 0; i < bars.size(); i++) { //Starts with the furthest date
			if(i == 0) { //First One
				Duration difference = Duration.between(date.toInstant(), bars.get(i).getTimestamp().toInstant());
				prevIndex = (int)difference.toHours();
				Arrays.fill(newBars, 0, prevIndex+1, bars.get(i));
			}else if(i == bars.size()-1) { // Last One
				Arrays.fill(newBars, prevIndex, newBars.length, bars.get(i));
			}else {
				Duration difference = Duration.between(bars.get(i).getTimestamp(), bars.get(i+1).getTimestamp());
				Arrays.fill(newBars, prevIndex, prevIndex+(int)difference.toHours()+1, bars.get(i));
				prevIndex += (int) difference.toHours();
			}
		}
		for(int i = 0; i < data.length; i++) {
			if(i < 26) {
				data[i] = new AppData((i-1 > -1 ? data[i-1] : null), i+1, newBars[i]);
			}else {
				data[i] = new AppData(data[i-1], newBars[i]);
			}
		}
		tickers = Arrays.copyOf(tickers, tickers.length+1);
		tickers[tickers.length-1] = ticker;
		stockData.put(ticker, data);
	}
	
	/**
	 * Do not Use Directly
	 * @param date The current Date. Wait 15 minutes unless they have the premium package
	 * @param symbol The ticker symbol (Example: APPL)
	 * @return A list of the StockBars, hourly.
	 */
	@SuppressWarnings("deprecation")
	private StockBarsResponse getBars(Date date, String symbol) {
		Date prevDate = Date.from(
				LocalDateTime.ofInstant(
						date.toInstant(),
						EST
						).minusDays(STOCK_DAYS).atZone(EST).toInstant()
				);
		ZonedDateTime start = ZonedDateTime.of(prevDate.getYear()+1900, prevDate.getMonth()+1, prevDate.getDate(), prevDate.getHours(), prevDate.getMinutes(), 0, 0, EST);
		ZonedDateTime end = ZonedDateTime.of(date.getYear()+1900, date.getMonth()+1, date.getDate(), date.getHours(), date.getMinutes(), 0, 0, EST);
		try {
			StockBarsResponse barsResponse = alpaca.stockMarketData().getBars(
					symbol,
					start,
					end,
					STOCK_DAYS*24, null,
					1,
					BarTimePeriod.HOUR,
					BarAdjustment.ALL,
					BarFeed.IEX);
			System.out.println("Recieved Data for "+barsResponse.getSymbol());
			return barsResponse;
		} catch (AlpacaClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Gets the Watchlist for the app
	 * @return the Watchlist associated with the app, or a newly created list
	 */
	private synchronized Watchlist getWatchlist() {
		try {
			if(watchlist != null) {
				return alpaca.watchlist().get(watchlist.getId());
			}
			List<Watchlist> ret = alpaca.watchlist().get();
			for(Watchlist w : ret) {
				if(w.getName().equals("xmb.alpaca.app")) {
					return w;
				}
			}
			return alpaca.watchlist().create("xmb.alpaca.app");
		} catch (AlpacaClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Removes the selected tag from the app watchlist
	 * @param tag the ticker to remove
	 */
	public void removeFromWatchlist(String tag) {
		if(watchlist == null) {
			return;
		}
		try {
			alpaca.watchlist().removeSymbol(watchlist.getId(), tag);
		} catch (AlpacaClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Adds the selected tag to the app watchlist
	 * @param tag the ticker to add
	 */
	public void addToWatchlist(String tag) {
		if(watchlist == null) {
			return;
		}
		try {
			alpaca.watchlist().addAsset(watchlist.getId(), tag);
		} catch (AlpacaClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Gets all the data associated with the given ticker
	 * @param key the ticker to search for
	 * @return the AppData[] associated with the ticker
	 */
	public AppData[] getData(String key) {
		return stockData.get(key);
	}
	
	/**
	 * Gets the currently selected ticker's data
	 * @return AppData[] associated with the ticker
	 */
	public AppData[] getCurrentData() {
		return getData(getCurrentTicker());
	}
	
	/**
	 * Gets all the tickers in the watchlist for the app
	 * @return String[] of all the tickers
	 */
	public String[] getTickers() {
		return tickers;
	}
	
	/**
	 * Gets the ticker that the pointer is pointing to
	 * @return a String of the current ticker
	 */
	public String getCurrentTicker() {
		return tickers[pointer];
	}
	
	/**
	 * Changes the pointer by adding the given number to the current pointer
	 * @param p the amount to change the pointer
	 */
	public void movePointer(int p) {
		pointer += p;
		pointer %= tickers.length;
	}
	
}
