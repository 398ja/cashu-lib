package cashu.mint.gateway.mock;

import cashu.common.model.PaymentMethod;
import cashu.mint.gateway.Gateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

public class MockGateway implements Gateway {

    private static final String BASE_URL = "http://localhost:7777";

    @Override
    public String createRequest(int amount) {
        return UUID.randomUUID().toString();
    }

    @Override
    public String getRequest(@NonNull String quoteId) {
        var url = BASE_URL + "/mint/quote/" + quoteId;
        try {
            HttpURLConnection con = (HttpURLConnection) new URI(url).toURL().openConnection();
            con.setRequestMethod("GET");
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return new ObjectMapper().readTree(con.getInputStream()).get("request").asText();
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Failed to get request");
    }

    @Override
    public boolean checkPaymentStatus(@NonNull String quoteId) {
        return Math.random() < .85;
    }

    @Override
    public String getPaymentPreimage(@NonNull String quoteId) {
        return "Preimage_" + quoteId;
    }

    @Override
    public void pay(@NonNull String quoteId) {
    }

    @Override
    public int getAmount(@NonNull String quoteId) {
        return 96;
    }

    @Override
    public int getPaymentExpiry(@NonNull String quoteId) {
        return (int) (System.currentTimeMillis() / 1000L + 60 * 60 * 24);
    }

    @Override
    public int getFeeReserve(@NonNull String requestId) {
        return 0;
    }

    @Override
    public String getMethod() {
        return PaymentMethod.MOCK.name().toLowerCase();
    }
}