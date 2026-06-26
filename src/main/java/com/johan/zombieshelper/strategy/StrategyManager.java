package com.johan.zombieshelper.strategy;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.johan.zombieshelper.GameStateManager;
import com.johan.zombieshelper.config.ZombiesConfig;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

public class StrategyManager {
   private static Map<String, String> generalStrategies = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
   private static Map<String, Map<String, Map<Integer, String>>> allStrategies = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

   public static void init() {
      loadStrategies();
   }

   private static File getConfigDir() {
      return new File("config/zombieshelper");
   }

   public static void loadStrategies() {
      generalStrategies.clear();
      allStrategies.clear();
      File dir = getConfigDir();
      if (!dir.exists() || !dir.isDirectory()) {
         dir.mkdirs();
      }

      File[] files = dir.listFiles();
      if (files != null) {
         Gson gson = new Gson();

         for (File file : files) {
            if (file.getName().endsWith(".json")) {
               try {
                  Reader reader = new FileReader(file);
                  Type type = (new TypeToken<Map<String, Object>>() {
                  }).getType();
                  Map<String, Object> data = (Map<String, Object>)gson.fromJson(reader, type);
                  reader.close();
                  if (data != null) {
                     if (file.getName().equalsIgnoreCase("general.json")) {
                        for (Entry<String, Object> entry : data.entrySet()) {
                           generalStrategies.put(entry.getKey(), String.valueOf(entry.getValue()));
                        }
                     } else if (!data.containsKey("maps") && !data.containsKey("modes")) {
                        if (data.containsKey("map") && data.containsKey("mode") && data.containsKey("rounds")) {
                           String mapName = String.valueOf(data.get("map"));
                           String modeName = String.valueOf(data.get("mode"));
                           Map<String, Map<Integer, String>> mapModes = allStrategies.get(mapName);
                           if (mapModes == null) {
                              mapModes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                              allStrategies.put(mapName, mapModes);
                           }

                           Map<Integer, String> roundMap = mapModes.get(modeName);
                           if (roundMap == null) {
                              roundMap = new HashMap<>();
                              mapModes.put(modeName, roundMap);
                           }

                           Map<String, Object> rawRounds = (Map<String, Object>)data.get("rounds");

                           for (Entry<String, Object> entry : rawRounds.entrySet()) {
                              try {
                                 roundMap.put(Integer.parseInt(entry.getKey()), String.valueOf(entry.getValue()));
                              } catch (NumberFormatException var23) {
                              }
                           }
                        }
                     } else {
                        if (data.containsKey("general")) {
                           try {
                              Map<String, String> rawGeneral = (Map<String, String>)data.get("general");
                              generalStrategies.putAll(rawGeneral);
                           } catch (Exception var26) {
                           }
                        }

                        if (data.containsKey("maps")) {
                           Map<String, Map<String, Map<String, String>>> rawMaps = (Map<String, Map<String, Map<String, String>>>)data.get("maps");

                           for (Entry<String, Map<String, Map<String, String>>> mapEntry : rawMaps.entrySet()) {
                              String mapNamex = mapEntry.getKey();
                              Map<String, Map<Integer, String>> modesMap = allStrategies.get(mapNamex);
                              if (modesMap == null) {
                                 modesMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                                 allStrategies.put(mapNamex, modesMap);
                              }

                              for (Entry<String, Map<String, String>> modeEntry : mapEntry.getValue().entrySet()) {
                                 String modeNamex = modeEntry.getKey();
                                 Map<Integer, String> roundMap = modesMap.get(modeNamex);
                                 if (roundMap == null) {
                                    roundMap = new HashMap<>();
                                    modesMap.put(modeNamex, roundMap);
                                 }

                                 for (Entry<String, String> roundEntry : modeEntry.getValue().entrySet()) {
                                    try {
                                       roundMap.put(Integer.parseInt(roundEntry.getKey()), roundEntry.getValue());
                                    } catch (NumberFormatException var25) {
                                    }
                                 }
                              }
                           }
                        } else if (data.containsKey("modes")) {
                           Map<String, Map<String, String>> rawModes = (Map<String, Map<String, String>>)data.get("modes");
                           Map<String, Map<Integer, String>> aaModes = allStrategies.get("Alien Arcadium");
                           if (aaModes == null) {
                              aaModes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                              allStrategies.put("Alien Arcadium", aaModes);
                           }

                           for (Entry<String, Map<String, String>> entry : rawModes.entrySet()) {
                              String modeNamex = entry.getKey();
                              Map<Integer, String> roundMap = new HashMap<>();

                              for (Entry<String, String> roundEntry : entry.getValue().entrySet()) {
                                 try {
                                    roundMap.put(Integer.parseInt(roundEntry.getKey()), roundEntry.getValue());
                                 } catch (NumberFormatException var24) {
                                 }
                              }

                              aaModes.put(modeNamex, roundMap);
                           }
                        }
                     }
                  }
               } catch (Exception var27) {
                  System.err.println("Failed to load strategy file: " + file.getName());
                  var27.printStackTrace();
               }
            }
         }
      }
   }

   public static String getStrategy(int round) {
      String currentMap = GameStateManager.currentMap;
      if (currentMap == null || currentMap.isEmpty() || currentMap.equalsIgnoreCase("Unknown")) {
         currentMap = "Alien Arcadium";
      }

      String mode = "QUAD";
      if (ZombiesConfig.INSTANCE != null) {
         mode = ZombiesConfig.INSTANCE.getAAChallengeString();
      }

      Map<String, Map<Integer, String>> mapModes = allStrategies.get(currentMap);
      if (mapModes == null) {
         for (String key : allStrategies.keySet()) {
            if (currentMap.toLowerCase().contains(key.toLowerCase()) || key.toLowerCase().contains(currentMap.toLowerCase())) {
               mapModes = allStrategies.get(key);
               break;
            }
         }
      }

      if (mapModes == null) {
         mapModes = allStrategies.get("Alien Arcadium");
      }

      if (mapModes == null) {
         return "Strategy not found";
      } else {
         Map<Integer, String> modeMap = mapModes.get(mode);
         if (modeMap == null) {
            modeMap = mapModes.get("QUAD");
         }

         return modeMap == null ? "Strategy not found" : modeMap.getOrDefault(round, "...");
      }
   }

   public static String getGeneralStrategy(String topic) {
      return generalStrategies.getOrDefault(topic, "Topic not found.");
   }

   public static List<String> getAvailableModes() {
      Set<String> modes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

      for (Map<String, Map<Integer, String>> mapModes : allStrategies.values()) {
         modes.addAll(mapModes.keySet());
      }

      return new ArrayList<>(modes);
   }
}
