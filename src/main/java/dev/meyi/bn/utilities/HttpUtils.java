package dev.meyi.bn.utilities;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.meyi.bn.json.resp.BazaarResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Utility class for HTTP operations and API calls.
 */
public final class HttpUtils {
    
    private static final String HYPIXEL_BAZAAR_API = "https://api.hypixel.net/v2/skyblock/bazaar";
    private static final String MOJANG_UUID_API = "https://api.mojang.com/users/profiles/minecraft/";
    
    // Trust manager that accepts all certificates (for GitHub resources)
    private static final TrustManager[] TRUST_ALL_CERTS = new TrustManager[] {
        new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                // Accept all client certificates
            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                // Accept all server certificates
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        }
    };

    public static BazaarResponse fetchBazaarData() {
        try (CloseableHttpClient client = createStandardHttpClient()) {
            HttpGet request = new HttpGet(HYPIXEL_BAZAAR_API);
            HttpResponse response = client.execute(request);
            
            String result = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            
            if (isValidJson(result)) {
                Gson gson = new Gson();
                return gson.fromJson(result, BazaarResponse.class);
            } else {
                return new BazaarResponse(false, 0, null);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new BazaarResponse(false, 0, null);
        }
    }

    public static String fetchPlayerUuid(String username) {
        try (CloseableHttpClient client = createStandardHttpClient()) {
            HttpGet request = new HttpGet(MOJANG_UUID_API + username);
            HttpResponse response = client.execute(request);
            
            String uuidResponse = IOUtils.toString(
                new BufferedReader(new InputStreamReader(response.getEntity().getContent())));
            
            Gson gson = new Gson();
            JsonObject jsonResponse = gson.fromJson(uuidResponse, JsonObject.class);
            return jsonResponse.get("id").getAsString();
            
        } catch (IOException | JsonSyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JsonObject fetchJsonWithTrustAll(String url) {
        try (CloseableHttpClient client = createTrustAllHttpClient()) {
            HttpGet request = new HttpGet(url);
            HttpResponse response = client.execute(request);
            
            String content = IOUtils.toString(new BufferedReader(
                new InputStreamReader(response.getEntity().getContent())));
            
            if (isValidJson(content)) {
                Gson gson = new Gson();
                return gson.fromJson(content, JsonObject.class);
            }
            
        } catch (IOException | KeyManagementException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        
        return null;
    }

    public static String fetchStringContent(String url) {
        try (CloseableHttpClient client = createStandardHttpClient()) {
            HttpGet request = new HttpGet(url);
            HttpResponse response = client.execute(request);
            
            return IOUtils.toString(new BufferedReader(
                new InputStreamReader(response.getEntity().getContent())));
                
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static CloseableHttpClient createStandardHttpClient() {
        return HttpClientBuilder.create().build();
    }

    private static CloseableHttpClient createTrustAllHttpClient() 
            throws KeyManagementException, NoSuchAlgorithmException {
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, TRUST_ALL_CERTS, new java.security.SecureRandom());
        
        return HttpClientBuilder.create()
            .setSslcontext(sslContext)
            .build();
    }

    public static boolean isValidJson(String jsonString) {
        try {
            Gson gson = new Gson();
            gson.fromJson(jsonString, JsonObject.class);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }
}
