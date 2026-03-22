package com.ecodana.evodanavn1.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Component
public class PayOSClient {
    
    @Value("${payos.client.id}")
    private String clientId;
    
    @Value("${payos.api.key}")
    private String apiKey;
    
    @Value("${payos.checksum.key}")
    private String checksumKey;
    
    private static final String BASE_URL = "https://api-merchant.payos.vn/v2";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public PayOSClient() {
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
    }
    
    public String createPaymentLink(long amount, String orderCode, String description, 
                                  String returnUrl, String cancelUrl) throws Exception {
        String url = BASE_URL + "/payment-requests";
        
        // Build items array (required by PayOS)
        java.util.List<Map<String, Object>> items = new java.util.ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        // Ensure item name is also max 25 chars
        String itemName = description.length() > 25 ? description.substring(0, 25) : description;
        item.put("name", itemName);
        item.put("quantity", 1);
        item.put("price", amount);
        items.add(item);
        
        // Build request body following PayOS API spec
        Map<String, Object> requestBody = new TreeMap<>(); // Use TreeMap for sorted keys
        // FIX: Use the passed-in orderCode, converted to a number
        requestBody.put("orderCode", Long.parseLong(orderCode));
        requestBody.put("amount", amount);
        // Ensure description is max 25 chars
        String shortDescription = description.length() > 25 ? description.substring(0, 25) : description;
        requestBody.put("description", shortDescription);
        requestBody.put("items", items);
        requestBody.put("returnUrl", returnUrl);
        requestBody.put("cancelUrl", cancelUrl);
        
        // Generate signature
        String signature = generateSignature(requestBody);
        requestBody.put("signature", signature);
        
        return sendPostRequest(url, requestBody);
    }
    
    private String generateSignature(Map<String, Object> data) throws Exception {
        // PayOS signature format: amount=xxx&cancelUrl=xxx&description=xxx&orderCode=xxx&returnUrl=xxx
        // Only include specific fields in specific order
        StringBuilder signData = new StringBuilder();
        
        // Add fields in alphabetical order (PayOS requirement)
        if (data.containsKey("amount")) {
            signData.append("amount=").append(data.get("amount"));
        }
        if (data.containsKey("cancelUrl")) {
            signData.append("&cancelUrl=").append(data.get("cancelUrl"));
        }
        if (data.containsKey("description")) {
            signData.append("&description=").append(data.get("description"));
        }
        if (data.containsKey("orderCode")) {
            signData.append("&orderCode=").append(data.get("orderCode"));
        }
        if (data.containsKey("returnUrl")) {
            signData.append("&returnUrl=").append(data.get("returnUrl"));
        }
        
        System.out.println("=== Signature Data ===");
        System.out.println("String to sign: " + signData.toString());
        System.out.println("Checksum key: " + checksumKey);
        
        // Generate HMAC SHA256 signature
        String signature = HmacUtils.hmacSha256Hex(checksumKey, signData.toString());
        System.out.println("Generated signature: " + signature);
        
        return signature;
    }
    
    public String getPaymentLinkInfo(String orderCode) throws Exception {
        String url = BASE_URL + "/payment-requests/" + orderCode;
        return sendGetRequest(url);
    }
    
    public String cancelPaymentLink(String orderCode) throws Exception {
        String url = BASE_URL + "/payment-requests/" + orderCode + "/cancel";
        return sendPostRequest(url, new HashMap<>());
    }
    
    public String refundPayment(String transactionId, long amount, String reason) throws Exception {
        String url = BASE_URL + "/refunds";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("transactionId", transactionId);
        requestBody.put("amount", amount);
        requestBody.put("reason", reason);
        
        return sendPostRequest(url, requestBody);
    }
    
    private String sendPostRequest(String url, Map<String, Object> requestBody) throws Exception {
        HttpPost request = new HttpPost(url);
        
        // Add headers
        request.setHeader("Content-Type", "application/json");
        request.setHeader("x-client-id", clientId);
        request.setHeader("x-api-key", apiKey);
        
        // Add request body
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        System.out.println("=== PayOS Request ===");
        System.out.println("URL: " + url);
        System.out.println("Headers: x-client-id=" + clientId + ", x-api-key=" + apiKey);
        System.out.println("Body: " + jsonBody);
        
        request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
        
        // Execute request
        HttpResponse response = httpClient.execute(request);
        String responseBody = EntityUtils.toString(response.getEntity());
        
        System.out.println("=== PayOS Response ===");
        System.out.println("Status: " + response.getStatusLine().getStatusCode());
        System.out.println("Body: " + responseBody);
        
        return responseBody;
    }
    
    private String sendGetRequest(String url) throws Exception {
        HttpGet request = new HttpGet(url);
        
        // Add headers
        request.setHeader("Content-Type", "application/json");
        request.setHeader("x-client-id", clientId);
        request.setHeader("x-api-key", apiKey);
        
        // Execute request
        HttpResponse response = httpClient.execute(request);
        return EntityUtils.toString(response.getEntity());
    }
}
