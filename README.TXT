BittrexJ

Open project in BlueJ.

This is an open-source implementation of the Bittrex API. It is currently work in progress and should be considered alpha.

Attached is the class "DriverAlarm" which demonstrates a notification system to send an email when an order changes.

In general, create the object BittrexUtils and use it to communicate. If it doesn't contain what you need to get, use the getCommunicator() function inside BittrexUtils to get an instance of BittrexCommunicator. This class returns raw JSON objects from the exchange.