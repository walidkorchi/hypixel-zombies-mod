package com.johan.zombieshelper.config;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.Button;
import cc.polyfrost.oneconfig.config.annotations.HUD;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.annotations.Text;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import com.johan.zombieshelper.hud.SessionHud;
import com.johan.zombieshelper.rpc.DiscordRPC;
import com.johan.zombieshelper.hud.StrategyGuideHud;
import com.johan.zombieshelper.hud.ZombiesStatsHud;
import com.johan.zombieshelper.session.SessionTracker;
import com.johan.zombieshelper.strategy.StrategyManager;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

public class ZombiesConfig extends Config {
   public static final ZombiesConfig INSTANCE = new ZombiesConfig();
   @Text(
      name = "Hypixel API Key",
      description = "Your Hypixel API key to fetch stats. Run /api new on Hypixel.",
      placeholder = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
      secure = true
   )
   public String apiKey = "";
   @Button(
      name = "Apply API Key",
      text = "Apply"
   )
   public Runnable applyApiKey = () -> {
      if (this.apiKey != null && !this.apiKey.trim().isEmpty()) {
         this.save();
         if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§a[ZombiesStats] §aAPI Key successfully applied and saved!"));
         }
      } else if (Minecraft.getMinecraft().thePlayer != null) {
         Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§c[ZombiesStats] §cAPI Key field is empty!"));
      }
   };
   @Button(
      name = "Delete API Key",
      text = "Delete"
   )
   public Runnable deleteApiKey = () -> {
      this.apiKey = "";
      this.save();
      if (Minecraft.getMinecraft().thePlayer != null) {
         Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§e[ZombiesStats] §eAPI Key successfully deleted."));
      }
   };
   @HUD(
      name = "Lobby Stats HUD",
      category = "Stats HUD"
   )
   public ZombiesStatsHud lobbyStatsHud = new ZombiesStatsHud();
   @Text(
      name = "Strategy Mode",
      description = "Type your mode, or click 'Cycle Modes' to pick from detected JSONs.",
      category = "Strategy Guide",
      placeholder = "QUAD"
   )
   public String strategyMode = "QUAD";
   @Button(
      name = "Cycle Detected Modes",
      text = "Next Mode",
      category = "Strategy Guide"
   )
   public Runnable cycleModes = () -> {
      List<String> modes = StrategyManager.getAvailableModes();
      if (modes.isEmpty()) {
         if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§c[ZombiesStats] §cNo strategy modes detected! Reload first."));
         }
      } else {
         int idx = -1;

         for (int i = 0; i < modes.size(); i++) {
            if (modes.get(i).equalsIgnoreCase(this.strategyMode.trim())) {
               idx = i;
               break;
            }
         }

         if (idx != -1 && idx != modes.size() - 1) {
            this.strategyMode = modes.get(idx + 1);
         } else {
            this.strategyMode = modes.get(0);
         }

         this.save();
         if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§a[ZombiesStats] §aMode set to: §e" + this.strategyMode));
         }
      }
   };
   @Switch(
      name = "Force Strategy HUD",
      description = "Muestra la guía incluso si el mapa no es detectado como Alien Arcadium",
      category = "Strategy Guide"
   )
   public boolean forceStrategyHud = false;
   @Button(
      name = "Reload Custom Strategies",
      text = "Reload",
      category = "Strategy Guide"
   )
   public Runnable reloadStrategies = () -> {
      StrategyManager.loadStrategies();
      if (Minecraft.getMinecraft().thePlayer != null) {
         Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§a[ZombiesStats] §aCustom strategies reloaded from JSON!"));
      }
   };
   @HUD(
      name = "Strategy Guide HUD",
      category = "Strategy Guide"
   )
   public StrategyGuideHud strategyHud = new StrategyGuideHud();
   @Switch(
      name = "Enable Discord Rich Presence",
      description = "Muestra tu estado en Zombies (mapa, ronda, lobby) en tu perfil de Discord. El timer se reinicia al cambiar de actividad.",
      category = "Discord RPC"
   )
   public boolean discordRpcEnabled = true;
   @Text(
      name = "Discord Client ID",
      description = "The Discord application Client ID used for Rich Presence. Required: leave empty to disable RPC.",
      placeholder = "Your Discord application Client ID",
      category = "Discord RPC"
   )
   public String discordClientId = "";
   @Button(
      name = "Apply Client ID",
      text = "Apply",
      category = "Discord RPC"
   )
   public Runnable applyDiscordClientId = () -> {
      this.save();
      DiscordRPC.getInstance().shutdown();
      DiscordRPC.getInstance().init();
      if (Minecraft.getMinecraft().thePlayer != null) {
         Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§a[ZombiesStats] §aDiscord Client ID applied. Reconnecting RPC..."));
      }
   };
   @HUD(
      name = "Session HUD",
      category = "Session"
   )
   public SessionHud sessionHud = new SessionHud();
   @Button(
      name = "Reset Session",
      text = "Reset",
      category = "Session"
   )
   public Runnable resetSession = () -> {
      SessionTracker.get().reset();
      if (Minecraft.getMinecraft().thePlayer != null) {
         Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§a[ZombiesStats] §aSession stats reset!"));
      }
   };

   public ZombiesConfig() {
      super(new Mod("Zombies Helper", ModType.UTIL_QOL), "zombieshelper.json");
      this.initialize();
   }

   public String getApiKey() {
      return this.apiKey;
   }

   public String getDiscordClientId() {
      return this.discordClientId == null ? "" : this.discordClientId.trim();
   }

   public String getAAChallengeString() {
      return this.strategyMode != null && !this.strategyMode.trim().isEmpty() ? this.strategyMode.trim().toUpperCase() : "QUAD";
   }
}
