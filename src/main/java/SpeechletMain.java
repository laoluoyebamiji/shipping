package main.java;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;

public class SpeechletMain implements Speechlet {

    /** ny times api wrapper */
    private GOOGLEGEOLOCATIONAPI googlegeolocationapi = new GOOGLEGEOLOCATIONAPI();
    static String numberone = "";
    static String numbertwo = "";
    static String streetone = "";
    static String streettwo = "";
    static String cityone = "";
    static String citytwo = "";
    static String stateone = "";
    static String statetwo = "";
    static String zipone =  "";
    static String ziptwo = "";
    /**
     * Initialize values for the skill here
     * @param sessionStartedRequest
     * @param session
     * @throws SpeechletException
     */
    @Override
    public void onSessionStarted(SessionStartedRequest sessionStartedRequest, Session session) throws SpeechletException {
        // This is said by the device when you first open the skill
        // Not going to do anything here
        System.out.println("Session started with session id=" + session.getSessionId());
    }

    /**
     * Run when the skill is opened
     * @param launchRequest
     * @param session
     * @return
     * @throws SpeechletException
     */
    @Override
    public SpeechletResponse onLaunch(LaunchRequest launchRequest, Session session) throws SpeechletException {
        System.out.println("Got launch request " + launchRequest.toString());
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText("Welcome. What address are you shipping from?");

        System.out.println("Sending text to user: " + speech.getText());

        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(speech);

        return SpeechletResponse.newAskResponse(speech, reprompt);
    }

    /**
     * Called to perform some kind of action
     * @param request The request (contains slots, intent, etc)
     * @param session Session info (session id, attributes, etc)
     * @return Response sent to device, alexa will say this.
     */
    @Override
    public SpeechletResponse onIntent(final IntentRequest request, final Session session) {
        Optional<String> intentNameOpt = getIntentName(request);
        if(intentNameOpt.isPresent()) {
            String intentName = intentNameOpt.get();
            System.out.println("Got intent name " + intentName);

            if ("shipping_from".equals(intentName)) {
                PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
                numberone = request.getIntent().getSlot("number_from").getValue();
                streetone = request.getIntent().getSlot("street_from").getValue();
                cityone = request.getIntent().getSlot("city_from").getValue();
                stateone = request.getIntent().getSlot("state_from").getValue();
                zipone = sendAddress(numberone, streetone, cityone, stateone);
                speech.setText("What address are you shipping to?");
                Reprompt reprompt = new Reprompt();
                reprompt.setOutputSpeech(speech);
                return SpeechletResponse.newAskResponse(speech, reprompt);
            }

            else if ("shipping_to".equals(intentName)) {
                PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
                numbertwo = request.getIntent().getSlot("number_to").getValue();
                streettwo = request.getIntent().getSlot("street_to").getValue();
                citytwo = request.getIntent().getSlot("city_to").getValue();
                statetwo = request.getIntent().getSlot("state_to").getValue();
                ziptwo = sendAddress(numbertwo, streettwo, citytwo, statetwo);
                String rateResponse = executePost(numberone, streetone, cityone, stateone, zipone, numbertwo , streettwo, citytwo, statetwo, ziptwo);
                speech.setText(rateResponse);
                Reprompt reprompt = new Reprompt();
                reprompt.setOutputSpeech(speech);
                return SpeechletResponse.newTellResponse(speech);
            }
            if ("shipping_label".equals(intentName)){
                PlainTextOutputSpeech speech = new PlainTextOutputSpeech();

            }

        }

        System.out.println("Intent isn't present");
        PlainTextOutputSpeech error = new PlainTextOutputSpeech();
        error.setText("Unknown intent, exiting");
        return SpeechletResponse.newTellResponse(error);
    }

    /**
     * Handles intent or intent name being null
     * @param request Request to extract entent name from
     * @return Optional of the intent name
     */
    private Optional<String> getIntentName(IntentRequest request) {
        Intent intent = request.getIntent();
        Optional<String> intentName;
        if(null == intent.getName()) {
            return Optional.empty();
        } else {
            return Optional.of(intent.getName());
        }
    }

    public static String sendAddress(String number, String street, String city, String state)
    {
        street = street.replace(" ", "+");
        String path = "https://maps.googleapis.com/maps/api/geocode/json?address=" + (number + "+") + (street + "+") + (city + "+") + (state) + "&key= key here";


        HttpURLConnection connection = null;
        try {
            URL url = new URL(path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);


            //Send request
            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream ());
            wr.flush ();
            wr.close ();

            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return extractZip(response.toString());
        }
        catch (Exception e ){

            return "Sorry, but I could not find the address at this time. Please try again.";

        } finally {

            if(connection != null) {
                connection.disconnect();
            }
        }

    }

    public static String extractZip (String googleJson) throws  Exception
    {
        JSONObject getZip = new JSONObject(googleJson);

        JSONArray addressComponents = getZip.getJSONArray("results")
                .getJSONObject(0)
                .getJSONArray("address_components");
        for(int i = 0; i < addressComponents.length(); i++ )
        {
            JSONObject addressComponent = addressComponents.getJSONObject(i);
            JSONArray types = addressComponent.getJSONArray("types");
            for(int j = 0; j < types.length(); j++)
            {
                if(types.getString(j).equals("postal_code"))
                {
                   return addressComponent.getString("long_name");
                }
            }

        }

        return "Sorry, I couldn't find the zip code for that address";
    }
    /**
     * Perform cleanup here
     * @param sessionEndedRequest
     * @param session
     * @throws SpeechletException
     */
    @Override
    public void onSessionEnded(SessionEndedRequest sessionEndedRequest, Session session) throws SpeechletException {
        System.out.println("Session ended with session id=" + session.getSessionId());

    }

    public static String getRate(String response, String zipone, String ziptwo) throws Exception{



        JSONObject rateResponse = new JSONObject(response);

        String price = rateResponse.getJSONObject("RateResponse")
                .getJSONObject("RatedShipment")
                .getJSONObject("RatedPackage")
                .getJSONObject("TotalCharges")
                .getString("MonetaryValue");
        if(Integer.parseInt(zipone)- Integer.parseInt(ziptwo) <= 2)
        {
            return "Your average shipping cost should be about $" + price + ". Since the locations are close, it might be cheaper to do a delivery by hand. Would you like me to print a shipping label?";
        }

        return "Your average shipping cost should be about $" + price + "Would you like me to print a shipping label?";
    }

    // Make the API Call and return the String you want to tell the user
    public static String executePost(String number1, String street1, String city1, String state1, String zipCode1, String number2, String street2, String city2, String state2, String zipCode2)
    {

        URL url;
        HttpURLConnection connection = null;
        try {

            JSONObject myObject = new JSONObject(createUPSData(number1, street1,  city1,  state1,  zipCode1,  number2,  street2,  city2, state2, zipCode2));


            //Create connection
            url = new URL("https://wwwcie.ups.com/rest/Rate");
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    "application/json");


            connection.setUseCaches (false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            //Send request
            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream ());
            wr.write(myObject.toString());
            wr.flush ();
            wr.close ();

            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return getRate(response.toString(), zipCode1, zipCode2);

        } catch (Exception e) {

            return "Sorry, I could not find that address.";

        } finally {

            if(connection != null) {
                connection.disconnect();
            }
        }


    }
    public static String createUPSData(String number1, String street1, String city1, String state1, String zipCode1, String number2, String street2, String city2, String state2, String zipCode2)
    {
        String upsdata ="{\n" +
                "  \"UPSSecurity\": {\n" +
                "    \"UsernameToken\": {\n" +
                "      \"Username\": \"naztuyo\",\n" +
                "      \"Password\": \"Irazan82102!\"\n" +
                "    },\n" +
                "    \"ServiceAccessToken\": {\n" +
                "      \"AccessLicenseNumber\": \" key here\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"RateRequest\": {\n" +
                "    \"Request\": {\n" +
                "      \"RequestOption\": \"Rate\"\n" +
                "    },\n" +
                "    \"Shipment\": {\n" +
                "      \"Shipper\": {\n" +
                "        \"Name\": \"Mary Jenkins\",\n" +
                "        \"Address\": {\n" +
                "          \"AddressLine\": [\n" +
                "            \"" + number1 + " " + street1 + "\"\n" +
                "          ],\n" +
                "          \"City\": \"" + city1 + "\",\n" +
                "          \"StateProvinceCode\": \""+ state1.toUpperCase()+ "\",\n" +
                "          \"PostalCode\": \"" + zipCode1 + "\",\n" +
                "          \"CountryCode\": \"US\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"ShipTo\": {\n" +
                "        \"Name\": \"Ship To Name\",\n" +
                "        \"Address\": {\n" +
                "          \"AddressLine\": [\n" +
                "            \"" + number2 + " " + street2 + "\"\n" +
                "          ],\n" +
                "          \"City\": \"" + city2 + "\",\n" +
                "          \"StateProvinceCode\": \"" + state2.toUpperCase() + "\",\n" +
                "          \"PostalCode\": \""+ zipCode2 +"\",\n" +
                "          \"CountryCode\": \"US\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"ShipFrom\": {\n" +
                "        \"Name\": \"Ship From Name\",\n" +
                "        \"Address\": {\n" +
                "          \"AddressLine\": [\n" +
                "            \"" + number1 + " " + street1 + "\"\n" +
                "          ],\n" +
                "          \"City\": \"" + city1 +"\",\n" +
                "          \"StateProvinceCode\": \""+state1.toUpperCase()+"\",\n" +
                "          \"PostalCode\": \""+zipCode1+"\",\n" +
                "          \"CountryCode\": \"US\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"Service\": {\n" +
                "        \"Code\": \"03\",\n" +
                "        \"Description\": \"Service Code Description\"\n" +
                "      },\n" +
                "      \"Package\": {\n" +
                "        \"PackagingType\": {\n" +
                "          \"Code\": \"02\",\n" +
                "          \"Description\": \"Rate\"\n" +
                "        },\n" +
                "        \"Dimensions\": {\n" +
                "          \"UnitOfMeasurement\": {\n" +
                "            \"Code\": \"IN\",\n" +
                "            \"Description\": \"inches\"\n" +
                "          },\n" +
                "          \"Length\": \"5\",\n" +
                "          \"Width\": \"4\",\n" +
                "          \"Height\": \"3\"\n" +
                "        },\n" +
                "        \"PackageWeight\": {\n" +
                "          \"UnitOfMeasurement\": {\n" +
                "            \"Code\": \"Lbs\",\n" +
                "            \"Description\": \"pounds\"\n" +
                "          },\n" +
                "          \"Weight\": \"1\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        return upsdata;

//        String upsdata ="{\n" +
//                "  \"UPSSecurity\": {\n" +
//                "    \"UsernameToken\": {\n" +
//                "      \"Username\": \"naztuyo\",\n" +
//                "      \"Password\": \"Irazan82102!\"\n" +
//                "    },\n" +
//                "    \"ServiceAccessToken\": {\n" +
//                "      \"AccessLicenseNumber\": \"1D4D17BFA91711A8\"\n" +
//                "    }\n" +
//                "  },\n" +
//                "  \"RateRequest\": {\n" +
//                "    \"Request\": {\n" +
//                "      \"RequestOption\": \"Rate\"\n" +
//                "    },\n" +
//                "    \"Shipment\": {\n" +
//                "      \"Shipper\": {\n" +
//                "        \"Name\": \"Mary Jenkins\",\n" +
//                "        \"ShipperNumber\": \"0000000000\",\n" +
//                "        \"Address\": {\n" +
//                "          \"AddressLine\": [\n" +
//                "            \"" + number1 + " " + street1 + "\"\n" +
//                "          ],\n" +
//                "          \"City\": \"" + city1 + "\",\n" +
//                "          \"StateProvinceCode\": \"" + state1 + "\",\n" +
//                "          \"PostalCode\": \"" + zipCode1 +"\",\n" +
//                "          \"CountryCode\": \"US\"\n" +
//                "        }\n" +
//                "      },\n" +
//                "      \"ShipTo\": {\n" +
//                "        \"Name\": \"Mary Jenkins 2 \",\n" +
//                "        \"Address\": {\n" +
//                "          \"AddressLine\": [\n" +
//                "            \""+ number2 + " " + street2 + " \"\n" +
//                "          ],\n" +
//                "          \"City\": \"" + city2 + "\",\n" +
//                "          \"StateProvinceCode\": \"" + state2 + "\",\n" +
//                "          \"PostalCode\": \"" + zipCode2 +"\",\n" +
//                "          \"CountryCode\": \"US\"\n" +
//                "        }\n" +
//                "      },\n" +
//                "      \"ShipFrom\": {\n" +
//                "        \"Name\": \"Ship From Name\",\n" +
//                "        \"Address\": {\n" +
//                "          \"AddressLine\": [\n" +
//                "            \""+ number1 + " " + street1 +" \"\n" +
//                "          ],\n" +
//                "          \"City\": \"" + city1 + "\",\n" +
//                "          \"StateProvinceCode\": \"" + state1 + "\",\n" +
//                "          \"PostalCode\": \"" + zipCode1 +"\",\n" +
//                "          \"CountryCode\": \"US\"\n" +
//                "        }\n" +
//                "      },\n" +
//                "      \"Service\": {\n" +
//                "        \"Code\": \"03\",\n" +
//                "        \"Description\": \"Service Code Description\"\n" +
//                "      },\n" +
//                "      \"Package\": {\n" +
//                "        \"PackagingType\": {\n" +
//                "          \"Code\": \"02\",\n" +
//                "          \"Description\": \"Rate\"\n" +
//                "        },\n" +
//                "        \"Dimensions\": {\n" +
//                "          \"UnitOfMeasurement\": {\n" +
//                "            \"Code\": \"IN\",\n" +
//                "            \"Description\": \"inches\"\n" +
//                "          },\n" +
//                "          \"Length\": \"5\",\n" +
//                "          \"Width\": \"4\",\n" +
//                "          \"Height\": \"3\"\n" +
//                "        },\n" +
//                "        \"PackageWeight\": {\n" +
//                "          \"UnitOfMeasurement\": {\n" +
//                "            \"Code\": \"Lbs\",\n" +
//                "            \"Description\": \"pounds\"\n" +
//                "          },\n" +
//                "          \"Weight\": \"1\"\n" +
//                "        }\n" +
//                "      }\n" +
//                "    }\n" +
//                "  }\n" +
//                "}";
//        return upsdata;
    }
}
