package com.johan.zombieshelper.data;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class StatsManager {
   private static final Map<String, PlayerStats> playerStatsMap = new LinkedHashMap<>();
   private static final Set<String> expectedUsers = new HashSet<>();

   public static synchronized void clear() {
      playerStatsMap.clear();
      expectedUsers.clear();
   }

   public static synchronized void removeUser(String username) {
      playerStatsMap.remove(username);
      expectedUsers.remove(username);
   }

   public static synchronized void setExpected(String username) {
      expectedUsers.add(username);
   }

   public static synchronized void addStats(PlayerStats stats) {
      if (expectedUsers.contains(stats.getUsername())) {
         playerStatsMap.put(stats.getUsername(), stats);
      }
   }

   public static synchronized Collection<PlayerStats> getStats() {
      return playerStatsMap.values();
   }

   public static synchronized boolean isEmpty() {
      return playerStatsMap.isEmpty();
   }

   public static synchronized int getPlayerCount() {
      return playerStatsMap.size();
   }

   public static synchronized int getExpectedCount() {
      return expectedUsers.size();
   }
}
