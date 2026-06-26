package com.johan.zombieshelper.api.scoreboard;

import java.util.List;

public interface IScoreboardReader {
   boolean isAvailable();

   String getTitle();

   List<String> getRawLines();

   List<String> getCleanLines();

   String getLine(int var1);

   int getScoreboardSize();

   boolean isEmpty();

   List<String> getLobbyPlayers();
}
