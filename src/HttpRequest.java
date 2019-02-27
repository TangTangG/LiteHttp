import com.sun.javaws.exceptions.InvalidArgumentException;

import java.security.InvalidParameterException;

public class HttpRequest {
    /**
     * 0、Header 描述本次请求的基本描述信息
     * 1、Body 包含基本数据信息
     * 2、URL
     * .....|scheme
     * .....|host
     * .....|port
     * .....|path  ----->  域名之后的扩展路径
     * .....|fragment ----> 用于指导浏览器动作，对服务器无实际意义  使用 # 隔断
     * 3、请求方法
     * .....|Post
     * ..........|body <-- form（对表单提供支持）  表单数据提交、文件上传等，请求数据会被包含在请求体中
     * ..........|header <-- content_type 描述Post数据的基本格式，常用的有如下几种：
     * ...............|application/x-www-form-urlencoded：数据被编码为名称/值对。这是标准的编码格式
     * ...............|multipart/form-data： 数据被编码为一条消息，用于文件上传等
     * ...............|text/plain： 数据以纯文本形式(text/json/xml/html)进行编码，其中不含任何控件或格式字符
     * .....|Get
     * ..........|query params
     * 4、Timeout define
     * 5、Retry?
     */

    final String method;
    final RequestUrl url;
    final RequestHeaders headers;
    final RequestBody body;

    /**
     * timeout setting (ms)
     */
    final int connectTimeout;
    final int readTimeout;

    final int retryLimit;

    @Override
    public String toString() {

        return url + " " + headers;
    }

    String url() {
        return url.toString();
    }

    private HttpRequest(Builder builder) {
        method = builder.method;
        url = builder.url;
        headers = builder.headers;
        body = builder.body;
        connectTimeout = builder.connectTimeout;
        readTimeout = builder.readTimeout;
        retryLimit = builder.retryLimit;
    }

    static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String method = "get";
        private RequestUrl url;
        private RequestHeaders headers;
        private RequestBody body;

        private int connectTimeout = 8 * 1000;
        private int readTimeout = 6 * 1000;
        private int retryLimit = 0;

        public Builder url(RequestUrl.Builder url) {
            this.url = url.build();
            return this;
        }

        public Builder url(String url) {
            this.url = RequestUrl.parse(url);
            return this;
        }

        public Builder get() {
            return method("GET", null);
        }

        public Builder head() {
            return method("HEAD", null);
        }

        public Builder post(RequestBody body) {
            return method("POST", body);
        }

        public Builder delete(RequestBody body) {
            return method("DELETE", body);
        }

        public Builder delete() {
            return delete(Util.EMPTY_REQUEST);
        }

        public Builder put(RequestBody body) {
            return method("PUT", body);
        }

        public Builder patch(RequestBody body) {
            return method("PATCH", body);
        }

        public Builder method(String method, RequestBody body) {
            if (method == null) throw new NullPointerException("method == null");
            if (method.length() == 0) throw new IllegalArgumentException("method.length() == 0");
            if (body != null && !Util.permitsRequestBody(method)) {
                throw new IllegalArgumentException("method " + method + " must not have a request body.");
            }
            if (body == null && Util.requiresRequestBody(method)) {
                throw new IllegalArgumentException("method " + method + " must have a request body.");
            }
            this.method = method;
            this.body = body;
            return this;
        }

        public Builder headers(RequestHeaders.Builder headers) {
            this.headers = headers.build();
            return this;
        }

        public Builder connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder readTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder retryLimit(int retryLimit) {
            this.retryLimit = retryLimit;
            return this;
        }

        HttpRequest build() {
            if (!"GET".equalsIgnoreCase(method) && body == null) {
                throw new InvalidParameterException("All method need a response body are required,except GET.");
            }
            if (headers == null) {
                headers = new RequestHeaders.Builder().build();
            }
            return new HttpRequest(this);
        }
    }
}
