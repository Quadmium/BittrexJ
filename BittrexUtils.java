import java.util.*;
import java.util.function.Function;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

public class BittrexUtils
{
    private BittrexCommunicator communicator;
    private int timeout = 5;
    
    /**
     * Constructor containing market, API_KEY, and SECRET.
     * 
     * @param market  market to communicate with, provided as "coin-coin". Example: "BTC-LTC"
     * @param API_KEY api key from Bittrex
     * @param SECRET  api secret from Bittrex
     */
    public BittrexUtils(String market, String API_KEY, String SECRET)
    {
        communicator = new BittrexCommunicator(market, API_KEY, SECRET);
    }
    
    /**
     * Constructor containing market, API_KEY, SECRET, and timeout.
     * 
     * @param market  market to communicate with, provided as "coin-coin". Example: "BTC-LTC"
     * @param API_KEY api key from Bittrex
     * @param SECRET  api secret from Bittrex
     * @param timeout amount of tries to make per each request
     */
    public BittrexUtils(String market, String API_KEY, String SECRET, int timeout)
    {
        this(market, API_KEY, SECRET);
        this.timeout = timeout;
    }
    
    /**
     * Returns the communicator for raw data.
     * 
     * @return a BittrexCommunicator
     */
    public BittrexCommunicator getCommunicator()
    {
        return communicator;
    }
    
    /**
     * Sets the amount of times the communicator will make a request.
     * 
     * @param timeout number of times
     */
    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }
    
    /**
     * Returns the amount of times the communicator will make a request.
     * 
     * @return the amount of times
     */
    public int getTimeout()
    {
        return timeout;
    }
    
    /**
     * Gets the first order that matches given conditions, starting from the top of the order book.
     * 
     * @param  buyOrSell      true for looking in the buy order book, false for looking in the sell order book
     * @param  underBTCAmount the amount of bitcoins down an order must be
     * @param  worseThanRate  the rate you are willing to buy or sell at
     * @return a map of order data, containing "Quantity" and "Rate" paired with double values, null if fetching failed, -1 for both if no order is found
     */
    public Map<String, Double> getTopOrder(boolean buyOrSell, double underBTCAmount, double worseThanRate)
    {
        JSONObject orderBookBoth = (JSONObject) iterate((o) -> communicator.getOrderBook(50), timeout);
        if(orderBookBoth == null)
            return null;
            
        JSONArray orderBook = (JSONArray)(buyOrSell ? orderBookBoth.get("buy") : orderBookBoth.get("sell"));
        if(orderBook == null)
            return null;
            
        double btcSum = 0;
        for(Object obj : orderBook)
        {
            JSONObject order = (JSONObject) obj;
            double quantity = (double) order.get("Quantity");
            double rate = (double) order.get("Rate");
            btcSum += quantity * rate;
            
            if((worseThanRate == -1 ||
                buyOrSell && rate <= worseThanRate || 
                !buyOrSell && rate >= worseThanRate) && btcSum > underBTCAmount)
            {
                Map<String, Double> foundOrder = new TreeMap<String, Double>();
                foundOrder.put("Quantity", quantity);
                foundOrder.put("Rate", rate);
                return foundOrder;
            }
        }
        
        Map<String, Double> foundOrder = new TreeMap<String, Double>();
        foundOrder.put("Quantity", -1.0);
        foundOrder.put("Rate", -1.0);
        return foundOrder;
    }
    
    /**
     * Returns an array of rates of orders only.
     * 
     * @param buyOrSell true for buy book false for sell book
     * @return an array of rates
     */
    public double[] getOrderRates(boolean buyOrSell)
    {
        JSONObject orderBookBoth = (JSONObject) iterate((o) -> communicator.getOrderBook(50), timeout);
        if(orderBookBoth == null)
            return null;
            
        JSONArray orderBook = (JSONArray)(buyOrSell ? orderBookBoth.get("buy") : orderBookBoth.get("sell"));
        if(orderBook == null)
            return null;
            
        double[] result = new double[orderBook.size()];
        for(int i=0; i<result.length; i++)
            result[i] = (double)((JSONObject)orderBook.get(i)).get("Rate");
            
        return result;
    }
    
    /**
     * Gets the first order that matches given conditions, starting from the top of the order book.
     * 
     * @param  buyOrSell      true for looking in the buy order book, false for looking in the sell order book
     * @param  underBTCAmount the amount of bitcoins down an order must be
     * @return a map of order data, containing "Quantity" and "Rate" paired with double values, null if fetching failed
     */
    public Map<String, Double> getTopOrder(boolean buyOrSell, double underBTCAmount)
    {
        return getTopOrder(buyOrSell, underBTCAmount, -1);
    }
    
    /**
     * Gets the first order that matches given conditions, starting from the top of the order book.
     * 
     * @param  buyOrSell      true for looking in the buy order book, false for looking in the sell order book
     * @return a map of order data, containing "Quantity" and "Rate" paired with double values, null if fetching failed
     */
    public Map<String, Double> getTopOrder(boolean buyOrSell)
    {
        return getTopOrder(buyOrSell, 0);
    }
    
    /**
     * Returns a summary of the main data in the order provided.
     * 
     * @param  uuid the uuid of the order
     * @return a map of order data, containing "Quantity", "QuantityRemaining", and "Rate" paired with double values, null if fetching failed
     */
    public Map<String, Double> getOrderStatistics(String uuid)
    {
        JSONObject order = (JSONObject) iterate((o) -> communicator.getOrder(uuid), timeout);
        if(order == null)
            return null;
            
        Map<String, Double> result = new TreeMap<String, Double>();
        result.put("Quantity", (double) order.get("Quantity"));
        result.put("QuantityRemaining", (double) order.get("QuantityRemaining"));
        result.put("Limit", (double) order.get("Limit"));
        
        return result;
    }
    
    /**
     * Places an order with the provided parameters.
     * 
     * @param  buyOrSell true for placing a buy order, false for placing a sell order
     * @param  quantity  the amount of coins being bought
     * @param  rate      the rate of coins being bought
     * @return uuid of the order, null if order failed
     */
    public String placeOrder(boolean buyOrSell, double quantity, double rate)
    {
        return (String) iterate((o) -> buyOrSell ? communicator.placeBuyOrder(quantity, rate) : communicator.placeSellOrder(quantity, rate), timeout);
    }
    
    /**
     * Cancels an order.
     * 
     * @param  uuid uuid of the order
     * @return true for success, false for failure
     */
    public boolean cancelOrder(String uuid)
    {
        return (boolean) iterate((o) -> communicator.cancelOrder(uuid), timeout);
    }
    
    /**
     * Returns a collection of open orders, split up into buy and sell books.
     * The collection has a list of two books, buy and sell, accessable by get(0) and get(1) respectively.
     * Inside each book is a list of orders, represented as maps of String and Double, containing "Quantity", "QuantityRemaining", and "Limit".
     * 
     * @return a collection of type List<List<Map<String, Double>>>, null if failed to fetch
     * 
     * @deprecated
     * Bittrex does not provide order type, once they do this will work.
     */
    @Deprecated
    public List<List<Map<String, Double>>> getOpenOrders()
    {
        JSONArray orders = (JSONArray) iterate((o) -> communicator.getOpenOrders(), timeout);
        if(orders == null)
            return null;
            
        List<List<Map<String, Double>>> result = new ArrayList<List<Map<String, Double>>>();
        result.add(new ArrayList<Map<String, Double>>());
        result.add(new ArrayList<Map<String, Double>>());
        
        for(Object obj : orders)
        {
            JSONObject order = (JSONObject) obj;
            
            Map<String, Double> orderMap = new TreeMap<String, Double>();
            orderMap.put("Quantity", (double) order.get("Quantity"));
            orderMap.put("QuantityRemaining", (double) order.get("QuantityRemaining"));
            orderMap.put("Limit", (double) order.get("Limit"));
            
            if(order.get("OrderUuid").equals("LIMIT_BUY"))
                result.get(0).add(orderMap);
            else if(order.get("OrderType").equals("LIMIT_SEll"))
                result.get(1).add(orderMap);
            else
                return null;
        }
        
        return result;
    }
    
    /**
     * Returns the available balance of a given currency.
     * 
     * @param currency currency ticker to get balance for. Example: "BTC"
     * @return a double representing the balance
     */
    public double getAvailableBalance(String currency)
    {
        return communicator.getBalance(currency) == null ? 0.0 : (double) communicator.getBalance(currency).get("Available");
    }
    
    private static Object iterate(Function<Object, Object> func, int times)
    {
        Object result = null;
        int count = 0;
        while(count < times && result == null)
        {
            result = func.apply(new Object());
            count++;
        }
        
        return result;
    }
}