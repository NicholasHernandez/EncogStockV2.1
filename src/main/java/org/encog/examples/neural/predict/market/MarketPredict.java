/*
 * Encog(tm) Java Examples v3.3
 * http://www.heatonresearch.com/encog/
 * https://github.com/encog/encog-java-examples
 *
 * Copyright 2008-2014 Heaton Research, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *   
 * For more information on Heaton Research copyrights, licenses 
 * and trademarks visit:
 * http://www.heatonresearch.com/copyright
 */
package org.encog.examples.neural.predict.market;

import java.io.File;

import org.encog.Encog;
import org.encog.ml.data.market.TickerSymbol;



/**
 * Use the saved market neural network, and now attempt to predict for today, and the
 * last 60 days and see what the results are.
 */
public class MarketPredict {
		
	public static void processEnv(String[] args){
		//Enviornment vars
		
		String val = System.getenv("QUOTE_LOADER");
		
		if (val != null){
			int pred = Integer.parseInt(val);
			switch (pred){
			case 0:
				Config.QUOTE_LOADER = QuoteLoaderEnum.YahooFinance;
				break;
			case 1:
				Config.QUOTE_LOADER = QuoteLoaderEnum.QuoteMedia;					
				break;
			case 2:
				Config.QUOTE_LOADER = QuoteLoaderEnum.MySQLDB;					
				break;
			case 3:
				Config.QUOTE_LOADER = QuoteLoaderEnum.MySQLDB10;					
				break;
			case 4:
				Config.QUOTE_LOADER = QuoteLoaderEnum.MySQLDBV;					
				break;	
			}
			
		}
		
		val = System.getenv("SAVE_ALL_PREDICTIONS");
		if (val != null){
			Config.SAVE_ALL_PREDICTIONS = Integer.parseInt(val);
		}
		val = System.getenv("INPUT_WINDOW");
		if (val != null){
			Config.INPUT_WINDOW = Integer.parseInt(val);
		}			
		val = System.getenv("HIDDEN1_COUNT");
		if (val != null){
			Config.HIDDEN1_COUNT = Integer.parseInt(val);
		}
		val = System.getenv("HIDDEN2_COUNT");
		if (val != null){
			Config.HIDDEN2_COUNT = Integer.parseInt(val);
		}
		val = System.getenv("HISTORY_YEARS");
		if (val != null){
			Config.HISTORY_YEARS = Integer.parseInt(val);
		}
		val = System.getenv("PREDICT_OFFSET");
		if (val != null){
			Config.PREDICIT_OFFSET = Integer.parseInt(val);
		}		
		val = System.getenv("TRAINING_MINUTES");
		if (val != null){
			Config.TRAINING_MINUTES = Integer.parseInt(val);
		}
		val = System.getenv("USE_VOLUME");
		if (val != null){
			Config.USE_VOLUME = Integer.parseInt(val);
		}
		val = System.getenv("MINUTES_IN_INTERVAL");
		if (val != null){
			Config.MINUTES_IN_INTERVAL = Integer.parseInt(val);
		}
		
		
		if (args.length >= 3){
			Config.TICKER = new TickerSymbol( args[2].toUpperCase());
		}
				

		Config.TRAINING_FILE = Config.TICKER.getSymbol() + Config.TRAINING_FILE;
		Config.NETWORK_FILE = Config.TICKER.getSymbol() + Config.NETWORK_FILE;
		//Symbol

					
		System.out.println("Ticker Symbol: " + Config.TICKER.getSymbol());
		//Version
		if (args.length >= 4){
			Config.TRAINING_FILE = Config.TRAINING_FILE + "-" + args[3] ;
			Config.NETWORK_FILE = Config.NETWORK_FILE + "-" + args[3];
		}
		
		//logprefix
		if (args.length >= 5){
			Config.LOG_FILE = args[4] + Config.LOG_FILE ;
		}
		
		if (args.length >= 6){
			Config.PREDICIT_OFFSET = Integer.parseInt(args[5]);
		}
	
		
	}
	
	public static void main(String[] args)
	{		
		if( args.length < 3 ) {
			System.out.println("MarketPredict [data dir] [generate/train/incremental/evaluate/evaluate_db] symbol [version] [logprefix] [predict offset]");
		}
		else
		{
			processEnv(args);
			
			//function
			File dataDir = new File(args[0]);
			
			if( args[1].equalsIgnoreCase("fetch") ) {
				MarketBuildTraining.fetch(dataDir);
			} else if( args[1].equalsIgnoreCase("generate") ) {	
				MarketBuildTraining.generate(dataDir);			
			} 
			
			else if( args[1].equalsIgnoreCase("generateNeat") ) {	
				MarketBuildTraining.generateNeatX(dataDir);			
			} 
		
			else if( args[1].equalsIgnoreCase("train") ) {
				MarketTrain.train(dataDir);
				if (Config.QUOTE_LOADER == QuoteLoaderEnum.MySQLDB10 || Config.QUOTE_LOADER ==  QuoteLoaderEnum.MySQLDBV ){
					MarketEvaluate.evaluateDay(dataDir, Config.PREDICIT_OFFSET);					
				} else {
					MarketEvaluate.evaluate(dataDir, Config.PREDICIT_OFFSET);		
				}
			} 
			else if( args[1].equalsIgnoreCase("evaluate") ) {
				if (Config.QUOTE_LOADER == QuoteLoaderEnum.MySQLDB10 || Config.QUOTE_LOADER ==  QuoteLoaderEnum.MySQLDBV){
					MarketEvaluate.evaluateDay(dataDir, Config.PREDICIT_OFFSET);					
				} else {
					MarketEvaluate.evaluate(dataDir, Config.PREDICIT_OFFSET);		
				}
			}
			
			else if( args[1].equalsIgnoreCase("evaluate_db") ) {
				Config.SAVE_ALL_PREDICTIONS = 2;
				if (Config.QUOTE_LOADER == QuoteLoaderEnum.MySQLDB10 || Config.QUOTE_LOADER ==  QuoteLoaderEnum.MySQLDBV){
					MarketEvaluate.evaluateDay(dataDir, Config.PREDICIT_OFFSET);					
				} else {
					MarketEvaluate.evaluate(dataDir, Config.PREDICIT_OFFSET);		
				}
				
			} else if ( args[1].equalsIgnoreCase("test") ) {
				for ( int i=Config.INPUT_WINDOW+1; i < Config.PREDICIT_OFFSET; i++ ){
					if (Config.QUOTE_LOADER == QuoteLoaderEnum.MySQLDB10 || Config.QUOTE_LOADER ==  QuoteLoaderEnum.MySQLDBV){
						MarketEvaluate.evaluateDay(dataDir, Config.PREDICIT_OFFSET);					
					} else {
						MarketEvaluate.evaluate(dataDir, Config.PREDICIT_OFFSET);		
					}
				}
			} else if( args[1].equalsIgnoreCase("prune") ) {
				MarketPrune.incremental(dataDir);
				if (Config.QUOTE_LOADER == QuoteLoaderEnum.MySQLDB10 || Config.QUOTE_LOADER ==  QuoteLoaderEnum.MySQLDBV){
					MarketEvaluate.evaluateDay(dataDir, Config.PREDICIT_OFFSET);					
				} else {
					MarketEvaluate.evaluate(dataDir, Config.PREDICIT_OFFSET);		
				}	
			} else if( args[1].equalsIgnoreCase("search") ) {
				MarketPrune.search(dataDir);
				if (Config.QUOTE_LOADER == QuoteLoaderEnum.MySQLDB10 || Config.QUOTE_LOADER ==  QuoteLoaderEnum.MySQLDBV){
					MarketEvaluate.evaluateDay(dataDir, Config.PREDICIT_OFFSET);					
				} else {
					MarketEvaluate.evaluate(dataDir, Config.PREDICIT_OFFSET);		
				}				
			} else if (args[1].equalsIgnoreCase("all") ) {
				MarketBuildTraining.generate(dataDir);
				MarketTrain.train(dataDir);
				if (Config.QUOTE_LOADER == QuoteLoaderEnum.MySQLDB10  || Config.QUOTE_LOADER ==  QuoteLoaderEnum.MySQLDBV){
					MarketEvaluate.evaluateDay(dataDir, Config.PREDICIT_OFFSET);					
				} else {
					MarketEvaluate.evaluate(dataDir, Config.PREDICIT_OFFSET);		
				}

				//MarketPrune.incremental(dataDir);
				//MarketEvaluate.evaluate(dataDir);
				
				//MarketTrain.train(dataDir);
				//MarketEvaluate.evaluate(dataDir);
				
			}  else {
				System.out.println("MarketPredict [data dir] [generate/train/incremental/evaluate/evaluate_db] symbol [version] [logprefix] [predict offset]");
			}
			
			Encog.getInstance().shutdown();
		}
	}
	
}
