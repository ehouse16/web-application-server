package webserver;

import model.Database;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequest;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Map;

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
                if(request.getUrl().equals("/user/list")){
                    handleUserList(request, dos);
                }else {
                    doGet(request, dos);
                }
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

            //회원가입 하면 유저 추가하기
            Database.addUser(user);

            log.debug("New User Created : {}", user);

            response302Header(dos, "/index.html");
        }
        //로그인으로 넘어갈 때
        else if(url.startsWith("/user/login")) {
            Map<String, String> params = UserRequestParser.getParams(request.getBody());

            User user = Database.getUser(params.get("userId"));

            if(user == null){
                log.debug("User not found : {}", params.get("userId"));
                response302HeaderWithCookie(dos, "/user/login_failed.html", "logined=false");
            }else if(user.getPassword().equals(params.get("password"))){
                log.debug("Login Success");
                response302HeaderWithCookie(dos, "/index.html", "logined=true");
            }else{
                log.debug("Wrong password : {}", params.get("password"));
                response302HeaderWithCookie(dos, "/user/login_failed.html", "logined=false");
            }
        }
        else{
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

    private void response302HeaderWithCookie(DataOutputStream dos, String location, String cookie) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: " + location + "\r\n");
            dos.writeBytes("Set-Cookie: " + cookie + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void handleUserList(HttpRequest request, DataOutputStream dos) throws IOException {
        Map<String, String> cookies = request.getCookies();
        String logined = cookies.get("logined");

        log.info("로그인 여부 {} ", logined);

        if ("true".equals(logined)) {
            StringBuilder sb = new StringBuilder();
            sb.append("<html><body>");
            sb.append("<h1>User List</h1>");
            sb.append("<table border='1'>");
            sb.append("<tr><th>UserId</th><th>Name</th><th>Email</th></tr>");

            for (User user : Database.findAll()) {
                sb.append("<tr>")
                        .append("<td>").append(user.getUserId()).append("</td>")
                        .append("<td>").append(user.getName()).append("</td>")
                        .append("<td>").append(user.getEmail()).append("</td>")
                        .append("</tr>");
            }

            sb.append("</table>");
            sb.append("</body></html>");

            byte[] body = sb.toString().getBytes();
            response200Header(dos, body.length, request.getUrl());
            responseBody(dos, body);
        } else {
            response302Header(dos, "/user/login.html");
        }
    }
}
