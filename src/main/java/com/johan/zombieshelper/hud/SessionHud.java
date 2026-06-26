package com.johan.zombieshelper.hud;

import cc.polyfrost.oneconfig.hud.Hud;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import com.johan.zombieshelper.session.SessionTracker;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

public class SessionHud extends Hud {
   private transient int margin = 3;

   public SessionHud() {
      super(true, 1720.0F, 0.0F, 0, 1.0F);
   }

   protected void draw(UMatrixStack matrices, float x, float y, float scale, boolean example) {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc != null && mc.fontRendererObj != null) {
         if (!example) {
            SessionTracker.get().markHudActive();
         }

         int games;
         int wins;
         int kills;
         int deaths;
         int downs;
         int revives;
         int bestRound;
         int accuracy;
         int gold;
         long sessionSecs;
         double wlr;
         double kpg;
         if (example) {
            games = 12;
            wins = 7;
            kills = 1080;
            deaths = 3;
            downs = 14;
            revives = 32;
            bestRound = 105;
            accuracy = 77;
            gold = 1250450;
            sessionSecs = 4937L;
            wlr = 0.5833333333333334;
            kpg = (double)kills / (double)games;
         } else {
            SessionTracker st = SessionTracker.get();
            games = st.getGamesPlayed();
            wins = st.getWins();
            kills = st.getKills();
            deaths = st.getDeaths();
            downs = st.getDowns();
            revives = st.getRevives();
            bestRound = st.getBestRound();
            accuracy = st.getCriticalAccuracyPct();
            gold = st.getTotalGoldEarned();
            sessionSecs = st.getSessionElapsedSeconds();
            wlr = games > 0 ? (double)wins / (double)games : 0.0;
            kpg = games > 0 ? (double)kills / (double)games : 0.0;
         }

         String winsLine = "§7Games: §e" + games + " §7/ WR: §a" + wins + "  §7WLR: " + this.wlrColor(wlr) + String.format("%.2f", wlr);
         String killLine = "§7Kills: §b" + kills + " §7/ KPG: §b" + (games > 0 ? String.format("%.1f", kpg) : "0.0");
         String deathLine = "§7Deaths: §c" + deaths + "  §7Downs: §6" + downs;
         String reviveLine = "§7Revives: §a" + revives + (accuracy >= 0 ? "  §7Crit Acc: " + this.critColor(accuracy) + accuracy + "%" : "");
         String goldLine = "§7Total Gold: §6" + String.format(Locale.US, "%,d", gold);
         String roundLine = "§7Best Round: §c" + bestRound;
         String timeLine = "§7Session Time: §d" + this.fmtTime(sessionSecs);
         int contentW = 200;
         String[] rows = new String[]{winsLine, killLine, deathLine, reviveLine, goldLine, roundLine, timeLine};

         for (String row : rows) {
            int rw = 18 + mc.fontRendererObj.getStringWidth(this.stripColor(row));
            if (rw > contentW) {
               contentW = rw;
            }
         }

         int contentH = 108;
         float bgX = x + (float)this.margin * scale;
         float bgY = y + (float)this.margin * scale;
         float bgW = (float)contentW * scale;
         float bgH = (float)contentH * scale;
         NanoVGHelper.INSTANCE.setupAndDraw(true, vg -> {
            NanoVGHelper.INSTANCE.drawRoundedRect(vg, bgX, bgY, bgW, bgH, 2013265920, 8.0F);
            NanoVGHelper.INSTANCE.drawRoundedRect(vg, bgX, bgY + 2.0F * scale, 3.0F * scale, bgH - 4.0F * scale, -866779307, 3.0F);
         });
         GlStateManager.pushMatrix();
         GlStateManager.translate(bgX, bgY, 0.0F);
         GlStateManager.scale(scale, scale, 1.0F);
         int yo = 6;
         String header = "§a§lSession";
         mc.fontRendererObj.drawStringWithShadow(header, (float)contentW / 2.0F - (float)mc.fontRendererObj.getStringWidth(header) / 2.0F, (float)yo, 16777215);
         yo += 14;

         for (String rowx : rows) {
            mc.fontRendererObj.drawStringWithShadow(rowx, 12.0F, (float)yo, 16777215);
            yo += 12;
         }

         GlStateManager.popMatrix();
      }
   }

   private String wlrColor(double wlr) {
      if (wlr >= 0.6) {
         return "§a";
      } else {
         return wlr >= 0.3 ? "§6" : "§c";
      }
   }

   private String critColor(int crit) {
      if (crit >= 70) {
         return "§a";
      } else {
         return crit >= 40 ? "§e" : "§c";
      }
   }

   private String fmtTime(long totalSecs) {
      long h = totalSecs / 3600L;
      long m = totalSecs % 3600L / 60L;
      long s = totalSecs % 60L;
      return h > 0L ? String.format("%d:%02d:%02d", h, m, s) : String.format("%d:%02d", m, s);
   }

   private String stripColor(String str) {
      return str.replaceAll("§[0-9a-fk-or]", "");
   }

   protected float getWidth(float scale, boolean example) {
      return (float)(200 + this.margin * 2) * scale;
   }

   protected float getHeight(float scale, boolean example) {
      int contentH = 108;
      return (float)(contentH + this.margin * 2) * scale;
   }
}
