package cashu.mint.gateway;

public interface Gateway {

    String createRequest(int amount);

    String getRequest(String quoteId);

    boolean checkPaymentStatus(String quoteId);

    String getPaymentPreimage(String quoteId);

    void pay(String quoteId);

    int getAmount(String quoteId);

    int getPaymentExpiry(String quoteId);

    int getFeeReserve(String requestId);

    String getMethod();
}
