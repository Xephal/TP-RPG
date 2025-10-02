package rpg.validation;

import rpg.settings.GameSettings;

public class PartyValidator extends BaseValidator {
    @Override
    protected ValidationResult validateSpecific(ValidationContext context) {
        // Ce validateur peut être utilisé pour valider l'ajout d'un personnage à une
        // partie
        // Pour l'instant, il s'assure juste que les règles de base sont respectées

        // La validation spécifique aux parties est maintenant gérée directement dans
        // SwingView
        // lors de l'ajout de personnages aux parties

        return ValidationResult.success();
    }
}