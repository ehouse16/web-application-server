package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import webserver.WebServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    String method;
    String url;
    String body;
    Map<String, String> headers = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(HttpRequest.class);


    public HttpRequest(BufferedReader reader) throws IOException{
        // 1. 요청 라인 파싱 ("GET /index.html HTTP/1.1")
        String line = reader.readLine();
        if(line == null || line.isEmpty()){
            throw new IOException("Empty Request line");
        }

        String[] header = line.split(" ");

        if(header.length < 2){
            log.debug("Headers 2개보다 적음");
        }
        this.method = header[0]; // method 저장
        this.url = header[1]; // URL 저장

        if("/favicon.ico".equals(url)) {
            log.debug("Favicon 요청 무시하기");
        }

        // Header 파싱
        while((line = reader.readLine()) != null && !line.isEmpty()){
            String[] headerToken = line.split(": ");

            headers.put(headerToken[0], headerToken[1]);
        }

        // 3. Body 파싱
        if(headers.containsKey("Content-Length")){
            int length = Integer.parseInt(headers.get("Content-Length"));
            char[] chars = new char[length];
            int read = reader.read(chars, 0, length);
            body = new String(chars, 0, read);
        }
    }

    public String getUrl(){
        return url;
    }

    public String getMethod(){
        return method;
    }

    public Map<String,String> getHeader(){
        return headers;
    }

    public String getBody(){
        return body;
    }

    public Map<String, String> getCookies(){
        String cookieHeader = headers.get("Cookie");

        return HttpRequestUtils.parseCookies(cookieHeader);
    }
}