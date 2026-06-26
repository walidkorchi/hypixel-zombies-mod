package com.johan.zombieshelper.rpc;

import com.johan.zombieshelper.GameStateManager;
import com.johan.zombieshelper.config.ZombiesConfig;
import com.johan.zombieshelper.data.StatsManager;
import com.johan.zombieshelper.session.SessionTracker;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class DiscordRPC {
   private DiscordRPC.Activity lastActivity = null;
   private String lastMap = "";
   private int lastRound = -1;
   private int lastPlayerCount = -1;
   private int lastKills = -1;
   private long activityStart = 0L;
   private RandomAccessFile pipe = null;
   private boolean ready = false;
   private long nonce = 1L;
   private long lastUpdateMs = 0L;
   private int tickCounter = 0;
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
      this.closePipe();
   }

   @SubscribeEvent
   public void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         if (!ZombiesConfig.INSTANCE.discordRpcEnabled) {
            if (this.ready) {
               this.clearPresence();
               this.closePipe();
            }
         } else if (!this.ready) {
            this.tickCounter++;
            if (this.tickCounter % 200 == 0) {
               this.connectAsync();
            }
         } else {
            this.tickCounter++;
            if (this.tickCounter % 20 == 0) {
               boolean onHypixel = false;
               if (Minecraft.getMinecraft().getCurrentServerData() != null) {
                  String ip = Minecraft.getMinecraft().getCurrentServerData().serverIP.toLowerCase();
                  if (ip.contains("hypixel.net")) {
                     onHypixel = true;
                  }
               }

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

   private void connectAsync() {
      Thread t = new Thread(() -> {
         try {
            RandomAccessFile p = null;

            for (int i = 0; i <= 9; i++) {
               try {
                  p = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
                  break;
               } catch (FileNotFoundException var4) {
               }
            }

            if (p == null) {
               return;
            }

            this.pipe = p;
            String hs = "{\"v\":1,\"client_id\":\"" + ZombiesConfig.INSTANCE.getDiscordClientId() + "\"}";
            this.writeFrame(0, hs);
            byte[] resp = this.readFrame();
            if (resp != null) {
               this.ready = true;
               System.out.println("[ZombiesHelper] Discord RPC connected.");
               this.activityStart = System.currentTimeMillis() / 1000L;
               this.lastActivity = null;
            }
         } catch (Exception var5) {
            System.out.println("[ZombiesHelper] Discord RPC connect failed: " + var5.getMessage());
            this.closePipe();
         }
      }, "ZH-DiscordRPC-Connect");
      t.setDaemon(true);
      t.start();
   }

   private void sendPresence(DiscordRPC.Activity activity, String map, int round, int playerCount, int kills) {
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

         String smallImgKey = mapImageKey(map);
         String assetsStr = "\"large_image\":\"logo\",\"large_text\":\"Hypixel Zombies\"";
         if (!smallImgKey.equals("logo") && activity != DiscordRPC.Activity.IDLE) {
            assetsStr = assetsStr + ",\"small_image\":\"" + smallImgKey + "\",\"small_text\":\"" + map + "\"";
         }

         String payload = "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":"
            + getPid()
            + ",\"activity\":{\"details\":"
            + jsonStr(details)
            + ",\"state\":"
            + jsonStr(state)
            + ",\"timestamps\":{\"start\":"
            + this.activityStart
            + "},\"assets\":{"
            + assetsStr
            + "}}},\"nonce\":\""
            + this.nonce++
            + "\"}";
         this.writeFrame(1, payload);
         this.readFrameAsync();
      } catch (Exception var13) {
         System.out.println("[ZombiesHelper] Discord RPC send failed: " + var13.getMessage());
         this.closePipe();
         this.ready = false;
      }
   }

   private void clearPresence() {
      try {
         String payload = "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + getPid() + "},\"nonce\":\"" + this.nonce++ + "\"}";
         this.writeFrame(1, payload);
         this.readFrameAsync();
      } catch (Exception var2) {
      }
   }

   private synchronized void writeFrame(int opcode, String json) throws IOException {
      if (this.pipe == null) {
         throw new IOException("Pipe is null");
      } else {
         byte[] data = json.getBytes(StandardCharsets.UTF_8);
         ByteBuffer buf = ByteBuffer.allocate(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
         buf.putInt(opcode);
         buf.putInt(data.length);
         buf.put(data);
         this.pipe.write(buf.array());
      }
   }

   private synchronized byte[] readFrame() throws IOException {
      if (this.pipe == null) {
         return null;
      } else {
         byte[] header = new byte[8];
         this.pipe.readFully(header);
         ByteBuffer h = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
         h.getInt();
         int len = h.getInt();
         if (len > 0 && len <= 65536) {
            byte[] body = new byte[len];
            this.pipe.readFully(body);
            return body;
         } else {
            return null;
         }
      }
   }

   private void readFrameAsync() {
      Thread t = new Thread(() -> {
         try {
            this.readFrame();
         } catch (Exception var2) {
         }
      }, "ZH-DiscordRPC-Read");
      t.setDaemon(true);
      t.start();
   }

   private void closePipe() {
      this.ready = false;

      try {
         if (this.pipe != null) {
            this.pipe.close();
            this.pipe = null;
         }
      } catch (Exception var2) {
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

   private static String jsonStr(String s) {
      return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
   }

   private static int getPid() {
      try {
         String name = ManagementFactory.getRuntimeMXBean().getName();
         return Integer.parseInt(name.split("@")[0]);
      } catch (Exception var1) {
         return 0;
      }
   }

   private static enum Activity {
      IDLE,
      LOBBY,
      IN_GAME;
   }
}
