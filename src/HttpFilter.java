
public interface HttpFilter {

    boolean onRequest(HttpFilterChain chain, HttpRequest request);

    void onResponse(HttpFilterChain chain, HttpResponse response);
}
