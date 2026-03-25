package receiptprint.app;

import receiptprint.api.APIFetcher;
import receiptprint.api.impl.LocalRandWordGetter;
import receiptprint.system.DataDispatcher;

import java.net.http.HttpClient;


public class Main {
    // TODO CREATE LOGGER
    public static void main(String[] args) {
        DataDispatcher dispatcher = new DataDispatcher();
        dispatcher.run();
    }
}
