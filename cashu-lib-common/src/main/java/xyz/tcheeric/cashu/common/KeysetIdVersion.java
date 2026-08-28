package xyz.tcheeric.cashu.common;

import lombok.Getter;

import java.util.Locale;

/**
 * NUT-02 keyset identifier versions, distinguished by the leading version byte of the id.
 *
 * <p>Version {@code 0x00} ids are 8 bytes (16 hex characters) and are the only form this
 * library derives NUT-13 deterministic secrets for. Version {@code 0x01} ids are 33 bytes
 * (66 hex characters) and use a different NUT-13 derivation, which is not implemented yet.
 */
@Getter
public enum KeysetIdVersion {

    V1("00", 16),
    V2("01", 66);

    private static final int VERSION_BYTE_HEX_LENGTH = 2;

    private final String versionByteHex;
    private final int hexLength;

    KeysetIdVersion(String versionByteHex, int hexLength) {
        this.versionByteHex = versionByteHex;
        this.hexLength = hexLength;
    }

    /**
     * Resolves the version of a keyset id from its leading version byte and length.
     *
     * <p>Ids of the version 1 length that do not carry the {@code 0x00} version byte are treated
     * as version 1, because that byte convention postdates ids already in circulation.
     *
     * @param keysetIdHex hexadecimal keyset id
     * @return the matching version
     * @throws IllegalArgumentException if no version matches the id's version byte and length
     */
    public static KeysetIdVersion of(String keysetIdHex) {
        if (keysetIdHex == null || keysetIdHex.length() < VERSION_BYTE_HEX_LENGTH) {
            throw new IllegalArgumentException("Keyset id is too short to carry a version byte. Got: " + keysetIdHex);
        }

        String versionByteHex = keysetIdHex.substring(0, VERSION_BYTE_HEX_LENGTH).toLowerCase(Locale.ROOT);
        for (KeysetIdVersion version : values()) {
            if (version.matches(versionByteHex, keysetIdHex.length())) {
                return version;
            }
        }

        if (keysetIdHex.length() == V1.hexLength) {
            return V1;
        }

        throw new IllegalArgumentException(
                "Unknown keyset id version byte 0x" + versionByteHex + " for length " + keysetIdHex.length()
                        + " in keyset id: " + keysetIdHex);
    }

    private boolean matches(String candidateVersionByteHex, int candidateHexLength) {
        return versionByteHex.equals(candidateVersionByteHex) && hexLength == candidateHexLength;
    }
}
