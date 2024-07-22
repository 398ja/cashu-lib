package cashu.mint.gateway.mock;

import cashu.common.model.PaymentMethod;
import cashu.mint.gateway.Gateway;
import cashu.util.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
public abstract class MockGateway implements Gateway {

    private static final String BASE_URL = "http://localhost:7777";

    private final String code;
    private final String operation;

    @Override
    public String createRequest(int amount) {
        return UUID.randomUUID().toString();
    }

    @Override
    public String getRequest(@NonNull String quoteId) {
        var url = BASE_URL + "/" + operation + "/quote/" + quoteId;
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
        return getPaymentStatus();
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
        return getAmount();
    }

    @Override
    public int getPaymentExpiry(@NonNull String quoteId) {
        return (int) (System.currentTimeMillis() / 1000L + getExpiry());
    }

    @Override
    public String getMethod() {
        return PaymentMethod.MOCK.name().toLowerCase();
    }

    protected String getParameter(@NonNull String name) {
        Configuration configuration = Configuration.load(Objects.requireNonNull(MockGateway.class.getResourceAsStream("/gateway_" + code + "_" + operation + ".properties")));
        return configuration.getValue(name);
    }

    private Integer getAmount() {
        return Integer.valueOf(getParameter("amount"));
    }

    private Integer getExpiry() {
        return Integer.valueOf(getParameter("expiry"));
    }

    private boolean getPaymentStatus() {
        String paymentStatus = getParameter("payment_status");
        int status = Integer.parseInt(paymentStatus);
        return Math.random() < status / 100.0;
    }
}