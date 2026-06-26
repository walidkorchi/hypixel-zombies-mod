package com.johan.zombieshelper.session;

import com.johan.zombieshelper.GameStateManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;

public class SessionListener {
   private static final Pattern KILL_PATTERN = Pattern.compile("You killed (\\d+) [Zz]ombie", 2);
   private static final Pattern WIN_PATTERN = Pattern.compile("(ZOMBIES|game|round).*?(complete|won|victory|finished)", 2);
   private static final Pattern DEATH_PATTERN = Pattern.compile("You (were killed|died|have been knocked)", 2);
   private static final Pattern REVIVE_PATTERN = Pattern.compile("You (revived|were revived)", 2);
   private static final Pattern REVIVE_ANY_PATTERN = Pattern.compile("^(\\w+) revived (\\w+)!$");
   private static final Pattern DOWN_ANY_PATTERN = Pattern.compile("^(\\w+) was knocked down by .*!$");
   private static final Pattern DEATH_ANY_PATTERN = Pattern.compile("^(\\w+) was killed by .*!$");
   private static final Pattern GOLD_CRIT_PATTERN = Pattern.compile(
      "^\\+\\d+\\s+Gold\\s+\\((?:Critical Hit|Golpe Cr.tico|Coup Critique|Kritischer Treffer)\\)", 2
   );
   private static final Pattern GOLD_SHOT_PATTERN = Pattern.compile("^\\+\\d+\\s+Gold(?:\\s+\\(\\d+\\))?$");
   private static final Pattern WINDOW_REPAIR_PATTERN = Pattern.compile(
      "(?:Repairing windows|repaired this window|Reparando ventana|fenêtre réparée|Fenster repariert)", 2
   );
   private int tickCounter = 0;
   private boolean prevGameStarted = false;
   private boolean prevInZombies = false;
   private int prevRound = 0;
   private int prevAmmo = -1;
   private int prevHeldSlot = -1;
   private boolean ammoInitialized = false;
   private long lastAmmoSpentTime = 0L;
   private boolean lastLineWasRepair = false;

   @SubscribeEvent
   public void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         if (Minecraft.getMinecraft().thePlayer != null) {
            this.tickCounter++;
            if (this.tickCounter % 20 == 0) {
               SessionTracker st = SessionTracker.get();
               boolean nowInZombies = GameStateManager.inZombiesGame;
               boolean nowGameStarted = GameStateManager.gameStarted;
               int nowRound = GameStateManager.currentRound;
               if (nowGameStarted && !this.prevGameStarted) {
                  st.onGameStart();
               }

               if (nowGameStarted && nowRound != this.prevRound && nowRound > 0) {
                  st.onRoundUpdate(nowRound);
                  int winRound = GameStateManager.inAlienArcadium ? 105 : 30;
                  if (nowRound >= winRound && this.prevRound < winRound) {
                     st.addWin();
                  }
               }

               if (nowGameStarted) {
                  st.syncKills(GameStateManager.currentKills);
               }

               if (nowGameStarted) {
                  Minecraft mc = Minecraft.getMinecraft();
                  int currentAmmo = mc.thePlayer.experienceLevel;
                  int currentSlot = mc.thePlayer.inventory.currentItem;
                  if (!this.ammoInitialized || currentSlot != this.prevHeldSlot) {
                     this.prevAmmo = currentAmmo;
                     this.prevHeldSlot = currentSlot;
                     this.ammoInitialized = true;
                  } else if (currentAmmo < this.prevAmmo) {
                     this.lastAmmoSpentTime = System.currentTimeMillis();
                     this.prevAmmo = currentAmmo;
                  } else if (currentAmmo > this.prevAmmo) {
                     this.prevAmmo = currentAmmo;
                  }
               } else {
                  this.ammoInitialized = false;
               }

               if (!nowGameStarted && this.prevGameStarted && this.prevInZombies) {
                  st.onGameEnd(false);
               }

               if (!nowInZombies && this.prevInZombies && this.prevGameStarted) {
                  st.onGameEnd(false);
               }

               this.prevGameStarted = nowGameStarted;
               this.prevInZombies = nowInZombies;
               this.prevRound = nowRound;
            }
         }
      }
   }

   @SubscribeEvent
   public void onChatReceived(ClientChatReceivedEvent event) {
      if (GameStateManager.inZombiesGame) {
         String text = event.message.getUnformattedText();
         Matcher km = KILL_PATTERN.matcher(text);
         if (km.find()) {
            try {
               SessionTracker.get().addKills(Integer.parseInt(km.group(1)));
            } catch (NumberFormatException var8) {
            }
         }

         if (WINDOW_REPAIR_PATTERN.matcher(text).find()) {
            this.lastLineWasRepair = true;
         } else if (GOLD_CRIT_PATTERN.matcher(text).find()) {
            SessionTracker.get().addHit(true);
            this.lastLineWasRepair = false;
         } else if (GOLD_SHOT_PATTERN.matcher(text).find()) {
            long timeSinceAmmo = System.currentTimeMillis() - this.lastAmmoSpentTime;
            if (!this.lastLineWasRepair && timeSinceAmmo < 1000L) {
               SessionTracker.get().addHit(false);
            }

            this.lastLineWasRepair = false;
         } else if (!text.trim().isEmpty()) {
            this.lastLineWasRepair = false;
         }

         if (WIN_PATTERN.matcher(text).find()) {
            SessionTracker.get().addWin();
         }

         if (DEATH_PATTERN.matcher(text).find()) {
            if (text.toLowerCase().contains("knocked")) {
               SessionTracker.get().addDowns(1);
            } else {
               SessionTracker.get().addDeaths(1);
            }
         }

         if (REVIVE_PATTERN.matcher(text).find()) {
            SessionTracker.get().addRevives(1);
         }

         String myName = Minecraft.getMinecraft().thePlayer.getName();
         Matcher ram = REVIVE_ANY_PATTERN.matcher(text);
         if (ram.find() && ram.group(1).equalsIgnoreCase(myName)) {
            SessionTracker.get().addRevives(1);
         }

         Matcher dam = DOWN_ANY_PATTERN.matcher(text);
         if (dam.find() && dam.group(1).equalsIgnoreCase(myName)) {
            SessionTracker.get().addDowns(1);
         }

         Matcher deam = DEATH_ANY_PATTERN.matcher(text);
         if (deam.find() && deam.group(1).equalsIgnoreCase(myName)) {
            SessionTracker.get().addDeaths(1);
         }
      }
   }

   @SubscribeEvent
   public void onDisconnect(ClientDisconnectionFromServerEvent event) {
      SessionTracker.get().onGameEnd(false);
      this.prevGameStarted = false;
      this.prevInZombies = false;
      this.prevRound = 0;
   }
}
