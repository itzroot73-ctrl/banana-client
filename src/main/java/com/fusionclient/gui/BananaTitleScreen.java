package com.fusionclient.gui;

import com.fusionclient.alt.AltManagerScreen;
import com.fusionclient.gui.FusionGuiScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.render.RenderGameOverlay;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

public class BananaTitleScreen extends TitleScreen {
    private float menuAnimation = 0.0f;
    private int hoveredButton = -1;
    
    private static final int ORANGE_COLOR = 0xFFFFA500;
    private static final int WHITE_COLOR = 0xFFFFFFFF;
    private static final int BG_COLOR = 0x80FFFFFF;
    private static final int BUTTON_COLOR = 0x40FFFFFF;
    private static final int BUTTON_HOVER = 0x60FFA500;
    
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 280;
    private static final int BUTTON_WIDTH = 180;
    private static final int BUTTON_HEIGHT = 30;
    private static final int BUTTON_SPACING = 38;

    public BananaTitleScreen() {
        super();
    }

    @Override
    public void init() {
        super.init();
        this.menuAnimation = 0.0f;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (menuAnimation < 1.0f) {
            menuAnimation += delta * 0.03f;
            if (menuAnimation > 1.0f) menuAnimation = 1.0f;
        }

        int panelX = (this.width - PANEL_WIDTH) / 2 - 200;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        context.fill(0, 0, this.width, this.height, 0x99000000);

        float slideOffset = (1.0f - menuAnimation) * 50.0f;
        int renderPanelX = panelX + (int)slideOffset;

        context.fill(renderPanelX, panelY, renderPanelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, BG_COLOR);
        context.drawBorder(renderPanelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, ORANGE_COLOR);

        String title = "BANANA CLIENT";
        context.drawText(this.textRenderer, title, renderPanelX + 20, panelY + 15, ORANGE_COLOR, false);
        
        String version = "v1.0.0";
        context.drawText(this.textRenderer, version, renderPanelX + 20, panelY + 30, 0xFF808080, false);

        context.fill(renderPanelX + 10, panelY + 45, renderPanelX + PANEL_WIDTH - 10, panelY + 46, ORANGE_COLOR);

        int buttonY = panelY + 60;
        
        renderMenuButton(context, "Singleplayer", renderPanelX + 20, buttonY, mouseX, mouseY, 0);
        buttonY += BUTTON_SPACING;
        
        renderMenuButton(context, "Multiplayer", renderPanelX + 20, buttonY, mouseX, mouseY, 1);
        buttonY += BUTTON_SPACING;
        
        renderMenuButton(context, "Settings", renderPanelX + 20, buttonY, mouseX, mouseY, 2);
        buttonY += BUTTON_SPACING;
        
        renderMenuButton(context, "Alt Manager", renderPanelX + 20, buttonY, mouseX, mouseY, 3);
        buttonY += BUTTON_SPACING;
        
        renderMenuButton(context, "Quit", renderPanelX + 20, buttonY, mouseX, mouseY, 4);

        renderCopyright(context);
    }

    private void renderMenuButton(DrawContext context, String text, int x, int y, int mouseX, int mouseY, int id) {
        boolean isHovered = mouseX >= x && mouseX <= x + BUTTON_WIDTH && mouseY >= y && mouseY <= y + BUTTON_HEIGHT;
        
        float expandScale = isHovered ? 1.05f : 1.0f;
        int adjustedWidth = (int)(BUTTON_WIDTH * expandScale);
        int adjustedX = x - (adjustedWidth - BUTTON_WIDTH) / 2;
        
        int bgColor = isHovered ? BUTTON_HOVER : BUTTON_COLOR;
        
        context.fill(adjustedX, y, adjustedX + adjustedWidth, y + BUTTON_HEIGHT, bgColor);
        
        context.drawBorder(adjustedX, y, adjustedWidth, BUTTON_HEIGHT, ORANGE_COLOR);
        
        int textColor = isHovered ? ORANGE_COLOR : WHITE_COLOR;
        context.drawText(this.textRenderer, text, adjustedX + 10, y + 10, textColor, false);
        
        if (isHovered) {
            hoveredButton = id;
        }
    }

    private void renderCopyright(DrawContext context) {
        String copyright = "Minecraft 1.21.1";
        context.drawText(this.textRenderer, copyright, this.width - this.textRenderer.getWidth(copyright) - 5, this.height - 10, 0xFF808080, false);
        
        String clientCredit = "Banana Client";
        context.drawText(this.textRenderer, clientCredit, 5, this.height - 10, ORANGE_COLOR, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        
        if (hoveredButton == -1) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        
        switch (hoveredButton) {
            case 0:
                this.client.setScreen(new SelectWorldScreen(this));
                break;
            case 1:
                this.client.setScreen(MultiplayerScreen.create(this));
                break;
            case 2:
                this.client.setScreen(new KeybindsScreen(this, this.client.options));
                break;
            case 3:
                this.client.setScreen(new AltManagerScreen(this));
                break;
            case 4:
                this.client.close();
                break;
        }
        
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (InputUtil.isKeyPressed(this.client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) ||
            InputUtil.isKeyPressed(this.client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                if (!(this.client.currentScreen instanceof FusionGuiScreen)) {
                    this.client.setScreen(new FusionGuiScreen(this));
                    return true;
                }
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
