package com.atl.module.management;

import com.atl.event.PacketEvent;
import com.atl.module.Claude;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class BlinkManager {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private BlinkModules blinkModule = BlinkModules.NONE;
    private boolean blinking = false;
    private double startX, startY, startZ;
    public double getStartX() { return startX; }
    public double getStartY() { return startY; }
    public double getStartZ() { return startZ; }
    public Deque<Packet<?>> blinkedPackets = new ConcurrentLinkedDeque<>();

    public void register() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPacketSend(PacketEvent.Send event) {
        if (!blinking || blinkModule == BlinkModules.NONE) return;
        Packet<?> packet = event.getPacket();


        blinkedPackets.offer(packet);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.theWorld == null) {
            if (blinking) {
                blinking = false;
                blinkModule = BlinkModules.NONE;
                blinkedPackets.clear();
                Module blink = Claude.moduleManager.get("Blink");
                if (blink != null && blink.isEnabled()) {
                    blink.setEnabled(false);
                }
            }
            if (mc.thePlayer != null && mc.thePlayer.isDead) {
                setBlinkState(false, blinkModule);
            }
        }
    }

    public boolean setBlinkState(boolean state, BlinkModules module) {
        if (module == BlinkModules.NONE) return false;
        if (state) {
            blinkModule = module;
            blinking = true;
            startX = mc.thePlayer.posX;
            startY = mc.thePlayer.posY;
            startZ = mc.thePlayer.posZ;
        } else {
            if (blinkModule != module) return false;
            blinking = false;
            blinkModule = BlinkModules.NONE;
            Deque<Packet<?>> toSend = new ConcurrentLinkedDeque<>(blinkedPackets);
            blinkedPackets.clear();
            for (Packet<?> packet : toSend) {
                mc.getNetHandler().getNetworkManager().sendPacket(packet);
            }
        }
        return true;
    }

    public boolean isBlinking() { return blinking; }
    public long countMovement() {
        return blinkedPackets.stream().filter(p -> p instanceof C03PacketPlayer).count();
    }
    public BlinkModules getBlinkingModule() { return blinkModule; }
}