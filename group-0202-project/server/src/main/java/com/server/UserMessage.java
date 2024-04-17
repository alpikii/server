package com.server;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class UserMessage {
    private String locationName;
    private String locationDescription;
    private String locationCity;
    private String locationCountry;
    private String locationStreetAddress;
    private ZonedDateTime originalPostingTime;
    private Double latitude = null;
    private Double longitude = null;
    

    public UserMessage (String locationName, String locationDescription, String locationCity, ZonedDateTime originalPostingTime, String country, String address) {
        //constructor
        this.locationName = locationName;
        this.locationDescription = locationDescription;
        this.locationCity = locationCity;
        this.locationCountry = country;
        this.locationStreetAddress = address;
        this.originalPostingTime = originalPostingTime;
    }

    long dateAsInt() {
        //returns originalPostingTime as long
        return originalPostingTime.toInstant().toEpochMilli();
    }
    
    void setSent(long epoch) {
        originalPostingTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC);
    } 

    //get
    public String getLocation(){
        return this.locationName;
    }

    public String getDescription(){
        return this.locationDescription;
    }

    public String getCity(){
        return this.locationCity;
    }

    public ZonedDateTime getTime() {
        return this.originalPostingTime;
    }

    public String getCountry(){
        return this.locationCountry;
    }

    public String getAddress () {
        return this.locationStreetAddress;
    }

    public Double getLongitude(){
        return this.longitude;
    }

    public Double getLatitude(){
        return this.latitude;
    }

    //set
    public void setLocation(String locationName){
        this.locationName = locationName;
    }

    public void setDescription(String locationDescription){
        this.locationDescription = locationDescription;
    }

    public void setCity(String locationCity){
        this.locationCity = locationCity;
    }

    public void setTime(ZonedDateTime originalPostingTime){
        this.originalPostingTime = originalPostingTime;
    }

    public void setCountry(String country){
        this.locationCountry = country;
    }

    public void setAddress (String address) {
        this.locationStreetAddress = address;
    }

    public void setLongitude(Double longitude){
        this.longitude = longitude;
    }

    public void setLatitude(Double latitude){
        this.latitude = latitude;
    }
}
