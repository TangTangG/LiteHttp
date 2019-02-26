import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.zip.GZIPInputStream;

class HttpWorker {

    private static final ArrayDeque<MemoryOutputStream> bufferStack = new ArrayDeque<>(8);

    private static MemoryOutputStream getStreamBuffer(int initCapacity) {
        MemoryOutputStream stream = bufferStack.poll();
        if (stream == null) {
            stream = new WorkerOutputBuffer(initCapacity);
        }
        stream.reset(initCapacity);
        return stream;
    }

    private static class WorkerOutputBuffer extends MemoryOutputStream {
        WorkerOutputBuffer(int initialCapacity) {
            super(initialCapacity);
        }

        @Override
        public void onClose() {
            recycleOutputBuffer(this);
        }

    }

    private static void recycleOutputBuffer(WorkerOutputBuffer workerOutputBuffer) {
        bufferStack.offer(workerOutputBuffer);
    }

    private static AcceptAllHostnameVerifier hostnameVerifier;

    private static AcceptAllHostnameVerifier hostnameVerifier() {
        if (hostnameVerifier == null) {
            hostnameVerifier = new AcceptAllHostnameVerifier();
        }
        return hostnameVerifier;
    }

    private static SSLContext sslContext;

    private static SSLContext sslContext() {
        if (sslContext == null) {
            try {
                sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, new TrustManager[]{new AcceptAllTrustManager()}, new SecureRandom());
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                e.printStackTrace();
            }
        }
        return sslContext;
    }

    static HttpResponse doWork(final HttpRequest request) {
        HttpURLConnection connection = null;
        HttpResponse response = new HttpResponse();
        String requestUrl = request.url.toString();

        try {
            URL url = new URL(requestUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(request.method);
            connection.setConnectTimeout(request.connectTimeout);
            connection.setReadTimeout(request.readTimeout);
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.setInstanceFollowRedirects(true);

            String[] headers = request.headers.getNamesAndValues();
            if (headers != null && headers.length > 1) {
                for (int i = 0, length = headers.length; i < length; i += 2) {
                    connection.addRequestProperty(headers[i], headers[i + 1]);
                }
            }

            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection sConn = (HttpsURLConnection) connection;
                sConn.setHostnameVerifier(hostnameVerifier());
                if (sslContext() != null) {
                    ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext().getSocketFactory());
                }
            }

            //Post  ---> build body
            if ("POST".equalsIgnoreCase(request.method)) {
                if (request.body.contentLength() > 0) {
                    connection.setDoInput(true);
                    connection.getOutputStream().write(request.body.bytes());
                }
            }

            int contentLength = connection.getHeaderFieldInt("Content-Length", -1);
            String encoding = connection.getHeaderField("Content-Encoding");

            InputStream is = connection.getInputStream();

            response.code = connection.getResponseCode();
            response.message = connection.getResponseMessage();
            response.heders = connection.getHeaderFields();

            if (is != null) {
                MemoryOutputStream byteArrayOutputStream;
                if ("gzip".equalsIgnoreCase(encoding)) {
                    is = new GZIPInputStream(is);
                    byteArrayOutputStream = getStreamBuffer(32768);
                } else {
                    byteArrayOutputStream = getStreamBuffer(Math.max(contentLength > 0 ? contentLength : 8192, 1024));
                }

                int len;
                byte[] buffer = new byte[1024];
                while ((len = is.read(buffer)) > 0) {
                    byteArrayOutputStream.write(buffer, 0, len);
                }
                is.close();
                is = byteArrayOutputStream.toInputStream();
            }

            response.data = is;

            try {
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (ProtocolException e) {
            response.code = HttpConst.REQUEST_PROTOCOL_ERROR;
            response.message = "request on protocol error,"+requestUrl;
            e.printStackTrace();
        } catch (MalformedURLException e) {
            response.code = HttpConst.REQUEST_URL_ERROR;
            response.message = "request on invalid url,"+requestUrl;
            e.printStackTrace();
        } catch (IOException e) {
            response.code = HttpConst.REQUEST_IO_ERROR;
            response.message = "request on io error,"+requestUrl;
            e.printStackTrace();
        } catch (Exception e) {
            response.code = HttpConst.REQUEST_UNKNOWN_ERROR;
            response.message = "request on unknown error,"+requestUrl;
            e.printStackTrace();
        }
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return response;
    }


    private static class AcceptAllHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }

    }

    /**
     * 证书验证类
     */
    public static class AcceptAllTrustManager implements javax.net.ssl.TrustManager, javax.net.ssl.X509TrustManager {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public boolean isServerTrusted(java.security.cert.X509Certificate[] certs) {
            return true;
        }

        public boolean isClientTrusted(java.security.cert.X509Certificate[] certs) {
            return true;
        }

        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType)
                throws java.security.cert.CertificateException {
            return;
        }

        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType)
                throws java.security.cert.CertificateException {
            return;
        }
    }
}
