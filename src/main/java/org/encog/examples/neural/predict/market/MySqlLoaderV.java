package org.encog.examples.neural.predict.market;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.encog.ml.data.market.MarketDataType;
import org.encog.ml.data.market.TickerSymbol;
import org.encog.ml.data.market.loader.LoadedMarketData;
import org.encog.ml.data.market.loader.MarketLoader;
import org.encog.util.arrayutil.NormalizationAction;
import org.encog.util.arrayutil.NormalizedField;


public class MySqlLoaderV implements MarketLoader {


	public Collection<LoadedMarketData> load(TickerSymbol ticker, Set<MarketDataType> dataNeeded, Date from, Date to) {
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
         
            System.out.println("From: " + calendarFrom.getTime());
            System.out.println("To: " + calendarTo.getTime()); 
            
            String query = "select date, time, open_price, close_price, split_factor, volume  FROM mydb.Quote where stock_symbol = '" +  ticker.getSymbol().toLowerCase() + 
                	"' and (date >= '" + calendarFrom.get(Calendar.YEAR) + "-" + (calendarFrom.get(Calendar.MONTH) + 1) + "-" + calendarFrom.get(Calendar.DAY_OF_MONTH) +
                	"' and date <= '" + calendarTo.get(Calendar.YEAR) + "-" + (calendarTo.get(Calendar.MONTH)+1) + "-" + calendarTo.get(Calendar.DAY_OF_MONTH) +
                	"') and (`time` BETWEEN '09:00:00' and '16:00:00' )  order by date desc, time desc ;";
            
            System.out.println(query);
            rs = st.executeQuery(query);

			final Calendar cl = Calendar.getInstance();
			int lastInterval = -1;
			int totalvolume = 0;
    		LoadedMarketData data = null;			
    		NormalizedField norm = new NormalizedField(NormalizationAction.Normalize, 
    				"Volume",100000000,0,1,-1);
    		
            while (rs.next()) {    
    			final Date date = rs.getDate("date");
            	cl.setTime(date);


    			Time t = rs.getTime("time", cl);
            	cl.add(Calendar.SECOND, (int)(t.getTime()/1000));
            	cl.add(Calendar.HOUR, -11);
            	
            	//Points should be 30 minutes apart. However, some of the quotes do not occur exactly on the half hour or hour.
            	//To deal with this situation, the select pulls quotes that are up to 2 minutes prior than the 1/2 hour. 
            	//The quotes are sorted in descending order. We take the closest quote to the interval and skip the rest.
            	//To do this, we calculate the time interval that the quote corresponds to and then save the first quote for the interval
            	//and skip the rest.
            	
            	//Find the timeInverval this corresponds to. 
            	int timeInterval = (int)( ((double)t.getTime()/1000.0)/(Config.MINUTES_IN_INTERVAL*60.0)+0.999);
            	
            	if (timeInterval != lastInterval){

            		if (lastInterval != -1){
            			
            			double normTotalvolume = norm.normalize(totalvolume);
    	    			data.setData(MarketDataType.VOLUME, normTotalvolume);     
    	    			result.add(data);
    	    			totalvolume = 0;
            		}
            		
            		lastInterval = timeInterval;
	    			Date qDate = cl.getTime();
	
	    			//System.out.println(date.toString() + ' ' + t + ' ' + rs.getFloat("close_price"));
	    			
	    			final float split_factor = rs.getFloat("split_factor");
	    			final float close = rs.getFloat("close_price");
	    			final float adjClose = close * split_factor;	
	    		    data =  new LoadedMarketData(qDate, ticker);
	    			data.setData(MarketDataType.ADJUSTED_CLOSE, adjClose);
	    			data.setData(MarketDataType.CLOSE, close);
            	}
            	
            	final float volume = rs.getFloat("volume");
            	totalvolume += volume;
                     	
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
