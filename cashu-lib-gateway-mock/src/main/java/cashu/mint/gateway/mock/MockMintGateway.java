package cashu.mint.gateway.mock;

import lombok.NonNull;

public class MockMintGateway extends MockGateway {
    public MockMintGateway(@NonNull String code) {
        super(code, "mint");
    }

    @Override
    public int getFeeReserve(@NonNull String requestId) {
        throw new IllegalStateException("Not implemented");
    }
}
