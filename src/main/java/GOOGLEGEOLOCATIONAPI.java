package main.java;

import com.amazonaws.util.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * Simple REST demo on getting data
 *
 * Probably better to use a library if this was a real world thing, but for a demo this is fine
 */
public class GOOGLEGEOLOCATIONAPI {
    private static final String API_KEY = "no key";

    /**
     * Get the top article from a specific new york times category using REST (GET)
     * @param address The streetName
     * @return Title of the article
     * @throws Exception On errors w/ REST
     */
    public String getState(String address) throws Exception {
        String endpointUrl =
                String.format("https://maps.googleapis.com/maps/api/geocode/json?address=" + (address) +("&key= no key"),
                        address.toLowerCase().trim(),
                        API_KEY);

        URL url = new URL(endpointUrl);
        URLConnection connection = url.openConnection();
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String tmp;
        while((tmp = br.readLine()) != null) {
            sb.append(tmp);
        }

        System.out.println("The state is " + sb.toString());

        // Create a JsonObject from the JSON the API gives back
        // Basically just a map of either maps or lists
        JSONObject jsonObject = new JSONObject(sb.toString());
        return ((JSONObject)jsonObject.getJSONArray("results").get(3)).getString("long_name");
    }
}
