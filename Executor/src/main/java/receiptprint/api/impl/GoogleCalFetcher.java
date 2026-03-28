package receiptprint.api.impl;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import receiptprint.api.IapiFetcher;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import receiptprint.system.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class GoogleCalFetcher extends IapiFetcher {
    private int daysFromNow = 5;
    private int maxResults = 100;
    private List<String> calendars;


    public GoogleCalFetcher(HttpClient client) {
        super(client);
        calendars = new ArrayList<>();
        calendars.add("primary");   // default
    }

    @Override
    protected String fetchData() throws Exception {
        // load credentials.json
        InputStream in = getClass().getResourceAsStream("/GoogleCalCred.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                GsonFactory.getDefaultInstance(),
                new InputStreamReader(in)
        );
        FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(new File("tokens"));

        // Set up authorization flow (requests read-only access)
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                clientSecrets,
                Collections.singletonList(CalendarScopes.CALENDAR_READONLY))
                .setDataStoreFactory(dataStoreFactory)
                .setAccessType("offline")
                .build();

        // Triggers the browser login if no local token exists
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");

        // calendar service object
        Calendar service = new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("ReceiptPrinterProject")
                .build();

        // set parameters from config.json
        loadConfig();

        // set date range
        long nowMillis = System.currentTimeMillis();
        long futureMillis = nowMillis + ((long) daysFromNow * 24 * 60 * 60 * 1000);
        DateTime now = new DateTime(nowMillis);
        DateTime future = new DateTime(futureMillis);

        // ——— fetch calendars ———
        List<Event> eventsList = new ArrayList<>();
        var calendarList = service.calendarList().list().setShowHidden(true).execute();
                                                        // ^^ Birthdays are "hidden"

        // fetch all calendars
        for (var cal : calendarList.getItems()) {
            String calName = cal.getSummary();

            // only keep ones specified in config.json
            if (calendars.contains(calName)) {
                eventsList.addAll(
                                service.events().list(cal.getId())
                                .setMaxResults(maxResults)
                                .setTimeMin(now)
                                .setTimeMax(future)
                                .setOrderBy("startTime")  // does matter too much since cals wont be sorted later
                                .setSingleEvents(true)  // Required to expand recurring events into individual instances
                                .execute()
                                .getItems());
            }
        }

        // Convert results to a JSON
        JSONArray jsonEvents = new JSONArray();

        for (Event event : eventsList) {
            JSONObject jsonEvent = getJsonObject(event);
            jsonEvents.put(jsonEvent);
        }

        JSONObject result = new JSONObject();
        result.put("Calendar Events", jsonEvents);

        return result.toString();
    }

    @NotNull
    private static JSONObject getJsonObject(Event event) {
        // gather details about event (if any)
        String description = event.getDescription();
        String location = event.getLocation();

        // create event JSON and put details into it
        JSONObject jsonEvent = new JSONObject();
        if (description != null)
            jsonEvent.put("description", description);
        if (location != null)
            jsonEvent.put("location", location);
        jsonEvent.put("summary", event.getSummary());

        // start and end times
        DateTime start = event.getStart().getDateTime();

        // all day events
        if (start == null)
            start = event.getStart().getDate();
        jsonEvent.put("start", start.toString());
        return jsonEvent;
    }

    @Override
    protected JSONObject cleanData(JSONObject data) {
        for (int i = 0; i < data.getJSONArray("Calendar Events").length(); i++) {
            JSONObject event = data.getJSONArray("Calendar Events").getJSONObject(i);
            if (!event.has("description"))
                continue;

            // remove all Google Calender default descriptions
            if (event.getString("description").contains("Observance"))
                event.remove("description");
        }
        return data;
    }

    private void loadConfig() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.json")) {
            if (in == null) {
                throw new IllegalStateException("config.json not found");
            }

            // read from config
            JSONTokener tokener = new JSONTokener(in);
            JSONObject config = new JSONObject(tokener);
            JSONObject calendarConfig = config.getJSONObject("googleCal");

            // set parameters
            daysFromNow = calendarConfig.getInt("daysFromNow");
            maxResults = calendarConfig.getInt("maxResults");

            JSONArray calendarsJSON = calendarConfig.getJSONArray("calendars");
            calendars = new ArrayList<>(calendarsJSON.length());
            for (int i = 0; i < calendarsJSON.length(); i++)
                calendars.add(calendarsJSON.getString(i));
        }
        catch (Exception e){
            Logger.warn("Error loading config.json for Google Calendar: " + e.getMessage() + "\nUsing defaults.");
        }
    }
}
