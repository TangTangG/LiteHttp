import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class HttpResponse {

    public HttpRequest request;
    public int code;
    public String message;
    public InputStream data;
    public Map<String, List<String>> heders;

}
