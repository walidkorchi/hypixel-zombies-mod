package com.johan.zombieshelper.listeners;

import com.johan.zombieshelper.GameStateManager;
import com.johan.zombieshelper.api.HypixelAPI;
import com.johan.zombieshelper.data.StatsManager;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;

public class GameListener {
   private final Set<String> fetchedPlayers = new HashSet<>();
   private int tickCounter = 0;
   private String previousMap = "";

   @SubscribeEvent
   public void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         if (Minecraft.getMinecraft().thePlayer != null && Minecraft.getMinecraft().theWorld != null) {
            this.tickCounter++;
            if (this.tickCounter % 20 == 0) {
               if (!GameStateManager.inZombiesGame) {
                  if (!this.fetchedPlayers.isEmpty() || !this.previousMap.isEmpty()) {
                     this.fetchedPlayers.clear();
                     StatsManager.clear();
                     HypixelAPI.clearQueue();
                     this.previousMap = "";
                  }
               } else {
                  String map = GameStateManager.currentMap;
                  if (!map.isEmpty() && !map.equals(this.previousMap)) {
                     if (!this.previousMap.isEmpty()) {
                        this.fetchedPlayers.clear();
                        StatsManager.clear();
                        HypixelAPI.clearQueue();
                     }

                     this.previousMap = map;
                  }

                  Set<String> currentPlayers = new HashSet<>();
                  if (GameStateManager.reader != null) {
                     currentPlayers.addAll(GameStateManager.reader.getLobbyPlayers());
                  }

                  Iterator<String> it = this.fetchedPlayers.iterator();

                  while (it.hasNext()) {
                     String p = it.next();
                     if (!currentPlayers.contains(p)) {
                        it.remove();
                        StatsManager.removeUser(p);
                     }
                  }

                  if (!GameStateManager.gameStarted) {
                     for (String username : currentPlayers) {
                        if (!this.fetchedPlayers.contains(username)) {
                           this.fetchedPlayers.add(username);
                           StatsManager.setExpected(username);
                           HypixelAPI.fetchAndDisplayStats(username, GameStateManager.currentMap);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onDisconnect(ClientDisconnectionFromServerEvent event) {
      this.fetchedPlayers.clear();
      StatsManager.clear();
      HypixelAPI.clearQueue();
      HypixelAPI.clearSessionCache();
      this.previousMap = "";
   }
}
