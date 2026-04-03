package com.fusionclient.alt;

public class AltAccount {
    private String username;
    private String password;
    private boolean isCracked;
    private long lastLogin;
    
    public AltAccount(String username, String password, boolean isCracked) {
        this.username = username;
        this.password = password;
        this.isCracked = isCracked;
        this.lastLogin = 0;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public boolean isCracked() {
        return isCracked;
    }
    
    public long getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(long time) {
        this.lastLogin = time;
    }
}
