package com.fastwok.crawler.util;

public class BodyRequest {
    public static String GetbodyAuth(String id, String secret) {
        return "scopes=PublicApi.Access&grant_type=client_credentials&client_id=" + id + "&client_secret=" + secret;
    }
    public static String UpdateAccdoc(String description , String status)
    {
        return "{" +
                "\"description\": \""+description+"-"+status+"\"" +
            "}";
    }
}
