package receiptprint.api;
import org.json.JSONObject;


public interface APIFetcher {
    JSONObject getAPIData() throws Exception;
}


