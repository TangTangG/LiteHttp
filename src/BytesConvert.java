import java.io.*;
import java.util.Map;

/**
 * All input will output as bytes[].
 */
public class BytesConvert {

    /**
     * File
     */
    public static byte[] convertFile(String filePath) {
        return convertFile(new File(filePath));
    }

    public static byte[] convertFile(File file) {
        if (file == null || !file.exists()) {
            return Util.EMPTY_BYTE_ARRAY;
        }
        byte[] result = null;
        FileInputStream fis = null;
        ByteArrayOutputStream bos = null;
        try {
            fis = new FileInputStream(file);
            bos = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int idx;
            while ((idx = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, idx);
            }
            result = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (result == null) {
                result = Util.EMPTY_BYTE_ARRAY;
            }
            try {
                if (bos != null) {
                    bos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return result;
    }

    /**
     * json;text;xml;
     */
    public static byte[] convertString(String str) throws UnsupportedEncodingException {
        if (Util.strIsEmpty(str)) {
            return Util.EMPTY_BYTE_ARRAY;
        }
        return str.getBytes("UTF-8");
    }

    /**
     * form ----> key & val
     */
    public static byte[] convertForm(Map<String, String> form) throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();
        Util.buildQueryString(builder, form);
        return convertString(builder.toString());
    }


}
