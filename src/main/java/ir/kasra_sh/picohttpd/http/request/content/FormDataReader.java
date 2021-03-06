package ir.kasra_sh.picohttpd.http.request.content;

import ir.kasra_sh.picohttpd.exceptions.FormDataParseException;
import ir.kasra_sh.picohttpd.http.request.Request;
import ir.kasra_sh.picohttpd.utils.MimeTypes;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class FormDataReader {
    private byte[] bytes;
    private String mime;
    List<FormData> formDataList = new ArrayList<>();

    public FormDataReader(Request r) throws FormDataParseException {
        this.bytes = r.getBody();
        this.mime = r.getHeader("content-type");
        formDataList = readFormData(r);
    }

    public FormDataReader() {
    }

    public FormData getFormData(String name) {
        for (FormData fd: formDataList) {
            if (fd.name().equals(name)) return fd;
        }
        return null;
    }

    public List<FormData> readFormData(Request r) throws FormDataParseException {
        if (bytes == null) {
            bytes = r.getBody();
            mime = r.getHeader("content-type");
        }
        return readFormData(r.getHeader("Content-Type").split("; boundary=")[1]);
    }

    public List<FormData> readFormData(String boundary) throws FormDataParseException {
//        ArrayList<FormData> fd = new ArrayList<>();
        if (formDataList.size()>0) return formDataList;
        ByteBuffer bos = ByteBuffer.allocate(bytes.length);
        int len = bytes.length;
        byte[] bnd = ("--" + boundary).getBytes();
        int mark = 0;
        int cur = 0;
        int i = 0;
        while (i < len) {
            if (bnd[cur] == bytes[i]) {
                mark = i;
                while (cur < bnd.length) {
                    if (bnd[cur] == bytes[i]) {
                        i++;
                        cur++;
                    } else {
                        i = mark;
                        cur=0;
                        break;
                    }
                }
                if (i == mark) {
                    bos.put(bytes[i]);
                    i++;
                    continue;
                }
                if (cur == bnd.length) {
                    if (bos.position()>0) {
                        i+=1;
                        formDataList.add(procFormData(bos));
                        bos.clear();
                    }
                    if (i == bytes.length-3) {
                        i+=1;
                    }
                } else {
                    continue;
                }
                cur=0;
            } else {
                bos.put(bytes[i]);
            }

            i++;

        }
        return formDataList;
    }

    private FormData procFormData(ByteBuffer b) throws FormDataParseException {
        b.flip();
        boolean he = false;
        StringBuilder sb = new StringBuilder(2048);
        FormData f = new FormData();
        byte[] bd = null;
        int i = 0;
        while (b.hasRemaining()) {
            byte cur = b.get();
            if (!he) {
                sb.append((char) cur);
                if (sb.length()>1) { // can check last 2 chars
                    if (sb.charAt(sb.length()-2) == '\r' &&
                            sb.charAt(sb.length()-1) == '\n') {
                        if (sb.length()==2) { // end of header
                            he = true;
                            bd = new byte[b.remaining()-2];
                        } else {
                            sb.setLength(sb.length()-2);
                            String[] kv = sb.toString().split(": ", 2);
                            if (kv.length == 2) {
                                f.putHeader(kv[0], kv[1]);
                                sb.setLength(0);
                            } else {
                                throw new FormDataParseException("FormData header error!");
                                // ERROR
                            }
                        }
                    }
                }
            } else { // headers ended
                if (i == bd.length) {
//                    System.out.println("from "+b.position());
                    b.get();
//                    System.out.println("to "+(b.position()));
                    continue;
                }
                bd[i] = cur;
                i++;
            }
        }
        f.setData(bd);
        return f;
    }

}
