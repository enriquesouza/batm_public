package com.generalbytes.batm.server.extensions.extra.smartcash.exchanges.lib.exmo;

/*
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import si.mazi.rescu.SynchronizedValueFactory;

// todo: strongly type the returned values

@Path("v1")
public interface Exmo {

  @GET
  @Path("/trades")
  Map<String, List<Map>> trades(@QueryParam("pair") String pair);

  @POST
  @Path("/ticker")
  Map<String, Map<String, String>> ticker() throws IOException;

  @POST
  @Path("/pair_settings")
  Map<String, Map<String, String>> pairSettings() throws IOException;

  @POST
  @Path("/order_book/")
  Map<String, Map<String, Object>> orderBook(@QueryParam("pair") String pair) throws IOException;

  @POST
  @Path("/user_info/")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  Map userInfo(
      @HeaderParam("Sign") ExmoDigest signatureCreator,
      @HeaderParam("Key") String publicKey,
      @FormParam("nonce") SynchronizedValueFactory<Long> nonceFactory);

  @POST
  @Path("/order_create/")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  Map orderCreate(
      @HeaderParam("Sign") ExmoDigest signatureCreator,
      @HeaderParam("Key") String publicKey,
      @FormParam("nonce") SynchronizedValueFactory<Long> nonceFactory,
      @FormParam("pair") String pair,
      @FormParam("quantity") BigDecimal quantity,
      @FormParam("price") BigDecimal price,
      @FormParam("type") String type);

  @POST
  @Path("/order_cancel/")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  Map orderCancel(
      @HeaderParam("Sign") ExmoDigest signatureCreator,
      @HeaderParam("Key") String publicKey,
      @FormParam("nonce") SynchronizedValueFactory<Long> nonceFactory,
      @FormParam("order_id") String orderId);

  @POST
  @Path("/user_open_orders/")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  Map<String, List<Map<String, String>>> userOpenOrders(
      @HeaderParam("Sign") ExmoDigest signatureCreator,
      @HeaderParam("Key") String publicKey,
      @FormParam("nonce") SynchronizedValueFactory<Long> nonceFactory);

  @POST
  @Path("/user_trades/")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  Map<String, List<Map<String, String>>> userTrades(
      @HeaderParam("Sign") ExmoDigest signatureCreator,
      @HeaderParam("Key") String publicKey,
      @FormParam("nonce") SynchronizedValueFactory<Long> nonceFactory,
      @FormParam("pair") String pair,
      @FormParam("offset") Long offset,
      @FormParam("limit") Integer limit);

  @POST
  @Path("/order_trades/")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  Map<String, Object> orderTrades(
      @HeaderParam("Sign") ExmoDigest signatureCreator,
      @HeaderParam("Key") String publicKey,
      @FormParam("nonce") SynchronizedValueFactory<Long> nonceFactory,
      @FormParam("order_id") String orderId);

  @POST
  @Path("/deposit_address/")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  Map<String, String> depositAddress(
      @HeaderParam("Sign") ExmoDigest signatureCreator,
      @HeaderParam("Key") String publicKey,
      @FormParam("nonce") SynchronizedValueFactory<Long> nonceFactory);

  @POST
  @Path("/wallet_history/")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  Map<String, Object> walletHistory(
      @HeaderParam("Sign") ExmoDigest signatureCreator,
      @HeaderParam("Key") String publicKey,
      @FormParam("nonce") SynchronizedValueFactory<Long> nonceFactory,
      @QueryParam("date") long date);

  @POST
  @Path("/withdraw_crypt/")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  Map<String, Object> withdrawCrypt(
      @HeaderParam("Sign") ExmoDigest signatureCreator,
      @HeaderParam("Key") String publicKey,
      @FormParam("nonce") SynchronizedValueFactory<Long> nonceFactory,
      @QueryParam("amount") BigDecimal amount,
      @QueryParam("currency") String currency,
      @QueryParam("address") String address,
      @QueryParam("invoice") String invoice);
}
*/
/**
 * Created by Admin on 2/18/2016.
 */

import okhttp3.*;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class Exmo {
    private static long _nonce;
    private String _key;
    private String _secret;

    public Exmo(String key, String secret) {
        _nonce = Instant.now().toEpochMilli();
        _key = key;
        _secret = secret;
    }

    public final String Request(String method, Map<String, String> arguments) {
        Mac mac;
        SecretKeySpec key;
        String sign;
        this._nonce = Instant.now().toEpochMilli();

        if (arguments == null) {  // If the user provided no arguments, just create an empty argument array.
            arguments = new HashMap<>();
        }

        arguments.put("nonce", "" + ++_nonce);  // Add the dummy nonce.

        String postData = "";

        for (Map.Entry<String, String> stringStringEntry : arguments.entrySet()) {
            Map.Entry argument = (Map.Entry) stringStringEntry;

            if (postData.length() > 0) {
                postData += "&";
            }
            postData += argument.getKey() + "=" + argument.getValue();
        }

        // Create a new secret key
        try {
            key = new SecretKeySpec(_secret.getBytes("UTF-8"), "HmacSHA512");
        } catch (UnsupportedEncodingException uee) {
            System.err.println("Unsupported encoding exception: " + uee.toString());
            return null;
        }

        // Create a new mac
        try {
            mac = Mac.getInstance("HmacSHA512");
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("No such algorithm exception: " + nsae.toString());
            return null;
        }

        // Init mac with key.
        try {
            mac.init(key);
        } catch (InvalidKeyException ike) {
            System.err.println("Invalid key exception: " + ike.toString());
            return null;
        }


        // Encode the post data by the secret and encode the result as base64.
        try {
            sign = Hex.encodeHexString(mac.doFinal(postData.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException uee) {
            System.err.println("Unsupported encoding exception: " + uee.toString());
            return null;
        }

        // Now do the actual request
        MediaType form = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");

        OkHttpClient client = new OkHttpClient();
        try {

            RequestBody body = RequestBody.create(form, postData);
            Request request = new Request.Builder()
                    .url("https://api.exmo.com/v1/" + method)
                    .addHeader("Key", _key)
                    .addHeader("Sign", sign)
                    .post(body)
                    .build();
            
            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            System.err.println("Request fail: " + e.toString());
            return null;  // An error occured...
        }
    }
}