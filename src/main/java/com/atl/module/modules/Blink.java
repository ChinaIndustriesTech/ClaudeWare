package com.atl.module.modules;

import com.atl.event.PacketEvent;
import com.atl.module.management.Category;
import com.atl.module.management.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Blink extends Module {
    private final List<Packet<?>> packetQueue = new ArrayList<>();

    public Blink() {
        super("Blink", "Holds movement packets until disabled", Category.PLAYER);
    }

    @Override
    public void onEnable() {
        packetQueue.clear();
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getNetHandler() != null && !packetQueue.isEmpty()) {
            // Send all stored packets to the server
            for (Packet<?> packet : packetQueue) {
                mc.getNetHandler().getNetworkManager().sendPacket(packet);
            }
            packetQueue.clear();
        }
    }

    @SubscribeEvent
    public void onPacketSend(PacketEvent.Send event) {
        if (!isEnabled()) return;

        Packet<?> packet = event.getPacket();
        // Intercept movement, entity actions (sprint/sneak), and animations (swings)
        if (packet instanceof C03PacketPlayer ||
                packet instanceof C0BPacketEntityAction ||
                packet instanceof C0APacketAnimation) {

            packetQueue.add(packet);
            event.setCanceled(true);
        }
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList("Queued: " + packetQueue.size());
    }
}