package rpg.core;

import java.util.Random;

public class Combat {
    private static final Random RNG = new Random();

    public static Character simulate(Character a, Character b, boolean verbose) {
        int hpA = Math.max(10, a.getStrength() * 10 + a.getIntelligence() * 2);
        int hpB = Math.max(10, b.getStrength() * 10 + b.getIntelligence() * 2);

        int turn = 0;
        if (verbose) System.out.println("--- Combat start: " + a.getName() + " vs " + b.getName() + " ---");

        while (hpA > 0 && hpB > 0) {
            if (turn % 2 == 0) {
                int dmg = a.getStrength() + RNG.nextInt(Math.max(1, a.getAgility() + 1));
                hpB -= dmg;
                if (verbose) System.out.printf("%s hits %s for %d (hpB=%d)\n", a.getName(), b.getName(), dmg, Math.max(0, hpB));
            } else {
                int dmg = b.getStrength() + RNG.nextInt(Math.max(1, b.getAgility() + 1));
                hpA -= dmg;
                if (verbose) System.out.printf("%s hits %s for %d (hpA=%d)\n", b.getName(), a.getName(), dmg, Math.max(0, hpA));
            }
            turn++;
            if (turn > 1000) break; 
        }

        Character winner = hpA > hpB ? a : b;
        if (verbose) System.out.println("--- Combat end. Winner: " + winner.getName() + " ---");
        return winner;
    }

    // simulate and return a textual log of the combat
    public static String simulateWithLog(Character a, Character b) {
        StringBuilder log = new StringBuilder();
        int hpA = Math.max(10, a.getStrength() * 10 + a.getIntelligence() * 2);
        int hpB = Math.max(10, b.getStrength() * 10 + b.getIntelligence() * 2);
        int turn = 0;
        log.append("--- Combat start: ").append(a.getName()).append(" vs ").append(b.getName()).append(" ---\n");
        while (hpA > 0 && hpB > 0) {
            if (turn % 2 == 0) {
                int dmg = a.getStrength() + RNG.nextInt(Math.max(1, a.getAgility() + 1));
                hpB -= dmg;
                log.append(String.format("%s hits %s for %d (hpB=%d)\n", a.getName(), b.getName(), dmg, Math.max(0, hpB)));
            } else {
                int dmg = b.getStrength() + RNG.nextInt(Math.max(1, b.getAgility() + 1));
                hpA -= dmg;
                log.append(String.format("%s hits %s for %d (hpA=%d)\n", b.getName(), a.getName(), dmg, Math.max(0, hpA)));
            }
            turn++;
            if (turn > 1000) break;
        }
        Character winner = hpA > hpB ? a : b;
        log.append("--- Combat end. Winner: ").append(winner.getName()).append(" ---\n");
        return log.toString();
    }
}
