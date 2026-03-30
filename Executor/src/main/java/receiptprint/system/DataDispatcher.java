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


/// Manages the scheduling and dispatching of data retrieval tasks.
///
/// DataDisbatcher sets up a disbatch schedule (configured `config.json` file)
/// for retrieving data using APIFetchers (configured in the `config.json`).
/// The DataDisbatcher then dispatches the data to a locally hosted server for
/// processing. All errors, warnings, and standard logs are logged using the
/// custom Logger class.
public class DataDispatcher {

    /// Background thread for efficiently scheduling
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /// Configured schedule
    private List<HourMin> schedule = new ArrayList<>();

    /// Configured Fetchers
    private List<APIFetcher> fetchers = new ArrayList<>();

    /// HTTP client for Fetchers
    private HttpClient client = HttpClient.newHttpClient();


    /// **Run the Data Dispatcher**
    ///
    /// This method will run indefinitely—until the program is terminated by
    /// the user or a fatal error occurs. The Disbatcher will execute on the
    /// configured schedule in `config.json`.
    public void run() {
        // sets schedule and fetchers from config.json
        loadConfigParams();

        // create infinite schedules
        for (HourMin hm : schedule) {
            schedule(hm.hour, hm.min);
        }
    }


    /// Set the scheduler's execution time(s).
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

    /// Method called when execution is called by the scheduler.
    ///
    /// Calls fetchData(), then dispatches the data to the LLMPrinter server,
    /// and logs the response from the server.
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

    /// Fetch data using API Fetchers.
    ///
    /// Handles internal exceptions and logs errors.
    private List<JSONObject> fetchData(){
        // fetch data and combine
        List<JSONObject> data = new ArrayList<>(fetchers.size());
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

    /// Load user configurations from `config.json`.
    ///
    /// Throws an exception if there is a failure.
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

    /// Set the eternal schedule of the program from system configurations.
    ///
    /// Helper Method
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

    /// Set the type of fetchers to use configured by the user.
    ///
    /// Helper Method
    private void setFetchers(JSONObject system){
        // helper method
        JSONArray activeAPIs = system.getJSONArray("activeAPIs");

        for (int i = 0; i < activeAPIs.length(); i++) {
            String apiName = activeAPIs.getString(i);

            // hardcoded API names since they have to be implemented anyway
            switch (apiName) {
                case "CheckiDay":
                    fetchers.add(new CheckiDayFetcher(client));
                    break;
                case "Weather":
                    fetchers.add(new WeatherFetcher(client));
                    break;
                case "GoogleCal":
                    fetchers.add(new GoogleCalFetcher(client));
                    break;
                case "RandWord":
                    fetchers.add(new LocalRandWordGetter(client));
                    break;
                default:
                    Logger.warn("Unknown API: " + apiName);
            }
        }
    }
}
