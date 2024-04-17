package com.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;

public class RegistrationHandler  implements com.sun.net.httpserver.HttpHandler{
    private UserAuthenticator userAuthenticator;

    public RegistrationHandler(UserAuthenticator userAuthenticator) {
        //constructor
        this.userAuthenticator = userAuthenticator;
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        //GET and POST handling for path /registration
        
        String response = "Something went wrong";
        int code = 400;

        try {
            if (t.getRequestMethod().equalsIgnoreCase("POST")) {
                //Handle POST requests here (users send this for sending messages)
                String contentType = "";
                if (t.getRequestHeaders().containsKey("Content-Type")) {
                    //check that message has content type otherwise set code as 411
                    contentType = t.getRequestHeaders().get("Content-Type").get(0);
                } else {
                    code = 411;
                    response = "No content type in request";
                }
                if (contentType.equalsIgnoreCase("application/json")) {
                    // check that content-type is application/json otherwise set code as 407
                    InputStream stream = t.getRequestBody();
                    BufferedReader bReader = new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8));
                    String newUser = bReader.lines().collect(Collectors.joining("\n"));
                    stream.close();
                    try{
                        //try catch for json exception that jsonobject might throw, the format is wrong if throws exception
                        JSONObject obj = new JSONObject(newUser);
                        //check that json has all the needed keys and that the values are correct variable type
                        if(obj != null && obj.has("username") && obj.get("username") instanceof String
                            && obj.has("password") && obj.get("password") instanceof String
                            && obj.has("email") && obj.get("email") instanceof String
                            && obj.has("userNickname") && obj.get("userNickname") instanceof String){
                            //check that username and password are not empty
                            if (obj.getString("username").length()>0 && obj.getString("password").length()>0) {
                                User user = new User(obj.getString("username"), obj.getString("password"), obj.getString("email"), obj.getString("userNickname"));
                                
                                if(userAuthenticator.addUser(user) == false){
                                    code = 405;
                                    response ="user already exist";
                                }else{
                                    code = 200;
                                    response = "User registered";
                                }
                            }else{
                            code = 413;
                            response ="no proper user credentials";
                            }
                        }else {
                            code = 412;
                            response = "No user credentials";
                        }   
                    }catch(JSONException e){
                        code = 412;
                        response = "json parse error, faulty user json";
                    }
                        
                }else{
                    code = 407;
                    response = "content type is not application/json";
                }
            } else {
                // Inform user here that only POST is supported and send an error code
                code = 401;
                response = "only POST is accepted";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //send response
        byte[] messagebytes = response.getBytes("UTF-8");
        t.sendResponseHeaders(code, messagebytes.length);
        OutputStream stream = t.getResponseBody();
        stream.write(messagebytes);
        stream.flush();
        stream.close();

    }

}