package receiptprint.api.impl;

import org.json.JSONObject;
import receiptprint.api.IapiFetcher;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class LocalRandWordGetter extends IapiFetcher {
    final String WORDS_FILE = "/RandomWords.csv";

    public LocalRandWordGetter(HttpClient client) {
        super(client);
    }

    @Override
    protected String fetchData() {
        try (var is = getClass().getResourceAsStream(WORDS_FILE)) {
            // read in words CSV
            String data = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String[] words = data.split(",");

            // pick random
            Random rand = new Random();
            int index = rand.nextInt(words.length);
            String word = words[index].trim();  // for new line char or extra spaces

            return new JSONObject().put("ASCII Art Inspiration", word).toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected JSONObject cleanData(JSONObject data) {
        System.out.println("LocalRandWordGetter: " + data);
        return data;
    }
}
