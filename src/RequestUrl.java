import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class RequestUrl {

    private final String schema;
    private final String host;
    private final int port;
    private final String fragment;
    private final List<String> pathArgs;
    /**
     * Get only
     */
    private final LinkedHashMap<String, String> queries;

    static RequestUrl parse(String url) {
        return newBuilder().parse(url);
    }

    @Override
    public String toString() {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(schema)
                .append("://").append(host);
        if (port > 0) {
            urlBuilder.append(":").append(port);
        }
        if (!Util.collectionIsEmpty(pathArgs)) {
            for (String p : pathArgs) {
                if (!p.startsWith("/")) {
                    urlBuilder.append("/");
                }
                urlBuilder.append(p);
            }
        }
        if (!Util.collectionIsEmpty(queries)) {
            urlBuilder.append("?");
            Util.buildQueryString(urlBuilder, queries);
        }
        if (!Util.strIsEmpty(fragment)) {
            urlBuilder.append("#")
                    .append(fragment);
        }
        return urlBuilder.toString();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    private RequestUrl(Builder builder) {
        schema = builder.scheme;
        host = builder.host;
        pathArgs = builder.pathArgs;
        queries = builder.queries;
        port = builder.port != -1 ? defaultPort(schema) : builder.port;
        fragment = builder.fragment;
    }

    boolean isHttps() {
        return "https".equals(schema);
    }

    int defaultPort(String scheme) {
        if ("http".equals(scheme)) {
            return 80;
        } else if ("https".equals(scheme)) {
            return 443;
        } else {
            return -1;
        }
    }

    public static class Builder {
        String scheme = "http";
        String host;
        int port = -1;
        String fragment;
        List<String> pathArgs;
        LinkedHashMap<String, String> queries;

        public Builder schema(String schema) {
            this.scheme = schema;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder fragment(String fragment) {
            this.fragment = fragment;
            return this;
        }

        public Builder addQuery(String name, String val) {
            if (queries == null) {
                queries = new LinkedHashMap<>();
            }
            if (Util.strIsEmpty(name) || Util.strIsEmpty(val)) {
                return this;
            }
            this.queries.put(name, val.trim());
            return this;
        }

        public Builder contact(String path) {
            if (pathArgs == null) {
                pathArgs = new ArrayList<>();
            }
            if (!Util.strIsEmpty(path)) {
                this.pathArgs.add(path.trim());
            }
            return this;
        }

        RequestUrl build() {
            return new RequestUrl(this);
        }

        /**
         * Copy from okhttp
         */
        public RequestUrl parse(String url) {
            if (Util.strIsEmpty(url)) {
                return build();
            }
            //check whitespace,head & tail.
            int pos = Util.skipLeadingAsciiWhitespace(url, 0, url.length());
            int limit = Util.skipTrailingAsciiWhitespace(url, pos, url.length());

            // Scheme
            // Delimiter 分隔符
            int schemeDelimiterOffset = schemeDelimiterOffset(url, pos, limit);
            if (schemeDelimiterOffset != -1) {
                if (url.regionMatches(true, pos, "https:", 0, 6)) {
                    this.scheme = "https";
                    pos += "https:".length();
                } else if (url.regionMatches(true, pos, "http:", 0, 5)) {
                    this.scheme = "http";
                    pos += "http:".length();
                } else {
                    throw new IllegalArgumentException("Expected URL scheme 'http' or 'https' but was '"
                            + url.substring(0, schemeDelimiterOffset) + "'");
                }
            } else {
                throw new IllegalArgumentException(
                        "Expected URL scheme 'http' or 'https' but no colon was found");
            }

            // Host
            // host[:port]
            int slashCount = slashCount(url, pos, limit);
            if (slashCount >= 2) {
                pos += slashCount;
                //find first delimiter char / \\ ? #
                int firstDelimiter = delimiterOffset(url, pos, limit, "/\\?#");
                int c = firstDelimiter == limit ? -1 : url.charAt(firstDelimiter);
                switch (c) {
                    case -1:
                    case '/':
                    case '\\':
                    case '?':
                    case '#':
                        // find host last pos;
                        int colonPos = delimiterOffset(url, pos, firstDelimiter, ":");
                        if (colonPos == firstDelimiter) {
                            //only host
                            this.host = url.substring(pos, firstDelimiter);
                        } else {
                            this.host = url.substring(pos, colonPos);
                            this.port = parsePort(url, colonPos + 1, firstDelimiter);
                        }
                        pos = firstDelimiter;
                        break;
                }
            } else {
                //No host
            }

            //extend path
            int pathDelimiterOffset = delimiterOffset(url, pos, limit, "?#");
            if (pathDelimiterOffset <= limit) {
                contact(url.substring(pos, pathDelimiterOffset));
            }
            pos = pathDelimiterOffset;

            //query params
            if (pos < limit && url.charAt(pos) == '?') {
                int fragmentDelimiterOffset = delimiterOffset(url, pos, limit, "#");
                if (fragmentDelimiterOffset < limit) {
                    String queries = url.substring(pos + 1, fragmentDelimiterOffset);
                    String[] c = queries.split("&");
                    for (String q : c) {
                        String[] split = q.split("=");
                        if (split.length < 2) {
                            continue;
                        }
                        addQuery(split[0], split[1]);
                    }
                }
                pos = fragmentDelimiterOffset;
            }

            //fragment
            if (pos < limit && url.charAt(pos) == '#') {
                fragment = url.substring(pos + 1, limit);
            }
            return build();
        }

        /**
         * TCP & UDP
         * **********source-port | target-port*******
         * **            16      |       16        **
         */
        private int parsePort(String url, int i, int firstDelimiter) {
            int port;
            String portStr = url.substring(i, firstDelimiter);
            try {
                port = Integer.parseInt(portStr);
                if (port < 0 || port > 65535) {
                    port = -1;
                }
            } catch (Exception e) {
                port = -1;
            }
            return port;
        }

        private int delimiterOffset(String url, int pos, int limit, String s) {
            for (int i = pos; i < limit; i++) {
                if (s.indexOf(url.charAt(i)) != -1) {
                    return i;
                }
            }
            return limit;
        }

        /**
         * Returns the number of '/' and '\' slashes in {@code input}, starting at {@code pos}.
         */
        private static int slashCount(String input, int pos, int limit) {
            int slashCount = 0;
            while (pos < limit) {
                char c = input.charAt(pos);
                if (c == '\\' || c == '/') {
                    slashCount++;
                    pos++;
                } else {
                    break;
                }
            }
            return slashCount;
        }

        /**
         * Finds the first ':' in {@code input}, skipping characters between square braces "[...]".
         */
        private static int portColonOffset(String input, int pos, int limit) {
            for (int i = pos; i < limit; i++) {
                switch (input.charAt(i)) {
                    case '[':
                        while (++i < limit) {
                            if (input.charAt(i) == ']') break;
                        }
                        break;
                    case ':':
                        return i;
                }
            }
            return limit; // No colon.
        }

        /**
         * Copy from okhttp
         */
        private static int schemeDelimiterOffset(String input, int pos, int limit) {
            if (limit - pos < 2) return -1;

            char c0 = input.charAt(pos);
            if ((c0 < 'a' || c0 > 'z') && (c0 < 'A' || c0 > 'Z')) return -1; // Not a scheme start char.

            for (int i = pos + 1; i < limit; i++) {
                char c = input.charAt(i);

                if ((c >= 'a' && c <= 'z')
                        || (c >= 'A' && c <= 'Z')
                        || (c >= '0' && c <= '9')
                        || c == '+'
                        || c == '-'
                        || c == '.') {
                    continue; // Scheme character. Keep going.
                } else if (c == ':') {
                    return i; // Scheme prefix!
                } else {
                    return -1; // Non-scheme character before the first ':'.
                }
            }

            return -1; // No ':'; doesn't start with a scheme.
        }

    }
}
