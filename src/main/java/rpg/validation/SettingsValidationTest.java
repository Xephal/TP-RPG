package rpg.validation;

import java.util.ArrayList;
import java.util.List;

import rpg.composite.Army;
import rpg.composite.GroupComponent;
import rpg.core.Character;

/**
 * Validateur centralisé pour les changements de paramètres globaux (GameSettings).
 * Vérifie que les nouvelles limites n'invalident pas les données existantes.
 * 
 * Cette classe applique le principe DRY en éliminant les doublons de validation
 * entre l'UI et les validateurs métier.
 */
public class SettingsValidationTest {

    /**
     * Valide les personnages existants contre les nouvelles limites de stats.
     * Utilise directement le validateur StatsValidator pour éviter la duplication.
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
     * Valide si une armée respecte les nouvelles limites de groupes.
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
     * Valide si une partie respecte les nouvelles limites de personnages.
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
     * Valide globalement tous les paramètres existants contre les nouvelles limites.
     * Centralise la validation pour éliminer les doublons dans l'UI.
     * 
     * @param characters Liste de tous les personnages existants
     * @param armies Liste de toutes les armées existantes (avec leurs parties)
     * @param newMaxStatPoints Nouvelle limite de points de stats
     * @param newMaxCharactersPerGroup Nouvelle limite de personnages par groupe
     * @param newMaxGroupsPerArmy Nouvelle limite de groupes par armée
     * @return ValidationResult contenant toutes les violations
     */
    public static ValidationResult validateAllAgainstNewSettings(
            List<Character> characters,
            List<Army> armies,
            int newMaxStatPoints,
            int newMaxCharactersPerGroup,
            int newMaxGroupsPerArmy) {
        
        List<String> allViolations = new ArrayList<>();
        
        // Valider les personnages
        allViolations.addAll(validateCharactersAgainstNewStatLimit(characters, newMaxStatPoints));
        
        // Valider les armées et leurs parties
        for (Army army : armies) {
            List<GroupComponent> parties = army.getChildren();
            
            // Valider le nombre de parties dans l'armée
            ValidationResult armyResult = validateArmyAgainstNewLimits(
                    parties.size(),
                    newMaxGroupsPerArmy,
                    army.getName());
            
            if (!armyResult.isValid()) {
                allViolations.addAll(armyResult.getErrors());
            }
            
            // Valider chaque partie
            for (GroupComponent party : parties) {
                int characterCount = party.getChildren().size();
                ValidationResult partyResult = validatePartyAgainstNewLimits(
                        characterCount,
                        newMaxCharactersPerGroup,
                        party.getName());
                
                if (!partyResult.isValid()) {
                    allViolations.addAll(partyResult.getErrors());
                }
            }
        }
        
        if (allViolations.isEmpty()) {
            return ValidationResult.success();
        }
        
        ValidationResult result = ValidationResult.failure("");
        for (String violation : allViolations) {
            result.addError(violation);
        }
        return result;
    }

    /**
     * Test complet de validation des paramètres.
     * Cette méthode est conservée pour des tests manuels si besoin.
     */
    public static void runValidationTests() {
        System.out.println("=== Settings Validation Tests ===");

        // Test des validateurs
        ArmyValidator armyValidator = new ArmyValidator(3); // Armée avec 3 parties
        PartyValidator partyValidator = new PartyValidator(8); // Partie avec 8 personnages

        // Test: réduire le max de parties par armée
        ValidationResult armyResult = armyValidator.validateArmySettings(3);
        if (armyResult.isValid()) {
            System.out.println("✓ Army validation: OK");
        } else {
            System.out.println("✗ Army validation: " + armyResult.getAllErrorsMessage());
        }

        // Test: réduire le max de personnages par partie
        ValidationResult partyResult = partyValidator.validatePartySettings(8);
        if (partyResult.isValid()) {
            System.out.println("✓ Party validation: OK");
        } else {
            System.out.println("✗ Party validation: " + partyResult.getAllErrorsMessage());
        }

        System.out.println("=== Tests completed ===");
    }
}