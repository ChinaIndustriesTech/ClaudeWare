package com.atl.ui.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

public class InventoryMoveReplayer {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static List<FakeGuiInventory.SlotOperation> pendingOps = new ArrayList<>();
    private static boolean replaying = false;
    private static boolean waitingToOpen = false;
    private static int replayIndex = 0;
    private static long nextOpTime = 0;
    private static final long OP_DELAY_MS = 50;

    public static void schedule(List<FakeGuiInventory.SlotOperation> ops) {
        pendingOps = new ArrayList<>(ops);
        waitingToOpen = true;
        replayIndex = 0;
    }

    public static boolean isReplaying() {
        return replaying || waitingToOpen;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) {
            replaying = false;
            waitingToOpen = false;
            pendingOps.clear();
            return;
        }

        if (waitingToOpen) {
            waitingToOpen = false;
            if (!pendingOps.isEmpty()) {
                replaying = true;
                nextOpTime = System.currentTimeMillis() + 50;
            }
            return;
        }

        if (!replaying) return;
        if (System.currentTimeMillis() < nextOpTime) return;

        if (replayIndex >= pendingOps.size()) {
            replaying = false;
            pendingOps.clear();
            return;
        }

        FakeGuiInventory.SlotOperation op = pendingOps.get(replayIndex);
        mc.playerController.windowClick(
                0,
                op.slot,
                op.button,
                op.mode,
                mc.thePlayer
        );

        replayIndex++;
        nextOpTime = System.currentTimeMillis() + OP_DELAY_MS;
    }
}