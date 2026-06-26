package com.johan.zombieshelper;

import com.johan.zombieshelper.config.ZombiesConfig;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
   private static final Map<String, String[]> dict = new HashMap<>();

   public static String get(String key) {
      int index = 0;
      if (ZombiesConfig.INSTANCE != null) {
         String lang = ZombiesConfig.INSTANCE.getLanguageString();
         index = "ES".equalsIgnoreCase(lang) ? 1 : 0;
      }

      return dict.containsKey(key) ? dict.get(key)[index] : key;
   }

   static {
      dict.put("cmd.api.usage", new String[]{"Usage: /zh api <key>", "Uso: /zh api <key>"});
      dict.put("cmd.w.usage", new String[]{"Usage: /zh w <username>", "Uso: /zh w <usuario>"});
      dict.put("cmd.unknown", new String[]{"Unknown subcommand. ", "Subcomando desconocido. "});
      dict.put("cmd.lang.success", new String[]{"[ZH] Language changed to English.", "[ZH] Idioma cambiado a Español."});
      dict.put("api.key.success", new String[]{"[ZH] API Key configured successfully.", "[ZH] API Key configurada correctamente."});
      dict.put("hud.stats.enabled", new String[]{"[ZH] Stats HUD enabled.", "[ZH] Stats HUD activado."});
      dict.put("hud.stats.disabled", new String[]{"[ZH] Stats HUD disabled.", "[ZH] Stats HUD desactivado."});
      dict.put("hud.strategy.enabled", new String[]{"[ZH] Strategy HUD enabled.", "[ZH] Strategy HUD activado."});
      dict.put("hud.strategy.disabled", new String[]{"[ZH] Strategy HUD disabled.", "[ZH] Strategy HUD desactivado."});
      dict.put(
         "hud.reset.success", new String[]{"[ZH] HUD position reset to bottom-right corner.", "[ZH] Posición del HUD reseteada a la esquina inferior derecha."}
      );
      dict.put("api.key.missing", new String[]{"[ZH] API Key not configured. Use /zh api <key>", "[ZH] API Key no configurada. Usa /zh api <key>"});
      dict.put(
         "api.key.expired",
         new String[]{
            "[ZH] Your Hypixel API Key is invalid or expired! Please update it in the OneConfig menu.",
            "[ZH] ¡Tu API Key de Hypixel es inválida o expiró! Actualízala en el menú OneConfig."
         }
      );
      dict.put("api.fetching", new String[]{"[ZH] Fetching stats for ", "[ZH] Buscando stats de "});
      dict.put("api.fetch.error", new String[]{"[ZH] Error fetching stats for ", "[ZH] Error al obtener stats de "});
      dict.put("api.uuid.error", new String[]{"[ZH] Could not fetch UUID for ", "[ZH] No se pudo obtener el UUID de "});
      dict.put("api.hypixel.error", new String[]{"[ZH] Hypixel API failed ", "[ZH] Falló la API de Hypixel "});
      dict.put("api.code", new String[]{"(Code ", "(Código "});
      dict.put("api.not.played", new String[]{" has not played on Hypixel.", " no ha jugado en Hypixel."});
      dict.put("api.no.zombies", new String[]{" has no Arcade/Zombies stats.", " no tiene stats de Arcade/Zombies."});
      dict.put("hud.drag", new String[]{"Drag the HUD to move it", "Arrastra el HUD para moverlo"});
      dict.put("hud.searching", new String[]{"Searching...", "Buscando..."});
      dict.put("hud.not_in_zombies", new String[]{"Not in Zombies", "No estás en Zombies"});
      dict.put("hud.round", new String[]{"Round", "Ronda"});
      dict.put("hud.kills", new String[]{"Kills", "Kills"});
      dict.put("hud.wins", new String[]{"Wins", "Wins"});
      dict.put("hud.fast", new String[]{"Fast", "T. Rápido"});
      dict.put("hud.win", new String[]{"Win", "Win"});
      dict.put("hud.loading", new String[]{"Loading stats...", "Cargando stats..."});
      dict.put("stats.map", new String[]{"Map", "Mapa"});
      dict.put("stats.downs", new String[]{"Downs", "Downs"});
      dict.put("stats.revives", new String[]{"Revives", "Revs"});
      dict.put("stats.doors", new String[]{"Doors", "Puertas"});
      dict.put("stats.windows", new String[]{"Windows", "Ventanas"});
      dict.put("stats.kills", new String[]{"Kills", "Kills"});
      dict.put("stats.deaths", new String[]{"Deaths", "Muertes"});
      dict.put("stats.best_r", new String[]{"Best R", "Mejor R"});
      dict.put("stats.wins", new String[]{"Wins", "Victorias"});
      dict.put("stats.fast_w", new String[]{"Fast W", "T. Rápido"});
      dict.put("stats.title", new String[]{"- Zombies Stats", "- Stats de Zombies"});
      dict.put("gui.return.game", new String[]{"Return to game", "Volver al juego"});
      dict.put("gui.strategy.title", new String[]{"Zombies Strategy Menu", "Menú de Estrategia Zombies"});
      dict.put("gui.strategy.current", new String[]{"Strategy: Current Round", "Estrategia: Ronda Actual"});
      dict.put("gui.strategy.invite", new String[]{"Invite Bots", "Invitar Bots (/p)"});
      dict.put("gui.strategy.close", new String[]{"Close", "Cerrar"});
      dict.put("gui.strategy.no_game", new String[]{"No Active Game", "Sin Partida Activa"});
      dict.put("gui.strategy.no_game_desc", new String[]{"You are not in a Zombies game (or round 0).", "No estas en una partida de Zombies (o ronda 0)."});
      dict.put("gui.strategy.round", new String[]{"Round ", "Ronda "});
      dict.put("gui.strategy.strat", new String[]{" Strategy", " Estrategia"});
   }
}
