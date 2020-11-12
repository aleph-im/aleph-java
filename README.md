# Aleph JAVA API (Ethereum)

## Introduction

With this implementation you can interact with the Aleph API in JAVA :

- Aggregates (key-value) 
- Posts (document-like)
- Store (store files/files hashes.)

It's an attempt to transition the [JS API](https://github.com/aleph-im/aleph-js) to JAVA.

## Limitations

This implementation only works for Ethereum accounts currently.

## Documentation
The API server can be changed with `<your_aleph_instance>.changeAPI(NEW_API_ADDRESS)`

For different requests : 
- Aggregate : `AlephAggregate` class
- Posts : `AlephPosts` class
- Store : `AlephStore` class


Example for Aggregate :

```java
        AlephAggregate agg = new AlephAggregate();
        Credentials creds = Credentials.create(PRIVATE_KEY);
        final String PUBLIC_ADDRESS; // your public address, creds.getAddress() won't match the case sensitive Aleph address
        
        // {"KEY" : {"json" : "value"}} at PUBLIC_ADDRESS
        agg.submit(PUBLIC_ADDRESS, cred.getEcKeyPair(), "CHANNEL", "KEY", "{\"json\": \"value\"}", true /* ipfs */ , false /* inline */); 

        // Get "KEY" at PUBLIC_ADDRESS
        System.out.println(agg.fetch(PUBLIC_ADDRESS, "KEY"));

```

The functions are documented and were reproduced as close as possible as the [aleph-js ones](https://aleph-im.github.io/aleph-js/guide/getting-started.html) without taking a JSON object as a parameter.

Prototypes :
<details>
  <summary>Aggregate</summary>

```java
public JSONObject submit(String address, ECKeyPair account, String channel, String key, String content, Boolean ipfs, Boolean inline) throws ParseException, IOException
public JSONObject fetch(String address, String... keys) throws ParseException, IOException
public JSONObject fetch_one(String address, String key) throws ParseException, IOException
```
     
</details>

<details>
  <summary>Posts</summary>

```java
public JSONObject submit(String address, ECKeyPair account, String channel, String type, String content, Boolean ipfs, Boolean inline, String ref) throws ParseException, IOException;    
public JSONArray get_posts(String[] types, String[] refs, String[] addresses, String[] tags, String[] hashes, int pagination, int page) throws ParseException, IOException
```
</details>


<details>
  <summary>Store</summary>

```java
public JSONObject submit(String address, ECKeyPair account, String channel, File file, String fileHash, boolean hash, Boolean ipfs) throws ParseException, IOException
public byte[] getFile(String hash) throws IOException
```

</details>



## Credits :

This development is at the initiative of the EvidenZ.io and this project exists thanks to all the people who contribute.
 

 
