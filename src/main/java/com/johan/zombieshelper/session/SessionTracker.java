package com.johan.zombieshelper.session;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SessionTracker {
   private static final SessionTracker INSTANCE = new SessionTracker();
   private volatile long sessionStartMs = 0L;
   private volatile boolean sessionStarted = false;
   private volatile long gameStartMs = 0L;
   private volatile boolean wasInGame = false;
   private volatile int lastRoundSeen = 0;
   private final AtomicInteger gamesPlayed = new AtomicInteger(0);
   private final AtomicInteger wins = new AtomicInteger(0);
   private final AtomicInteger kills = new AtomicInteger(0);
   private final AtomicInteger deaths = new AtomicInteger(0);
   private final AtomicInteger downs = new AtomicInteger(0);
   private final AtomicInteger revives = new AtomicInteger(0);
   private final AtomicInteger bestRound = new AtomicInteger(0);
   private final AtomicInteger totalRounds = new AtomicInteger(0);
   private final AtomicLong bulletsHit = new AtomicLong(0L);
   private final AtomicLong bulletsShot = new AtomicLong(0L);
   private final AtomicInteger criticalHits = new AtomicInteger(0);
   private final AtomicInteger totalHits = new AtomicInteger(0);
   private final AtomicInteger totalGoldEarned = new AtomicInteger(0);
   private volatile int lastGoldSeen = 0;
   private volatile long totalPlayMs = 0L;
   private final AtomicInteger previousGamesKills = new AtomicInteger(0);

   public static SessionTracker get() {
      return INSTANCE;
   }

   private SessionTracker() {
   }

   public void markHudActive() {
   }

   public int getGamesPlayed() {
      return this.gamesPlayed.get();
   }

   public int getWins() {
      return this.wins.get();
   }

   public int getKills() {
      return this.kills.get();
   }

   public int getDeaths() {
      return this.deaths.get();
   }

   public int getDowns() {
      return this.downs.get();
   }

   public int getRevives() {
      return this.revives.get();
   }

   public int getBestRound() {
      return this.bestRound.get();
   }

   public int getTotalRounds() {
      return this.totalRounds.get();
   }

   public int getTotalGoldEarned() {
      return this.totalGoldEarned.get();
   }

   public int getCriticalAccuracyPct() {
      int hits = this.totalHits.get();
      return hits == 0 ? -1 : (int)Math.round((double)this.criticalHits.get() * 100.0 / (double)hits);
   }

   public synchronized void syncGold(int currentGold) {
      if (this.wasInGame) {
         if (currentGold > this.lastGoldSeen) {
            this.totalGoldEarned.addAndGet(currentGold - this.lastGoldSeen);
         }

         this.lastGoldSeen = currentGold;
      }
   }

   public long getSessionElapsedSeconds() {
      return this.sessionStarted && this.sessionStartMs != 0L ? (System.currentTimeMillis() - this.sessionStartMs) / 1000L : 0L;
   }

   public long getTotalPlaySeconds() {
      long total = this.totalPlayMs;
      if (this.wasInGame && this.gameStartMs > 0L) {
         total += System.currentTimeMillis() - this.gameStartMs;
      }

      return total / 1000L;
   }

   public synchronized void addKills(int k) {
      this.kills.addAndGet(k);
   }

   public synchronized void addDeaths(int d) {
      this.deaths.addAndGet(d);
   }

   public synchronized void addDowns(int d) {
      this.downs.addAndGet(d);
   }

   public synchronized void addRevives(int r) {
      this.revives.addAndGet(r);
   }

   public synchronized void addBullets(long hit, long shot) {
      this.bulletsHit.addAndGet(hit);
      this.bulletsShot.addAndGet(shot);
   }

   public synchronized void addHit(boolean critical) {
      this.totalHits.incrementAndGet();
      if (critical) {
         this.criticalHits.incrementAndGet();
      }
   }

   public synchronized void addWin() {
      this.wins.incrementAndGet();
   }

   public synchronized void syncKills(int currentKills) {
      if (this.wasInGame && currentKills > 0) {
         int newTotal = this.previousGamesKills.get() + currentKills;
         if (newTotal > this.kills.get()) {
            this.kills.set(newTotal);
         }
      }
   }

   public synchronized void onGameStart() {
      if (!this.sessionStarted) {
         this.sessionStarted = true;
         this.sessionStartMs = System.currentTimeMillis();
      }

      if (!this.wasInGame) {
         this.wasInGame = true;
         this.gameStartMs = System.currentTimeMillis();
         this.gamesPlayed.incrementAndGet();
         this.lastRoundSeen = 0;
         this.lastGoldSeen = 0;
      }
   }

   public synchronized void onRoundUpdate(int round) {
      this.lastRoundSeen = round;
      if (round > this.bestRound.get()) {
         this.bestRound.set(round);
      }
   }

   public synchronized void onGameEnd(boolean won) {
      if (this.wasInGame) {
         this.wasInGame = false;
         if (this.gameStartMs > 0L) {
            this.totalPlayMs = this.totalPlayMs + (System.currentTimeMillis() - this.gameStartMs);
            this.gameStartMs = 0L;
         }

         if (this.lastRoundSeen > 0) {
            this.totalRounds.addAndGet(this.lastRoundSeen);
         }

         this.previousGamesKills.set(this.kills.get());
         if (won) {
            this.wins.incrementAndGet();
         }
      }
   }

   public synchronized void reset() {
      this.gamesPlayed.set(0);
      this.wins.set(0);
      this.kills.set(0);
      this.deaths.set(0);
      this.downs.set(0);
      this.revives.set(0);
      this.bestRound.set(0);
      this.totalRounds.set(0);
      this.previousGamesKills.set(0);
      this.bulletsHit.set(0L);
      this.bulletsShot.set(0L);
      this.criticalHits.set(0);
      this.totalHits.set(0);
      this.totalPlayMs = 0L;
      this.gameStartMs = 0L;
      this.wasInGame = false;
      this.lastRoundSeen = 0;
      this.lastGoldSeen = 0;
      this.sessionStarted = false;
      this.sessionStartMs = 0L;
   }
}
