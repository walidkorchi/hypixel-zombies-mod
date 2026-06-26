package com.johan.zombieshelper.keybinds;

import com.johan.zombieshelper.config.ZombiesConfig;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;

public class KeyHandler {
   public static KeyBinding openConfig = new KeyBinding("Open Config", 44, "Hypixel Zombies Helper + Tracker");

   public KeyHandler() {
      ClientRegistry.registerKeyBinding(openConfig);
   }

   @SubscribeEvent
   public void onKeyInput(KeyInputEvent event) {
      if (openConfig.isPressed()) {
         ZombiesConfig.INSTANCE.openGui();
      }
   }
}
