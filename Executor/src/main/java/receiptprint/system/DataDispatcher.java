package receiptprint.system;

import org.json.JSONArray;
import org.json.JSONObject;
import receiptprint.api.APIFetcher;
import receiptprint.api.impl.CheckiDayFetcher;
import receiptprint.api.impl.GoogleCalFetcher;
import receiptprint.api.impl.LocalRandWordGetter;
import receiptprint.api.impl.WeatherFetcher;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.time.*;


public class DataDispatcher {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private List<HourMin> schedule = new ArrayList<>();
    private List<APIFetcher> fetchers = new ArrayList<>();


    public void run() {
        // sets schedule and fetchers
        loadConfigParams();

        // create infinite schedules
        for (HourMin hm : schedule) {
            schedule(hm.hour, hm.min);
        }
    }

    private void schedule(int hour, int minute) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.withHour(hour).withMinute(minute);

        // shift to next day
        if (now.isAfter(future))
            future = future.plusDays(1);

        long delay = Duration.between(now, future).toSeconds();   // when to run
        long period = TimeUnit.DAYS.toSeconds(1);   // run indefinitely

        // tell scheduler when to run
        scheduler.scheduleAtFixedRate(this::dispatch, delay, period, TimeUnit.SECONDS);
    }

    private void dispatch(){
        // called by scheduler
        var data = fetchData();
        Logger.log("Data Successfully Fetched! Data size: " + data.size());

        // create request
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8001/print-llm"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(data.toString()))
                .build();

        // try to send request to llm receipt server
        HttpResponse<String> response = null;
        try{
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch (Exception e){
            Logger.err("Error sending data to server: " + e.getMessage());
            return;
        }

        // log results of request
        if (response.statusCode() != 200)
            Logger.err("Server Response: " + response.statusCode() + " - " + response.body());
        else
            Logger.log("Server Response: " + response.statusCode() + " - " + response.body());
    }


    private List<JSONObject> fetchData(){
        // create fetchers
        HttpClient client = HttpClient.newHttpClient();
        APIFetcher[] fetchers = {
                new CheckiDayFetcher(client),
                new WeatherFetcher(client),
                new GoogleCalFetcher(client),
        };

        // fetch data and combine
        List<JSONObject> data = new ArrayList<>(fetchers.length);
        for (APIFetcher fetcher : fetchers) {
            try {
                data.add(fetcher.getAPIData());
            } catch (Exception e) {
                Logger.warn("API fetch failed for " + fetcher.getClass().getSimpleName() + e.getMessage());
            }
        }

        // stamp date into data
        data.addFirst(new JSONObject().put("Today's Date", LocalDateTime.now().toString()));

        return data;
    }

    private void loadConfigParams() throws RuntimeException {
        try {
            // Use the thread's context class loader to be safe with Gradle/JAR environments
            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.json");

            // config is required since it's only opened on init
            if (in == null) {
                Logger.err("config.json not found in src/main/resources/");
                throw new RuntimeException("config.json not found in src/main/resources/");
            }

            // Read bytes first to ensure we aren't passing a null/closed stream to the JSON parser
            byte[] bytes = in.readAllBytes();
            String content = new String(bytes, StandardCharsets.UTF_8);

            JSONObject root = new JSONObject(content);
            JSONObject systemJSON = root.getJSONObject("system");

            setSchedule(systemJSON);
            setFetchers(systemJSON);

            in.close();
        } catch (Exception e) {
            Logger.err("Config Load Failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private void setSchedule(JSONObject system){
        // helper method
        JSONArray runTimes = system.getJSONArray("runTimes");

        // set schedules from config
        for (int i = 0; i < runTimes.length(); i++) {
            String time = runTimes.getString(i);
            String[] parts = time.split(":");
            schedule.add(
                    new HourMin(
                            Integer.parseInt(parts[0]),
                            Integer.parseInt(parts[1])));
        }
    }

    private void setFetchers(JSONObject system){
        // helper method
        JSONArray activeAPIs = system.getJSONArray("activeAPIs");

        for (int i = 0; i < activeAPIs.length(); i++) {
            String apiName = activeAPIs.getString(i);

            // hardcoded API names since they have to be implemented anyway
            switch (apiName) {
                case "CheckiDay":
                    fetchers.add(new CheckiDayFetcher(HttpClient.newHttpClient()));
                    break;
                case "Weather":
                    fetchers.add(new WeatherFetcher(HttpClient.newHttpClient()));
                    break;
                case "GoogleCal":
                    fetchers.add(new GoogleCalFetcher(HttpClient.newHttpClient()));
                    break;
                case "RandWord":
                    fetchers.add(new LocalRandWordGetter(HttpClient.newHttpClient()));
                    break;
                default:
                    Logger.warn("Unknown API: " + apiName);
            }
        }
    }
}
