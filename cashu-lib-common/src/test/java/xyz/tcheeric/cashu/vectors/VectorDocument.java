package xyz.tcheeric.cashu.vectors;

import lombok.NonNull;
import lombok.Value;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The vendored cashubtc/nuts test vector documents, read as an ordered list of fenced code blocks.
 *
 * <p>Upstream publishes the vectors as Markdown prose with the values inside fenced blocks, so the
 * documents are vendored verbatim (see {@code src/test/resources/vectors/cashubtc-nuts/README.md}
 * for the pinned commit) and parsed here. Keeping the files byte-identical to upstream makes a
 * refresh a plain copy whose diff is reviewable against the specification repository.
 */
@Value
public class VectorDocument {

    private static final String RESOURCE_ROOT = "/vectors/cashubtc-nuts/";

    private static final Pattern FENCED_BLOCK =
            Pattern.compile("```(\\w*)\\R(.*?)```", Pattern.DOTALL);

    private static final Pattern INLINE_HEX_LITERAL = Pattern.compile("h'([0-9a-fA-F]{16,})'");

    String markdown;
    List<CodeBlock> blocks;

    /**
     * Reads a vendored vector document.
     *
     * @param fileName file name under the vendored vector directory, for example {@code 00-tests.md}
     * @return the parsed document
     * @throws IllegalArgumentException if the document is not on the test classpath
     */
    public static VectorDocument load(@NonNull String fileName) {
        String markdown = readResource(RESOURCE_ROOT + fileName);
        List<CodeBlock> blocks = new ArrayList<>();
        Matcher matcher = FENCED_BLOCK.matcher(markdown);
        while (matcher.find()) {
            blocks.add(new CodeBlock(matcher.group(1), matcher.group(2)));
        }
        if (blocks.isEmpty()) {
            throw new IllegalArgumentException("Vector document contains no code blocks: " + fileName);
        }
        return new VectorDocument(markdown, List.copyOf(blocks));
    }

    /**
     * The code blocks of a given fence language, in document order.
     *
     * @param language fence language, for example {@code json} or {@code shell}
     */
    public List<CodeBlock> blocksOfLanguage(@NonNull String language) {
        return blocks.stream().filter(block -> block.getLanguage().equals(language)).toList();
    }

    /**
     * The code block at {@code index} among those of {@code language}.
     */
    public CodeBlock block(@NonNull String language, int index) {
        List<CodeBlock> matching = blocksOfLanguage(language);
        if (index < 0 || index >= matching.size()) {
            throw new IllegalArgumentException("No " + language + " block at index " + index
                    + "; the document has " + matching.size() + " of them.");
        }
        return matching.get(index);
    }

    /**
     * The first inline {@code h'...'} hex literal in the document.
     *
     * <p>Upstream writes the raw binary TokenV4 vector as prose rather than a fenced block.
     *
     * @throws IllegalStateException if the document holds no such literal
     */
    public String inlineHexLiteral() {
        Matcher matcher = INLINE_HEX_LITERAL.matcher(markdown);
        if (!matcher.find()) {
            throw new IllegalStateException("Document contains no inline h'...' hex literal");
        }
        return matcher.group(1);
    }

    private static String readResource(String resourcePath) {
        try (InputStream stream = VectorDocument.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalArgumentException("Missing vendored vector document: " + resourcePath);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read vendored vector document: " + resourcePath, e);
        }
    }

    /**
     * One fenced code block of a vector document.
     */
    @Value
    public static class CodeBlock {

        String language;
        String content;

        /**
         * The block's lines, with upstream's {@code #} commentary and blank lines removed.
         */
        public List<String> lines() {
            return content.lines()
                    .map(CodeBlock::stripComment)
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .toList();
        }

        /**
         * The block's {@code key: value} lines as records, a new record starting at each
         * occurrence of {@code recordKey}.
         *
         * <p>Upstream packs several vectors into one block, repeating the same keys, so a flat map
         * would silently keep only the last one.
         *
         * @param recordKey key that opens each record, for example {@code Message}
         */
        public List<Map<String, String>> records(@NonNull String recordKey) {
            List<Map<String, String>> records = new ArrayList<>();
            Map<String, String> current = null;
            for (String line : lines()) {
                int separator = line.indexOf(':');
                if (separator < 0) {
                    continue;
                }
                String key = line.substring(0, separator).trim();
                String value = unquote(line.substring(separator + 1).trim());
                if (key.equals(recordKey) || current == null) {
                    current = new LinkedHashMap<>();
                    records.add(current);
                }
                current.put(key, value);
            }
            return records;
        }

        /**
         * The block's single record of {@code key: value} lines.
         */
        public Map<String, String> labelledValues() {
            Map<String, String> values = new LinkedHashMap<>();
            for (String line : lines()) {
                int separator = line.indexOf(':');
                if (separator < 0) {
                    continue;
                }
                values.put(line.substring(0, separator).trim(), unquote(line.substring(separator + 1).trim()));
            }
            return values;
        }

        /**
         * The block's lines as opaque values, for blocks holding serialized artefacts rather than
         * labelled fields.
         */
        public List<String> values() {
            return lines().stream().map(CodeBlock::unquote).toList();
        }

        /**
         * The block's whole content, for blocks holding one multi-line artefact such as JSON.
         */
        public String text() {
            return content.trim();
        }

        /**
         * Removes upstream's trailing {@code # ...} commentary.
         *
         * <p>Safe for these documents: every value is hex, base64url or a quoted string, none of
         * which contains {@code #} outside of the token bodies, which never start a line comment
         * because the separator requires preceding whitespace or line start.
         */
        private static String stripComment(String line) {
            int hash = line.indexOf('#');
            if (hash == 0) {
                return "";
            }
            if (hash > 0 && Character.isWhitespace(line.charAt(hash - 1))) {
                return line.substring(0, hash);
            }
            return line;
        }

        private static String unquote(String value) {
            String trimmed = value.trim();
            if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                return trimmed.substring(1, trimmed.length() - 1);
            }
            return trimmed;
        }
    }
}
