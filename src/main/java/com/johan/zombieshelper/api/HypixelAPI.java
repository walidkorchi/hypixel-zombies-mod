package com.johan.zombieshelper.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.johan.zombieshelper.LanguageManager;
import com.johan.zombieshelper.config.ZombiesConfig;
import com.johan.zombieshelper.data.FullMapStats;
import com.johan.zombieshelper.data.FullPlayerStats;
import com.johan.zombieshelper.data.PlayerStats;
import com.johan.zombieshelper.data.StatsManager;
import com.johan.zombieshelper.gui.FullStatsHudRenderer;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

public class HypixelAPI {
   private static final ExecutorService executor = Executors.newFixedThreadPool(16);
   private static long lastExpiredWarning = 0L;
   private static final ConcurrentHashMap<String, HypixelAPI.MojangProfile> profileCache = new ConcurrentHashMap<>();
   private static final ConcurrentHashMap<String, HypixelAPI.CachedArcade> arcadeCache = new ConcurrentHashMap<>();
   private static final long CACHE_TTL_MS = 600000L;
   private static final String[][] MAPS = new String[][]{
      {"Dead End", "deadend", "§d", "fastest_time_30_zombies_deadend_normal", "fastest_time_30_zombies_deadend_hard", "fastest_time_30_zombies_deadend_rip"},
      {
            "Bad Blood",
            "badblood",
            "§c",
            "fastest_time_30_zombies_badblood_normal",
            "fastest_time_30_zombies_badblood_hard",
            "fastest_time_30_zombies_badblood_rip"
      },
      {
            "Alien Arcadium",
            "alienarcadium",
            "§a",
            "fastest_time_30_zombies_alienarcadium_normal",
            "fastest_time_30_zombies_alienarcadium_hard",
            "fastest_time_30_zombies_alienarcadium_rip"
      },
      {"Prison", "prison", "§b", "fastest_time_30_zombies_prison_normal", "fastest_time_30_zombies_prison_hard", "fastest_time_30_zombies_prison_rip"}
   };

   public static void clearSessionCache() {
      profileCache.clear();
      arcadeCache.clear();
   }

   public static void fetchAndDisplayStats(String username, String mapDisplayName) {
      String apiKey = ZombiesConfig.INSTANCE.getApiKey();
      if (apiKey != null && !apiKey.isEmpty()) {
         executor.submit(new Runnable() {
            @Override
            public void run() {
               int retries = 0;

               while (retries < 3) {
                  try {
                     HypixelAPI.ArcadeResult result = HypixelAPI.fetchArcadeStats(username, true);
                     if (result == null) {
                        return;
                     }

                     if (result.stats == null) {
                        StatsManager.addStats(new PlayerStats(result.correctName, 0, 0, 0, 0, result.isNicked));
                        return;
                     }

                     String mapKey = HypixelAPI.toApiKey(mapDisplayName);
                     int kills = HypixelAPI.getInt(result.stats, "zombie_kills_zombies_" + mapKey);
                     int maxRound = HypixelAPI.getInt(result.stats, "best_round_zombies_" + mapKey);
                     int wins = HypixelAPI.getInt(result.stats, "wins_zombies_" + mapKey);
                     int fastWin = HypixelAPI.bestFastestWin(result.stats, mapDisplayName);
                     StatsManager.addStats(new PlayerStats(result.correctName, maxRound, wins, kills, fastWin, result.isNicked));
                     return;
                  } catch (Exception var9) {
                     if (++retries >= 3) {
                        var9.printStackTrace();
                        StatsManager.addStats(new PlayerStats(username, 0, 0, 0, 0));
                        return;
                     }

                     try {
                        Thread.sleep(1500L);
                     } catch (InterruptedException var8) {
                     }
                  }
               }
            }
         });
      } else {
         Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + LanguageManager.get("api.key.missing")));
      }
   }

   public static void fetchFullStats(String username) {
      String apiKey = ZombiesConfig.INSTANCE.getApiKey();
      if (apiKey != null && !apiKey.isEmpty()) {
         Minecraft.getMinecraft()
            .thePlayer
            .addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + LanguageManager.get("api.fetching") + username + "..."));
         executor.submit(
            new Runnable() {
               @Override
               public void run() {
                  try {
                     HypixelAPI.ArcadeResult arcadeResult = HypixelAPI.fetchArcadeStats(username, false);
                     if (arcadeResult == null || arcadeResult.stats == null) {
                        return;
                     }

                     JsonObject zombies = arcadeResult.stats;
                     String correctName = arcadeResult.correctName;
                     List<FullMapStats> mapList = new ArrayList<>();

                     for (String[] mapEntry : HypixelAPI.MAPS) {
                        String displayName = mapEntry[0];
                        String key = mapEntry[1];
                        int downs = HypixelAPI.getInt(zombies, "times_knocked_down_zombies_" + key);
                        int revives = HypixelAPI.getInt(zombies, "players_revived_zombies_" + key);
                        int doors = HypixelAPI.getInt(zombies, "doors_opened_zombies_" + key);
                        int windows = HypixelAPI.getInt(zombies, "windows_repaired_zombies_" + key);
                        int kills = HypixelAPI.getInt(zombies, "zombie_kills_zombies_" + key);
                        int deaths = HypixelAPI.getInt(zombies, "deaths_zombies_" + key);
                        int bestRd = HypixelAPI.getInt(zombies, "best_round_zombies_" + key);
                        int wins = HypixelAPI.getInt(zombies, "wins_zombies_" + key);
                        int fastWin = HypixelAPI.bestFastestWin(zombies, displayName);
                        mapList.add(new FullMapStats(displayName, downs, revives, doors, windows, kills, deaths, bestRd, wins, fastWin));
                     }

                     final FullPlayerStats result = new FullPlayerStats(correctName, mapList);
                     MinecraftForge.EVENT_BUS.register(new Object() {
                        @SubscribeEvent
                        public void onTick(ClientTickEvent e) {
                           FullStatsHudRenderer.show(result);
                           MinecraftForge.EVENT_BUS.unregister(this);
                        }
                     });
                  } catch (Exception var20) {
                     var20.printStackTrace();
                     Minecraft.getMinecraft()
                        .thePlayer
                        .addChatMessage(new ChatComponentText(EnumChatFormatting.RED + LanguageManager.get("api.fetch.error") + username));
                  }
               }
            }
         );
      } else {
         Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + LanguageManager.get("api.key.missing")));
      }
   }

   private static int bestFastestWin(JsonObject arcade, String mapDisplayName) {
      for (String[] m : MAPS) {
         if (m[0].equalsIgnoreCase(mapDisplayName)) {
            int normal = getInt(arcade, m[3]);
            int hard = getInt(arcade, m[4]);
            int rip = getInt(arcade, m[5]);
            int best = 0;
            if (normal > 0) {
               best = normal;
            }

            if (hard > 0 && (best == 0 || hard < best)) {
               best = hard;
            }

            if (rip > 0 && (best == 0 || rip < best)) {
               best = rip;
            }

            return best;
         }
      }

      return 0;
   }

   private static HypixelAPI.ArcadeResult fetchArcadeStats(String username, boolean silentError) throws Exception {
      String apiKey = ZombiesConfig.INSTANCE.getApiKey();
      HypixelAPI.LookupResult lookup = fetchMojangProfile(username);
      if (lookup.status == HypixelAPI.LookupStatus.NETWORK_ERROR) {
         if (!silentError) {
            Minecraft.getMinecraft()
               .thePlayer
               .addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[ZH] Could not reach Mojang API for " + username + " (network error)"));
         }

         throw new Exception("Mojang network error for " + username);
      } else if (lookup.status != HypixelAPI.LookupStatus.NOT_FOUND && lookup.profile != null) {
         HypixelAPI.MojangProfile profile = lookup.profile;
         String uuid = profile.uuid.replace("-", "");
         String correctName = profile.name;
         HypixelAPI.CachedArcade cached = arcadeCache.get(uuid);
         if (cached != null && !cached.expired()) {
            return new HypixelAPI.ArcadeResult(cached.arcade, correctName, false);
         } else {
            URL hypixelUrl = new URL("https://api.hypixel.net/v2/player?uuid=" + uuid);
            HttpURLConnection hc = (HttpURLConnection)hypixelUrl.openConnection();
            hc.setConnectTimeout(5000);
            hc.setReadTimeout(5000);
            hc.setRequestMethod("GET");
            hc.setRequestProperty("API-Key", apiKey);
            hc.setRequestProperty("User-Agent", "ZombiesHelper/1.0");
            if (hc.getResponseCode() == 200) {
               InputStreamReader hr = new InputStreamReader(hc.getInputStream());
               JsonObject hypixel = new JsonParser().parse(hr).getAsJsonObject();
               hr.close();
               if (hypixel.has("success") && !hypixel.get("success").getAsBoolean()) {
                  String cause = hypixel.has("cause") ? hypixel.get("cause").getAsString() : "Unknown";
                  if (!silentError) {
                     Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[ZH] Hypixel API Error: " + cause));
                  }

                  throw new Exception("Hypixel API error: " + cause);
               } else if (hypixel.has("player") && !hypixel.get("player").isJsonNull()) {
                  JsonObject player = hypixel.getAsJsonObject("player");
                  if (player.has("stats") && !player.get("stats").isJsonNull() && player.getAsJsonObject("stats").has("Arcade")) {
                     JsonObject arcade = player.getAsJsonObject("stats").getAsJsonObject("Arcade");
                     arcadeCache.put(uuid, new HypixelAPI.CachedArcade(arcade));
                     return new HypixelAPI.ArcadeResult(arcade, correctName, false);
                  } else {
                     if (!silentError) {
                        Minecraft.getMinecraft()
                           .thePlayer
                           .addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[ZH] " + correctName + LanguageManager.get("api.no.zombies")));
                     }

                     return new HypixelAPI.ArcadeResult(null, correctName, false);
                  }
               } else {
                  if (!silentError) {
                     Minecraft.getMinecraft()
                        .thePlayer
                        .addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[ZH] " + correctName + LanguageManager.get("api.not.played")));
                  }

                  return new HypixelAPI.ArcadeResult(null, correctName, false);
               }
            } else {
               int code = hc.getResponseCode();
               if (code == 403) {
                  if (!silentError || System.currentTimeMillis() - lastExpiredWarning > 30000L) {
                     Minecraft.getMinecraft()
                        .thePlayer
                        .addChatMessage(new ChatComponentText(EnumChatFormatting.RED + LanguageManager.get("api.key.expired")));
                     lastExpiredWarning = System.currentTimeMillis();
                  }
               } else if (!silentError) {
                  Minecraft.getMinecraft()
                     .thePlayer
                     .addChatMessage(
                        new ChatComponentText(EnumChatFormatting.RED + LanguageManager.get("api.hypixel.error") + LanguageManager.get("api.code") + code + ")")
                     );
               }

               throw new Exception("Hypixel API returned " + code);
            }
         }
      } else {
         if (!silentError) {
            Minecraft.getMinecraft()
               .thePlayer
               .addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[ZH] " + username + " not found — likely Nicked"));
         }

         return new HypixelAPI.ArcadeResult(null, username, true);
      }
   }

   private static HypixelAPI.LookupResult fetchMojangProfile(String username) {
      String key = username.toLowerCase();
      HypixelAPI.MojangProfile cached = profileCache.get(key);
      if (cached != null) {
         return new HypixelAPI.LookupResult(cached, HypixelAPI.LookupStatus.FOUND);
      } else {
         AtomicReference<HypixelAPI.MojangProfile> found = new AtomicReference<>(null);
         AtomicBoolean anyNotFound = new AtomicBoolean(false);
         CountDownLatch allDone = new CountDownLatch(3);
         Runnable[] tasks = new Runnable[]{() -> {
            try {
               HypixelAPI.MojangProfile p = fetchFromAshcon(username);
               if (p != null) {
                  found.compareAndSet(null, p);
               } else {
                  anyNotFound.set(true);
               }
            } catch (HypixelAPI.PlayerNotFoundException var9) {
               anyNotFound.set(true);
            } catch (Exception var10x) {
            } finally {
               allDone.countDown();
            }
         }, () -> {
            try {
               HypixelAPI.MojangProfile p = fetchFromMinecraftServices(username);
               if (p != null) {
                  found.compareAndSet(null, p);
               } else {
                  anyNotFound.set(true);
               }
            } catch (HypixelAPI.PlayerNotFoundException var9) {
               anyNotFound.set(true);
            } catch (Exception var10x) {
            } finally {
               allDone.countDown();
            }
         }, () -> {
            try {
               HypixelAPI.MojangProfile p = fetchFromPlayerDB(username);
               if (p != null) {
                  found.compareAndSet(null, p);
               } else {
                  anyNotFound.set(true);
               }
            } catch (HypixelAPI.PlayerNotFoundException var9) {
               anyNotFound.set(true);
            } catch (Exception var10x) {
            } finally {
               allDone.countDown();
            }
         }};

         for (Runnable t : tasks) {
            executor.submit(t);
         }

         try {
            for (int i = 0; i < 80 && found.get() == null && allDone.getCount() != 0L; i++) {
               Thread.sleep(50L);
            }
         } catch (InterruptedException var11) {
         }

         HypixelAPI.MojangProfile profile = found.get();
         if (profile != null) {
            profileCache.put(key, profile);
            return new HypixelAPI.LookupResult(profile, HypixelAPI.LookupStatus.FOUND);
         } else {
            return anyNotFound.get()
               ? new HypixelAPI.LookupResult(null, HypixelAPI.LookupStatus.NOT_FOUND)
               : new HypixelAPI.LookupResult(null, HypixelAPI.LookupStatus.NETWORK_ERROR);
         }
      }
   }

   private static HypixelAPI.MojangProfile fetchFromAshcon(String username) throws HypixelAPI.PlayerNotFoundException, Exception {
      URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + username);
      HttpURLConnection c = (HttpURLConnection)url.openConnection();
      c.setConnectTimeout(2000);
      c.setReadTimeout(2000);
      c.setRequestProperty("User-Agent", "ZombiesHelper/1.0");
      int code = c.getResponseCode();
      if (code == 404) {
         throw new HypixelAPI.PlayerNotFoundException();
      } else if (code != 200) {
         throw new Exception("Ashcon HTTP " + code);
      } else {
         InputStreamReader r = new InputStreamReader(c.getInputStream());
         JsonObject json = new JsonParser().parse(r).getAsJsonObject();
         r.close();
         return new HypixelAPI.MojangProfile(json.get("uuid").getAsString(), json.get("username").getAsString());
      }
   }

   private static HypixelAPI.MojangProfile fetchFromMinecraftServices(String username) throws HypixelAPI.PlayerNotFoundException, Exception {
      URL url = new URL("https://api.minecraftservices.com/minecraft/profile/lookup/name/" + username);
      HttpURLConnection c = (HttpURLConnection)url.openConnection();
      c.setConnectTimeout(2000);
      c.setReadTimeout(2000);
      c.setRequestProperty("User-Agent", "ZombiesHelper/1.0");
      int code = c.getResponseCode();
      if (code == 404) {
         throw new HypixelAPI.PlayerNotFoundException();
      } else if (code != 200) {
         throw new Exception("MojangServices HTTP " + code);
      } else {
         InputStreamReader r = new InputStreamReader(c.getInputStream());
         JsonObject json = new JsonParser().parse(r).getAsJsonObject();
         r.close();
         return new HypixelAPI.MojangProfile(json.get("id").getAsString(), json.get("name").getAsString());
      }
   }

   private static HypixelAPI.MojangProfile fetchFromPlayerDB(String username) throws HypixelAPI.PlayerNotFoundException, Exception {
      URL url = new URL("https://playerdb.co/api/player/minecraft/" + username);
      HttpURLConnection c = (HttpURLConnection)url.openConnection();
      c.setConnectTimeout(2000);
      c.setReadTimeout(2000);
      c.setRequestProperty("User-Agent", "ZombiesHelper/1.0");
      int code = c.getResponseCode();
      if (code == 404) {
         throw new HypixelAPI.PlayerNotFoundException();
      } else if (code != 200) {
         throw new Exception("PlayerDB HTTP " + code);
      } else {
         InputStreamReader r = new InputStreamReader(c.getInputStream());
         JsonObject json = new JsonParser().parse(r).getAsJsonObject();
         r.close();
         if (!json.get("success").getAsBoolean()) {
            throw new HypixelAPI.PlayerNotFoundException();
         } else {
            JsonObject player = json.getAsJsonObject("data").getAsJsonObject("player");
            return new HypixelAPI.MojangProfile(player.get("raw_id").getAsString(), player.get("username").getAsString());
         }
      }
   }

   private static int getInt(JsonObject obj, String key) {
      return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : 0;
   }

   private static String toApiKey(String displayName) {
      if (displayName == null) {
         return "deadend";
      } else {
         String lower = displayName.toLowerCase();

         for (String[] m : MAPS) {
            if (lower.contains(m[1]) || m[0].toLowerCase().contains(lower) || lower.contains(m[0].toLowerCase())) {
               return m[1];
            }
         }

         return "deadend";
      }
   }

   public static String getMapColor(String displayName) {
      if (displayName == null) {
         return "§f";
      } else {
         String lower = displayName.toLowerCase();

         for (String[] m : MAPS) {
            if (lower.contains(m[1]) || m[0].toLowerCase().contains(lower) || lower.contains(m[0].toLowerCase())) {
               return m[2];
            }
         }

         return "§f";
      }
   }

   public static void clearQueue() {
      if (executor instanceof ThreadPoolExecutor) {
         ((ThreadPoolExecutor)executor).getQueue().clear();
      }
   }

   private static class ArcadeResult {
      JsonObject stats;
      String correctName;
      boolean isNicked;

      ArcadeResult(JsonObject stats, String name, boolean isNicked) {
         this.stats = stats;
         this.correctName = name;
         this.isNicked = isNicked;
      }
   }

   private static class CachedArcade {
      final JsonObject arcade;
      final long timestamp;

      CachedArcade(JsonObject a) {
         this.arcade = a;
         this.timestamp = System.currentTimeMillis();
      }

      boolean expired() {
         return System.currentTimeMillis() - this.timestamp > 600000L;
      }
   }

   private static class LookupResult {
      final HypixelAPI.MojangProfile profile;
      final HypixelAPI.LookupStatus status;

      LookupResult(HypixelAPI.MojangProfile p, HypixelAPI.LookupStatus s) {
         this.profile = p;
         this.status = s;
      }
   }

   private static enum LookupStatus {
      FOUND,
      NOT_FOUND,
      NETWORK_ERROR;
   }

   private static class MojangProfile {
      final String uuid;
      final String name;

      MojangProfile(String uuid, String name) {
         this.uuid = uuid;
         this.name = name;
      }
   }

   private static class PlayerNotFoundException extends Exception {
      private PlayerNotFoundException() {
      }
   }
}
