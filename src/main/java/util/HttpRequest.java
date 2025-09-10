package util;

import java.io.BufferedReader;
import java.io.IOException;

public class HttpRequest {
    String url;
    StringBuilder fullHttpRequest = new StringBuilder();

    public HttpRequest(BufferedReader reader) throws IOException{
        String http = reader.readLine();

        if(http == null) {
            throw new IOException("Not Found");
        }

        String[] tokens = http.split(" ");
        url = tokens[1];

        if("/favicon.ico".equals(url)) {
            throw new IOException("Favicon 요청 무시하기");
        }

        String httpLines;
        while((httpLines = reader.readLine()) != null) {
            if(httpLines.isEmpty()) {
                break;
            }

            fullHttpRequest.append(httpLines).append("\n");
        }
    }

    public String getUrl(){
        return url;
    }

    public String getFullHttpRequest(){
        return fullHttpRequest.toString();
    }
}