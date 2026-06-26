package com.johan.zombieshelper.hud;

import cc.polyfrost.oneconfig.hud.Hud;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import com.johan.zombieshelper.GameStateManager;
import com.johan.zombieshelper.config.ZombiesConfig;
import com.johan.zombieshelper.strategy.StrategyManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;

public class StrategyGuideHud extends Hud {
   private final transient int PADDING = 6;
   private final transient int ROW_HEIGHT = 12;
   private transient int margin = 3;
   private transient int currentContentWidth = 200;
   private transient int currentContentHeight = 100;

   public StrategyGuideHud() {
      super(true, 0.0F, 0.0F, 0, 1.0F);
   }

   protected void draw(UMatrixStack matrices, float x, float y, float scale, boolean example) {
      boolean forceHud = ZombiesConfig.INSTANCE != null && ZombiesConfig.INSTANCE.forceStrategyHud;
      if (example || GameStateManager.inZombiesGame || forceHud) {
         Minecraft mc = Minecraft.getMinecraft();
         if (mc != null && mc.fontRendererObj != null) {
            FontRenderer fr = mc.fontRendererObj;
            int currentRound = Math.max(1, example ? 1 : GameStateManager.currentRound);
            List<String> linesToDraw = new ArrayList<>();
            int maxRound = !GameStateManager.inAlienArcadium && currentRound <= 30 ? 30 : 105;

            for (int i = 0; i <= 5; i++) {
               int roundToCheck = currentRound + i;
               if (roundToCheck > maxRound) {
                  break;
               }

               String strategy = StrategyManager.getStrategy(roundToCheck);
               if (example && (strategy == null || strategy.isEmpty() || strategy.equals("Strategy not found") || strategy.equals("Unknown Round / Game Over"))
                  )
                {
                  strategy = "Buy shotgun whenever you can";
               }

               if (strategy != null && !strategy.equals("Strategy not found") && !strategy.equals("Unknown Round / Game Over")) {
                  String prefix = i == 0 ? "§e§lR" + roundToCheck + ": " : "§7R" + roundToCheck + ": ";
                  String color = i == 0 ? "§f" : "§7";
                  linesToDraw.add(prefix + color + strategy);
               }
            }

            if (linesToDraw.isEmpty()) {
               linesToDraw.add("§7No strategies available.");
            }

            int maxWidth = 0;
            List<String> finalLines = new ArrayList<>();

            for (String line : linesToDraw) {
               if (fr.getStringWidth(line) > 300) {
                  for (String w : fr.listFormattedStringToWidth(line, 300)) {
                     finalLines.add(w);
                     int w2 = fr.getStringWidth(w);
                     if (w2 > maxWidth) {
                        maxWidth = w2;
                     }
                  }
               } else {
                  finalLines.add(line);
                  int w2 = fr.getStringWidth(line);
                  if (w2 > maxWidth) {
                     maxWidth = w2;
                  }
               }
            }

            String challenge = ZombiesConfig.INSTANCE != null ? ZombiesConfig.INSTANCE.getAAChallengeString() : "QUAD";
            String mapName = example ? "Alien Arcadium" : (GameStateManager.currentMap.isEmpty() ? "Zombies" : GameStateManager.currentMap);
            String header = "§e" + mapName + " Strategy (" + challenge + ")";
            int headerW = fr.getStringWidth(header);
            if (headerW > maxWidth) {
               maxWidth = headerW;
            }

            int contentW = maxWidth + 12;
            int contentH = (finalLines.size() + 1) * 12 + 12;
            this.currentContentWidth = contentW;
            this.currentContentHeight = contentH;
            float bgX = x + (float)this.margin * scale;
            float bgY = y + (float)this.margin * scale;
            float bgW = (float)contentW * scale;
            float bgH = (float)contentH * scale;
            NanoVGHelper.INSTANCE.setupAndDraw(true, vg -> NanoVGHelper.INSTANCE.drawRoundedRect(vg, bgX, bgY, bgW, bgH, 2013265920, 8.0F));
            GlStateManager.pushMatrix();
            GlStateManager.translate(bgX, bgY, 0.0F);
            GlStateManager.scale(scale, scale, 1.0F);
            int yo = 8;
            fr.drawStringWithShadow(header, 6.0F, (float)yo, 16777215);
            yo += 12;

            for (int i = 0; i < finalLines.size(); i++) {
               fr.drawStringWithShadow(finalLines.get(i), 6.0F, (float)yo, 16777215);
               yo += 12;
            }

            GlStateManager.popMatrix();
         }
      }
   }

   protected float getWidth(float scale, boolean example) {
      return (float)(this.currentContentWidth + this.margin * 2);
   }

   protected float getHeight(float scale, boolean example) {
      return (float)(this.currentContentHeight + this.margin * 2);
   }
}
