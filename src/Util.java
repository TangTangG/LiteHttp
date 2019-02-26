import java.net.URLEncoder;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

public class Util {
    static ThreadFactory threadFactory(String name, boolean daemon) {
        return runnable -> {
            Thread result = new Thread(runnable, name);
            result.setDaemon(daemon);
            return result;
        };
    }

    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    public static final String[] EMPTY_STRING_ARRAY = new String[0];


//    public static final ResponseBody EMPTY_RESPONSE = ResponseBody.create(null, EMPTY_BYTE_ARRAY);
    static final RequestBody EMPTY_REQUEST = RequestBody.create(null, EMPTY_BYTE_ARRAY);

    static boolean requiresRequestBody(String method) {
        return method.equals("POST")
                || method.equals("PUT")
                || method.equals("PATCH")
                || method.equals("PROPPATCH") // WebDAV
                || method.equals("REPORT");   // CalDAV/CardDAV (defined in WebDAV Versioning)
    }

    static boolean permitsRequestBody(String method) {
        return !(method.equals("GET") || method.equals("HEAD"));
    }

    /**
     * Increments {@code pos} until {@code input[pos]} is not ASCII whitespace. Stops at {@code
     * limit}.
     */
    public static int skipLeadingAsciiWhitespace(String input, int pos, int limit) {
        for (int i = pos; i < limit; i++) {
            switch (input.charAt(i)) {
                case '\t':
                case '\n':
                case '\f':
                case '\r':
                case ' ':
                    continue;
                default:
                    return i;
            }
        }
        return limit;
    }

    /**
     * Decrements {@code limit} until {@code input[limit - 1]} is not ASCII whitespace. Stops at
     * {@code pos}.
     */
    public static int skipTrailingAsciiWhitespace(String input, int pos, int limit) {
        for (int i = limit - 1; i >= pos; i--) {
            switch (input.charAt(i)) {
                case '\t':
                case '\n':
                case '\f':
                case '\r':
                case ' ':
                    continue;
                default:
                    return i + 1;
            }
        }
        return pos;
    }

    static boolean strIsEmpty(String s) {
        return s == null || "".equals(s);
    }

    static boolean collectionIsEmpty(Collection c) {
        return c == null || c.size() == 0;
    }

    static boolean collectionIsEmpty(Map c) {
        return c == null || c.size() == 0;
    }

    static StringBuilder buildQueryString(StringBuilder builder, Map<String, String> params) {
        if (params != null) {
            int idx = 0;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (idx > 0) {
                    builder.append("&");
                }
                ++idx;
                builder.append(URLEncoder.encode(entry.getKey()));
                if (entry.getValue() != null) {
                    builder.append("=")
                            .append(URLEncoder.encode(entry.getValue()));
                }
            }
        }
        return builder;
    }
}
