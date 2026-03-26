package receiptprint.api;

public class ApiFetchException extends RuntimeException {
    public ApiFetchException(String apiName, int code) {
        super("Error fetching data from " + apiName + " (status code: " + code + ")");
    }
    public ApiFetchException(String msg) {
        super(msg);
    }
}
