package com.johan.zombieshelper;

import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import com.johan.zombieshelper.api.scoreboard.IScoreboardReader;
import com.johan.zombieshelper.session.SessionTracker;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;

public class GameStateManager {
   public static boolean inZombiesGame = false;
   public static boolean inAlienArcadium = false;
   public static int currentRound = 0;
   public static String currentMap = "";
   public static boolean gameStarted = false;
   public static int currentKills = 0;
   public static int currentGold = 0;
   public static IScoreboardReader reader = null;
   private static final Pattern ROUND_PATTERN = Pattern.compile("(?i)(?:Round|Ronda|Manche|Runde)[^\\d]*([\\d,]+)");
   private static final Pattern KILLS_PATTERN = Pattern.compile("(?i)(?:Time|Tiempo|Temps|Zeit).*?\\d+:\\d+[^\\d]*([\\d,]+)");
   private int tickCounter = 0;
   private int invalidTicks = 0;

   @Subscribe
   public void onClientTick(TickEvent event) {
      if (event.stage == Stage.START) {
         this.tickCounter++;
         if (this.tickCounter % 5 == 0) {
            if (reader != null && reader.isAvailable()) {
               String title = reader.getTitle();
               String cleanTitle = title == null ? "" : title.replaceAll("§.", "").toUpperCase();
               boolean foundZombies = cleanTitle.contains("ZOMBIES") || cleanTitle.contains("ZOMBI");
               List<String> lines = reader.getCleanLines();
               String detectedMap = null;
               int foundRound = -1;
               int foundKills = -1;
               int foundGold = -1;
               boolean foundGameStarted = false;
               boolean hasMapLine = false;
               boolean isLobby = false;

               for (String line : lines) {
                  String lower = line.toLowerCase();
                  Matcher mapMatcher = Pattern.compile("(?i)(?:map|mapa|carte|karte)\\s*:(.+)").matcher(line);
                  if (mapMatcher.find()) {
                     hasMapLine = true;
                     detectedMap = mapMatcher.group(1).replaceAll("[^a-zA-Z ]", "").trim();
                  }

                  Matcher m = ROUND_PATTERN.matcher(line);
                  if (m.find()) {
                     try {
                        foundRound = Integer.parseInt(m.group(1).replace(",", ""));
                     } catch (NumberFormatException var25) {
                     }
                  }

                  Matcher km = KILLS_PATTERN.matcher(line);
                  if (km.find()) {
                     try {
                        foundKills = Integer.parseInt(km.group(1).replace(",", ""));
                     } catch (NumberFormatException var24) {
                     }
                  }

                  String myName = "";
                  if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().getSession() != null) {
                     myName = Minecraft.getMinecraft().getSession().getUsername();
                  }

                  if (!myName.isEmpty() && line.contains(myName)) {
                     int colonIdx = line.indexOf(58);
                     if (colonIdx != -1) {
                        String goldStr = line.substring(colonIdx + 1).trim().replaceAll("[^\\d]", "");
                        if (!goldStr.isEmpty()) {
                           try {
                              foundGold = Integer.parseInt(goldStr);
                           } catch (NumberFormatException var23) {
                           }
                        }
                     }
                  }

                  if (lower.contains("round")
                     || lower.contains("ronda")
                     || lower.contains("manche")
                     || lower.contains("runde")
                     || lower.contains("zombies left")
                     || lower.contains("zombies restantes")
                     || lower.contains("zombies restants")
                     || lower.contains("verbleibende zombies")) {
                     foundGameStarted = true;
                  }

                  if (lower.startsWith("coins:")
                     || lower.startsWith("monedas:")
                     || lower.startsWith("pièces:")
                     || lower.startsWith("münzen:")
                     || lower.startsWith("level:")
                     || lower.startsWith("nivel:")
                     || lower.startsWith("niveau:")
                     || lower.startsWith("tokens:")) {
                     isLobby = true;
                  }
               }

               if (isLobby) {
                  this.resetState();
               } else if (!foundZombies) {
                  this.invalidTicks++;
                  if (this.invalidTicks >= 5) {
                     this.resetState();
                  }
               } else {
                  this.invalidTicks = 0;
                  inZombiesGame = true;
                  if (hasMapLine && detectedMap != null) {
                     currentMap = detectedMap;
                  } else if (currentMap.isEmpty()) {
                     currentMap = "Unknown";
                  }

                  inAlienArcadium = currentMap.toLowerCase().contains("alien") || currentMap.toLowerCase().contains("arcadium");
                  gameStarted = foundGameStarted;
                  if (foundRound > 0) {
                     currentRound = foundRound;
                  }

                  if (foundKills >= 0) {
                     currentKills = foundKills;
                  }

                  if (foundGold >= 0) {
                     currentGold = foundGold;
                     SessionTracker.get().syncGold(foundGold);
                  }
               }
            } else {
               this.invalidTicks++;
               if (this.invalidTicks >= 3) {
                  this.resetState();
               }
            }
         }
      }
   }

   private void resetState() {
      inZombiesGame = false;
      inAlienArcadium = false;
      currentRound = 0;
      currentMap = "";
      gameStarted = false;
      currentKills = 0;
      currentGold = 0;
   }
}
