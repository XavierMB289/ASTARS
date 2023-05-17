package ASTARS.backend;

import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.StockBar;

public class AppData {
	
	private double close;
	private double ema12;
	private double ema26;
	private double macd;
	
	/**
	 * Setup the AppData Normally
	 * @param prev The previous entry for AppData, cannot be null
	 * @param bar The current StockBar Object
	 */
	public AppData(AppData prev, StockBar bar) {
		close = bar.getClose();
		ema12 = (close * (2/13)) + (prev.getEMA12()*(1-(2/13)));
		ema26 = (close * (2/27)) + (prev.getEMA26()*(1-(2/27)));
		macd = ema12 - ema26;
	}
	
	/**
	 * Setup the AppData class prior to getting the actual ema26
	 * @param prev The previous entry for AppData, or null
	 * @param count The current cycle count
	 * @param bar The current StockBar Object
	 */
	public AppData(AppData prev, int count, StockBar bar) {
		close = bar.getClose();
		if(count < 13) {
			if(count != 1) { //Every one 2-12
				ema12 = ((prev.getEMA12()*count) + close)/(count+1);
			}else { //The First One ONLY
				ema12 = close;
			}
		}else { // 13+
			ema12 = (close * (2/13)) + (prev.getEMA12()*(1-(2/13)));
		}
		if(count == 1) { //First One ONLY
			ema26 = close;
		}else { //Others
			ema26 = ((prev.getEMA26()*count) + close)/(count+1);
		}
		macd = ema12 - ema26;
	}
	
	public double getClose() {
		return close;
	}
	
	public double getEMA12() {
		return ema12;
	}
	
	public double getEMA26() {
		return ema26;
	}
	
	public double getMACD() {
		return macd;
	}
	
}
