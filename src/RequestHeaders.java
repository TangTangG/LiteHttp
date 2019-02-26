import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Content-Type
 * Cache-Control:Max-Age ...
 */
public class RequestHeaders {

    private final String[] namesAndValues;

    private RequestHeaders(Builder builder) {
        if (Util.collectionIsEmpty(builder.container)) {
            this.namesAndValues = new String[2];
            namesAndValues[0] = "Content-Type";
            namesAndValues[1] = "application/x-www-form-urlencoded";
        } else {
            this.namesAndValues = (String[]) builder.container.toArray();
        }
    }

    public String[] getNamesAndValues() {
        return namesAndValues;
    }

    @Override
    public String toString() {
        StringBuilder headerBuilder = new StringBuilder();
        for (int i = 0,length = namesAndValues.length; i < length; i += 2) {
            headerBuilder
                    .append(namesAndValues[i])//name
                    .append(":")
                    .append(namesAndValues[i + 1])//value
                    .append("\n");
        }
        return headerBuilder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RequestHeaders
                && Arrays.equals(namesAndValues, ((RequestHeaders) obj).namesAndValues);
    }



    public static class Builder {
        List<String> container;

        Builder add(String name, String val) {
            if (Util.strIsEmpty(name) || Util.strIsEmpty(val)) {
                return this;
            }
            if (container == null) {
                container = new ArrayList<>();
            }
            container.add(name);
            container.add(val);
            return this;
        }

        RequestHeaders build() {
            return new RequestHeaders(this);
        }
    }
}
