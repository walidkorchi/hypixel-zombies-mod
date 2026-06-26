package com.johan.zombieshelper.commands;

import com.johan.zombieshelper.GameStateManager;
import com.johan.zombieshelper.api.HypixelAPI;
import com.johan.zombieshelper.api.scoreboard.IScoreboardReader;
import com.johan.zombieshelper.session.SessionTracker;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class ZombiesHelperCommand extends CommandBase {
   public String getCommandName() {
      return "zh";
   }

   public String getCommandUsage(ICommandSender sender) {
      return "/zh <w|debug>";
   }

   public int getRequiredPermissionLevel() {
      return 0;
   }

   public void processCommand(ICommandSender sender, String[] args) throws CommandException {
      if (args.length == 0) {
         sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "ZombiesHelper: /zh w <user>, /zh debug"));
      } else {
         String sub = args[0].toLowerCase();
         if (sub.equals("w")) {
            if (args.length < 2) {
               sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "Usage: /zh w <player>"));
            } else {
               HypixelAPI.fetchFullStats(args[1]);
            }
         } else if (sub.equals("debug")) {
            this.sendDebug(sender);
         } else if (sub.equals("debug_regex")) {
            this.sendDebugRegex(sender);
         } else {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Unknown subcommand."));
         }
      }
   }

   private void sendDebug(ICommandSender sender) {
      sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "=== ZombiesHelper Debug ==="));
      sender.addChatMessage(new ChatComponentText("In Zombies: " + GameStateManager.inZombiesGame));
      sender.addChatMessage(new ChatComponentText("Started:    " + GameStateManager.gameStarted));
      sender.addChatMessage(new ChatComponentText("Map:        " + GameStateManager.currentMap));
      sender.addChatMessage(new ChatComponentText("Round:      " + GameStateManager.currentRound));
      sender.addChatMessage(new ChatComponentText("Kills (GSM): " + GameStateManager.currentKills));
      sender.addChatMessage(new ChatComponentText("Kills (Tracker): " + SessionTracker.get().getKills()));
      IScoreboardReader r = GameStateManager.reader;
      if (r == null) {
         sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Reader is NULL!"));
      } else if (!r.isAvailable()) {
         sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Scoreboard NOT AVAILABLE"));
      } else {
         sender.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "Lines:"));

         for (String line : r.getCleanLines()) {
            sender.addChatMessage(new ChatComponentText("  " + line));
         }
      }
   }

   private void sendDebugRegex(ICommandSender sender) {
      sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "=== Regex Debug ==="));
      IScoreboardReader r = GameStateManager.reader;
      if (r != null && r.isAvailable()) {
         Pattern p = Pattern.compile("(?i)(?:Kills|Asesinatos|Bajas|Muertes)[^\\d]*([\\d,]+)");

         for (String line : r.getCleanLines()) {
            Matcher m = p.matcher(line);
            if (m.find()) {
               sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "MATCH: " + line + " -> " + m.group(1)));
            } else if (line.toLowerCase().contains("kills")) {
               sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "FAIL: " + line));
            }
         }
      } else {
         sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Reader not available"));
      }
   }

   public List<String> getCommandAliases() {
      List<String> list = new ArrayList<>();
      list.add("zombieshelper");
      list.add("zs");
      return list;
   }
}
