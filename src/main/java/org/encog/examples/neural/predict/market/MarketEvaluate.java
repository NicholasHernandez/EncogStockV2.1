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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.sql.*;

import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.market.MarketDataDescription;
import org.encog.ml.data.market.MarketDataType;
import org.encog.ml.data.market.MarketMLDataSet;
import org.encog.ml.data.market.loader.MarketLoader;
import org.encog.ml.data.temporal.TemporalDataDescription;
import org.encog.ml.data.temporal.TemporalDataDescription.Type;
import org.encog.ml.data.temporal.TemporalPoint;
import org.encog.neural.networks.BasicNetwork;
import org.encog.persist.EncogDirectoryPersistence;
import org.encog.util.time.TimeUnit;

public class MarketEvaluate {
/*	private class ticker_stats {
		int count=0;
		double sum=0.0;
		double thr=0.0;
		int correct=0;
	}*/
	
	enum Direction {
		up, down
	};

	public static String determineDirection(double d) {
		if (d < 0)
			return "-";
		else
			return "+";
	}

	public static MarketMLDataSet grabData(final int predictOffset) {
		//MarketLoader loader = new MySqlLoader();
		//MarketLoader loader = new QuoteMediaLoader();
		final MarketLoader loader = LoaderFactory.getLoader(Config.QUOTE_LOADER);

		MarketMLDataSet result = new MarketMLDataSet(loader,
				Config.INPUT_WINDOW, Config.PREDICT_WINDOW);
		
		if (Config.QUOTE_LOADER ==  QuoteLoaderEnum.MySQLDB10 || Config.QUOTE_LOADER ==  QuoteLoaderEnum.MySQLDBV ){
			result.setSequenceGrandularity(TimeUnit.MINUTES);
		}
		
		MarketDataDescription desc = new MarketDataDescription(Config.TICKER,
				MarketDataType.ADJUSTED_CLOSE, true, true);
		result.addDescription(desc);
		
		if ( Config.USE_VOLUME ==  1  ){
			final MarketDataDescription descV = new MarketDataDescription(
					Config.TICKER, MarketDataType.VOLUME, Type.RAW, true, false);
			result.addDescription(descV);
		}	
		
		SimpleDateFormat fmt1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar end = Calendar.getInstance();;// end today
//		System.out.println("End: " + fmt1.format(end.getTime()));
		end.set(Calendar.HOUR_OF_DAY, 0);
		end.set(Calendar.MINUTE, 0);
		end.set(Calendar.SECOND, 0);
		end.set(Calendar.MILLISECOND, 0);
		System.out.println("End: " + fmt1.format(end.getTime()));
		
		Calendar begin = (Calendar) end.clone();// begin Y days ago
		begin.add(Calendar.DATE, -predictOffset);
		
		result.load(begin.getTime(), end.getTime());
		result.generate();

		return result;

	}
	
	private static double getDataPercentChange(final TemporalDataDescription desc, final MarketMLDataSet data,
			final int index) {
		if (index == 0) {
			return 0.0;
		}
		final TemporalPoint point = data.getPoints().get(index);
		final TemporalPoint previousPoint = data.getPoints().get(index - 1);
		final double currentValue = point.getData(desc.getIndex());
		final double previousValue = previousPoint.getData(desc.getIndex());
		return (currentValue - previousValue) / previousValue;
	}

	
	private static double predictNext(final MarketMLDataSet data, final BasicNetwork network )
	{
		//Prediction for next interval 
		//System.out.println("Record Count: " + data.getRecordCount());
		MLData input = data.generateInputNeuralData((int)data.getRecordCount()+1);
	
		MLData predictData = network.compute(input);

		double predict = predictData.getData(0);
		

		return predict;
	}
	
	private static double predictTomorrow(final MarketMLDataSet data, final BasicNetwork network )
	{
		//Prediction for tomorrow 
		//System.out.println("Record Count: " + data.getRecordCount());
		MLDataPair lastDay = data.get((int)data.getRecordCount()-1);

		MLData input = lastDay.getInput();
					
		//Shift down
		for(int i=0; i < input.size()-1; i++){
			input.setData(i, input.getData(i+1));
		}
	
		
		for (final TemporalDataDescription desc : data.getDescriptions()) {
			if (desc.isInput()) {
				input.setData(input.size()-1, getDataPercentChange(desc, data, data.calculatePointsInRange()-1));
			}
		}
		
//		for(double d : input.getData()){
//			System.out.println(pctFormat.format(d*100));
//		}
		
		MLData predictData = network.compute(input);

		double predict = predictData.getData(0);
		

		return predict;
	}
	
	public static void save_prediction(String stock_symbol, Calendar pdate, double pct_pred, double price_pred, double rmse, double one_pct_ret, double one_pct_prob, 
			double two_pct_ret, double two_pct_prob, double five_pct_ret, double five_pct_prob, String nn_config, String path)
	{
		try {

	        String url = Config.getSQLConnectionString();
	        String user = "root";
	        String password = "a0viper";
	        Connection con = DriverManager.getConnection(url, user, password);
                      
            String query = "INSERT INTO mydb.`Predict`" +
                    "( entry_date, stock_symbol, pdate, ptime, pct_pred, price_pred, rmse, one_pct_ret, one_pct_prob, two_pct_ret, two_pct_prob," +
            		" five_pct_ret, five_pct_prob, nn_config, path ) " +
                    "VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
            
            // create a cql date object so we can use it in our INSERT statement
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeZone(TimeZone.getTimeZone("US/Eastern"));
            //java.sql.Date entryDate = new java.sql.Date(calendar.getTime().getTime());
            java.sql.Timestamp entryDate = new java.sql.Timestamp(new java.util.Date().getTime());
            
            java.sql.Date pdate_sql = new java.sql.Date(pdate.getTimeInMillis());
            java.sql.Time ptime_sql = new java.sql.Time(pdate.getTimeInMillis());
            
            // create the mysql insert preparedstatement
            PreparedStatement preparedStmt = con.prepareStatement(query);
            int i=1;
            preparedStmt.setTimestamp(i++, entryDate);
            preparedStmt.setString(i++, stock_symbol);
            preparedStmt.setDate(i++, pdate_sql);
            preparedStmt.setTime(i++, ptime_sql);
            preparedStmt.setFloat(i++, (float)pct_pred);
            preparedStmt.setFloat(i++, (float)price_pred);
            preparedStmt.setFloat(i++, (float)rmse);
            preparedStmt.setFloat(i++, (float)one_pct_ret);
            preparedStmt.setFloat(i++, (float)one_pct_prob);
            preparedStmt.setFloat(i++, (float)two_pct_ret);
            preparedStmt.setFloat(i++, (float)two_pct_prob);
            preparedStmt.setFloat(i++, (float)five_pct_ret);
            preparedStmt.setFloat(i++, (float)five_pct_prob);
            preparedStmt.setString(i++, nn_config);
            preparedStmt.setString(i++, path);
     
            preparedStmt.execute();
            con.close();	
            		
	    } catch (SQLException se){
	    	se.printStackTrace();
	    } catch(Exception e){
	    	e.printStackTrace();
	    } finally {
	
	    }
	}
	
	
	public static void evaluate(File dataDir, int predictOffset) {

		File file = new File(dataDir, Config.NETWORK_FILE);

		if (!file.exists()) {
			System.out.println("Can't read file: " + file.getAbsolutePath());
			return;
		}

		String nn_config = "ME: " + Config.INPUT_WINDOW + 
        		"/" + Config.HIDDEN1_COUNT + "/" +
        		Config.HIDDEN2_COUNT + "/" + Config.PREDICT_WINDOW + " Y:" + Config.HISTORY_YEARS + " PO:" +
        		predictOffset;
		
		BasicNetwork network = (BasicNetwork)EncogDirectoryPersistence.loadObject(file);	

		MarketMLDataSet data = grabData(predictOffset);

		DecimalFormat format = new DecimalFormat("###0.00");
		DecimalFormat priceFormat = new DecimalFormat("###0.00");
		DecimalFormat pctFmt = new DecimalFormat("###0.00");
		DecimalFormat probFmt = new DecimalFormat("###0");
		
		int count = 0;
		int count0 = 0;
		int count1 = 0;
		int count2 = 0;

		int correct = 0;
		int correct0 = 0;
		int correct1 = 0;
		int correct2 = 0;

		double rmse = 0.0;
		
		Calendar end = new GregorianCalendar();// end today		end.set
		end.set(Calendar.HOUR_OF_DAY, 12);
		end.set(Calendar.MINUTE, 30);
		end.set(Calendar.SECOND, 0);
		end.set(Calendar.MILLISECOND, 0);
		
		Calendar currentDay = (Calendar) end.clone();// begin Y days ago
		currentDay.add(Calendar.DATE, -predictOffset);		
		
		System.out.println("Start Date: " +  currentDay.getTime());
		
		List<TemporalPoint> points = data.getPoints();
		double rms = 0.0;
		double sum0 = 0.0;
		double sum1 = 0.0;
		double sum2 = 0.0;
		final double thr0 = 0.00;
		final double thr1 = 0.01;
		final double thr2 = 0.02;
		final double maxLoss = -0.05;
		
		for (MLDataPair pair : data) {			
			MLData input = pair.getInput();
			MLData actualData = pair.getIdeal();
			MLData predictData = network.compute(input);

			double actual = actualData.getData(0);
			double predict = predictData.getData(0);
			double diff = Math.abs(predict - actual);

			String actualDirection = determineDirection(actual);
			String predictDirection = determineDirection(predict);

			if (actualDirection == predictDirection)
				correct++;
			
			count++;		
			double p = points.get(count+Config.INPUT_WINDOW).getData(0);

			if (predict > thr2){
				count2++;count1++;count0++;
				
				sum2 += Math.max(actual, maxLoss);
				sum1 += Math.max(actual, maxLoss);
				sum0 += Math.max(actual, maxLoss);

				if (actualDirection == predictDirection){
					correct2++;	correct1++; correct0++;
				}
					
			}	else if (predict > thr1){
				count1++; count0++;
				sum1 += Math.max(actual, maxLoss);
				sum0 += Math.max(actual, maxLoss);					

				if (actualDirection == predictDirection){
					correct1++; correct0++;
				}
									
			} else if (predict > thr0){
				count0++;
				sum0 += Math.max(actual, maxLoss);
				if (actualDirection == predictDirection){
					correct0++;
				}
			}
			
			rms += (diff)*(diff);

			Calendar qDay = (Calendar) currentDay.clone();// begin Y days ago	
			qDay.add(Calendar.DATE, points.get(count+Config.INPUT_WINDOW).getSequence());
		
			double one_pct_prob = (count0 == 0) ? 0.0 : 100.0*(double)correct0/(double)count0;
			double two_pct_prob = (count1 == 0) ? 0.0 : 100.0*(double)correct1/(double)count1;
			double five_pct_prob = (count2 == 0) ? 0.0 : 100.0*(double)correct2/(double)count2;
			
			System.out.println("Day " + ((count < 10) ? " ": "") + count + " sq: " +points.get(count+Config.INPUT_WINDOW).getSequence() +  " " + qDay.getTime() + " : Q " + priceFormat.format(p)  +" A:"
					+ priceFormat.format(actual*p) + "\t" + priceFormat.format(actual*100)+ "% \t(" + actualDirection + predictDirection + ")"
					+ ",\tP: " + priceFormat.format(predict*100) + "%"
					+ ", D:" + priceFormat.format(diff*100) + "% " + "\t"

				+ pctFmt.format(thr0*100) + "%: " + priceFormat.format(sum0*100) + "%\t" 
				+ probFmt.format( one_pct_prob) + "%,\t"
				+ pctFmt.format(thr1*100) + "%: " + priceFormat.format(sum1*100) + "%\t" 
				+ probFmt.format( two_pct_prob) + "%\t"					
				+ pctFmt.format(thr2*100) + "%: " + priceFormat.format(sum2*100) + "%\t"
				+ probFmt.format( five_pct_prob)+"%");				

					
			rmse = Math.sqrt(rms/count);
			double pred_price = p/(1+actual)*(predict+1);
		
			if (Config.SAVE_ALL_PREDICTIONS >= 2){
				save_prediction( Config.TICKER.getSymbol(), qDay, predict, pred_price,  rmse,  sum0,  one_pct_prob/100.0, 
						sum1,  two_pct_prob/100.0, sum2, five_pct_prob/100.0, nn_config, dataDir.getAbsolutePath() + "/" + Config.NETWORK_FILE);	
			}

		}
		
		int pcount = (int)data.getRecordCount()+Config.INPUT_WINDOW;
		double last_price = points.get(pcount).getData(0);
		
		//Prediction for tomorrow 
		Calendar qDay = (Calendar) currentDay.clone();// begin Y days ago
		qDay.add(Calendar.DATE, points.get(pcount).getSequence());		
		System.out.println("Last Quote: " + priceFormat.format(last_price) + " " + qDay.getTime());
		
		qDay.add(Calendar.DATE, 1);
	
		double pct_tomorrow = predictNext(data, network);
		System.out.println("Predict Tomorrow: " + qDay.getTime() + ' ' + format.format(pct_tomorrow*100) +"%");
				
		
		double percent = (double) correct / (double) count;
		System.out.println("Direction correct:" + correct + "/" + count);
		System.out.println("Directional Accuracy:"
				+ format.format(percent * 100) + "%");
		
		rmse = Math.sqrt(rms/count);
		double one_pct_prob = (count0 == 0) ? 0.0 : 100.0*(double)correct0/(double)count0;
		double two_pct_prob = (count1 == 0) ? 0.0 : 100.0*(double)correct1/(double)count1;
		double five_pct_prob = (count2 == 0) ? 0.0 : 100.0*(double)correct2/(double)count2;
		
		System.out.println("RMSE = " + format.format(rmse));
		System.out.println("Exceed : " + correct0 +", " + correct1 + ", " + correct2);
		System.out.println("Price Change: " + format.format(((last_price/points.get(Config.INPUT_WINDOW+1).getData(0)) - 1.0)*100.0 )    + '%');

		
		
		System.out.println("S: " + Config.TICKER.getSymbol() + ", " + priceFormat.format(pct_tomorrow*100) + "\t"
				+ pctFmt.format(thr0*100) + "%: " + priceFormat.format(sum0*100) + "%\t" 
				+ probFmt.format( one_pct_prob) + "%,\t"
				+ pctFmt.format(thr1*100) + "%: " + priceFormat.format(sum1*100) + "%\t" 
				+ probFmt.format( two_pct_prob) + "%\t"					
				+ pctFmt.format(thr2*100) + "%: " + priceFormat.format(sum2*100) + "%\t"
				+ probFmt.format( five_pct_prob)+"%");
		
		File log_file = new File(dataDir,Config.LOG_FILE);

		if (!log_file.exists()) {
			try {
				log_file.createNewFile();
			} catch (IOException e) {

				e.printStackTrace();
			}
		}

		if (Config.SAVE_ALL_PREDICTIONS >= 1){
			save_prediction( Config.TICKER.getSymbol(), qDay, pct_tomorrow, last_price*(1.0 +pct_tomorrow),  rmse,  sum0,  one_pct_prob/100.0, 
					sum1,  two_pct_prob/100.0, sum2, five_pct_prob/100.0, nn_config, dataDir.getAbsolutePath() + "/" + Config.NETWORK_FILE);	
		}
//		FileWriter logfileWriter = null;
//		try {
//			logfileWriter = new FileWriter(log_file.getName(), true);
//		} catch (IOException e) {
//
//			e.printStackTrace();
//		}
		
		PrintWriter logWriter = null;
		try {
			logWriter = new PrintWriter(new FileOutputStream(log_file, true) );
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		}

		java.util.Date date= new java.util.Date();

		one_pct_prob = (count0 == 0) ? 0.0 : 100.0*(double)correct0/(double)count0;
		two_pct_prob = (count1 == 0) ? 0.0 : 100.0*(double)correct1/(double)count1;
	    five_pct_prob = (count2 == 0) ? 0.0 : 100.0*(double)correct2/(double)count2;
		
        logWriter.println( new Timestamp(date.getTime()) + "\t" + Config.TICKER.getSymbol() + " ME: " + Config.INPUT_WINDOW + 
        		"/" + Config.HIDDEN1_COUNT + "/" +
        		Config.HIDDEN2_COUNT + "/" + Config.PREDICT_WINDOW + " Y:" + Config.HISTORY_YEARS + " PO:" +
        		predictOffset + " A: " + priceFormat.format(percent * 100) + "% R2 = " + format.format(rmse) +
        		" FC: " + priceFormat.format(pct_tomorrow*100.0) + ",\t"
				+ pctFmt.format(thr0*100.0) + "%: " + priceFormat.format(sum0*100) + "% " 
				+ probFmt.format( one_pct_prob)+"%, "
				+ pctFmt.format(thr1*100.0) + "%: " + priceFormat.format(sum1*100) + "% " 
				+ probFmt.format( two_pct_prob)+"%, "					
				+ pctFmt.format(thr2*100.0) + "%: " + priceFormat.format(sum2*100) + "% "
				+ probFmt.format(five_pct_prob)+"%");
       	
 
        logWriter.flush();
        
	}
	
	public static void evaluateDay(File dataDir, int predictOffset) {

		File file = new File(dataDir, Config.NETWORK_FILE);

		if (!file.exists()) {
			System.out.println("Can't read file: " + file.getAbsolutePath());
			return;
		}

		String nn_config = "MIN" + ((Config.USE_VOLUME == 1)?"V:" : ":")  + Config.INPUT_WINDOW + 
        		"/" + Config.HIDDEN1_COUNT + "/" +
        		Config.HIDDEN2_COUNT + "/" + Config.PREDICT_WINDOW + " Y:" + Config.HISTORY_YEARS + " PO:" +
        		predictOffset;
		
		
		BasicNetwork network = (BasicNetwork)EncogDirectoryPersistence.loadObject(file);	

		MarketMLDataSet data = grabData(predictOffset);
		System.out.println("Data Starting Date: " + data.getStartingPoint());
		
		DecimalFormat format = new DecimalFormat("###0.00");
		DecimalFormat priceFormat = new DecimalFormat("###0.00");
		DecimalFormat pctFmt = new DecimalFormat("###0");
		SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm zzz");
		dateFmt.setTimeZone(TimeZone.getTimeZone("US/Pacific"));
		Calendar predictionDate =  Calendar.getInstance();

		int count = 0;

		int count0 = 0;
		int count1 = 0;
		int count2 = 0;

		int correct = 0;
		int correct0 = 0;
		int correct1 = 0;
		int correct2 = 0;

		double rmse = 0.0;
		
		Calendar end = new GregorianCalendar();// end today		end.set
		end.set(Calendar.HOUR_OF_DAY, 0);
		end.set(Calendar.MINUTE, 0);
		end.set(Calendar.SECOND, 0);
		end.set(Calendar.MILLISECOND, 0);
		
		Calendar currentDay = (Calendar) end.clone();// begin Y days ago
		currentDay.add(Calendar.DATE, -predictOffset);		
		
		System.out.println("Start Date: " +  currentDay.getTime());
		
		List<TemporalPoint> points = data.getPoints();
		double rms = 0.0;
		double sum0 = 0.0;
		double sum1 = 0.0;
		double sum2 = 0.0;
		final double thr0 = 0.00;
		final double thr1 = 0.0034;
		final double thr2 = 0.005;
		final double maxLoss = -0.05;
		
		boolean holding=false;
		int transx = 0;
		
		for (MLDataPair pair : data) {

			
			MLData input = pair.getInput();
			MLData actualData = pair.getIdeal();
			MLData predictData = network.compute(input);

			double actual = actualData.getData(0);
			double predict = predictData.getData(0);
			
			double diff = Math.abs(predict - actual);

			String actualDirection = determineDirection(actual);
			String predictDirection = determineDirection(predict);

			if (actualDirection == predictDirection)
				correct++;
			
			
			count++;		
			double p = points.get(count+Config.INPUT_WINDOW).getData(0);
			
			if (predictDirection.contentEquals("+")){
				if (!holding){
					holding=true;
					transx++;
				}
			} else {
				if (holding){
					holding = false;
					transx++;
				}
			}
	
			
			
			if (predict > thr2){
				count2++;count1++;count0++;
				

				
				if (actualDirection == predictDirection){
					correct2++;	correct1++; correct0++;
				}
					
			}	else if (predict > thr1){
				
				count1++; count0++;
				
				
				sum1 += Math.max(actual, maxLoss);
				sum0 += Math.max(actual, maxLoss);					

				if (actualDirection == predictDirection){
					correct1++; correct0++;
				}
									
			} else if (predict > thr0){
				count0++;
				if (holding){
					sum0 += Math.max(actual, maxLoss);
				}				

				if (actualDirection == predictDirection){
					correct0++;
				}
			}
			
			rms += (diff)*(diff);
			
			Date qday =  new Date( (long)data.getStartingPoint().getTime() +(long) points.get(count+Config.INPUT_WINDOW).getSequence()*60*1000);
				
			//qDay.add(Calendar.DATE, points.get(count+Config.INPUT_WINDOW).getSequence());
		
			double one_pct_prob = (count0 == 0) ? 0.0 : 100.0*(double)correct0/(double)count0;
			double two_pct_prob = (count1 == 0) ? 0.0 : 100.0*(double)correct1/(double)count1;
			double five_pct_prob = (count2 == 0) ? 0.0 : 100.0*(double)correct2/(double)count2;
			
			double pred_price = p/(1+actual)*(predict+1);
						
			System.out.println("Count " + ((count < 10) ? " ": "") + count + " " + points.get(count+Config.INPUT_WINDOW).getSequence()  +  " " 
			+ dateFmt.format(qday) + " : Q " + priceFormat.format(p) + " " +priceFormat.format(pred_price) +" D:"
					+ priceFormat.format(actual*p) + " " + priceFormat.format(predict*p) 
					+ " \t" + priceFormat.format(actual*100)+ "% \t(" + actualDirection + predictDirection + ")"
					+ ",\tP: " + priceFormat.format(predict*100) + "%"
					+ ", D:" + priceFormat.format(diff*100) + "% " + "\t"

				+ format.format(thr0*100) + "%: " + priceFormat.format(sum0*100) + "%\t" 
				+ pctFmt.format( one_pct_prob) + "%,\t"
				+ format.format(thr1*100) + "%: " + priceFormat.format(sum1*100) + "%\t" 
				+ pctFmt.format( two_pct_prob) + "%\t"					
				+ format.format(thr2*100) + "%: " + priceFormat.format(sum2*100) + "%\t"
				+ pctFmt.format( five_pct_prob)+"%");				
					
			rmse = Math.sqrt(rms/count)*100.0;
			
			predictionDate.setTime(qday);		
			if (Config.SAVE_ALL_PREDICTIONS >= 2){
				save_prediction( Config.TICKER.getSymbol(), predictionDate, predict, pred_price,  rmse,  sum0,  one_pct_prob/100.0, 
						sum1,  two_pct_prob/100.0, sum2, five_pct_prob/100.0, nn_config, dataDir.getAbsolutePath() + "/" + Config.NETWORK_FILE);	
			}

		}
		
		int pcount = (int)data.getRecordCount()+Config.INPUT_WINDOW;
		double last_price = points.get(pcount).getData(0);
		
		Date qday =  new Date( (long)data.getStartingPoint().getTime() +(long) points.get(pcount).getSequence()*60*1000);
		
		predictionDate.setTime(qday);
		predictionDate.setTimeZone(TimeZone.getTimeZone("PDT"));
//		qDay.add(Calendar.DATE, points.get(pcount).getSequence()+1);
	
		System.out.println("Last Quote: " + priceFormat.format(last_price) + " " + dateFmt.format(qday));
		predictionDate.add(Calendar.MINUTE, 30);
		//System.out.println("H: " + predictionDate.get(Calendar.HOUR) + " M: " + predictionDate.get(Calendar.MINUTE) + " D:" + predictionDate.get(Calendar.DAY_OF_WEEK));
		int day_minutes = predictionDate.get(Calendar.HOUR) * 60 + predictionDate.get(Calendar.MINUTE);
		//System.out.println("H: " + predictionDate.get(Calendar.HOUR) + " MIN: " + predictionDate.get(Calendar.MINUTE));
		if (day_minutes >= ((9.5*60)-2) ){
			predictionDate.add(Calendar.HOUR, 17);
			if (predictionDate.get(Calendar.DAY_OF_WEEK) == 7){
				predictionDate.add(Calendar.DAY_OF_YEAR, 2);
			}
		}
		
		//Prediction for tomorrow 	
		double pct_next = predictNext(data, network);
		System.out.println("Predict Next: " + dateFmt.format(predictionDate.getTime()) + " " + format.format(pct_next*100) +"% P:" + format.format((1+pct_next)*last_price));
					
		double percent = (double) correct / (double) count;
		System.out.println("Direction correct:" + correct + "/" + count);
		System.out.println("Directional Accuracy:"+ format.format(percent * 100) + "%");
		
		rmse = Math.sqrt(rms/count)*100.0;
		double one_pct_prob = (count0 == 0) ? 0.0 : 100.0*(double)correct0/(double)count0;
		double two_pct_prob = (count1 == 0) ? 0.0 : 100.0*(double)correct1/(double)count1;
		double five_pct_prob = (count2 == 0) ? 0.0 : 100.0*(double)correct2/(double)count2;
		
		System.out.println("RMSE = " + format.format(rmse));
		System.out.println("Exceed : " + correct0 +", " + correct1 + ", " + correct2);
		double cost = Math.max(1.5, (10000/last_price)*0.005)*transx;
		System.out.println("Transx: " + transx + " Cost: " + priceFormat.format(cost ));
		
		System.out.println("Price Change: " + format.format(((last_price/points.get(Config.INPUT_WINDOW+1).getData(0)) - 1.0)*100.0 )    + '%');

		System.out.println("S: " + Config.TICKER.getSymbol() + ", " + priceFormat.format(pct_next*100) + "\t"
				+ format.format(thr0*100) + "%: " + priceFormat.format(sum0*100) + "%\t" 
				+ pctFmt.format( one_pct_prob) + "%,\t"
				+ format.format(thr1*100) + "%: " + priceFormat.format(sum1*100) + "%\t" 
				+ pctFmt.format( two_pct_prob) + "%\t"					
				+ format.format(thr2*100) + "%: " + priceFormat.format(sum2*100) + "%\t"
				+ pctFmt.format( five_pct_prob)+"% Cost: " + priceFormat.format(cost ));
		
		File log_file = new File(dataDir,Config.LOG_FILE);

		if (!log_file.exists()) {
			try {
				log_file.createNewFile();
			} catch (IOException e) {

				e.printStackTrace();
			}
		}

		if (Config.SAVE_ALL_PREDICTIONS >= 1){
			save_prediction( Config.TICKER.getSymbol(), predictionDate, pct_next, last_price*(1.0 +pct_next),  rmse,  sum0,  one_pct_prob/100.0, 
					sum1,  two_pct_prob/100.0, sum2, five_pct_prob/100.0, nn_config, dataDir.getAbsolutePath() + "/" + Config.NETWORK_FILE);	
		}

		PrintWriter logWriter = null;
		try {
			logWriter = new PrintWriter(new FileOutputStream(log_file, true) );
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		}

		java.util.Date date= new java.util.Date();

		one_pct_prob = (count0 == 0) ? 0.0 : 100.0*(double)correct0/(double)count0;
		two_pct_prob = (count1 == 0) ? 0.0 : 100.0*(double)correct1/(double)count1;
	    five_pct_prob = (count2 == 0) ? 0.0 : 100.0*(double)correct2/(double)count2;
		
        logWriter.println( new Timestamp(date.getTime()) + "\t" + Config.TICKER.getSymbol() + " ME: " + Config.INPUT_WINDOW + 
        		"/" + Config.HIDDEN1_COUNT + "/" +
        		Config.HIDDEN2_COUNT + "/" + Config.PREDICT_WINDOW + " Y:" + Config.HISTORY_YEARS + " PO:" +
        		predictOffset + " A: " + priceFormat.format(percent * 100) + "% R2 = " + format.format(rmse) +
        		" FC: " + priceFormat.format(pct_next*100.0) + ",\t"
				+ pctFmt.format(thr0*100.0) + "%: " + priceFormat.format(sum0*100) + "% " 
				+ pctFmt.format( one_pct_prob)+"%, "
				+ pctFmt.format(thr1*100.0) + "%: " + priceFormat.format(sum1*100) + "% " 
				+ pctFmt.format( two_pct_prob)+"%, "					
				+ pctFmt.format(thr2*100.0) + "%: " + priceFormat.format(sum2*100) + "% "
				+ pctFmt.format(five_pct_prob)+"%");
       	
 
        logWriter.flush();
        
	}
	
	
	
}
