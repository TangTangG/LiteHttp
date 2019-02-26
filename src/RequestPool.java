import java.util.concurrent.*;

class RequestPool {

    private int runningSize;

    private final ArrayBlockingQueue<HttpTask.AsyncTask> runningQueue = new ArrayBlockingQueue<>(8);

    private final ArrayBlockingQueue<HttpTask.AsyncTask> waitingQueue = new ArrayBlockingQueue<>(8);
    /**
     * running Async task.
     */
    private ExecutorService executorService;

    RequestPool(int runningSize) {
        this.runningSize = runningSize;
    }

    void offer(HttpTask.AsyncTask async) {
        if (async == null) {
            return;
        }
        if (runningQueue.size() < runningSize) {
            runningQueue.offer(async);
            executorService().execute(async);
        } else {
            waitingQueue.offer(async);
        }
    }

    void finish(HttpTask.AsyncTask async) {
        if (async == null) {
            return;
        }
        if (runningQueue.remove(async)) {
            if (waitingQueue.size() > 0) {
                offer(waitingQueue.poll());
            }
            return;
        }
        System.out.println("Finish with wrong async task.");
    }

    private synchronized ExecutorService executorService() {
        if (executorService == null) {
            executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                    new SynchronousQueue<>(), Util.threadFactory("LiteHttp RequestPool", false));
        }
        return executorService;
    }


}