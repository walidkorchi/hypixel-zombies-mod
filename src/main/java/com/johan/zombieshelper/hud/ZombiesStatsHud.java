package com.johan.zombieshelper.hud;

import cc.polyfrost.oneconfig.hud.Hud;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import com.johan.zombieshelper.GameStateManager;
import com.johan.zombieshelper.LanguageManager;
import com.johan.zombieshelper.api.HypixelAPI;
import com.johan.zombieshelper.data.PlayerStats;
import com.johan.zombieshelper.data.StatsManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;

public class ZombiesStatsHud extends Hud {
   private final transient int PADDING = 6;
   private final transient int ROW_HEIGHT = 14;
   private final transient int HEADER_HEIGHT = 30;
   private final transient int MIN_WIDTH = 200;
   private transient int margin = 3;
   private transient int cachedContentW = 200;
   private transient int lastPlayerCount = -1;
   private transient String lastMapName = "";

   public ZombiesStatsHud() {
      super(true, 1720.0F, 1000.0F, 0, 1.0F);
   }

   private int computeContentWidth(Minecraft mc, Collection<PlayerStats> stats, boolean example) {
      String mapName = example ? "Alien Arcadium" : (GameStateManager.currentMap != null ? GameStateManager.currentMap : "...");
      int playerCount = stats != null ? stats.size() : 0;
      if (!example && playerCount == this.lastPlayerCount && mapName.equals(this.lastMapName)) {
         return this.cachedContentW;
      } else {
         String headerLabel = this.buildHeaderLabel();
         int maxW = 18 + mc.fontRendererObj.getStringWidth("Player") + 20 + mc.fontRendererObj.getStringWidth(headerLabel) + 6;
         if (stats != null) {
            List<PlayerStats> sortedStats = new ArrayList<>(stats);
            Collections.sort(sortedStats, new Comparator<PlayerStats>() {
               public int compare(PlayerStats p1, PlayerStats p2) {
                  if (p1.getWins() != p2.getWins()) {
                     return Integer.compare(p2.getWins(), p1.getWins());
                  } else if (p1.getBestRound() != p2.getBestRound()) {
                     return Integer.compare(p2.getBestRound(), p1.getBestRound());
                  } else if (p1.getKills() != p2.getKills()) {
                     return Integer.compare(p2.getKills(), p1.getKills());
                  } else {
                     int t1 = p1.getFastestWin() > 0 ? p1.getFastestWin() : Integer.MAX_VALUE;
                     int t2 = p2.getFastestWin() > 0 ? p2.getFastestWin() : Integer.MAX_VALUE;
                     return Integer.compare(t1, t2);
                  }
               }
            });

            for (PlayerStats ps : sortedStats) {
               String nameStr = ps.getUsername() + (ps.isNicked() ? " (Nicked)" : "");
               int rw = 18 + mc.fontRendererObj.getStringWidth(nameStr) + 20 + mc.fontRendererObj.getStringWidth(this.buildStatsString(ps)) + 6;
               if (rw > maxW) {
                  maxW = rw;
               }
            }
         }

         String mapColor = HypixelAPI.getMapColor(mapName);
         String title = mapColor + "§lZombies Stats §r§f- " + mapName;
         int tw = mc.fontRendererObj.getStringWidth(title.replaceAll("§.", "")) + 12;
         if (tw > maxW) {
            maxW = tw;
         }

         int result = Math.max(200, maxW);
         if (!example) {
            this.cachedContentW = result;
            this.lastPlayerCount = playerCount;
            this.lastMapName = mapName;
         }

         return result;
      }
   }

   private String buildHeaderLabel() {
      return LanguageManager.get("hud.wins")
         + " / "
         + LanguageManager.get("hud.round")
         + " / "
         + LanguageManager.get("hud.kills")
         + " / "
         + LanguageManager.get("hud.fast");
   }

   private String buildStatsString(PlayerStats ps) {
      return ps.getWins() + " / " + ps.getBestRound() + " / " + ps.getKills() + " / " + this.fmtTime(ps.getFastestWin());
   }

   protected void draw(UMatrixStack matrices, float x, float y, float scale, boolean example) {
      if (example || GameStateManager.inZombiesGame) {
         if (example || !GameStateManager.gameStarted) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.fontRendererObj != null) {
               Collection<PlayerStats> stats = StatsManager.getStats();
               boolean hasStats = stats != null && !stats.isEmpty();
               int contentW = this.computeContentWidth(mc, hasStats ? stats : null, example);
               int expected = StatsManager.getExpectedCount();
               int loaded = StatsManager.getPlayerCount();
               int rows = example && !hasStats ? 3 : (hasStats ? stats.size() : 1);
               int contentH = 30 + rows * 14 + 6;
               float bgX = x + (float)this.margin * scale;
               float bgY = y + (float)this.margin * scale;
               float bgW = (float)contentW * scale;
               float bgH = (float)contentH * scale;
               NanoVGHelper.INSTANCE.setupAndDraw(true, vg -> NanoVGHelper.INSTANCE.drawRoundedRect(vg, bgX, bgY, bgW, bgH, 2013265920, 8.0F));
               GlStateManager.pushMatrix();
               GlStateManager.translate(bgX, bgY, 0.0F);
               GlStateManager.scale(scale, scale, 1.0F);
               String mapName = example ? "Alien Arcadium" : (GameStateManager.currentMap != null ? GameStateManager.currentMap : "...");
               String mapColor = HypixelAPI.getMapColor(mapName);
               String title = mapColor + "§lZombies Stats §r§f- " + mapName;
               mc.fontRendererObj.drawStringWithShadow(title, (float)contentW / 2.0F - (float)mc.fontRendererObj.getStringWidth(title) / 2.0F, 8.0F, 16777215);
               String headerLabel = this.buildHeaderLabel();
               int startY = 22;
               mc.fontRendererObj.drawStringWithShadow("§7Player", 18.0F, (float)startY, 16777215);
               mc.fontRendererObj.drawStringWithShadow("§7" + headerLabel, (float)(contentW - 6 - mc.fontRendererObj.getStringWidth(headerLabel)), (float)startY, 16777215);
               int yo = startY + 14;
               if (!hasStats && example) {
                  this.drawRow(mc, "Neros999", 10, 105, 5000, 4878, false, yo, contentW);
                  yo += 14;
                  this.drawRow(mc, "Notch", 2, 30, 400, 1843, false, yo, contentW);
                  yo += 14;
                  this.drawRow(mc, "Herobrine", 0, 15, 100, 0, true, yo, contentW);
               } else if (!hasStats) {
                  String loading;
                  if (expected > 0) {
                     loading = "§7" + LanguageManager.get("hud.loading") + " §e(" + loaded + "/" + expected + ")";
                  } else {
                     loading = "§7" + LanguageManager.get("hud.loading");
                  }

                  mc.fontRendererObj.drawStringWithShadow(loading, (float)contentW / 2.0F - (float)mc.fontRendererObj.getStringWidth(loading) / 2.0F, (float)yo, 16777215);
               } else {
                  List<PlayerStats> sortedStats = new ArrayList<>(stats);
                  Collections.sort(sortedStats, new Comparator<PlayerStats>() {
                     public int compare(PlayerStats p1, PlayerStats p2) {
                        if (p1.getWins() != p2.getWins()) {
                           return Integer.compare(p2.getWins(), p1.getWins());
                        } else if (p1.getBestRound() != p2.getBestRound()) {
                           return Integer.compare(p2.getBestRound(), p1.getBestRound());
                        } else if (p1.getKills() != p2.getKills()) {
                           return Integer.compare(p2.getKills(), p1.getKills());
                        } else {
                           int t1 = p1.getFastestWin() > 0 ? p1.getFastestWin() : Integer.MAX_VALUE;
                           int t2 = p2.getFastestWin() > 0 ? p2.getFastestWin() : Integer.MAX_VALUE;
                           return Integer.compare(t1, t2);
                        }
                     }
                  });

                  for (PlayerStats ps : sortedStats) {
                     String uname = ps.getUsername();
                     if (mc.getNetHandler() != null) {
                        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(uname);
                        if (info != null) {
                           mc.getTextureManager().bindTexture(info.getLocationSkin());
                           GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                           Gui.drawScaledCustomSizeModalRect(8, yo, 8.0F, 8.0F, 8, 8, 8, 8, 64.0F, 64.0F);
                        }
                     }

                     mc.fontRendererObj.drawStringWithShadow("§a" + uname, 20.0F, (float)yo, 16777215);
                     if (ps.isNicked()) {
                        mc.fontRendererObj.drawStringWithShadow("§c (Nicked)", (float)(20 + mc.fontRendererObj.getStringWidth(uname)), (float)yo, 16777215);
                     }

                     String statsText = "§e"
                        + ps.getWins()
                        + "§f / §c"
                        + ps.getBestRound()
                        + "§f / §b"
                        + ps.getKills()
                        + "§f / §d"
                        + this.fmtTime(ps.getFastestWin());
                     mc.fontRendererObj.drawStringWithShadow(statsText, (float)(contentW - 6 - mc.fontRendererObj.getStringWidth(statsText)), (float)yo, 16777215);
                     yo += 14;
                  }
               }

               GlStateManager.popMatrix();
            }
         }
      }
   }

   private void drawRow(Minecraft mc, String name, int round, int wins, int kills, int fast, boolean nicked, int yo, int w) {
      mc.fontRendererObj.drawStringWithShadow("§a" + name, 20.0F, (float)yo, 16777215);
      if (nicked) {
         mc.fontRendererObj.drawStringWithShadow("§c (Nicked)", (float)(20 + mc.fontRendererObj.getStringWidth(name)), (float)yo, 16777215);
      }

      String s = "§e" + wins + "§f / §c" + round + "§f / §b" + kills + "§f / §d" + this.fmtTime(fast);
      mc.fontRendererObj.drawStringWithShadow(s, (float)(w - 6 - mc.fontRendererObj.getStringWidth(s)), (float)yo, 16777215);
   }

   private String fmtTime(int seconds) {
      if (seconds <= 0) {
         return "N/A";
      } else {
         int h = seconds / 3600;
         int m = seconds % 3600 / 60;
         return h > 0 ? h + "h, " + m + "m" : m + "m";
      }
   }

   protected float getWidth(float scale, boolean example) {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc != null && mc.fontRendererObj != null) {
         Collection<PlayerStats> stats = StatsManager.getStats();
         return (float)(this.computeContentWidth(mc, stats != null && !stats.isEmpty() ? stats : null, example) + this.margin * 2);
      } else {
         return (float)(200 + this.margin * 2);
      }
   }

   protected float getHeight(float scale, boolean example) {
      Collection<PlayerStats> stats = StatsManager.getStats();
      int rows = stats != null && !stats.isEmpty() ? stats.size() : (example ? 3 : 1);
      return (float)(30 + rows * 14 + 6 + this.margin * 2);
   }
}
