import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Map;

public abstract class RequestBody {

    abstract MediaType contentType();

    abstract int contentLength();

    abstract byte[] bytes();

    static RequestBody create(final MediaType type, final File file) {
        return create(type, BytesConvert.convertFile(file));
    }

    static RequestBody create(final MediaType type, final String string) throws UnsupportedEncodingException {
        return create(type, BytesConvert.convertString(string));
    }

    static RequestBody create(final MediaType type, final Map<String, String> form) throws UnsupportedEncodingException {
        return create(type, BytesConvert.convertForm(form));
    }

    static RequestBody create(final MediaType type, final byte[] bytes) {
        return new RequestBody() {
            @Override
            MediaType contentType() {
                return type;
            }

            @Override
            int contentLength() {
                return bytes == null ? 0 : bytes.length;
            }

            @Override
            byte[] bytes() {
                return bytes == null ? Util.EMPTY_BYTE_ARRAY : bytes;
            }
        };
    }
}
