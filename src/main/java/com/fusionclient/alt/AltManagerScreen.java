package com.fusionclient.alt;

import com.fusionclient.FusionClientMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class AltManagerScreen extends Screen {
    private final Screen parent;
    private List<AltAccount> accounts;
    private int selectedIndex = -1;
    
    private static final int ORANGE_COLOR = 0xFFFFA500;
    private static final int WHITE_COLOR = 0xFFFFFFFF;
    private static final int BG_COLOR = 0x80FFFFFF;
    private static final int DISABLED_COLOR = 0xFF303030;
    private static final int HOVER_COLOR = 0xFF505050;
    
    private String newUsername = "";
    private String newPassword = "";
    private boolean addingAccount = false;
    
    private int hoveredButton = -1;

    public AltManagerScreen(Screen parent) {
        super(Text.literal("Alt Manager"));
        this.parent = parent;
        this.accounts = AltManager.getInstance().getAccounts();
    }

    @Override
    protected void init() {
        super.init();
        this.accounts = AltManager.getInstance().getAccounts();
        addingAccount = false;
        newUsername = "";
        newPassword = "";
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int panelWidth = 400;
        int panelHeight = 350;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        context.fill(0, 0, this.width, this.height, 0xCC000000);
        
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, BG_COLOR);
        context.drawBorder(panelX, panelY, panelWidth, panelHeight, ORANGE_COLOR);
        
        context.drawText(this.textRenderer, "Alt Manager", panelX + 20, panelY + 15, ORANGE_COLOR, false);
        
        context.fill(panelX + 10, panelY + 35, panelX + panelWidth - 10, panelY + 36, ORANGE_COLOR);
        
        int listY = panelY + 45;
        int listHeight = panelHeight - 100;
        
        for (int i = 0; i < accounts.size(); i++) {
            if (listY + 50 > panelY + listHeight + panelY) break;
            
            AltAccount account = accounts.get(i);
            boolean isHovered = mouseX >= panelX + 15 && mouseX <= panelX + panelWidth - 15 && 
                              mouseY >= listY && mouseY <= listY + 40;
            boolean isSelected = i == selectedIndex;
            
            int bgColor = isSelected ? ORANGE_COLOR : (isHovered ? HOVER_COLOR : DISABLED_COLOR);
            context.fill(panelX + 10, listY, panelX + panelWidth - 10, listY + 40, bgColor);
            context.drawBorder(panelX + 10, listY, panelWidth - 20, 40, ORANGE_COLOR);
            
            context.drawText(this.textRenderer, account.getUsername(), panelX + 20, listY + 12, WHITE_COLOR, false);
            
            String status = account.isCracked() ? "[Cracked]" : "[Premium]";
            context.drawText(this.textRenderer, status, panelX + 20, listY + 24, 0xFFAAAAAA, false);
            
            String loginText = "Login";
            int loginWidth = textRenderer.getWidth(loginText);
            context.drawText(this.textRenderer, loginText, panelX + panelWidth - 60, listY + 12, WHITE_COLOR, false);
            
            listY += 45;
        }
        
        if (addingAccount) {
            int inputY = panelY + panelHeight - 50;
            
            context.drawText(this.textRenderer, "Username:", panelX + 20, inputY, WHITE_COLOR, false);
            context.drawText(this.textRenderer, newUsername.isEmpty() ? "Enter username..." : newUsername, panelX + 100, inputY, 
                newUsername.isEmpty() ? 0xFF808080 : WHITE_COLOR, false);
            
            context.drawText(this.textRenderer, "Password:", panelX + 20, inputY + 15, WHITE_COLOR, false);
            context.drawText(this.textRenderer, newPassword.isEmpty() ? "Enter password (optional)..." : "******", panelX + 100, inputY + 15, 
                newPassword.isEmpty() ? 0xFF808080 : WHITE_COLOR, false);
        }
        
        int buttonY = panelY + panelHeight - 35;
        
        renderButton(context, panelX + 20, buttonY, 100, 25, addingAccount ? "Add +" : "Add Account", mouseX, mouseY, 0);
        renderButton(context, panelX + 130, buttonY, 80, 25, "Remove", mouseX, mouseY, 1);
        renderButton(context, panelX + 220, buttonY, 80, 25, "Back", mouseX, mouseY, 2);
        
        context.drawText(this.textRenderer, "Shift + Right Click: Fusion GUI", panelX + 20, panelY + panelHeight - 10, 0xFF808080, false);
    }

    private void renderButton(DrawContext context, int x, int y, int width, int height, String text, int mouseX, int mouseY, int id) {
        boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        
        int bgColor = isHovered ? ORANGE_COLOR : 0x00000000;
        
        context.fill(x, y, x + width, y + height, bgColor);
        context.drawBorder(x, y, width, height, ORANGE_COLOR);
        
        context.drawText(this.textRenderer, text, x + 10, y + 8, isHovered ? WHITE_COLOR : ORANGE_COLOR, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelWidth = 400;
        int panelHeight = 350;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        if (button == 0) {
            int listY = panelY + 45;
            
            for (int i = 0; i < accounts.size(); i++) {
                if (listY + 50 > panelY + panelHeight - 60) break;
                
                if (mouseX >= panelX + 15 && mouseX <= panelX + panelWidth - 15 && 
                    mouseY >= listY && mouseY <= listY + 40) {
                    selectedIndex = i;
                    
                    int loginButtonX = panelX + panelWidth - 60;
                    if (mouseX >= loginButtonX && mouseX <= loginButtonX + 40) {
                        loginToAccount(i);
                        return true;
                    }
                }
                listY += 45;
            }
            
            int buttonY = panelY + panelHeight - 35;
            
            if (mouseX >= panelX + 20 && mouseX <= panelX + 120 && mouseY >= buttonY && mouseY <= buttonY + 25) {
                addingAccount = !addingAccount;
                if (!addingAccount && !newUsername.isEmpty()) {
                    AltManager.getInstance().addAccount(newUsername, newPassword, true);
                    newUsername = "";
                    newPassword = "";
                    accounts = AltManager.getInstance().getAccounts();
                }
                return true;
            }
            
            if (mouseX >= panelX + 130 && mouseX <= panelX + 210 && mouseY >= buttonY && mouseY <= buttonY + 25) {
                if (selectedIndex >= 0) {
                    AltManager.getInstance().removeAccount(selectedIndex);
                    accounts = AltManager.getInstance().getAccounts();
                    selectedIndex = -1;
                }
                return true;
            }
            
            if (mouseX >= panelX + 220 && mouseX <= panelX + 300 && mouseY >= buttonY && mouseY <= buttonY + 25) {
                this.client.setScreen(parent);
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (addingAccount) {
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                if (!newUsername.isEmpty()) {
                    AltManager.getInstance().addAccount(newUsername, newPassword, newPassword.isEmpty());
                    newUsername = "";
                    newPassword = "";
                    accounts = AltManager.getInstance().getAccounts();
                    addingAccount = false;
                }
                return true;
            }
            
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!newUsername.isEmpty()) {
                    newUsername = newUsername.substring(0, newUsername.length() - 1);
                } else if (!newPassword.isEmpty()) {
                    newPassword = newPassword.substring(0, newPassword.length() - 1);
                }
                return true;
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (addingAccount) {
            if (Character.isLetterOrDigit(chr) || chr == '_' || chr == '-') {
                if (newUsername.length() < 16) {
                    newUsername += chr;
                }
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    private void loginToAccount(int index) {
        AltAccount account = accounts.get(index);
        account.setLastLogin(System.currentTimeMillis());
        AltManager.getInstance().save();
        
        FusionClientMod.LOGGER.info("Logging in as: " + account.getUsername());
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
