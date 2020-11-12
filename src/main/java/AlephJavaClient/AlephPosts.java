package AlephJavaClient;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.web3j.crypto.ECKeyPair;
import java.io.IOException;

@SuppressWarnings("unchecked")
public class AlephPosts extends Aleph {

    /**
     *
     * @param types types to get
     * @param refs default: null
     * @param addresses default: null
     * @param tags default: null
     * @param hashes default: null
     * @param pagination default: 200
     * @param page default: 1
     * @return result
     */
    public JSONArray get_posts(String[] types, String[] refs, String[] addresses, String[] tags, String[] hashes, int pagination, int page) throws ParseException, IOException {
        if (types == null)
            return new JSONArray();
        String URL = API_SERVER + "/api/v0/posts.json?types=" + String.join(",", types);

        if (refs != null)
            URL += "&refs=" + String.join(",", refs);

        if (addresses != null)
            URL += "&addresses=" + String.join(",", addresses);

        if (tags != null)
            URL += "&tags=" + String.join(",", tags);

        if (hashes != null)
            URL += "&hashes=" + String.join(",", hashes);

        URL += "&pagination=" + (pagination <= 0 ? "200" : pagination);
        URL += "&page=" + (page <= 0 ? "1" : page);

        String response = getRequest(URL);
        JSONParser parser = new JSONParser();
        JSONObject result = (JSONObject) parser.parse(response);
        if (result.containsKey("posts"))
            return (JSONArray) result.get("posts");
        return null;
    }

    /**
     *
     * @param address sender's address
     * @param account Private and Public key address used to sign the tx
     * @param channel channel to send to
     * @param type type of doc
     * @param content content of file
     * @param ipfs send to ipfs ?
     * @param inline Should the message be stored as a separate file or inserted inline - in alephJS default is true.
     * @param ref reference
     * @return message sent
     * @throws ParseException JSON parsing
     * @throws IOException Input/Output exception
     */
    public JSONObject submit(String address, ECKeyPair account, String channel, String type, String content, Boolean ipfs, Boolean inline, String ref) throws ParseException, IOException {
        JSONObject JSONcontent = new JSONObject();
        JSONObject JSONmessage = new JSONObject();
        float time = (float) (System.currentTimeMillis() / 1000.0);

        JSONcontent.put("address", address);
        JSONcontent.put("type", type);
        JSONcontent.put("time", time);

        try {
            JSONParser parser = new JSONParser();
            JSONcontent.put("content", parser.parse(content));
        } catch (ParseException e) {
            System.out.println("Aborting, error parsing value content (should be JSON): " + e.toString());
            return JSONmessage; // empty
        }

        if (ref != null)
            JSONcontent.put("ref", ref);

        JSONmessage.put("chain", "ETH");
        JSONmessage.put("channel", channel);
        JSONmessage.put("sender", address);
        JSONmessage.put("type", "POST");
        JSONmessage.put("time", time);

        JSONmessage = putContent(JSONmessage, JSONcontent, inline, ipfs);

        // Signing
        String msgToSign =  "ETH\n" + address + "\nPOST\n" + JSONmessage.get("item_hash");
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
