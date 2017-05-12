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

import org.encog.ml.data.market.TickerSymbol;

/**
 * Basic config info for the market prediction example.
 * 
 * @author jeff
 * 
 */
//23-35-11-1

public class Config {

	
	public static QuoteLoaderEnum QUOTE_LOADER = QuoteLoaderEnum.MySQLDB; 
	public static  String TRAINING_FILE = "marketData.egb";
	public static  String NETWORK_FILE = "marketNetwork.eg";
	public static  String LOG_FILE = "marketlog.txt";
	public static int SAVE_ALL_PREDICTIONS = 1;
	public static int TRAINING_MINUTES = 1;
	public static int INPUT_WINDOW = 5; //5 ;//10;
	public static int HIDDEN1_COUNT = 5; //7; //21; //20;
	public static int HIDDEN2_COUNT = 1; //0;
	public static final int PREDICT_WINDOW = 1;
	public static int HISTORY_YEARS = 3;
	public static int PREDICIT_OFFSET= 60;
	public static int USE_VOLUME = 0;
	public static int MINUTES_IN_INTERVAL=15;
	public static String SQL_CONNECT_STRING = "jdbc:mysql://caffe:3306/mydb?verifyServerCertificate=false&useSSL=true";
	
	
	public static  TickerSymbol TICKER = new TickerSymbol("XXX");
	
	public static String getSQLConnectionString(){
		return SQL_CONNECT_STRING;
	}
}
