# Stock Price(Sample Spring Reactor project)

Stock price project should provide an endpoint to access to Spot(current) and Daily average stock prices. 
Stocks could be sent as a list of comma separated stocks. To access to spot and daily prices, application 
should call two third-party APIs, However the call to Spot API should be sent as batch of 5 requests or after 
5 seconds. 
Here are some concerns that should be considered:

1. The API should accept a list of comma separated stocks like: GOOG,AAPL,RACE
2. The API should return a JSON with the list of Spot price and Daily average price and stock name.
3. To access Spot price and Daily average price, The API should call 2 third-party APIs.
4. The Spot third-party API should be called by a list of 5 comma separated stocks or after 5 seconds to reduce load on that API.
5. The calls to the APIs should be non-blocking.
