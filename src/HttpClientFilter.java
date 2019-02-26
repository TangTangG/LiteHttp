

class HttpClientFilter implements HttpFilter{

    @Override
    public boolean onRequest(HttpFilterChain chain, HttpRequest request) {
        chain.handleResponse(HttpWorker.doWork(request));
        return false;
    }

    @Override
    public void onResponse(HttpFilterChain chain, HttpResponse response) {

    }
}
