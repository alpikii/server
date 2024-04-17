package com.server;

public class User {
    private String username;
    private String password;
    private String email;
    private String userNickname;

    public User (String user, String passwd, String mail, String nick) {
        //constructor
        this.username = user;
        this.password = passwd;
        this.email = mail;
        this.userNickname = nick;
    }
    
    //get
    public String getUserName(){
        return this.username;
    }

    public String getPassword(){
        return this.password;
    }

    public String getEmail(){
        return this.email;
    }

    public String getNickname(){
        return this.userNickname;
    }

    //set
    public void setUserName(String username){
        this.username = username;
    }

    public void setPassword(String password){
        this.password = password;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public void setNickname(String nick){
        this.userNickname = nick;
    }
}
