package org.encog.examples.neural.predict.market;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;

import org.encog.ml.data.market.MarketDataType;
import org.encog.ml.data.market.TickerSymbol;
import org.encog.ml.data.market.loader.LoadedMarketData;
import org.encog.ml.data.market.loader.MarketLoader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Time;



public class MySqlLoader10 implements MarketLoader {
	/**
	 * Load the specified financial data.
	 * 
	 * @param ticker
	 *            The ticker symbol to load.
	 * @param dataNeeded
	 *            The financial data needed.
	 * @param from
	 *            The beginning date to load data from.
	 * @param to
	 *            The ending date to load data to.
	 * @return A collection of LoadedMarketData objects that represent the data
	 *         loaded.
	 */
	public Collection<LoadedMarketData> load(final TickerSymbol ticker,
			final Set<MarketDataType> dataNeeded, final Date from, 
			final Date to) {
		
		final Collection<LoadedMarketData> result = 
				new ArrayList<LoadedMarketData>();
		
		try {
	        Connection con = null;
	        Statement st = null;
	        ResultSet rs = null;

	        String url = Config.getSQLConnectionString();
	        String user = "root";
	        String password = "a0viper";			
			
			final Calendar calendarFrom = Calendar.getInstance();
			calendarFrom.setTime(from);
			final Calendar calendarTo = Calendar.getInstance();
			calendarTo.setTime(to);	        
	                
            con = DriverManager.getConnection(url, user, password);
            st = con.createStatement();
//            String query = "select date, time, open_price, close_price, split_factor FROM mydb.Quote where stock_symbol = '" +  ticker.getSymbol().toLowerCase() + 
//            	"' and (date >= '" + calendarFrom.get(Calendar.YEAR) + "-" + calendarFrom.get(Calendar.MONTH) + "-" + calendarFrom.get(Calendar.DAY_OF_MONTH) +
//            	"' and date <= '" + calendarTo.get(Calendar.YEAR) + "-" + calendarTo.get(Calendar.MONTH) + "-" + calendarTo.get(Calendar.DAY_OF_MONTH) +
//            	"') and ( `time` = '16:00:00') order by date, time;";
           
//            String query = "select date, time, open_price, close_price, split_factor FROM mydb.Quote where stock_symbol = '" +  ticker.getSymbol().toLowerCase() + 
//                	"' and (date >= '" + calendarFrom.get(Calendar.YEAR) + "-" + calendarFrom.get(Calendar.MONTH) + "-" + calendarFrom.get(Calendar.DAY_OF_MONTH) +
//                	"' and date <= '" + calendarTo.get(Calendar.YEAR) + "-" + calendarTo.get(Calendar.MONTH) + "-" + calendarTo.get(Calendar.DAY_OF_MONTH) +
//                	"') and (`time` = '15:30:00') order by date, time;";
            
//            String query3 = "SELECT Q.date, max(Q.time), Q.stock_symbol, Q.close_price, sum(Q.volume), truncate(time_to_sec(Q.time)/(30*60)+0.99, 0)  as inter " +
//                           "FROM mydb.Quote as Q where Q.date = '2017-02-24' and Q.stock_symbol = 'gmlp' group by date, inter  order by Q.stock_symbol, Q.time asc ;"    ;       
//            
            
            System.out.println("From: " + calendarFrom.getTime());
            System.out.println("To: " + calendarTo.getTime()); 
            
            String query = "select date, time, open_price, close_price, split_factor, volume FROM mydb.Quote where stock_symbol = '" +  ticker.getSymbol().toLowerCase() + 
                	"' and (date >= '" + calendarFrom.get(Calendar.YEAR) + "-" + (calendarFrom.get(Calendar.MONTH) + 1) + "-" + calendarFrom.get(Calendar.DAY_OF_MONTH) +
                	"' and date <= '" + calendarTo.get(Calendar.YEAR) + "-" + (calendarTo.get(Calendar.MONTH)+1) + "-" + calendarTo.get(Calendar.DAY_OF_MONTH) +
                	"') and (`time` BETWEEN '09:00:00' and '16:00:00' )  and ( mod(time_to_sec(time), 1800) >= 1680  or mod(time_to_sec(time), 1800) = 0) order by date desc, time desc ;";
            
            System.out.println(query);
            rs = st.executeQuery(query);

			final Calendar cl = Calendar.getInstance();
			int lastInterval = 0;
            while (rs.next()) {    

    			final Date date = rs.getDate("date");
            	cl.setTime(date);

    			Time t = rs.getTime("time", cl);
            	cl.add(Calendar.SECOND, (int)(t.getTime()/1000));
            	cl.add(Calendar.HOUR, -11);
            	
            	//Points should be 30 minutes apart. However, some of the quotes do not occur exactly on the half hour or hour.
            	//To deal with this situation, the select pulls quotes that are up to 2 minutes prior than the 1/2 hour. 
            	//The quotes are sorted in descending order. We take the closest quote to the half hour and skip the rest.
            	//To do this, we calculate the time interval that the quote corresponds to and then save the first quote for the interval
            	//and skip the rest.
            	
            	//Find the timeInverval this corresponds to. 
            	int timeInterval = (int)( ((t.getTime()/1000) +120)/(30*60));
    			
            	if (timeInterval != lastInterval){
            		lastInterval = timeInterval;
	    			Date qDate = cl.getTime();
	
	    			//System.out.println(date.toString() + ' ' + t + ' ' + rs.getFloat("close_price"));
	    			
	    			final float split_factor = rs.getFloat("split_factor");
	    			final float close = rs.getFloat("close_price");
	    			final float adjClose = close * split_factor;	
	    			final float volume = rs.getFloat("volume");
	    			final LoadedMarketData data = 
	    				new LoadedMarketData(qDate, ticker);
	    			data.setData(MarketDataType.ADJUSTED_CLOSE, adjClose);
	    			data.setData(MarketDataType.CLOSE, close);
	    			data.setData(MarketDataType.VOLUME, volume);
	    			result.add(data);
            	}
            }
            
            rs.close();
            st.close();
            con.close();


	        } catch (SQLException se){
	        	se.printStackTrace();
	        } catch(Exception e){
	        	e.printStackTrace();
	        } finally {

	        }
		return result;
	}
}

