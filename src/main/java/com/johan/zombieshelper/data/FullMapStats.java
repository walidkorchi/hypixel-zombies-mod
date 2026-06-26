package com.johan.zombieshelper.data;

public class FullMapStats {
   public final String mapName;
   public final int downs;
   public final int revives;
   public final int doorsOpened;
   public final int windowsRepaired;
   public final int kills;
   public final int deaths;
   public final int bestRound;
   public final int wins;
   public final int fastestWinSecs;

   public FullMapStats(
      String mapName, int downs, int revives, int doorsOpened, int windowsRepaired, int kills, int deaths, int bestRound, int wins, int fastestWinSecs
   ) {
      this.mapName = mapName;
      this.downs = downs;
      this.revives = revives;
      this.doorsOpened = doorsOpened;
      this.windowsRepaired = windowsRepaired;
      this.kills = kills;
      this.deaths = deaths;
      this.bestRound = bestRound;
      this.wins = wins;
      this.fastestWinSecs = fastestWinSecs;
   }
}
