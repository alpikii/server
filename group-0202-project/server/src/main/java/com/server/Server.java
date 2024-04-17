package com.server;
import java.net.InetSocketAddress;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.Base64;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;
import org.json.JSONException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;



public class Server implements com.sun.net.httpserver.HttpHandler {
    
    private Server() {}

    private MessageDatabase db = new MessageDatabase();

    String response = "Something went wrong";
    int code = 400;

    @Override
    public void handle(HttpExchange t) throws IOException {
        //GET and POST handling for path /info

        if (t.getRequestMethod().equalsIgnoreCase("POST")) {
            //Handle POST requests here (users send this for sending messages)
            String contentType = "";

            if (t.getRequestHeaders().containsKey("Content-Type")) {
                //check that message has content type otherwise set code as 411
                contentType = t.getRequestHeaders().get("Content-Type").get(0);

            } else {
                response = "No content type in request";
                code = 411;
            }
            if (contentType.equalsIgnoreCase("application/json")) {
                // check that content-type is application/json otherwise set code as 407
                InputStream stream = t.getRequestBody();
                BufferedReader bReader = new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8));
                
                try{
                    //try catch for json exception that jsonobject might throw, the format is wrong if throws exception
                    JSONObject obj = new JSONObject(bReader.lines().collect(Collectors.joining("\n")));
                    
                    //check that json has all the needed keys and that the values are correct variable type
                    if(obj.has("locationName") && obj.get("locationName")instanceof String && 
                        obj.has("locationDescription") &&obj.get("locationDescription") instanceof String &&
                        obj.has("locationCity") &&obj.get("locationCity") instanceof String && 
                        obj.has("originalPostingTime") &&obj.get("originalPostingTime") instanceof String &&
                        obj.has("locationCountry") &&obj.get("locationCountry") instanceof String &&
                        obj.has("locationStreetAddress") &&obj.get("locationStreetAddress")instanceof String) {
                        
                        try {
                            //try catch for datetimeparseexception that checking that the time is in correct format might throw
                            // if that exception is thrown -> time is in wrong format
                            //formatter to check that originalPostingTime is in correct format
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd'T'HH:mm:ss.SSSX");
                            formatter.parse(obj.getString("originalPostingTime"));
                            LocalDateTime time = OffsetDateTime.parse((CharSequence) obj.getString("originalPostingTime")).toLocalDateTime();
                            ZonedDateTime zoneDateTime = time.atZone(ZoneId.of("UTC"));

                            // get credentials so that nickname can be included in the user message
                            String encodedCredentials = t.getRequestHeaders().get("Authorization").get(0).split(" ")[1];
                            // Decode and split credentials into username and password
                            String[] decodedCredentials = new String(Base64.getDecoder().decode(encodedCredentials)).split(":");
                            
                            UserMessage message = new UserMessage(obj.getString("locationName"), 
                                obj.getString("locationDescription"), 
                                obj.getString("locationCity"), 
                                zoneDateTime,
                                obj.getString("locationCountry"), 
                                obj.getString("locationStreetAddress"));

                            if (obj.has("latitude") && (obj.get("latitude") instanceof BigDecimal || obj.get("latitude") instanceof Double) 
                                && (obj.has("longitude") && (obj.get("longitude") instanceof BigDecimal || obj.get("longitude") instanceof Double))) {
                                //if json included latitude and longitude check that they are in correct format and add to message if they are
                                message.setLatitude(obj.getDouble("latitude"));
                                message.setLongitude(obj.getDouble("longitude"));
                            }
                            
                            //add message to database and include username so that addmessage method in database can include nickname
                            if (db.addMessage(message, decodedCredentials[0]) == true){
                                response = "successfull";
                                code = 200;
                            }
                            stream.close();
                        } catch (DateTimeParseException e) {
                            response = "time in wrong format";
                            code = 407;
                        }
                    } else {
                        response = "incorrect format";
                        code = 400;
                    } 
                }catch(JSONException e){
                    e.printStackTrace();
                    response = "incorrect format";
                    code = 400;
                } 
                stream.close();
            } else {
                response = "content type is not application/json";
                code = 407;
            }

        } else if (t.getRequestMethod().equalsIgnoreCase("GET")) {
            // Handle GET requests here (users use this to get messages
            try {
                String messagesfromDB = db.getMessage();
                //check if the string is empty, because we use getstring on an array 
                //it alway has brackets thats why bigger than 2 == there's info in the table
                if (messagesfromDB.length() <= 2 ) {  
                    response = "No messages";
                    code = 204;
                } else {
                    t.getResponseHeaders().set("Content-Type", "application/json");
                    response = messagesfromDB;
                    code = 200;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
        // Inform user here that only POST and GET functions are supported and send an error code
            response = "Not supported";
            code = 400;
        }

        //send response to message
        OutputStream outputStream =  t.getResponseBody();
        byte [] messagebytes = response.getBytes("UTF-8");
        t.sendResponseHeaders(code, messagebytes.length);
        outputStream.write(messagebytes);
        outputStream.flush();
        outputStream.close();
    }


    private static SSLContext myServerSSLContext(String[] args) throws Exception {
        char[] passphrase = args[1].toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(args[0]), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return ssl;
    }


    public static void main(String[] args) throws Exception {
        
        try {
            //create the http server to port 8001 with default logger
            HttpsServer server = HttpsServer.create(new InetSocketAddress(8001),0);
            
            SSLContext sslContext = myServerSSLContext(args);
            server.setHttpsConfigurator (new HttpsConfigurator(sslContext) {
                public void configure (HttpsParameters params) {
                    //InetSocketAddress remote = params.getClientAddress();
                    SSLContext c = getSSLContext();
                    SSLParameters sslparams = c.getDefaultSSLParameters();
                    params.setSSLParameters(sslparams);
                    }
            });
            UserAuthenticator userAuthenticator = new UserAuthenticator();

             //create context that defines path for the resource, in this case a "help"
            HttpContext context = server.createContext("/info", new Server());
            context.setAuthenticator(userAuthenticator);
            server.createContext("/registration", new RegistrationHandler(userAuthenticator));
            // creates a default executor 
            server.setExecutor(Executors.newCachedThreadPool());
            server.start(); 
        } catch (FileNotFoundException e) {
            // Certificate file not found!
            System.out.println("Certificate not found!");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}