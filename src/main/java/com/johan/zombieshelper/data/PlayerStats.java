package com.johan.zombieshelper.data;

public class PlayerStats {
   private final String username;
   private final int bestRound;
   private final int wins;
   private final int kills;
   private final int fastestWin;
   private final boolean isNicked;

   public PlayerStats(String username, int bestRound, int wins, int kills, int fastestWin) {
      this(username, bestRound, wins, kills, fastestWin, false);
   }

   public PlayerStats(String username, int bestRound, int wins, int kills, int fastestWin, boolean isNicked) {
      this.username = username;
      this.bestRound = bestRound;
      this.wins = wins;
      this.kills = kills;
      this.fastestWin = fastestWin;
      this.isNicked = isNicked;
   }

   public String getUsername() {
      return this.username;
   }

   public int getBestRound() {
      return this.bestRound;
   }

   public int getWins() {
      return this.wins;
   }

   public int getKills() {
      return this.kills;
   }

   public int getFastestWin() {
      return this.fastestWin;
   }

   public boolean isNicked() {
      return this.isNicked;
   }
}
