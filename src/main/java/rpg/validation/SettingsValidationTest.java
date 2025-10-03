package rpg.validation;

import rpg.core.Character;
import rpg.settings.GameSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe utilitaire pour tester et valider les paramètres globaux
 */
public class SettingsValidationTest {

    /**
     * Valide les personnages existants contre les nouvelles limites de stats
     */
    public static List<String> validateCharactersAgainstNewStatLimit(
            List<Character> characters,
            int newMaxStatPoints) {

        List<String> violations = new ArrayList<>();

        for (Character c : characters) {
            int totalStats = c.getStrength() + c.getAgility() + c.getIntelligence();
            if (totalStats > newMaxStatPoints) {
                violations.add("Character '" + c.getName() + "' has " + totalStats +
                        " total stats but new limit is " + newMaxStatPoints);
            }
        }

        return violations;
    }

    /**
     * Valide si une armée respecte les nouvelles limites
     */
    public static ValidationResult validateArmyAgainstNewLimits(
            int partyCount,
            int newMaxGroupsPerArmy,
            String armyName) {

        if (partyCount > newMaxGroupsPerArmy) {
            return ValidationResult.failure(
                    "Army '" + armyName + "' has " + partyCount +
                            " parties but new limit is " + newMaxGroupsPerArmy);
        }

        return ValidationResult.success();
    }

    /**
     * Valide si une partie respecte les nouvelles limites
     */
    public static ValidationResult validatePartyAgainstNewLimits(
            int characterCount,
            int newMaxCharactersPerGroup,
            String partyName) {

        if (characterCount > newMaxCharactersPerGroup) {
            return ValidationResult.failure(
                    "Party '" + partyName + "' has " + characterCount +
                            " characters but new limit is " + newMaxCharactersPerGroup);
        }

        return ValidationResult.success();
    }

    /**
     * Test complet de validation des paramètres
     */
    public static void runValidationTests() {
        System.out.println("=== Settings Validation Tests ===");

        // Test des validateurs
        ArmyValidator armyValidator = new ArmyValidator(3); // Armée avec 3 parties
        PartyValidator partyValidator = new PartyValidator(8); // Partie avec 8 personnages

        // Simuler des changements de paramètres
        GameSettings settings = GameSettings.getInstance();

        // Test: réduire le max de parties par armée
        ValidationResult armyResult = armyValidator.validateArmySettings(3);
        if (armyResult.isValid()) {
            System.out.println("✓ Army validation: OK");
        } else {
            System.out.println("✗ Army validation: " + armyResult.getMessage());
        }

        // Test: réduire le max de personnages par partie
        ValidationResult partyResult = partyValidator.validatePartySettings(8);
        if (partyResult.isValid()) {
            System.out.println("✓ Party validation: OK");
        } else {
            System.out.println("✗ Party validation: " + partyResult.getMessage());
        }

        System.out.println("=== Tests completed ===");
    }
}