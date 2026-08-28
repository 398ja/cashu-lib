package xyz.tcheeric.cashu.entities.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.nut00.CashuErrorCode;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import java.util.Optional;

/**
 * The error body a mint returns, as specified by
 * <a href="https://github.com/cashubtc/nuts/blob/main/00.md">NUT-00 &sect; 0.2</a>:
 * {@code {"detail": "oops", "code": 1337}}.
 *
 * <p>Serialization goes exclusively through Jackson, so a detail containing quotes or backslashes
 * stays valid JSON. Never build this payload with string formatting.
 *
 * @param detail human-readable error message
 * @param code numeric error code from the NUT registry, or our reserved extension range
 */
@JsonPropertyOrder({"detail", "code"})
public record ErrorResponse(
        @JsonProperty("detail") String detail,
        @JsonProperty("code") int code) {

    /** Status returned when a code is outside every range this library knows. */
    private static final int UNKNOWN_CODE_HTTP_STATUS = 500;

    @JsonCreator
    public ErrorResponse(
            @JsonProperty("detail") String detail,
            @JsonProperty("code") int code) {
        this.detail = detail;
        this.code = code;
    }

    /**
     * Builds the response for an error code, using that code's default detail message.
     */
    public ErrorResponse(@NonNull CashuErrorCode errorCode) {
        this(errorCode.getDefaultDetail(), errorCode.getCode());
    }

    /**
     * Builds the response for an error code with a caller-supplied detail message.
     */
    public ErrorResponse(@NonNull CashuErrorCode errorCode, @NonNull String detail) {
        this(detail, errorCode.getCode());
    }

    /**
     * The error code as an enum, or empty when the numeric code is not one we know.
     */
    @JsonIgnore
    public Optional<CashuErrorCode> errorCode() {
        return CashuErrorCode.forCode(code);
    }

    /**
     * The HTTP status a mint returns with this error, defaulting to 500 for an unknown code.
     */
    @JsonIgnore
    public int httpStatus() {
        return errorCode().map(CashuErrorCode::getHttpStatus).orElse(UNKNOWN_CODE_HTTP_STATUS);
    }

    /**
     * Serializes this response with Jackson, so any character in the detail stays correctly escaped.
     *
     * @throws IllegalStateException if Jackson cannot write this response
     */
    public String toJson() {
        try {
            return JsonUtils.JSON_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize ErrorResponse with code " + code, e);
        }
    }

    /**
     * Parses an error body a mint produced. Accepts only the NUT-00 shape.
     *
     * @throws IllegalArgumentException if the payload is not a NUT-00 error body
     */
    public static ErrorResponse fromJson(@NonNull String json) {
        try {
            return JsonUtils.JSON_MAPPER.readValue(json, ErrorResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Not a NUT-00 error body. Got: " + json, e);
        }
    }
}
