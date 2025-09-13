package webserver;

import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequest;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;
    private BufferedReader bf = null;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            bf = new BufferedReader(new InputStreamReader(in));
            DataOutputStream dos = new DataOutputStream(out);

            // 1. 요청 파싱
            HttpRequest request = new HttpRequest(bf);

            // 2. HTTP method에 따라 분기
            if("GET".equals(request.getMethod())){
                doGet(request, dos);
            } else if("POST".equals(request.getMethod())){
                doPost(request, dos);
            } else{
                log.info(request.getMethod() + " not supported");
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void doGet(HttpRequest request, DataOutputStream dos) throws IOException {
        String url = request.getUrl();

        if(url.equals("/"))
            url = "/index.html";

        try{
            File file = new File("./webapp" + url);
            byte[] bytes = Files.readAllBytes(file.toPath());

            response200Header(dos, bytes.length, url);
            responseBody(dos, bytes);
        } catch(NoSuchFileException e) {
            log.warn(e.getMessage());
            response404Header(dos, e.getMessage().getBytes().length);
            responseBody(dos, e.getMessage().getBytes());
        }
    }

    private void doPost(HttpRequest request, DataOutputStream dos) throws IOException {
        String url = request.getUrl();

        if(url.startsWith("/user/create")) {
            User user = UserRequestParser.parserFromBody(request.getBody());
            log.debug("New User Created : {}", user);

            response302Header(dos, "/index.html");
        } else{
            log.info(request.getMethod() + " not supported");
            responseBody(dos, "Not Found".getBytes());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent, String url){
        try {
            String contentType = getContentType(url);
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: " + contentType + "\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private String getContentType(String url) {
        if(url.endsWith(".css")) return "text/css";
        if(url.endsWith(".js")) return "text/javascript";
        if(url.endsWith(".png")) return "image/png";
        if(url.endsWith("jpg") || url.endsWith("jpeg")) return "image/jpeg";

        return "text/html;charset=utf-8";
    }

    private void response404Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 404 Not Found \r\n");
            dos.writeBytes("Content-Type: text/plain;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302Header(DataOutputStream dos, String location) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: " + location + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
