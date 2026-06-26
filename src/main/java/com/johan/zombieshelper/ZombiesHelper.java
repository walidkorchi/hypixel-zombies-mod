package com.johan.zombieshelper;

import cc.polyfrost.oneconfig.events.EventManager;
import com.johan.zombieshelper.api.scoreboard.IScoreboardReader;
import com.johan.zombieshelper.commands.ZombiesHelperCommand;
import com.johan.zombieshelper.config.ZombiesConfig;
import com.johan.zombieshelper.gui.FullStatsHudRenderer;
import com.johan.zombieshelper.keybinds.KeyHandler;
import com.johan.zombieshelper.listeners.GameListener;
import com.johan.zombieshelper.rpc.DiscordRPC;
import com.johan.zombieshelper.session.SessionListener;
import com.johan.zombieshelper.strategy.StrategyManager;
import com.johan.zombieshelper.update.UpdateNotifier;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(
   modid = "zombieshelper",
   name = "ZombiesHelper",
   version = "4.3",
   acceptedMinecraftVersions = "[1.8.9]"
)
public class ZombiesHelper {
   public static final String MODID = "zombieshelper";
   public static final String NAME = "ZombiesHelper";
   public static final String VERSION = "4.3";
   @Instance("zombieshelper")
   public static ZombiesHelper instance;

   @EventHandler
   public void preInit(FMLPreInitializationEvent event) {
      // Touch the config singleton so OneConfig registers it during pre-init.
      ZombiesConfig.INSTANCE.getClass();
   }

   @EventHandler
   public void init(FMLInitializationEvent event) {
      GameStateManager gsm = new GameStateManager();

      try {
         Class<?> readerClass = Class.forName("com.johan.zombieshelper.forge.ScoreboardReader189");
         GameStateManager.reader = (IScoreboardReader)readerClass.newInstance();
      } catch (Exception var6) {
         System.err.println("[ZombiesHelper] FATAL: Could not instantiate ScoreboardReader189: " + var6);
      }

      EventManager.INSTANCE.register(gsm);
      GameListener gameListener = new GameListener();
      EventManager.INSTANCE.register(gameListener);
      MinecraftForge.EVENT_BUS.register(gameListener);
      MinecraftForge.EVENT_BUS.register(new FullStatsHudRenderer());
      KeyHandler keyHandler = new KeyHandler();
      MinecraftForge.EVENT_BUS.register(keyHandler);
      StrategyManager.init();
      ClientCommandHandler.instance.registerCommand(new ZombiesHelperCommand());
      SessionListener sessionListener = new SessionListener();
      MinecraftForge.EVENT_BUS.register(sessionListener);
      DiscordRPC.getInstance().init();
      MinecraftForge.EVENT_BUS.register(DiscordRPC.getInstance());
      UpdateNotifier.checkAsync();
      MinecraftForge.EVENT_BUS.register(new UpdateNotifier());
   }
}
