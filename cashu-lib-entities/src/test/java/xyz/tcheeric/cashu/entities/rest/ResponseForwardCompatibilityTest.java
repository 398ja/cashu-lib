package xyz.tcheeric.cashu.entities.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every response type must tolerate fields it does not know.
 *
 * <p>A mint that adds a field to a response breaks every already-deployed
 * client unless those clients ignore unknown properties — Jackson's default
 * is to throw {@code UnrecognizedPropertyException}, which fails the entire
 * response rather than the one field.
 *
 * <p>This is not hypothetical. cashu-mint#466 added the NUT-05 quote fields
 * to {@code PostMeltResponse}, and none of the sixteen response types in this
 * module carried the annotation. A wallet built before that change, verified
 * against the jar actually running on staging, threw on its next melt:
 *
 * <pre>
 *   Unrecognized field "quote" (class PostMeltResponse), not marked as
 *   ignorable (3 known properties: "change", "payment_preimage", "paid")
 * </pre>
 *
 * <p>A melt is the worst possible place for that. By the time the response is
 * parsed the invoice is paid and the customer's proofs are spent, so an
 * unparseable success is indistinguishable from a failure — and a wallet that
 * retries has already lost the proofs.
 *
 * <p>The check is structural rather than per-class because the property has
 * to hold for types that do not exist yet. A new response added without the
 * annotation is a future outage, and this is the only place that will say so.
 */
@DisplayName("every response type ignores unknown properties")
class ResponseForwardCompatibilityTest {

    private static final Path SOURCE_ROOT = Path.of("src", "main", "java");

    @Test
    @DisplayName("no response type can be broken by a field the mint adds later")
    void everyResponseTypeIgnoresUnknownProperties() throws Exception {
        List<String> unprotected = new ArrayList<>();

        try (Stream<Path> sources = Files.walk(SOURCE_ROOT)) {
            for (Path source : sources.filter(p -> p.getFileName().toString().endsWith("Response.java")).toList()) {
                String body = Files.readString(source);
                if (!body.contains("@JsonIgnoreProperties")) {
                    unprotected.add(source.getFileName().toString());
                }
            }
        }

        assertThat(unprotected)
                .as("""
                        Add @JsonIgnoreProperties(ignoreUnknown = true). Without it, the first \
                        time a mint adds a field to this response every client built before \
                        that change throws UnrecognizedPropertyException and fails the whole \
                        response. For a melt that happens after the money has already moved.""")
                .isEmpty();
    }

    /**
     * The structural check reads source text, so it would pass on a file that
     * mentions the annotation in a comment. This confirms the behaviour is
     * real on the type that actually broke.
     */
    @Test
    @DisplayName("the annotation is effective, not merely present in the text")
    void theAnnotationIsEffectiveOnTheTypeThatBroke() {
        JsonIgnoreProperties annotation = xyz.tcheeric.cashu.entities.rest.nut05.PostMeltResponse.class
                .getAnnotation(JsonIgnoreProperties.class);

        assertThat(annotation)
                .as("PostMeltResponse is the type that broke deployed wallets in #466")
                .isNotNull();
        assertThat(annotation.ignoreUnknown())
                .as("present but ignoreUnknown=false would be worse than absent: it reads "
                        + "like protection and provides none")
                .isTrue();
    }

    @Test
    @DisplayName("the source root the scan depends on actually exists")
    void theScanIsLookingAtSomething() {
        assertThat(SOURCE_ROOT.toFile())
                .as("a scan over a missing directory finds nothing and passes, which is the "
                        + "failure mode this whole test is guarding against elsewhere")
                .isDirectory();

        File[] packages = SOURCE_ROOT.toFile().listFiles();
        assertThat(packages).isNotEmpty();
    }
}
