package webserver;

import model.User;
import util.HttpRequestUtils;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import java.util.Map;

public class UserRequestParser {
    public static User parser(String url) {
        int index = url.indexOf('?');

        if (index == -1) {
            throw new IllegalArgumentException("Invalid URL");
        }

        String requestParams = url.substring(index + 1);
        Map<String, String> params = HttpRequestUtils.parseQueryString(requestParams);

        String userId = decode(params.getOrDefault("userId", ""));
        String name = decode(params.getOrDefault("name", ""));
        String password = decode(params.getOrDefault("password", ""));
        String email = decode(params.getOrDefault("email", ""));

        return new User(userId, password, name, email);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
