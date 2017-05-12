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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.encog.Encog;
import org.encog.ml.CalculateScore;
import org.encog.ml.data.market.MarketDataDescription;
import org.encog.ml.data.market.MarketDataType;
import org.encog.ml.data.market.MarketMLDataSet;
import org.encog.ml.data.market.loader.MarketLoader;
import org.encog.ml.data.market.loader.YahooFinanceLoader;
import org.encog.ml.data.temporal.TemporalPoint;
import org.encog.ml.data.temporal.TemporalDataDescription.Type;
import org.encog.ml.ea.train.EvolutionaryAlgorithm;
import org.encog.neural.neat.NEATNetwork;
import org.encog.neural.neat.NEATPopulation;
import org.encog.neural.neat.NEATUtil;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.TrainingSetScore;
import org.encog.persist.EncogDirectoryPersistence;
import org.encog.util.simple.EncogUtility;
import org.encog.util.time.TimeUnit;

/**
 * Build the training data for the prediction and store it in an Encog file for
 * later training.
 * 
 * @author jeff
 * 
 */
public class MarketBuildTraining {

	public static void generate(File dataDir) {
		
		final MarketLoader loader = LoaderFactory.getLoader(Config.QUOTE_LOADER);
		
		final MarketMLDataSet market = new MarketMLDataSet(loader,
				Config.INPUT_WINDOW, Config.PREDICT_WINDOW);
		
		if (Config.QUOTE_LOADER ==  QuoteLoaderEnum.MySQLDB10 || Config.QUOTE_LOADER ==  QuoteLoaderEnum.MySQLDBV  ){
			 market.setSequenceGrandularity(TimeUnit.MINUTES);
		}
		
	
		
		final MarketDataDescription desc = new MarketDataDescription(
				Config.TICKER, MarketDataType.ADJUSTED_CLOSE, true, true);
		market.addDescription(desc);

		if ( Config.USE_VOLUME ==  1 ){
			final MarketDataDescription descV = new MarketDataDescription(
					Config.TICKER, MarketDataType.VOLUME, Type.RAW, true, false);
			market.addDescription(descV);
		}
			
		
		
		Calendar end = new GregorianCalendar();// end today
		end.set(Calendar.HOUR_OF_DAY, 0);
		end.set(Calendar.MINUTE, 0);
		end.set(Calendar.SECOND, 0);
		end.set(Calendar.MILLISECOND, 0);
		
		
		Calendar begin = (Calendar) end.clone();// 
		
		// Gather training data for the last N years, stopping Y days short of today.
		// The Y days will be used to evaluate prediction.
		begin.add(Calendar.DATE, -Config.PREDICIT_OFFSET);
		end.add(Calendar.DATE, -Config.PREDICIT_OFFSET);
		begin.add(Calendar.YEAR, -Config.HISTORY_YEARS);
		
		market.load(begin.getTime(), end.getTime());
		market.generate();

		List<TemporalPoint> points = market.getPoints();
		System.out.println( "Points = " + points.size() + " Market Size = " + market.getInputSize());
		
		
		
		EncogUtility.saveEGB(new File(dataDir, Config.TRAINING_FILE), market);

		// create a network
		final BasicNetwork network = EncogUtility.simpleFeedForward(
				market.getInputSize(), 
				Config.HIDDEN1_COUNT, 
				Config.HIDDEN2_COUNT, 
				market.getIdealSize(), 
				true);	

		// save the network and the training
		EncogDirectoryPersistence.saveObject(new File(dataDir, Config.NETWORK_FILE), network);
	}
	
	public static void fetch(File dataDir) {
		final MarketLoader loader = LoaderFactory.getLoader(Config.QUOTE_LOADER);

		//final MarketLoader loader = new MySqlLoader();
		final MarketMLDataSet market = new MarketMLDataSet(loader,
				Config.INPUT_WINDOW, Config.PREDICT_WINDOW);
		

		final MarketDataDescription desc = new MarketDataDescription(
				Config.TICKER, MarketDataType.ADJUSTED_CLOSE, true, true);
		market.addDescription(desc);
		
		if (Config.USE_VOLUME == 1){
			final MarketDataDescription descV = new MarketDataDescription(
					Config.TICKER, MarketDataType.VOLUME, Type.RAW, true, false);
			market.addDescription(descV);
		}		
				
		
		Calendar end = new GregorianCalendar();// end today
		end.set(Calendar.HOUR_OF_DAY, 0);
		end.set(Calendar.MINUTE, 0);
		end.set(Calendar.SECOND, 0);
		end.set(Calendar.MILLISECOND, 0);
		
		Calendar begin = (Calendar) end.clone();// 
		
		// Gather training data for the last N years, stopping Y days short of today.
		// The Y days will be used to evaluate prediction.
		begin.add(Calendar.DATE, -Config.PREDICIT_OFFSET);
		end.add(Calendar.DATE, -Config.PREDICIT_OFFSET);
		begin.add(Calendar.YEAR, -Config.HISTORY_YEARS);
		
		market.load(begin.getTime(), end.getTime());
		market.generate();

		List<TemporalPoint> points = market.getPoints();
		System.out.println( "Points = " + points.size() + " Input Size = " + market.getInputSize());
		
		
		EncogUtility.saveEGB(new File(dataDir, Config.TRAINING_FILE), market);

	}
	
	//Incomplete -- Needs work
	public static void generateNeatX(File dataDir) {
		
		final MarketLoader loader = LoaderFactory.getLoader(Config.QUOTE_LOADER);
		
		final MarketMLDataSet market = new MarketMLDataSet(loader,
				Config.INPUT_WINDOW, Config.PREDICT_WINDOW);
		
		if (Config.QUOTE_LOADER ==  QuoteLoaderEnum.MySQLDB10){
			 market.setSequenceGrandularity(TimeUnit.MINUTES);
		}
		

		
		final MarketDataDescription desc = new MarketDataDescription(
				Config.TICKER, MarketDataType.ADJUSTED_CLOSE, true, true);
		
		market.addDescription(desc);
		
		if ( Config.USE_VOLUME ==  1  ){
			final MarketDataDescription descV = new MarketDataDescription(
					Config.TICKER, MarketDataType.VOLUME, Type.RAW, true, false);
			market.addDescription(descV);
		}	
		
	
		
		
		Calendar end = new GregorianCalendar();// end today
		end.set(Calendar.HOUR_OF_DAY, 0);
		end.set(Calendar.MINUTE, 0);
		end.set(Calendar.SECOND, 0);
		end.set(Calendar.MILLISECOND, 0);
		
		
		Calendar begin = (Calendar) end.clone();// 
		
		// Gather training data for the last N years, stopping Y days short of today.
		// The Y days will be used to evaluate prediction.
		begin.add(Calendar.DATE, -Config.PREDICIT_OFFSET);
		end.add(Calendar.DATE, -Config.PREDICIT_OFFSET);
		begin.add(Calendar.YEAR, -Config.HISTORY_YEARS);
		
		market.load(begin.getTime(), end.getTime());
		market.generate();

		List<TemporalPoint> points = market.getPoints();
		System.out.println( "Points = " + points.size() + " Market Size = " + market.getInputSize());
		
		
		
		EncogUtility.saveEGB(new File(dataDir, Config.TRAINING_FILE), market);

		// create a network
//		final BasicNetwork network = EncogUtility.simpleFeedForward(
//				market.getInputSize(), 
//				Config.HIDDEN1_COUNT, 
//				Config.HIDDEN2_COUNT, 
//				market.getIdealSize(), 
//				true);	

		// save the network and the training
		NEATPopulation pop = new NEATPopulation(5,1,10000);
		pop.setInitialConnectionDensity(1.0);// not required, but speeds training
		pop.reset();

		CalculateScore score = new TrainingSetScore(market);
		// train the neural network

 
		final EvolutionaryAlgorithm train = NEATUtil.constructNEATTrainer(pop,score);
		
		do {
			train.iteration();
			System.out.println("Epoch #" + train.getIteration() + " Error:" + train.getError()+ ", Species:" + pop.getSpecies().size());
		} while(train.getError() > 0.000131);

		NEATNetwork network = (NEATNetwork)train.getCODEC().decode(train.getBestGenome());

		// test the neural network
		System.out.println("Neural Network Results:");
		EncogUtility.evaluate(network, market);
		
		Encog.getInstance().shutdown();		
		
		
	//	EncogDirectoryPersistence.saveObject(new File(dataDir, Config.NETWORK_FILE), network);
	}

	
}
