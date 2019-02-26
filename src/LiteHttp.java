import java.util.ArrayList;

public final class LiteHttp {

    private final ArrayList<HttpFilter> filterContainer = new ArrayList<>(5);

    private final int MAX_RUNNING_SIZE = 10;
    private int runningLimited = MAX_RUNNING_SIZE;
    private volatile RequestPool pool;

    RequestPool executePool() {
        if (pool == null) {
            synchronized (RequestPool.class) {
                if (pool == null) {
                    pool = new RequestPool(getLimitedRunningSize());
                }
            }
        }
        return pool;
    }

    private int getLimitedRunningSize() {
        return Math.max(runningLimited, MAX_RUNNING_SIZE);
    }

    public LiteHttp setRunningLimited(int runningLimited) {
        this.runningLimited = runningLimited;
        return this;
    }

    public void addHttpFilter(HttpFilter filter) {
        if (filter == null) {
            return;
        }
        filterContainer.add(filter);
    }

    ArrayList<HttpFilter> filters() {
        return filterContainer;
    }

    HttpTask obtainTask(HttpRequest request) {
        return HttpTask.newTask(request, this);
    }

    HttpRequest.Builder obtainRequestBuilder() {
        return new HttpRequest.Builder();
    }

    RequestHeaders.Builder obtainHeadersBuilder() {
        return new RequestHeaders.Builder();
    }

    RequestUrl.Builder obtainUrlBuilder() {
        return RequestUrl.newBuilder();
    }

}
