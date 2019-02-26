

class CacheHttpFilter implements HttpFilter{

    @Override
    public boolean onRequest(HttpFilterChain chain, HttpRequest request) {
        return false;
    }

    @Override
    public void onResponse(HttpFilterChain chain, HttpResponse response) {

    }
}
