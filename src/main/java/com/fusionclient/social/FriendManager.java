package com.fusionclient.social;

import com.fusionclient.FusionClientMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FriendManager {
    private static FriendManager instance;
    private final Set<FriendEntry> friends;
    private final Path friendsFilePath;
    private final Gson gson;

    private FriendManager() {
        this.friends = new HashSet<>();
        
        Path configDir = Paths.get(System.getProperty("user.home"), ".fusionclient");
        try {
            Files.createDirectories(configDir);
        } catch (Exception e) {
            FusionClientMod.LOGGER.error("Failed to create config directory", e);
        }
        
        this.friendsFilePath = configDir.resolve("friends.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        load();
    }

    public static FriendManager getInstance() {
        if (instance == null) {
            instance = new FriendManager();
        }
        return instance;
    }

    public boolean addFriend(String name) {
        String normalizedName = name.toLowerCase();
        
        for (FriendEntry friend : friends) {
            if (friend.getName().equals(normalizedName)) {
                return false;
            }
        }
        
        friends.add(new FriendEntry(normalizedName, System.currentTimeMillis()));
        save();
        return true;
    }

    public boolean removeFriend(String name) {
        String normalizedName = name.toLowerCase();
        
        boolean removed = friends.removeIf(friend -> friend.getName().equals(normalizedName));
        
        if (removed) {
            save();
        }
        return removed;
    }

    public boolean isFriend(String name) {
        String normalizedName = name.toLowerCase();
        
        for (FriendEntry friend : friends) {
            if (friend.getName().equals(normalizedName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isFriendIgnoreCase(String name) {
        for (FriendEntry friend : friends) {
            if (friend.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public Set<FriendEntry> getFriends() {
        return friends;
    }

    public int getFriendCount() {
        return friends.size();
    }

    public void clearFriends() {
        friends.clear();
        save();
    }

    public void save() {
        try {
            JsonArray jsonArray = new JsonArray();
            
            for (FriendEntry friend : friends) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", friend.getName());
                obj.addProperty("addedAt", friend.getAddedAt());
                if (friend.getNote() != null) {
                    obj.addProperty("note", friend.getNote());
                }
                jsonArray.add(obj);
            }
            
            try (FileWriter writer = new FileWriter(friendsFilePath.toFile())) {
                gson.toJson(jsonArray, writer);
            }
            
            FusionClientMod.LOGGER.info("Saved {} friends", friends.size());
        } catch (Exception e) {
            FusionClientMod.LOGGER.error("Failed to save friends", e);
        }
    }

    public void load() {
        try {
            File file = friendsFilePath.toFile();
            if (!file.exists()) {
                save();
                return;
            }
            
            friends.clear();
            
            try (FileReader reader = new FileReader(file)) {
                JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
                
                if (jsonArray != null) {
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JsonObject obj = jsonArray.get(i).getAsJsonObject();
                        String name = obj.get("name").getAsString();
                        long addedAt = obj.has("addedAt") ? obj.get("addedAt").getAsLong() : System.currentTimeMillis();
                        String note = obj.has("note") ? obj.get("note").getAsString() : null;
                        
                        FriendEntry friend = new FriendEntry(name, addedAt);
                        if (note != null) {
                            friend.setNote(note);
                        }
                        friends.add(friend);
                    }
                }
            }
            
            FusionClientMod.LOGGER.info("Loaded {} friends", friends.size());
        } catch (Exception e) {
            FusionClientMod.LOGGER.error("Failed to load friends", e);
        }
    }

    public static class FriendEntry {
        private final String name;
        private final long addedAt;
        private String note;

        public FriendEntry(String name, long addedAt) {
            this.name = name;
            this.addedAt = addedAt;
        }

        public String getName() {
            return name;
        }

        public long getAddedAt() {
            return addedAt;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }
}
