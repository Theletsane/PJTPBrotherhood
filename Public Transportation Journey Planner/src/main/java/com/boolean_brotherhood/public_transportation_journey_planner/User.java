package com.boolean_brotherhood.public_transportation_journey_planner;

public class User {
    private String username;
    private String email;

    public User() {} // no-args constructor for Jackson

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
}