package org.encog.examples.neural.predict.market;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
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



/**
 * This class loads financial data from mysql.
 * 
 * @author ahernandez
 */
public class MySqlLoader implements MarketLoader {

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
            System.out.println("From: " + calendarFrom.getTime());
            System.out.println("To: " + calendarTo.getTime()); 
            
            String query = "select date, time, open_price, close_price, split_factor FROM mydb.Quote where stock_symbol = '" +  ticker.getSymbol().toLowerCase() + 
                	"' and (date >= '" + calendarFrom.get(Calendar.YEAR) + "-" + (calendarFrom.get(Calendar.MONTH) + 1) + "-" + calendarFrom.get(Calendar.DAY_OF_MONTH) +
                	"' and date <= '" + calendarTo.get(Calendar.YEAR) + "-" + (calendarTo.get(Calendar.MONTH)+1) + "-" + calendarTo.get(Calendar.DAY_OF_MONTH) +
                	"') and (`time` BETWEEN '15:28:00' and '15:36:00' ) order by date, abs(TIMEDIFF(time,'15:30:00'));";
            
            System.out.println(query);
            rs = st.executeQuery(query);

            Date lastDate = new Date(); 
            while (rs.next()) {    
//            	Calendar cl = Calendar.getInstance();
				final Date date = rs.getDate("date");
//				Time t = rs.getTime("time");
//				cl.setTime(date);
//				cl.add(Calendar.SECOND, (int)t.getTime()/1000);
//
//				Date qDate = new Date(date.getTime()+t.getTime());

				//date.setTime(rs.getTime("time").getTime());
				
				if (lastDate.compareTo(date) != 0){
					//System.out.println(date.toString() + ' ' + t + ' ' + rs.getFloat("close_price"));
					
					final float split_factor = rs.getFloat("split_factor");
					final float close = rs.getFloat("close_price");
					final float adjClose = close * split_factor;	
					final LoadedMarketData data = 
						new LoadedMarketData(date, ticker);
					data.setData(MarketDataType.ADJUSTED_CLOSE, adjClose);
					data.setData(MarketDataType.CLOSE, close);
					result.add(data);
				}
				lastDate = date;
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
	        
	   


				

