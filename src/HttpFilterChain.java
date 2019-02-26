import java.util.Collection;

public class HttpFilterChain {

    HttpTask.AsyncTask mTask;
    FilterNode node;

    public HttpFilterChain(HttpTask.AsyncTask mTask) {
        this.mTask = mTask;
    }

    HttpFilterChain add(Collection<HttpFilter> filters) {
        for (HttpFilter filter : filters) {
            add(filter);
        }
        return this;
    }

    HttpFilterChain add(HttpFilter filter) {
        if (node == null) {
            node = FilterNode.obtain(filter);
        }
        node.add(filter);
        return this;
    }

    public void handleRequest(HttpRequest request) {
        FilterNode head = node;
        while (head != null) {
            if (mTask.canceled()) {
                break;
            }
            if (head.filter.onRequest(this, request)) {
                break;
            }
            head = head.next;
        }
        node = head;
    }

    public void handleResponse(HttpResponse response) {
        FilterNode tail = node;
        while (tail != null) {
            if (mTask.canceled()) {
                break;
            }
            tail.filter.onResponse(this, response);
            tail = tail.prev;
        }
        //callback here
        //response code judge success?

        if (mTask.mCallback != null && !mTask.canceled()) {
            mTask.mCallback.onResponse(response);
        }
    }
}

class FilterNode {
    FilterNode prev;
    FilterNode next;
    HttpFilter filter;

    FilterNode(FilterNode prev, FilterNode next, HttpFilter filter) {
        this.prev = prev;
        this.next = next;
        this.filter = filter;
    }

    static FilterNode obtain(HttpFilter node) {
        return new FilterNode(null, null, node);
    }

    FilterNode add(HttpFilter node) {
        FilterNode newNode = obtain(node);
        FilterNode lastNode = this;
        while (lastNode.next != null) {
            lastNode = lastNode.next;
        }
        lastNode.next = newNode;
        newNode.prev = lastNode;
        return newNode;
    }


}
