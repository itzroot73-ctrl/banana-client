package com.fusionclient.command;

import com.fusionclient.FusionClientMod;
import com.fusionclient.module.Module;
import com.fusionclient.module.ModuleManager;
import com.fusionclient.social.FriendManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CommandManager {
    private static CommandManager instance;
    private static final char PREFIX = '.';
    private final Map<String, Command> commands;
    private final MinecraftClient client;

    private CommandManager() {
        this.commands = new HashMap<>();
        this.client = MinecraftClient.getInstance();
        registerDefaultCommands();
    }

    public static CommandManager getInstance() {
        if (instance == null) {
            instance = new CommandManager();
        }
        return instance;
    }

    private void registerDefaultCommands() {
        registerCommand(new HelpCommand());
        registerCommand(new BindCommand());
        registerCommand(new FriendCommand());
        registerCommand(new ToggleCommand());
        registerCommand(new ConfigCommand());
    }

    public void registerCommand(Command command) {
        commands.put(command.getName().toLowerCase(), command);
        
        for (String alias : command.getAliases()) {
            commands.put(alias.toLowerCase(), command);
        }
    }

    public void handleMessage(String message) {
        if (message.isEmpty() || message.charAt(0) != PREFIX) {
            return;
        }

        String rawArgs = message.substring(1);
        String[] parts = rawArgs.split(" ");
        String commandName = parts[0].toLowerCase();
        String[] args = parts.length > 1 ? java.util.Arrays.copyOfRange(parts, 1, parts.length) : new String[0];

        Command command = commands.get(commandName);
        
        if (command != null) {
            try {
                command.execute(args);
            } catch (Exception e) {
                sendMessage("§cError executing command: " + e.getMessage());
                FusionClientMod.LOGGER.error("Command error: " + commandName, e);
            }
        } else {
            sendMessage("§cUnknown command: " + commandName + ". Use .help for list.");
        }
    }

    public void sendMessage(String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), false);
        }
    }

    public void sendChatMessage(String message) {
        if (client.player != null) {
            client.player.networkHandler.sendChatMessage(message);
        }
    }

    public Map<String, Command> getCommands() {
        return commands;
    }

    public static abstract class Command {
        private final String name;
        private final String description;
        private final String usage;
        private final List<String> aliases;

        public Command(String name, String description, String usage, String... aliases) {
            this.name = name;
            this.description = description;
            this.usage = usage;
            this.aliases = new ArrayList<>();
            for (String alias : aliases) {
                this.aliases.add(alias);
            }
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getUsage() {
            return usage;
        }

        public List<String> getAliases() {
            return aliases;
        }

        public abstract void execute(String[] args);

        protected void sendMessage(String message) {
            CommandManager.getInstance().sendMessage(message);
        }
    }

    private class HelpCommand extends Command {
        public HelpCommand() {
            super("help", "Show available commands", ".help [command]");
        }

        @Override
        public void execute(String[] args) {
            if (args.length > 0) {
                Command cmd = commands.get(args[0].toLowerCase());
                if (cmd != null) {
                    sendMessage("§6=== §e" + cmd.getName() + " §6===");
                    sendMessage("§7" + cmd.getDescription());
                    sendMessage("§eUsage: §f" + PREFIX + cmd.getUsage());
                    return;
                }
                sendMessage("§cUnknown command: " + args[0]);
                return;
            }

            sendMessage("§6=== §eBanana Client Commands §6===");
            for (Command cmd : commands.values()) {
                sendMessage("§e" + PREFIX + cmd.getName() + " §7- " + cmd.getDescription());
            }
            sendMessage("§6================================");
        }
    }

    private class BindCommand extends Command {
        public BindCommand() {
            super("bind", "Bind module to key", ".bind <module> <key>");
        }

        @Override
        public void execute(String[] args) {
            if (args.length < 2) {
                sendMessage("§cUsage: .bind <module> <key>");
                sendMessage("§7Example: §f.bind Kill Aura KEY_G");
                return;
            }

            String moduleName = args[0];
            String keyName = args[1];

            Module module = ModuleManager.getInstance().getModule(moduleName);
            
            if (module == null) {
                sendMessage("§cModule not found: " + moduleName);
                List<Module> modules = new ArrayList<>(ModuleManager.getInstance().getAllModules());
                if (!modules.isEmpty()) {
                    sendMessage("§7Available modules:");
                    for (Module m : modules) {
                        sendMessage("§f- " + m.getName());
                    }
                }
                return;
            }

            try {
                int keyCode = net.minecraft.util.Util.getArbitraryKeyCode(keyName.toUpperCase());
                
                if (keyCode == -1) {
                    keyCode = net.minecraft.client.util.InputUtil.fromTranslationKey("key.keyboard." + keyName.toLowerCase()).getCode();
                }

                if (keyCode == 0) {
                    sendMessage("§cUnknown key: " + keyName);
                    return;
                }

                module.setKeybind(keyCode);
                sendMessage("§aBound §e" + module.getName() + " §ato §f" + keyName);
                
                com.fusionclient.config.ConfigManager.getInstance().saveAsync();
            } catch (Exception e) {
                sendMessage("§cFailed to bind: " + e.getMessage());
            }
        }
    }

    private class FriendCommand extends Command {
        public FriendCommand() {
            super("friend", "Manage friends", ".friend add/remove/list <name>");
        }

        @Override
        public void execute(String[] args) {
            if (args.length == 0) {
                sendMessage("§cUsage: .friend add/remove/list <name>");
                return;
            }

            String action = args[0].toLowerCase();

            switch (action) {
                case "add":
                    if (args.length < 2) {
                        sendMessage("§cUsage: .friend add <name>");
                        return;
                    }
                    String addName = args[1];
                    if (FriendManager.getInstance().addFriend(addName)) {
                        sendMessage("§aAdded §e" + addName + " §ato friends!");
                    } else {
                        sendMessage("§e" + addName + " §cis already your friend");
                    }
                    break;

                case "remove":
                case "delete":
                    if (args.length < 2) {
                        sendMessage("§cUsage: .friend remove <name>");
                        return;
                    }
                    String removeName = args[1];
                    if (FriendManager.getInstance().removeFriend(removeName)) {
                        sendMessage("§aRemoved §e" + removeName + " §afrom friends");
                    } else {
                        sendMessage("§e" + removeName + " §cis not your friend");
                    }
                    break;

                case "list":
                    sendMessage("§6=== §eFriends §6(" + FriendManager.getInstance().getFriendCount() + ") §6===");
                    for (FriendManager.FriendEntry friend : FriendManager.getInstance().getFriends()) {
                        sendMessage("§e" + friend.getName());
                    }
                    sendMessage("§6================================");
                    break;

                default:
                    sendMessage("§cUnknown action: " + action);
                    sendMessage("§7Usage: .friend add/remove/list <name>");
            }
        }
    }

    private class ToggleCommand extends Command {
        public ToggleCommand() {
            super("toggle", "Toggle a module", ".toggle <module>");
        }

        @Override
        public void execute(String[] args) {
            if (args.length < 1) {
                sendMessage("§cUsage: .toggle <module>");
                return;
            }

            String moduleName = args[0];
            Module module = ModuleManager.getInstance().getModule(moduleName);

            if (module == null) {
                sendMessage("§cModule not found: " + moduleName);
                return;
            }

            module.toggle();
            String status = module.isEnabled() ? "§aEnabled" : "§cDisabled";
            sendMessage(status + " §e" + module.getName());
        }
    }

    private class ConfigCommand extends Command {
        public ConfigCommand() {
            super("config", "Save/Load configuration", ".config save/load");
        }

        @Override
        public void execute(String[] args) {
            if (args.length < 1) {
                sendMessage("§cUsage: .config save/load");
                return;
            }

            String action = args[0].toLowerCase();

            switch (action) {
                case "save":
                    com.fusionclient.config.ConfigManager.getInstance().save();
                    sendMessage("§aConfiguration saved!");
                    break;
                case "load":
                    com.fusionclient.config.ConfigManager.getInstance().load();
                    sendMessage("§aConfiguration loaded!");
                    break;
                default:
                    sendMessage("§cUnknown action: " + action);
            }
        }
    }
}
