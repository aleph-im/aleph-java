package AlephJavaClient;

import okhttp3.Request;
import okhttp3.Response;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.web3j.crypto.ECKeyPair;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class AlephStore extends Aleph {

    /**
     *
     * @param hash file hash to get
     * @return file's byes
     * @throws IOException input/output error
     */
    public byte[] getFile(String hash) throws IOException {
        String url = API_SERVER + "/api/v0/storage/raw/" + hash + "?find";
        Request request = new Request.Builder().url(url).build();

        try (Response execResponse = client.newCall(request).execute()) {
            if (!execResponse.isSuccessful()) {
                System.out.println("No such file.");
                return null;
            }

            InputStream response = Objects.requireNonNull(execResponse.body()).byteStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];

            while ((nRead = response.read(data, 0, data.length)) != -1)
                buffer.write(data, 0, nRead);
            buffer.flush();
            buffer.close();

            return buffer.toByteArray();
        } catch (Exception e) {
            System.out.println("Error downloading file: " + e);
        }
        return null;
    }

    /**
     *
     * @param address sender's address
     * @param account private/public key
     * @param channel channel on which to send
     * @param file File class to send if it's a file
     * @param fileHash File hash to send if it's a hash
     * @param hash sending hash (true) ? or file (false) ?
     * @param ipfs send to ipfs ?
     * @return message sent
     * @throws ParseException JSON parsing
     * @throws IOException input/output error
     */
    public JSONObject submit(String address, ECKeyPair account, String channel, File file, String fileHash, boolean hash, Boolean ipfs) throws ParseException, IOException {
        JSONObject JSONcontent = new JSONObject();

        if ((!hash && file == null ) || (hash && (fileHash == null || fileHash.isEmpty()))) {
            System.out.println("Please provide file hash or file content.");
            return JSONcontent;
        }

        if (!hash)
           fileHash = push(file, (ipfs ? "ipfs" : "storage"));
        if (fileHash == null) {
            System.out.println("Upload error.");
            return JSONcontent;
        }

        float time = (float) (System.currentTimeMillis() / 1000.0);
        JSONcontent.put("address", address);
        JSONcontent.put("item_type", ipfs ? "ipfs" : "storage");
        JSONcontent.put("item_hash", fileHash);
        JSONcontent.put("time", time);

        JSONObject JSONmessage = new JSONObject();
        JSONmessage.put("chain", "ETH");
        JSONmessage.put("channel", channel);
        JSONmessage.put("sender", address);
        JSONmessage.put("type", "STORE");
        JSONmessage.put("time", time);

        JSONmessage = putContent(JSONmessage, JSONcontent, true, ipfs);

        String msgToSign =  "ETH\n" + address + "\nSTORE\n" + fileHash;
        try {
            JSONmessage.put("signature", createSignature(msgToSign, account));
        } catch (Exception e) {
            System.out.println("Aborting, error signing tx: " + e.toString());
            return new JSONObject();
        }

        String broadcastResult = broadcast(JSONmessage.toJSONString());
        System.out.println("Transaction status : " + broadcastResult);

        JSONmessage.put("content", JSONcontent);

        return JSONmessage;
    }

}