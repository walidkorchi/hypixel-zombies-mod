package com.johan.zombieshelper.data;

import java.util.List;

public class FullPlayerStats {
   public final String username;
   public final List<FullMapStats> maps;

   public FullPlayerStats(String username, List<FullMapStats> maps) {
      this.username = username;
      this.maps = maps;
   }
}
