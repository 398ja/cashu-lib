package cashu.crypto;

import cashu.crypto.BDHKEUtils;
import cashu.util.Utils;

public class Main {
    public static void main(String[] args) {
        String secret = "35ed6ef1b0da91b3b342561eb172b36410c3f36b57b6f558ce04018120008b89";
        byte[] k = Utils.hexStringToBytes("b33aff3a767863b76f325c3f24e6a9d1a3f9436c9d66085e90ab6adde2d08fcd");
        byte[] C = Utils.hexStringToBytes("036b031bd96900bff7bea574f6ea65e30938514f422f3862f5c4f6f148675c6cff");

        boolean isValid = BDHKEUtils.verify(secret, k, C);
        System.out.println("Is the proof valid? " + isValid);
    }
}