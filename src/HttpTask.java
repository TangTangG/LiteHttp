public final class HttpTask {

    private volatile boolean cancel = false;
    private boolean executed = false;
    private final HttpRequest request;
    private LiteHttp mHTTP;

    private HttpTask(HttpRequest request, LiteHttp liteHttp) {
        this.mHTTP = liteHttp;
        this.request = request;
    }

    static HttpTask newTask(HttpRequest request, LiteHttp liteHttp) {
        return new HttpTask(request, liteHttp);
    }

    void enqueue(Callback callback) {
        synchronized (this) {
            if (executed) {
                return;
            }
            executed = true;
        }
        mHTTP.executePool().offer(new AsyncTask("demo", callback));
    }

    void cancel() {
        cancel = true;
    }

    /**
     * Use an context manager callback and other info?
     */
    final class AsyncTask extends NamedRunnable {

        Callback mCallback;

        AsyncTask(String name, Callback callback) {
            super(String.format("LiteHttp-- %s", name));
            this.mCallback = callback;
        }

        boolean canceled() {
            return cancel;
        }

        @Override
        void execute() {
            if (cancel) {
                return;
            }
            HttpFilterChain chain = new HttpFilterChain(this);
            chain.add(mHTTP.filters())
                    .add(new CacheHttpFilter())
                    .add(new HttpClientFilter())
                    .handleRequest(request);

        }

    }

}
