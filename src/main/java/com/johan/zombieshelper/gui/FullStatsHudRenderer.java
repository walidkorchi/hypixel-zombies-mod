package com.johan.zombieshelper.gui;

import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import com.johan.zombieshelper.api.HypixelAPI;
import com.johan.zombieshelper.data.FullMapStats;
import com.johan.zombieshelper.data.FullPlayerStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Post;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class FullStatsHudRenderer extends Gui {
   private static final Object LOCK = new Object();
   private static volatile FullPlayerStats pendingStats = null;
   private FullPlayerStats currentStats = null;
   private long expiryTime = 0L;

   private static String[] getHeaders() {
      return new String[]{
         "Map",
         "Downs",
         "Revives",
         "Doors",
         "Windows",
         "Kills",
         "Deaths",
         "Best R",
         "Wins",
         "Fast W"
      };
   }

   public static void show(FullPlayerStats stats) {
      synchronized (LOCK) {
         pendingStats = stats;
      }
   }

   @SubscribeEvent
   public void onRenderOverlay(Post event) {
      if (event.type == ElementType.ALL) {
         synchronized (LOCK) {
            if (pendingStats != null) {
               this.currentStats = pendingStats;
               this.expiryTime = System.currentTimeMillis() + 20000L;
               pendingStats = null;
            }
         }

         if (this.currentStats != null) {
            if (System.currentTimeMillis() > this.expiryTime) {
               this.currentStats = null;
            } else {
               Minecraft mc = Minecraft.getMinecraft();
               FontRenderer fr = mc.fontRendererObj;
               ScaledResolution sr = new ScaledResolution(mc);
               String[][] rows = new String[this.currentStats.maps.size()][];

               for (int i = 0; i < this.currentStats.maps.size(); i++) {
                  rows[i] = this.buildRow(this.currentStats.maps.get(i));
               }

               String[] headers = getHeaders();
               int[] colW = new int[10];

               for (int c = 0; c < 10; c++) {
                  colW[c] = fr.getStringWidth(headers[c]);

                  for (String[] row : rows) {
                     int w = fr.getStringWidth(row[c]);
                     if (w > colW[c]) {
                        colW[c] = w;
                     }
                  }

                  colW[c] += 10;
               }

               int totalColsWidth = 0;

               for (int w : colW) {
                  totalColsWidth += w;
               }

               int PAD = 8;
               int ROW_H = 13;
               int TITLE_H = 14;
               int SEPARATOR = 2;
               int BAR_H = 4;
               int boxW = totalColsWidth + 16;
               int boxH = 39 + rows.length * 13 + 2 + 4 + 8;
               int boxX = (sr.getScaledWidth() - boxW) / 2;
               int tempBoxY = sr.getScaledHeight() - boxH - 20;
               if (tempBoxY < 10) {
                  tempBoxY = 10;
               }

               int finalBoxY = tempBoxY;
               NanoVGHelper.INSTANCE
                  .setupAndDraw(
                     true, vg -> NanoVGHelper.INSTANCE.drawRoundedRect(vg, (float)boxX, (float)finalBoxY, (float)boxW, (float)boxH, -871296751, 8.0F)
                  );
               GlStateManager.pushMatrix();
               GlStateManager.translate((float)boxX, (float)finalBoxY, 0.0F);
               String title = "§e§l" + this.currentStats.username + " §r§f" + "- Zombies Stats";
               fr.drawStringWithShadow(title, (float)(boxW / 2 - fr.getStringWidth(title) / 2), 8.0F, 16777215);
               int yHeader = 24;
               int xCursor = 8;

               for (int c = 0; c < 10; c++) {
                  int cellX = c == 0 ? xCursor : xCursor + colW[c] - fr.getStringWidth(headers[c]) - 5;
                  fr.drawStringWithShadow("§7" + headers[c], c == 0 ? (float)xCursor : (float)cellX, (float)yHeader, 16777215);
                  xCursor += colW[c];
               }

               drawRect(8, yHeader + 13 - 1, boxW - 8, yHeader + 13, 1157627903);
               int yData = yHeader + 13 + 2;

               for (int r = 0; r < rows.length; r++) {
                  String mapColor = HypixelAPI.getMapColor(this.currentStats.maps.get(r).mapName);
                  xCursor = 8;

                  for (int c = 0; c < 10; c++) {
                     String cell = rows[r][c];
                     String colored = c == 0 ? mapColor + cell : "§f" + cell;
                     int textX = c == 0 ? xCursor : xCursor + colW[c] - fr.getStringWidth(cell) - 5;
                     fr.drawStringWithShadow(colored, (float)textX, (float)(yData + r * 13), 16777215);
                     xCursor += colW[c];
                  }
               }

               int barY = boxH - 8 - 4;
               long remaining = this.expiryTime - System.currentTimeMillis();
               float fraction = (float)remaining / 20000.0F;
               if (fraction < 0.0F) {
                  fraction = 0.0F;
               }

               drawRect(8, barY, boxW - 8, barY + 4, 1157627903);
               int barFill = (int)((float)(boxW - 16) * fraction);
               int barColor = fraction > 0.5F ? -11141291 : (fraction > 0.25F ? -22016 : -43691);
               if (barFill > 0) {
                  drawRect(8, barY, 8 + barFill, barY + 4, barColor);
               }

               GlStateManager.popMatrix();
            }
         }
      }
   }

   private String[] buildRow(FullMapStats ms) {
      return new String[]{
         ms.mapName,
         this.fmtNum(ms.downs),
         this.fmtNum(ms.revives),
         this.fmtNum(ms.doorsOpened),
         this.fmtNum(ms.windowsRepaired),
         this.fmtNum(ms.kills),
         this.fmtNum(ms.deaths),
         String.valueOf(ms.bestRound),
         String.valueOf(ms.wins),
         ms.fastestWinSecs > 0 ? this.fmtTime(ms.fastestWinSecs) : "N/A"
      };
   }

   private String fmtNum(int n) {
      return n >= 1000000 ? String.format("%,d", n) : String.format("%,d", n);
   }

   private String fmtTime(int secs) {
      if (secs <= 0) {
         return "N/A";
      } else {
         int h = secs / 3600;
         int m = secs % 3600 / 60;
         return h > 0 ? h + "h, " + m + "m" : m + "m";
      }
   }
}
