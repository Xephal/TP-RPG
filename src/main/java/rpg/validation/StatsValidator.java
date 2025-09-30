package rpg.validation;

import rpg.settings.GameSettings;

public class StatsValidator extends BaseValidator {
    @Override
    protected ValidationResult validateSpecific(ValidationContext context) {
        int strength = context.getStrength();
        int agility = context.getAgility();
        int intelligence = context.getIntelligence();

        if (strength < 0 || agility < 0 || intelligence < 0) {
            return ValidationResult.failure("Stats cannot be negative");
        }

        int total = context.getTotalStats();
        int maxStats = GameSettings.getInstance().getMaxStatPoints();
        if (total > maxStats) {
            return ValidationResult.failure("Total stats " + total + " exceeds maximum " + maxStats);
        }

        return ValidationResult.success();
    }
}