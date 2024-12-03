package cashu.gateway.model;

import java.io.IOException;

public interface Request<T extends Request.Param, U extends Response> {

    default U getResponse() throws IOException {
        return null;
    }

    interface Param {
        enum Kind {
            PATH, QUERY
        }

        default Kind getKind() {
            return Kind.PATH;
        }
    }
}