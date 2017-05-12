package org.encog.examples.neural.predict.market;

import org.encog.ml.data.market.loader.MarketLoader;
import org.encog.ml.data.market.loader.YahooFinanceLoader;



public class LoaderFactory {
	
	public static MarketLoader getLoader(QuoteLoaderEnum loaderType ){
		
		MarketLoader marketLoader = null;
		
		switch (loaderType){
		case YahooFinance:
			marketLoader = new YahooFinanceLoader();
			break;
		
		case QuoteMedia:
			marketLoader = new QuoteMediaLoader();
			break;
			
		case MySQLDB:
			marketLoader = new MySqlLoader();
			break;
		case MySQLDB10:
			marketLoader = new MySqlLoader10();
			break;
		case MySQLDBV:
			marketLoader = new MySqlLoaderV();
			break;	
		}
		
		
		return marketLoader;
	}
	
}
