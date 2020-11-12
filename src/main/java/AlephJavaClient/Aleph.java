package AlephJavaClient;

import okhttp3.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

@SuppressWarnings("unchecked")
public class Aleph {
    final String ETH_SIG_PREFIX_STR = "\u0019Ethereum Signed Message:\n";
    String API_SERVER = "https://api1.aleph.im";
    OkHttpClient client = new OkHttpClient();

    protected String getRequest(String url) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful())
                return Objects.requireNonNull(response.body().string());
            else
                System.out.println("GET request error");
        } catch (Exception e) {
            System.out.println("GET request error: " + e);
        }
        return null;
    }
//${api_server}/api/v0/storage/raw/${file_hash}?find



    protected String postRequest(String url , String json) {
        RequestBody body = RequestBody.create(json,
                MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return Objects.requireNonNull(response.body()).string();
        } catch (Exception e) {
            System.out.println("POST request error: " + e);
            return null;
        }
    }

    protected String postFileRequest(String url, File file) throws IOException {
        String mime = Files.probeContentType(file.toPath());
        String fileType = mime == null ? "application/octet-stream" : mime;

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file",
                        file.getName(),
                        RequestBody.create(file, MediaType.parse(fileType)))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return Objects.requireNonNull(response.body()).string();
        } catch (Exception e) {
            System.out.println("POST request error: " + e);
            return null;
        }
    }

    /**
     *
     * @param value value
     * @param storage where to store (storage or ipfs)
     * @return  return content hash
     * @throws ParseException json parsing
     */
    protected String push(String value, String storage) throws ParseException {
        String url = API_SERVER + "/api/v0/" + storage.toLowerCase() + "/add_json";
        String stringResponse = postRequest(url, value);

        JSONParser parser = new JSONParser();
        JSONObject response = (JSONObject) parser.parse(stringResponse);
        if (response.containsKey("hash"))
                return (String) response.get("hash");
        System.out.println("Error pushing to " + storage);
        return null;
    }

    protected String push(File file, String storage) throws ParseException, IOException {
        String url = API_SERVER + "/api/v0/" + storage.toLowerCase() + "/add_file";
        String stringResponse = postFileRequest(url, file);

        JSONParser parser = new JSONParser();
        JSONObject response = (JSONObject) parser.parse(stringResponse);
        if (response.containsKey("hash"))
            return (String) response.get("hash");
        System.out.println("Error pushing to " + storage);
        return null;
    }


    protected String broadcast(String message) throws ParseException {
        JSONObject value = new JSONObject();

        value.put("topic", "ALEPH-TEST");
        value.put("data", message);

        String stringResponse = postRequest(API_SERVER + "/api/v0/ipfs/pubsub/pub", value.toJSONString());
        JSONParser parser = new JSONParser();
        JSONObject response = (JSONObject) parser.parse(stringResponse);
        return (String) response.get("status");
    }

    protected JSONObject putContent(JSONObject message,
                                 JSONObject aggregate_content,
                                 Boolean inline,
                                 Boolean ipfs) throws  ParseException {
        String serialized = aggregate_content.toJSONString();

        if (inline && serialized.length() <= 150000) {
            message.put("item_type", "inline");
            message.put("item_content", serialized);
            message.put("item_hash", DigestUtils.sha256Hex(serialized));
        }
        else {
            String storage = ipfs ? "ipfs" : "storage";
            message.put("item_type", storage);
            String hash = push(serialized, storage);
            message.put("item_hash", hash);
            System.out.println("Hash (" + storage + "): " + hash);
        }
        return message;
    }

    protected String createSignature(String message, ECKeyPair keyPair) throws IOException {
        String messageWithPrefix = ETH_SIG_PREFIX_STR + message.length() + message;

        Sign.SignatureData sigRVS = Sign.signMessage(messageWithPrefix.getBytes(), keyPair);

        ByteArrayOutputStream concat = new ByteArrayOutputStream();
        concat.write(sigRVS.getR());
        concat.write(sigRVS.getS());
        concat.write(sigRVS.getV()[0] != 27 ? 0x1c : 0x1b);

        byte[] bytes = concat.toByteArray();
        StringBuilder result_builder = new StringBuilder();
        for (byte b : bytes) {
            String st = String.format("%02x", b);
            result_builder.append(st);
        }

        String result = result_builder.toString();
        System.out.println("Signature : " + result);
        return result;
    }

    public void changeAPI(String newAPI) {
        API_SERVER = newAPI;
    }
}
