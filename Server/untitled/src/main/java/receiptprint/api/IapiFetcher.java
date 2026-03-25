package receiptprint.api;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;


public abstract class IapiFetcher implements APIFetcher {
    protected final HttpClient client;   // instantiate a single client
    protected int requestAttempts = 0;
    protected final int MAX_REQUEST_ATTEMPTS = 3;
    protected final String LOG_NAME = "";

    protected String apiKey;


    public IapiFetcher(HttpClient client) {
        this.client = client;
    }


    // PRE-DEFINED
    public JSONObject getAPIData() {
        String data = fetchData();
//        System.out.println(data);   // TODO DEBUGGING
        JSONObject jsonData = new JSONObject(data);
        return cleanData(jsonData);
    }

    /**
     * Load and return API key from secrets.json
     *
     * @param keyName  Name of key in secrets.json
     * @param resPath  Path to resource (default: "secrets.json")
     *
     * @return  API key
     */
    public String getApiKey(String keyName, String resPath) throws IllegalStateException {
        // get API key
        try {
            // input stream
            InputStream in = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(resPath));

            // convert to JSON
            JSONTokener tokener = new JSONTokener(in);
            JSONObject secrets = new JSONObject(tokener);
            return secrets.getString(keyName);
        }
        catch (Exception e) {
            throw new IllegalStateException("Could not read CheckiDay API key from secrets.json. Error: " + e);
        }
    }

    public String getApiKey(String keyName) throws IllegalStateException {
        return getApiKey(keyName, "secrets.json");
    }

    /**
     * Make API request attempts.
     *
     * @param request  Request to make.
     * @return  Response body, or empty string if no response was received.
     */
    public String makeRequests(HttpRequest request) throws ApiFetchException{
        HttpResponse<String> res = null;

        // make request attempts
        while (requestAttempts++ < MAX_REQUEST_ATTEMPTS) {
            // attempt request
            try {
                res = client.send(request, HttpResponse.BodyHandlers.ofString());

                // good status code
                if (res.statusCode() == 200) {
                    return res.body();
                }
                // bad status code
                else if (res.statusCode() < 500)
                    break;
                // maybe okay status code, but did not get valid response, loop again
            }
            catch (Exception e) {
                System.out.println(e.getMessage());    // TODO log error
            }
        }

        // request attempts failed to send
        if (res == null)
            throw new ApiFetchException("No request attempts made for " + LOG_NAME + ".");
        // request never gave valid response
        throw new ApiFetchException(LOG_NAME, res.statusCode());
    }


    // TO IMPLEMENT
    protected abstract String fetchData();
    protected abstract JSONObject cleanData(JSONObject data);
}
