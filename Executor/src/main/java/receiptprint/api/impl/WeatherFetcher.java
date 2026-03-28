package receiptprint.api.impl;

import org.json.JSONObject;
import receiptprint.api.ApiFetchException;
import receiptprint.api.IapiFetcher;
import receiptprint.system.Logger;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;


public class WeatherFetcher extends IapiFetcher {
    private final String apiGetName = "VisualCrossingAPIKey";
    private final String DEFAULT_LOCATION = "Durango, CO";

    public WeatherFetcher(HttpClient client) {
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
        // only run if API key is set
        if (apiKey == null)
            throw new ApiFetchException("No API key found for " + apiGetName);

        // build request url
        String baseUrl = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/";
        String elements = "datetimeEpoch,tempmax,tempmin,temp,feelslikemax,feelslikemin," +
                "feelslike,humidity,precip,precipprob,precipcover,preciptype," +
                "snow,windgust,windspeed,cloudcover,severerisk,sunset,moonphase";

        String url = String.format("%s%s/today?unitGroup=us&key=%s&contentType=json&include=days,current,alerts&elements=%s",
                baseUrl,
                URLEncoder.encode(getLocation(), StandardCharsets.UTF_8),
                apiKey,
                elements
        );

        // make requests
        return makeRequests(HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build()
        );
    }

    @Override
    protected JSONObject cleanData(JSONObject data) {
        // current weather JSON
        JSONObject current = data.getJSONObject("currentConditions");
        current.remove("datetimeEpoch");

        // today's weather JSON
        JSONObject today = data.getJSONArray("days").getJSONObject(0);

        // assemble final JSON
        JSONObject cleanData = new JSONObject();

        cleanData.put("address", data.getString("resolvedAddress"));
        cleanData.put("current", current);
        cleanData.put("today", today);
        cleanData.put("alerts", data.getJSONArray("alerts"));

        return cleanData;
    }

    private String getLocation() {
        // try with recourses to auto close stream (since using thread sleeping?)
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.json")) {

            if (in == null) {
                Logger.warn("config.json not found for location. Using default: " + DEFAULT_LOCATION);
                return DEFAULT_LOCATION;
            }

            // Read the entire content into string first
            String content = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

            // then parse string
            JSONObject config = new JSONObject(content);
            return config.getJSONObject("weather").getString("location");

        } catch (Exception e) {
        Logger.err("Could not read location from config.json. Error: " + e.getMessage()
                + "\nUsing default: " + DEFAULT_LOCATION);
            return DEFAULT_LOCATION;
        }
    }
}
