package com.johan.zombieshelper.update;

import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.johan.zombieshelper.config.ZombiesConfig;
import java.awt.Desktop;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent.Post;
import net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent.Pre;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import org.lwjgl.input.Mouse;

public class UpdateNotifier {
   public static boolean checked = false;
   public static boolean hasUpdate = false;
   public static String latestVersion = "";
   private static long joinedServerTime = -1L;

   public static void checkAsync() {
      if (!checked) {
         checked = true;
         new Thread(() -> {
            try {
               URL url = new URL("https://api.modrinth.com/v2/project/hypixel-zombies-helper+tracker/version");
               HttpURLConnection conn = (HttpURLConnection)url.openConnection();
               conn.setRequestMethod("GET");
               conn.setRequestProperty("User-Agent", "ZombiesHelper");
               conn.setConnectTimeout(5000);
               conn.setReadTimeout(5000);
               if (conn.getResponseCode() == 200) {
                  JsonArray arr = new JsonParser().parse(new InputStreamReader(conn.getInputStream())).getAsJsonArray();
                  if (arr.size() > 0) {
                     JsonObject latest = arr.get(0).getAsJsonObject();
                     String ver = latest.get("version_number").getAsString();
                     String current = "4.3".replace("v", "");
                     String remote = ver.replace("v", "");
                     if (!current.equals(remote)) {
                        hasUpdate = true;
                        latestVersion = remote;
                     }
                  }
               }
            } catch (Exception var7) {
            }
         }, "ZH-UpdateChecker").start();
      }
   }

   @SubscribeEvent
   public void onServerJoin(ClientConnectedToServerEvent event) {
      if (hasUpdate) {
         joinedServerTime = System.currentTimeMillis();
      }
   }

   @SubscribeEvent
   public void onDrawScreen(Post event) {
      if (hasUpdate) {
         if (event.gui instanceof GuiMainMenu || event.gui instanceof GuiMultiplayer || event.gui instanceof GuiIngameMenu) {
            float w = 190.0F;
            float h = 40.0F;
            float x = (float)event.gui.width - w - 10.0F;
            float y = (float)event.gui.height - h - 10.0F;
            this.drawUpdateBox(x, y, 1.0F, 100.0F);
         }
      }
   }

   @SubscribeEvent
   public void onMouseInput(Pre event) {
      if (hasUpdate) {
         if ((event.gui instanceof GuiMainMenu || event.gui instanceof GuiMultiplayer || event.gui instanceof GuiIngameMenu)
            && Mouse.getEventButton() == 0
            && Mouse.getEventButtonState()) {
            float w = 190.0F;
            float h = 40.0F;
            float x = (float)event.gui.width - w - 10.0F;
            float y = (float)event.gui.height - h - 10.0F;
            Minecraft mc = Minecraft.getMinecraft();
            int mouseX = Mouse.getEventX() * event.gui.width / mc.displayWidth;
            int mouseY = event.gui.height - Mouse.getEventY() * event.gui.height / mc.displayHeight - 1;
            if ((float)mouseX >= x && (float)mouseX <= x + w && (float)mouseY >= y && (float)mouseY <= y + h) {
               try {
                  Desktop.getDesktop().browse(new URI("https://modrinth.com/mod/hypixel-zombies-helper+tracker/version/" + latestVersion));
               } catch (Exception var10) {
                  var10.printStackTrace();
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onRenderOverlay(net.minecraftforge.client.event.RenderGameOverlayEvent.Post event) {
      if (hasUpdate) {
         if (event.type == ElementType.ALL) {
            if (joinedServerTime > 0L) {
               long elapsed = System.currentTimeMillis() - joinedServerTime;
               if (elapsed > 10000L) {
                  hasUpdate = false;
               } else {
                  float progressPct = 100.0F - (float)elapsed / 10000.0F * 100.0F;
                  if (progressPct < 0.0F) {
                     progressPct = 0.0F;
                  }

                  float scale = ZombiesConfig.INSTANCE.lobbyStatsHud.getScale();
                  float w = 190.0F * scale;
                  float h = 40.0F * scale;
                  float x = (float)event.resolution.getScaledWidth() - w - 10.0F;
                  float y = (float)event.resolution.getScaledHeight() - h - 10.0F;
                  this.drawUpdateBox(x, y, scale, progressPct);
               }
            }
         }
      }
   }

   private void drawUpdateBox(float x, float y, float scale, float progressPct) {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc != null && mc.fontRendererObj != null) {
         float bgW = 190.0F * scale;
         float bgH = 40.0F * scale;
         NanoVGHelper.INSTANCE.setupAndDraw(true, vg -> {
            NanoVGHelper.INSTANCE.drawRoundedRect(vg, x, y, bgW, bgH, 2013265920, 8.0F);
            float barX = x + 5.0F * scale;
            float barY = y + 35.0F * scale;
            float barW = 180.0F * scale;
            float barH = 3.0F * scale;
            NanoVGHelper.INSTANCE.drawRoundedRect(vg, barX, barY, barW, barH, 1157627903, 2.0F);
            float fillW = barW * (progressPct / 100.0F);
            NanoVGHelper.INSTANCE.drawRoundedRect(vg, barX, barY, fillW, barH, -11141291, 2.0F);
         });
         GlStateManager.pushMatrix();
         GlStateManager.translate(x, y, 0.0F);
         GlStateManager.scale(scale, scale, 1.0F);
         String title = "§aHypixel Zombies Helper + Tracker";
         String sub = "v" + latestVersion + " is out now.";
         mc.fontRendererObj.drawStringWithShadow(title, 95.0F - (float)mc.fontRendererObj.getStringWidth(title) / 2.0F, 9.0F, 16777215);
         mc.fontRendererObj.drawStringWithShadow(sub, 95.0F - (float)mc.fontRendererObj.getStringWidth(sub) / 2.0F, 21.0F, 11184810);
         GlStateManager.popMatrix();
      }
   }
}
