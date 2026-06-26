package com.johan.zombieshelper.forge;

import com.johan.zombieshelper.api.scoreboard.IScoreboardReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

public class ScoreboardReader189 implements IScoreboardReader {
   private static String strip(String s) {
      if (s == null) {
         return "";
      } else {
         String out = s.replaceAll("§.", "");
         out = out.replaceAll("[\u0000-\u001f]", "");
         return out.trim();
      }
   }

   private Scoreboard getScoreboard() {
      Minecraft mc = Minecraft.getMinecraft();
      return mc != null && mc.theWorld != null ? mc.theWorld.getScoreboard() : null;
   }

   private ScoreObjective getSidebarObjective() {
      Scoreboard sb = this.getScoreboard();
      return sb == null ? null : sb.getObjectiveInDisplaySlot(1);
   }

   private List<Score> getSortedScores() {
      Scoreboard sb = this.getScoreboard();
      ScoreObjective obj = this.getSidebarObjective();
      if (sb != null && obj != null) {
         Collection<Score> all = sb.getSortedScores(obj);
         List<Score> filtered = new ArrayList<>();

         for (Score score : all) {
            if (score.getPlayerName() != null && !score.getPlayerName().startsWith("#")) {
               filtered.add(score);
            }
         }

         if (filtered.size() > 15) {
            filtered = new ArrayList<>(filtered).subList(filtered.size() - 15, filtered.size());
         }

         return filtered;
      } else {
         return Collections.emptyList();
      }
   }

   @Override
   public boolean isAvailable() {
      return this.getSidebarObjective() != null;
   }

   @Override
   public String getTitle() {
      ScoreObjective obj = this.getSidebarObjective();
      return obj != null ? obj.getDisplayName() : "";
   }

   @Override
   public List<String> getRawLines() {
      List<String> lines = new ArrayList<>();
      Scoreboard sb = this.getScoreboard();
      if (sb == null) {
         return lines;
      } else {
         for (Score score : this.getSortedScores()) {
            ScorePlayerTeam team = sb.getPlayersTeam(score.getPlayerName());
            String prefix = team != null && team.getColorPrefix() != null ? team.getColorPrefix() : "";
            String suffix = team != null && team.getColorSuffix() != null ? team.getColorSuffix() : "";
            String rawName = score.getPlayerName() != null ? score.getPlayerName() : "";
            String cleanedName = rawName.replaceAll("[\u0000-\u001f]", "");
            lines.add(prefix + cleanedName + suffix);
         }

         return lines;
      }
   }

   @Override
   public List<String> getCleanLines() {
      List<String> raw = this.getRawLines();
      List<String> clean = new ArrayList<>(raw.size());

      for (String line : raw) {
         clean.add(strip(line));
      }

      return clean;
   }

   @Override
   public String getLine(int index) {
      List<String> lines = this.getCleanLines();
      return index >= 0 && index < lines.size() ? lines.get(index) : "";
   }

   @Override
   public int getScoreboardSize() {
      return this.getSortedScores().size();
   }

   @Override
   public boolean isEmpty() {
      return this.getSortedScores().isEmpty();
   }

   @Override
   public List<String> getLobbyPlayers() {
      List<String> players = new ArrayList<>();
      Minecraft mc = Minecraft.getMinecraft();
      if (mc != null && mc.getNetHandler() != null) {
         for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info.getGameProfile() != null) {
               String name = info.getGameProfile().getName();
               if (name != null && name.matches("^[A-Za-z0-9_]{3,16}$")) {
                  players.add(name);
               }
            }
         }

         return players;
      } else {
         return players;
      }
   }
}
