package rpg.core;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;

public class Combat {
    private static final Random RNG = new Random();

    public static String simulateWithLog(Character a, Character b) {
        Map<Character, Integer> hp = new IdentityHashMap<>();
        hp.put(a, Math.max(10, a.getStrength() * 10 + a.getIntelligence() * 2));
        hp.put(b, Math.max(10, b.getStrength() * 10 + b.getIntelligence() * 2));

        StringBuilder log = new StringBuilder();
        log.append("--- Combat start: ").append(a.getName()).append(" vs ").append(b.getName()).append(" ---\n");
        log.append(String.format("%s HP=%d | %s HP=%d%n", a.getName(), hp.get(a), b.getName(), hp.get(b)));

        int turn = 0;

        while (hp.get(a) > 0 && hp.get(b) > 0) {
            if (turn % 2 == 0) {
                a.beginTurn();
                b.beginTurn();

                int healA = a.healThisTurn();
                if (healA > 0) {
                    hp.put(a, hp.get(a) + healA);
                    log.append(String.format("%s heals %d -> HP=%d%n", a.getName(), healA, hp.get(a)));
                }
                int healB = b.healThisTurn();
                if (healB > 0) {
                    hp.put(b, hp.get(b) + healB);
                    log.append(String.format("%s heals %d -> HP=%d%n", b.getName(), healB, hp.get(b)));
                }

                log.append("-- New round --\n");
            }

            Character atk = (turn % 2 == 0) ? a : b;
            Character def = (turn % 2 == 0) ? b : a;

            int dmg = Math.max(0, atk.attackDamage());
            dmg += RNG.nextInt(Math.max(1, atk.getAgility() + 1)); // petit bonus RNG comme avant

            int applied = Math.max(0, def.onReceiveDamage(dmg));

            int before = hp.get(def);
            int after = Math.max(0, before - applied);
            hp.put(def, after);

            log.append(String.format(
                    "%s hits %s for %d (applied %d) | %s HP=%d%n",
                    atk.getName(), def.getName(), dmg, applied, def.getName(), hp.get(def)
            ));

            turn++;
            if (turn > 1000) break;
        }

        Character winner = hp.get(a) > hp.get(b) ? a : b;
        log.append("--- Combat end. Winner: ").append(winner.getName()).append(" ---\n");
        return log.toString();
    }

    public static Character simulate(Character a, Character b, boolean verbose) {
        Map<Character, Integer> hp = new IdentityHashMap<>();
        hp.put(a, Math.max(10, a.getStrength() * 10 + a.getIntelligence() * 2));
        hp.put(b, Math.max(10, b.getStrength() * 10 + b.getIntelligence() * 2));

        int turn = 0;
        if (verbose) {
            System.out.println("--- Combat start: " + a.getName() + " vs " + b.getName() + " ---");
            System.out.printf("%s HP=%d | %s HP=%d%n", a.getName(), hp.get(a), b.getName(), hp.get(b));
        }

        while (hp.get(a) > 0 && hp.get(b) > 0) {
            if (turn % 2 == 0) {
                // début de round: tick des effets
                a.beginTurn();
                b.beginTurn();

                // appliquer les soins éventuels
                int healA = a.healThisTurn();
                if (healA > 0) hp.put(a, hp.get(a) + healA);
                int healB = b.healThisTurn();
                if (healB > 0) hp.put(b, hp.get(b) + healB);

                if (verbose && (healA > 0 || healB > 0)) {
                    if (healA > 0) System.out.printf("%s heals %d -> HP=%d%n", a.getName(), healA, hp.get(a));
                    if (healB > 0) System.out.printf("%s heals %d -> HP=%d%n", b.getName(), healB, hp.get(b));
                }
                if (verbose) System.out.println("-- New round --");
            }

            Character atk = (turn % 2 == 0) ? a : b;
            Character def = (turn % 2 == 0) ? b : a;

            int dmg = Math.max(0, atk.attackDamage());
            dmg += RNG.nextInt(Math.max(1, atk.getAgility() + 1)); // petit sel RNG comme avant

            int applied = Math.max(0, def.onReceiveDamage(dmg));

            int before = hp.get(def);
            int after = Math.max(0, before - applied);
            hp.put(def, after);

            if (verbose) {
                System.out.printf("%s hits %s for %d (applied %d) | %s HP=%d%n",
                        atk.getName(), def.getName(), dmg, applied, def.getName(), hp.get(def));
            }

            turn++;
            if (turn > 1000) break;
        }

        Character winner = hp.get(a) > hp.get(b) ? a : b;
        if (verbose) System.out.println("--- Combat end. Winner: " + winner.getName() + " ---");
        return winner;
    }
}
