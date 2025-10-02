package rpg.validation;

import rpg.settings.GameSettings;

public class ArmyValidator extends BaseValidator {
    @Override
    protected ValidationResult validateSpecific(ValidationContext context) {
        // Ce validateur peut être utilisé pour valider l'ajout d'une partie à une armée
        // Pour l'instant, il s'assure juste que les règles de base sont respectées

        // La validation spécifique aux armées est maintenant gérée directement dans
        // SwingView
        // lors de l'ajout de parties aux armées

        return ValidationResult.success();
    }
}