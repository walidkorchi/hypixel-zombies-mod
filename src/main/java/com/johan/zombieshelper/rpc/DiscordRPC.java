package com.johan.zombieshelper.rpc;

import com.google.gson.JsonObject;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.entities.User;
import com.johan.zombieshelper.GameStateManager;
import com.johan.zombieshelper.config.ZombiesConfig;
import com.johan.zombieshelper.data.StatsManager;
import com.johan.zombieshelper.session.SessionTracker;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class DiscordRPC {
   private IPCClient client = null;
   private volatile boolean ready = false;
   private volatile boolean connecting = false;
   private DiscordRPC.Activity lastActivity = null;
   private String lastMap = "";
   private int lastRound = -1;
   private int lastPlayerCount = -1;
   private int lastKills = -1;
   private long activityStart = 0L;
   private long lastUpdateMs = 0L;
   private int tickCounter = 0;
   private boolean warnedMissingClientId = false;
   private static DiscordRPC instance;

   public static DiscordRPC getInstance() {
      if (instance == null) {
         instance = new DiscordRPC();
      }

      return instance;
   }

   public void init() {
      this.connectAsync();
   }

   public void shutdown() {
      this.closeClient();
   }

   @SubscribeEvent
   public void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         if (!ZombiesConfig.INSTANCE.discordRpcEnabled) {
            if (this.ready) {
               this.clearPresence();
               this.closeClient();
            }
         } else if (this.getClientId() == 0L) {
            if (this.ready) {
               this.clearPresence();
               this.closeClient();
            }

            this.tickCounter++;
            if (this.tickCounter % 20 == 0) {
               if (this.isOnHypixel()) {
                  if (!this.warnedMissingClientId && Minecraft.getMinecraft().thePlayer != null) {
                     this.warnedMissingClientId = true;
                     Minecraft.getMinecraft()
                        .thePlayer
                        .addChatMessage(
                           new ChatComponentText(
                              "§c[ZombiesHelper] §7Discord Rich Presence is on but no valid §eDiscord Client ID §7is set. Add one in the OneConfig menu (Discord RPC) to enable it."
                           )
                        );
                  }
               } else {
                  this.warnedMissingClientId = false;
               }
            }
         } else if (!this.ready) {
            this.tickCounter++;
            if (this.tickCounter % 200 == 0) {
               this.connectAsync();
            }
         } else {
            this.tickCounter++;
            if (this.tickCounter % 20 == 0) {
               boolean onHypixel = this.isOnHypixel();
               if (!onHypixel) {
                  if (this.ready && this.lastActivity != null) {
                     this.clearPresence();
                     this.lastActivity = null;
                  }
               } else {
                  DiscordRPC.Activity current;
                  if (!GameStateManager.inZombiesGame) {
                     current = DiscordRPC.Activity.IDLE;
                  } else if (!GameStateManager.gameStarted) {
                     current = DiscordRPC.Activity.LOBBY;
                  } else {
                     current = DiscordRPC.Activity.IN_GAME;
                  }

                  String map = GameStateManager.currentMap != null ? GameStateManager.currentMap : "";
                  int round = GameStateManager.currentRound;
                  int playerCount = StatsManager.getExpectedCount();
                  int kills = GameStateManager.currentKills;
                  boolean activityChanged = current != this.lastActivity;
                  if (activityChanged) {
                     this.activityStart = System.currentTimeMillis() / 1000L;
                     this.lastActivity = current;
                  }

                  boolean detailChanged = !map.equals(this.lastMap)
                     || round != this.lastRound
                     || playerCount != this.lastPlayerCount
                     || kills != this.lastKills;
                  long now = System.currentTimeMillis();
                  boolean isRateLimited = now - this.lastUpdateMs < 15000L;
                  if (activityChanged || detailChanged && !isRateLimited) {
                     this.lastUpdateMs = now;
                     this.lastMap = map;
                     this.lastRound = round;
                     this.lastPlayerCount = playerCount;
                     this.lastKills = kills;
                     this.sendPresence(current, map, round, playerCount, kills);
                  }
               }
            }
         }
      }
   }

   private boolean isOnHypixel() {
      return Minecraft.getMinecraft().getCurrentServerData() != null
         && Minecraft.getMinecraft().getCurrentServerData().serverIP.toLowerCase().contains("hypixel.net");
   }

   private long getClientId() {
      String id = ZombiesConfig.INSTANCE.getDiscordClientId();
      if (id.isEmpty()) {
         return 0L;
      } else {
         try {
            return Long.parseLong(id);
         } catch (NumberFormatException var3) {
            return 0L;
         }
      }
   }

   private void connectAsync() {
      long clientId = this.getClientId();
      if (clientId != 0L && !this.connecting && !this.ready) {
         this.connecting = true;
         Thread t = new Thread(() -> {
            try {
               IPCClient c = new IPCClient(clientId);
               c.setListener(new DiscordRPC.Listener());
               this.client = c;
               c.connect();
            } catch (Throwable var4) {
               System.out.println("[ZombiesHelper] Discord RPC connect failed: " + var4.getMessage());
               this.closeClient();
            } finally {
               this.connecting = false;
            }
         }, "ZH-DiscordRPC-Connect");
         t.setDaemon(true);
         t.start();
      }
   }

   private void sendPresence(DiscordRPC.Activity activity, String map, int round, int playerCount, int kills) {
      IPCClient c = this.client;
      if (c != null && this.ready) {
         try {
            String details;
            String state;
            switch (activity) {
               case IN_GAME:
                  String formattedGold = String.format(Locale.US, "%,d", GameStateManager.currentGold);
                  details = "Round " + round + " - Gold: " + formattedGold;
                  int downs = SessionTracker.get().getDowns();
                  int revives = SessionTracker.get().getRevives();
                  state = "Kills: " + kills + " - Downs: " + downs + " - Revs: " + revives;
                  break;
               case LOBBY:
                  details = "In Lobby" + (map != null && !map.isEmpty() ? " — " + map : "");
                  state = playerCount > 0 ? playerCount + "/4 players" : "Waiting for players...";
                  break;
               case IDLE:
               default:
                  int totalWins = SessionTracker.get().getWins();
                  int totalGames = SessionTracker.get().getGamesPlayed();
                  details = "Session: " + totalGames + " Games, " + totalWins + " Wins";
                  state = "Browsing Menus";
            }

            RichPresence.Builder builder = new RichPresence.Builder();
            builder.setDetails(details);
            builder.setState(state);
            builder.setStartTimestamp(this.activityStart);
            builder.setLargeImage("logo", "Hypixel Zombies");
            String smallImgKey = mapImageKey(map);
            if (!smallImgKey.equals("logo") && activity != DiscordRPC.Activity.IDLE) {
               builder.setSmallImage(smallImgKey, map);
            }

            c.sendRichPresence(builder.build());
         } catch (Exception var13) {
            System.out.println("[ZombiesHelper] Discord RPC send failed: " + var13.getMessage());
            this.closeClient();
         }
      }
   }

   private void clearPresence() {
      IPCClient c = this.client;
      if (c != null) {
         try {
            c.sendRichPresence(null);
         } catch (Exception var3) {
         }
      }
   }

   private void closeClient() {
      this.ready = false;
      IPCClient c = this.client;
      this.client = null;
      if (c != null) {
         try {
            c.close();
         } catch (Exception var3) {
         }
      }
   }

   private static String mapImageKey(String mapName) {
      if (mapName == null) {
         return "logo";
      } else {
         String lower = mapName.toLowerCase();
         if (lower.contains("alien") || lower.contains("arcadium")) {
            return "aa";
         } else if (lower.contains("dead")) {
            return "de";
         } else if (lower.contains("bad") || lower.contains("blood")) {
            return "bb";
         } else {
            return lower.contains("prison") ? "prison" : "logo";
         }
      }
   }

   private static enum Activity {
      IDLE,
      LOBBY,
      IN_GAME;
   }

   private class Listener implements IPCListener {
      public void onReady(IPCClient client) {
         DiscordRPC.this.ready = true;
         DiscordRPC.this.lastActivity = null;
         DiscordRPC.this.activityStart = System.currentTimeMillis() / 1000L;
         System.out.println("[ZombiesHelper] Discord RPC connected.");
      }

      public void onClose(IPCClient client, JsonObject json) {
         DiscordRPC.this.ready = false;
      }

      public void onDisconnect(IPCClient client, Throwable t) {
         DiscordRPC.this.ready = false;
      }

      public void onPacketSent(IPCClient client, Packet packet) {
      }

      public void onPacketReceived(IPCClient client, Packet packet) {
      }

      public void onActivityJoin(IPCClient client, String secret) {
      }

      public void onActivitySpectate(IPCClient client, String secret) {
      }

      public void onActivityJoinRequest(IPCClient client, String secret, User user) {
      }
   }
}
