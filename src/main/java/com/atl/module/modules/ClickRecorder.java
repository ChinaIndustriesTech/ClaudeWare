package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Mouse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClickRecorder extends Module {

    private final List<String> clickData = new ArrayList<>();
    private long lastClickTime = -1;
    private long lastReleaseTime = -1;
    private boolean isRecording = false;

    public ClickRecorder() {
        super("ClickRecorder", "Records your click intervals to a file", Category.MISC);
    }

    @Override
    public void onDisable() {
        isRecording = false;
        lastClickTime = -1;
        lastReleaseTime = -1;
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        if (!isEnabled() || !isRecording) return;

        // Mouse.getEventButton() == 0 is Left Click
        // Mouse.getEventButtonState() is true for Pressed, false for Released
        if (Mouse.getEventButton() == 0) {
            long now = System.currentTimeMillis();
            if (Mouse.getEventButtonState()) {
                // On Press
                if (lastClickTime != -1 && lastReleaseTime != -1) {
                    long interval = now - lastClickTime;
                    long gap = now - lastReleaseTime;

                    // Ignore long pauses to keep data clean
                    if (interval < 1000) {
                        clickData.add(interval + ":" + gap);
                    }
                }
                lastClickTime = now;
            } else {
                // On Release
                lastReleaseTime = now;
            }
        }
    }

    @Override
    public boolean handleSetCommand(String[] parts) {
        if (parts.length < 3) return false;
        String subCommand = parts[2].toLowerCase();

        switch (subCommand) {
            case "start":
                isRecording = true;
                lastClickTime = -1;
                sendMessage("Recording " + EnumChatFormatting.GREEN + "STARTED" + EnumChatFormatting.GRAY + ".");
                return true;
            case "stop":
                isRecording = false;
                sendMessage("Recording " + EnumChatFormatting.RED + "STOPPED" + EnumChatFormatting.GRAY + ". Total clicks: " + clickData.size());
                return true;
            case "save":
                if (parts.length < 4) {
                    sendMessage(EnumChatFormatting.RED + "Usage: ,set clickrecorder save <filename>");
                    return true;
                }
                saveToFile(parts[3]);
                return true;
            case "clear":
                clickData.clear();
                sendMessage("Data " + EnumChatFormatting.YELLOW + "CLEARED" + EnumChatFormatting.GRAY + ".");
                return true;
        }
        return false;
    }

    private void saveToFile(String filename) {
        if (clickData.isEmpty()) {
            sendMessage(EnumChatFormatting.RED + "No data to save!");
            return;
        }

        File dir = new File(Minecraft.getMinecraft().mcDataDir, "atl" + File.separator + "recordings");
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, filename + ".txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String data : clickData) {
                writer.write(data);
                writer.newLine();
            }
            sendMessage(EnumChatFormatting.GREEN + "Saved " + clickData.size() + " clicks to " + file.getName());
        } catch (IOException e) {
            e.printStackTrace();
            sendMessage(EnumChatFormatting.RED + "Failed to save file!");
        }
    }

    private void sendMessage(String msg) {
        if (Minecraft.getMinecraft().thePlayer == null) return;
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(
                EnumChatFormatting.GRAY + "[" + EnumChatFormatting.BLUE + "Recorder" + EnumChatFormatting.GRAY + "] " + msg
        ));
    }

    @Override
    public List<String> getSettings() {
        return java.util.Arrays.asList(
            "Commands:",
            "start - Begin capturing clicks",
            "stop  - Pause capture",
            "save [name] - Export to .txt",
            "clear - Reset current data"
        );
    }
}