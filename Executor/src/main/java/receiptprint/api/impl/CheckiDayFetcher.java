package receiptprint.api.impl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import receiptprint.api.ApiFetchException;
import receiptprint.api.IapiFetcher;
import receiptprint.system.Logger;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;


public class CheckiDayFetcher extends IapiFetcher {
    final String apiGetName = "CheckiDayAPIKey";

    public CheckiDayFetcher(HttpClient client) {
        super(client);

        // get API key
        try {
            apiKey = getApiKey(apiGetName);
        }
        catch (IllegalArgumentException e) {
            Logger.err("Count not get API key for " + this.getClass().getSimpleName() + e.getMessage());
        }
    }

    @Override
    protected String fetchData() throws ApiFetchException {
        // make request attempts
        try {
            return makeRequests(
                    HttpRequest.newBuilder()
                            .uri(URI.create("https://api.apilayer.com/checkiday/events"))
                            .header("apikey", apiKey)
                            .GET()
                            .build()
            );
        }
        catch (ApiFetchException e) {
            return "";
        }
    }

    @Override
    protected JSONObject cleanData(JSONObject data) {
        // ensure the JSON is the correct format
        if (!data.has("events"))
            throw new IllegalArgumentException("Invalid JSON data. Did not find \"events\" CheckiDay response.");

        JSONArray events = data.getJSONArray("events");

        // create final/cleaned JSON object that can be fed in LLM
        JSONObject cleanData = new JSONObject();
        cleanData.put("Day Observances", new JSONArray());
        JSONArray dayObservances = cleanData.getJSONArray("Day Observances");

        // Extract just the name of the "day," the rest of the data is not useful to LLM
        for (Object obj : events) {
            JSONObject event = (JSONObject) obj;
            if (event.has("name"))
                dayObservances.put(event.get("name"));
        }

        // Not harmful, but dont want to feed LLM empty data unnecessarily
        if (dayObservances.isEmpty())
            throw new IllegalArgumentException("No day observances found in JSON data.");

        return cleanData;
    }
}
