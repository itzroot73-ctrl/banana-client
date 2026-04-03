package com.fusionclient.alt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.fusionclient.FusionClientMod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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

public class AltManager {
    private static AltManager instance;
    private final List<AltAccount> accounts = new ArrayList<>();
    private final Path altFilePath;
    private final Gson gson;
    
    private AltManager() {
        Path configDir = Paths.get(System.getProperty("user.home"), ".fusionclient");
        try {
            Files.createDirectories(configDir);
        } catch (Exception e) {
            FusionClientMod.LOGGER.error("Failed to create config directory", e);
        }
        this.altFilePath = configDir.resolve("alts.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    public static AltManager getInstance() {
        if (instance == null) {
            instance = new AltManager();
        }
        return instance;
    }
    
    public void addAccount(String username, String password, boolean isCracked) {
        accounts.add(new AltAccount(username, password, isCracked));
        save();
    }
    
    public void removeAccount(int index) {
        if (index >= 0 && index < accounts.size()) {
            accounts.remove(index);
            save();
        }
    }
    
    public List<AltAccount> getAccounts() {
        return accounts;
    }
    
    public void save() {
        try {
            JsonArray jsonArray = new JsonArray();
            
            for (AltAccount account : accounts) {
                JsonObject obj = new JsonObject();
                obj.addProperty("username", account.getUsername());
                obj.addProperty("password", account.getPassword());
                obj.addProperty("isCracked", account.isCracked());
                obj.addProperty("lastLogin", account.getLastLogin());
                jsonArray.add(obj);
            }
            
            try (FileWriter writer = new FileWriter(altFilePath.toFile())) {
                gson.toJson(jsonArray, writer);
            }
        } catch (Exception e) {
            FusionClientMod.LOGGER.error("Failed to save alt accounts", e);
        }
    }
    
    public void load() {
        try {
            File file = altFilePath.toFile();
            if (!file.exists()) {
                return;
            }
            
            try (FileReader reader = new FileReader(file)) {
                JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
                
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject obj = jsonArray.get(i).getAsJsonObject();
                    String username = obj.get("username").getAsString();
                    String password = obj.has("password") ? obj.get("password").getAsString() : "";
                    boolean isCracked = obj.has("isCracked") && obj.get("isCracked").getAsBoolean();
                    
                    AltAccount account = new AltAccount(username, password, isCracked);
                    if (obj.has("lastLogin")) {
                        account.setLastLogin(obj.get("lastLogin").getAsLong());
                    }
                    accounts.add(account);
                }
            }
        } catch (Exception e) {
            FusionClientMod.LOGGER.error("Failed to load alt accounts", e);
        }
    }
}
