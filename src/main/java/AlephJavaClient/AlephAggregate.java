package AlephJavaClient;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.web3j.crypto.ECKeyPair;

import java.io.IOException;


@SuppressWarnings("unchecked")
public class AlephAggregate extends Aleph {

    /**
     *
     * @param address address on which to fetch
     * @param key key to fetch
     * @return value from key
     * @throws ParseException JSON parsing
     */
    public JSONObject fetch_one(String address, String key) throws ParseException, IOException {
        String response= getRequest(API_SERVER + "/api/v0/aggregates/" + address + ".json?keys=" + key);

        JSONParser parser = new JSONParser();
        JSONObject result = (JSONObject) parser.parse(response);
        if (result.containsKey("data")) {
            result = (JSONObject) result.get("data");
            if (result.containsKey(key))
                return (JSONObject) result.get(key);
        }
        return null;
    }

    /**
     *
     * @param address address on which to fetch
     * @param keys keys to fetch
     * @return return keys and values
     * @throws ParseException JSON parsing
     */

    public JSONObject fetch(String address, String... keys) throws ParseException, IOException {
        final String url = API_SERVER + "/api/v0/aggregates/" + address + ".json?keys=" + String.join(",", keys);
        String response = getRequest(url);

        JSONParser parser = new JSONParser();
        JSONObject result = (JSONObject) parser.parse(response);
        if (result.containsKey("data"))
            return (JSONObject) result.get("data");
        return null;
    }

    /**
     * Submit [key : {values}] into ALEPH (and ipfs if you want to)
     * @param address Sender's address
     * @param account Private and Public key address used to sign the tx
     * @param channel channel to send to
     * @param key key in key-value
     * @param content value in key-value : MUST BE JSON! (can contain multiple values)
     * @param ipfs Will it be posted on ipfs or not ? Put inline in false in order to post it to ipfs
     * @param inline Should the message be stored as a separate file or inserted inline - in alephJS default is true.
     * @return message sent
     * @throws ParseException JSON parsing
     * @throws IOException createSignature
     */
    public JSONObject submit(String address, ECKeyPair account, String channel, String key, String content, Boolean ipfs, Boolean inline) throws ParseException, IOException {
        JSONObject JSONcontent = new JSONObject();
        JSONObject JSONmessage = new JSONObject();

        JSONcontent.put("address", address);
        JSONcontent.put("key", key);

        try {
            JSONParser parser = new JSONParser();
            JSONcontent.put("content", parser.parse(content));
        } catch (ParseException e) {
            System.out.println("Aborting, error parsing value content (should be JSON): " + e.toString());
            return JSONmessage; // empty
        }

        float time = (float) (System.currentTimeMillis() / 1000.0);
        JSONcontent.put("time", time);

        JSONmessage.put("chain", "ETH");
        JSONmessage.put("channel", channel);
        JSONmessage.put("sender", address);
        JSONmessage.put("type", "AGGREGATE");
        JSONmessage.put("time", time);

        JSONmessage = putContent(JSONmessage, JSONcontent, inline, ipfs);

        // Signing
        String msgToSign =  "ETH\n" + address + "\nAGGREGATE\n" + JSONmessage.get("item_hash");
        try {
            JSONmessage.put("signature", createSignature(msgToSign, account));
        } catch (Exception e) {
            System.out.println("Aborting, error signing tx: " + e.toString());
            return new JSONObject();
        }
        String broadcastResult = broadcast(JSONmessage.toJSONString());
        System.out.println("Transaction status : " + broadcastResult);
        return JSONmessage;
    }


}
