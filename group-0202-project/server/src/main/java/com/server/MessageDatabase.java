package com.server;

import java.io.File;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.sql.ResultSet;

import org.apache.commons.codec.digest.Crypt;
import org.json.JSONArray;
import org.json.JSONObject;

public class MessageDatabase {

    private Connection connection = null;
    private static MessageDatabase dbInstance = null;
    private SecureRandom sr;

    public MessageDatabase (){
        //constructor
        try {
            open("MessageDB");
        } catch (SQLException e){
            System.out.println("Error in initializing database");
            e.printStackTrace();
        }
    }

    public static synchronized MessageDatabase getInstance() {
		if (null == dbInstance) {
			dbInstance = new MessageDatabase();
		}
        return dbInstance;
    }

    public void open(String dbname) throws SQLException{
        //method for opening database, if it doesn't exist create new one with init()
        File db = new File(dbname);
        
        if(db.exists() && !db.isDirectory()) { 
            try {
                String database = "jdbc:sqlite:" + dbname;
                this.connection = DriverManager.getConnection(database);
            } catch (SQLException e) {
                e.printStackTrace(System.out);
            }
            
        } else {
            try {
                init();
            } catch (SQLException e){
                //TODO: response to server that database could not be opened rather than print
                System.out.println("Error in initializing database");
                e.printStackTrace();
            }
        }
        
    }

    private boolean init() throws SQLException{
        //CREATE DATABASE
        String dbName = "MessageDB";

        String database = "jdbc:sqlite:" + dbName;
        try {
            connection = DriverManager.getConnection(database);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //if connection is not null create tables
        if (null != connection) {
            //create users table
            String createUsersString = "CREATE TABLE users (username varchar(50) NOT NULL, "
            + "password varchar(100) NOT NULL, email varchar(50), "
            + "nickname varchar(50), primary key(username))";
            Statement createStatement = connection.createStatement();
            createStatement.executeUpdate(createUsersString);
            createStatement.close();
            //create table for messages
            String createMessagesString = "CREATE TABLE messages (locationName varchar(50) NOT NULL,"
            + "locationDescription varchar(250) NOT NULL, "
            + "locationCity varchar(50), "
            + "locationCountry varchar(50), "
            + "locationStreetAddress varchar(50), "
            + "originalPoster varchar(50), "
            + "originalPostingTime long, "
            + "latitude double, longitude double)";
            Statement createStat = connection.createStatement();
            createStat.executeUpdate(createMessagesString);
            createStat.close();
            return true;
        }
        return false;
    }

    public boolean addUser(User user)throws SQLException{
        //method for adding new user to database
        Statement statement;
        this.sr = new SecureRandom();
       
        if(checkIfUserExists(user.getUserName())){
            System.out.println("user exists");
            return false;
        }else{
            byte bytes[] = new byte[13];
            sr.nextBytes(bytes); 
            String saltBytes = new String(Base64.getEncoder().encode(bytes));
            String salt = "$6$" + saltBytes; 
            String hashedPassword = Crypt.crypt(user.getPassword(), salt); 
            
            String setUserString = "insert into users " +
            "VALUES('" + user.getUserName() + 
            "','" + hashedPassword + "','" + 
            user.getEmail() + "','" +
            user.getNickname() + "')";
            statement = connection.createStatement();
            statement.executeUpdate(setUserString);
            statement.close();
            return true;
            
        }
    }

    public boolean addMessage(UserMessage message, String username)  {
        //method to add messages to database
        //latitude and longitude optional
        long time = message.dateAsInt();
        Statement statement;
        String setMessageString;
        try {
            String nickname = getNickname(username);
            if (message.getLatitude() == null || message.getLongitude()== null) {
                setMessageString = "insert into messages " +
                "VALUES('" + message.getLocation() + "','" + 
                message.getDescription() + "','" + 
                message.getCity() + "','" +
                message.getCountry() + "','" +
                message.getAddress() + "','" +
                nickname + "','" +
                time + "','" +
                "200.0" +"','" + //put incorrect coordinates that will be checked later and nto included in response
                "200.0"+ "')";  //getMessages would not work with null coordinates
                statement = connection.createStatement();
                statement.executeUpdate(setMessageString);
                statement.close();
            } else {
                setMessageString = "insert into messages " +
                "VALUES('" + message.getLocation() + "','" + 
                message.getDescription() + "','" + 
                message.getCity() + "','" +
                message.getCountry() + "','" +
                message.getAddress() + "','" +
                nickname + "','" +
                time + "','" +
                message.getLatitude() + "','" +
                message.getLongitude() +"')"; 
                statement = connection.createStatement();
                statement.executeUpdate(setMessageString);
                statement.close();
            }
            return true;
        } catch (Exception e){
            //return boolean based on if succesfull if not and inform user
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkIfUserExists(String givenUserName) throws SQLException{
        //check if user is already registered, used in user registration
        Statement queryStatement = null;
        ResultSet rs;

        String checkUser = "select username from users where username = '" + givenUserName + "'";
        queryStatement = connection.createStatement();
		rs = queryStatement.executeQuery(checkUser);
        
        if(rs.next()){
            queryStatement.close();
            return true;
        }else{
            queryStatement.close();
            return false;
        }
    }

    public boolean checkCredentials(String username, String password) throws SQLException{
        //check that credentials are valid
        Statement queryStatement = null;
        ResultSet rs;

        String checkUser = "SELECT username, password FROM users where username = '" + username + "'";
        queryStatement = connection.createStatement();
		rs = queryStatement.executeQuery(checkUser);
        
        if(rs.next() == false){
            queryStatement.close();
            return false;
        }else{
            if (rs.getString("password").equals(Crypt.crypt(password, rs.getString("password")))) {
                queryStatement.close();
                return true; // user authenticated
            } else {
                queryStatement.close();
                return false;
            }
        }
    }

    public String getMessage() throws SQLException{
        //methdod to get messages from database
        Statement queryStatement = null;
        ResultSet rs;
        JSONArray array = new JSONArray();
        
        String query = "select * from messages";
        queryStatement = connection.createStatement();
		rs = queryStatement.executeQuery(query);
        while (rs.next()){
            JSONObject ob = new JSONObject();
            ob.put("locationName", rs.getString("locationName"));
            ob.put("locationDescription", rs.getString("locationDescription"));
            ob.put("locationCity", rs.getString("locationCity"));
            ob.put("locationCountry", rs.getString("locationCountry"));
            ob.put("locationStreetAddress", rs.getString("locationStreetAddress"));
            ob.put("originalPoster", rs.getString("originalPoster"));
            ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochMilli(rs.getLong("originalPostingTime")), ZoneOffset.UTC);
            ob.put("originalPostingTime", date);
            //put latitude and longitude if they have been saved into the message and ccheck that they are valid coordinates
            //TODO:cheking coordinate validity could be done before saving it to database (null values don't work in table)
            if (rs.getDouble("latitude") <= 180.0 && rs.getDouble("latitude") >= -180.0) {
                ob.put("latitude", rs.getDouble("latitude"));
            }
            if (rs.getDouble("longitude") <= 180.0 && rs.getDouble("longitude") >= -180.0) {
                ob.put("longitude", rs.getDouble("longitude"));
            }
            array.put(ob);
        }
        queryStatement.close();
        return array.toString();
    }


    public String getNickname(String username) throws SQLException {
        //method to get nickanme from username
        String nickname = null;
        
        String query = "SELECT nickname FROM users WHERE username = ?";

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, username);
        ResultSet rs = statement.executeQuery();

        if (rs.next()) {
            nickname = rs.getString("nickname");
        }

        rs.close();
        statement.close();
        return nickname;
    }

    public void closeDB() throws SQLException {
		if (connection != null) {
			connection.close();
            System.out.println("closing database connection");
			connection = null;
		}
    }
}

