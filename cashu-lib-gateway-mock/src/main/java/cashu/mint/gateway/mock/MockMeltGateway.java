package cashu.mint.gateway.mock;

import lombok.NonNull;

public class MockMeltGateway extends MockGateway {
    public MockMeltGateway(@NonNull String code) {
        super(code, "melt");
    }

    @Override
    public int getFeeReserve(@NonNull String requestId) {
        return getFeeReserve();
    }

    private Integer getFeeReserve() {
        return Integer.valueOf(getParameter("fee_reserve"));
    }

}
