package com.server;
import java.sql.SQLException;

public class  UserAuthenticator extends com.sun.net.httpserver.BasicAuthenticator {

    private MessageDatabase db = MessageDatabase.getInstance();
    
    public UserAuthenticator() {
        super("info");
    }

    public boolean checkCredentials(String username, String password) {
        // Check if the provided username/password match the ones in database
        boolean valid;
        try {
            valid = db.checkCredentials(username, password);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return valid;
    }

    public boolean addUser(User user) throws SQLException {
        if (db.addUser(user) == false) {
            return false;
        }
        return true;    
    }

}