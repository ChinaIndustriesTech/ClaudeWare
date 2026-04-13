package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class TwoDESP extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();
    
    private final float[] modelMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final int[] viewport = new int[4];
    private boolean matricesCaptured = false;

    private final FloatBuffer modelBuf = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer projBuf = BufferUtils.createFloatBuffer(16);
    private final IntBuffer viewBuf = BufferUtils.createIntBuffer(16);

    public TwoDESP() {
        super("2DESP", "esp but 2d", Category.RENDER);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled()) return;

        modelBuf.rewind();
        projBuf.rewind();
        viewBuf.rewind();

        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelBuf);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projBuf);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewBuf);

        modelBuf.get(modelMatrix);
        projBuf.get(projectionMatrix);
        viewBuf.get(viewport, 0, 4);
        
        matricesCaptured = true;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (!isEnabled() || !matricesCaptured || event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;

        ScaledResolution sr = new ScaledResolution(mc);

        // Load matrices once per overlay render instead of inside the project() loop
        Matrix4f modelView = new Matrix4f();
        modelView.load(FloatBuffer.wrap(modelMatrix));
        Matrix4f projection = new Matrix4f();
        projection.load(FloatBuffer.wrap(projectionMatrix));

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer || player.isInvisible() || !player.isEntityAlive()) continue;
            
            // AntiBot Check
            if (AntiBot.isBot(player)) continue;

            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks;
            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks;
            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks;

            AxisAlignedBB bb = player.getEntityBoundingBox();
            if (bb == null) continue;
            
            double height = bb.maxY - bb.minY;
            double width = (bb.maxX - bb.minX) / 2.0;

            Vector3f[] corners = new Vector3f[]{
                    new Vector3f((float) (x - width), (float) y, (float) (z - width)),
                    new Vector3f((float) (x - width), (float) (y + height), (float) (z - width)),
                    new Vector3f((float) (x + width), (float) y, (float) (z - width)),
                    new Vector3f((float) (x + width), (float) (y + height), (float) (z - width)),
                    new Vector3f((float) (x - width), (float) y, (float) (z + width)),
                    new Vector3f((float) (x - width), (float) (y + height), (float) (z + width)),
                    new Vector3f((float) (x + width), (float) y, (float) (z + width)),
                    new Vector3f((float) (x + width), (float) (y + height), (float) (z + width))
            };

            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;

            boolean anyOnScreen = false;
            for (Vector3f corner : corners) {
                Vector3f projected = project(corner.x, corner.y, corner.z, sr.getScaleFactor(), modelView, projection);
                if (projected != null && projected.z >= 0 && projected.z < 1) {
                    minX = Math.min(minX, projected.x);
                    minY = Math.min(minY, projected.y);
                    maxX = Math.max(maxX, projected.x);
                    maxY = Math.max(maxY, projected.y);
                    anyOnScreen = true;
                }
            }

            if (anyOnScreen) {
                int color = getTeamColor(player);
                draw2DBox(minX, minY, maxX, maxY, color);
            }
        }
    }

    private int getTeamColor(EntityPlayer player) {
        Scoreboard scoreboard = player.getWorldScoreboard();
        if (scoreboard != null) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(player.getName());
            if (team != null) {
                String prefix = team.getColorPrefix();
                int index = prefix.indexOf('§');
                if (index != -1 && index < prefix.length() - 1) {
                    char code = prefix.charAt(index + 1);
                    return mc.fontRendererObj.getColorCode(code);
                }
            }
        }
        return 0xFFFFFFFF;
    }

    private void draw2DBox(float minX, float minY, float maxX, float maxY, int color) {
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        
        GL11.glLineWidth(1.5f);
        GlStateManager.color(r, g, b, 1.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(minX, minY);
        GL11.glVertex2f(maxX, minY);
        GL11.glVertex2f(maxX, maxY);
        GL11.glVertex2f(minX, maxY);
        GL11.glEnd();

        GL11.glLineWidth(0.5f);
        GlStateManager.color(0.0f, 0.0f, 0.0f, 1.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(minX - 0.5f, minY - 0.5f);
        GL11.glVertex2f(maxX + 0.5f, minY - 0.5f);
        GL11.glVertex2f(maxX + 0.5f, maxY + 0.5f);
        GL11.glVertex2f(minX - 0.5f, maxY + 0.5f);
        GL11.glEnd();

        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private Vector3f project(float x, float y, float z, int scaleFactor, Matrix4f modelView, Matrix4f projection) {
        RenderManager rm = mc.getRenderManager();
        Vector4f pos = new Vector4f((float) (x - rm.viewerPosX), (float) (y - rm.viewerPosY), (float) (z - rm.viewerPosZ), 1.0f);

        Matrix4f.transform(modelView, pos, pos);
        Matrix4f.transform(projection, pos, pos);

        if (pos.w > 0.0f) {
            pos.x /= pos.w;
            pos.y /= pos.w;
            pos.z /= pos.w;

            float screenX = (pos.x + 1) * viewport[2] / 2.0f;
            float screenY = (1 - pos.y) * viewport[3] / 2.0f;

            return new Vector3f(screenX / scaleFactor, screenY / scaleFactor, pos.z);
        }
        return null;
    }
}
