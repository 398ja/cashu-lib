/**
 * Cryptographic exception types for the Cashu protocol.
 *
 * <p>This package provides a hierarchy of exceptions for cryptographic
 * operation failures:
 *
 * <pre>
 * CashuCryptoException (base class)
 *   ├── InvalidKeyException (key validation failures)
 *   └── SignatureException (signature operation failures)
 * </pre>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * try {
 *     Schnorr.verify(message, publicKey, signature);
 * } catch (SignatureException e) {
 *     // Handle verification failure
 *     log.warn("Signature verification failed: {}", e.getOperationType());
 * } catch (InvalidKeyException e) {
 *     // Handle invalid key
 *     log.error("Invalid key provided: {}", e.getKeyType());
 * }
 * }</pre>
 *
 * <h2>Security Considerations</h2>
 *
 * <p>Exception messages are designed to avoid leaking sensitive information:
 * <ul>
 *   <li>Key values are never included in exception messages</li>
 *   <li>Verification failures provide minimal detail to prevent oracle attacks</li>
 *   <li>Stack traces should be logged at DEBUG level in production</li>
 * </ul>
 *
 * @see xyz.tcheeric.cashu.crypto.exception.CashuCryptoException
 * @see xyz.tcheeric.cashu.crypto.exception.InvalidKeyException
 * @see xyz.tcheeric.cashu.crypto.exception.SignatureException
 */
package xyz.tcheeric.cashu.crypto.exception;
