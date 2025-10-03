package rpg.validation;

import rpg.settings.GameSettings;

public class ArmyValidator extends BaseValidator {
    private final int currentPartyCount;

    public ArmyValidator() {
        this.currentPartyCount = 0;
    }

    public ArmyValidator(int currentPartyCount) {
        this.currentPartyCount = currentPartyCount;
    }

    @Override
    protected ValidationResult validateSpecific(ValidationContext context) {
        GameSettings settings = GameSettings.getInstance();
        int maxGroupsPerArmy = settings.getMaxGroupsPerArmy();

        if (currentPartyCount >= maxGroupsPerArmy) {
            return ValidationResult.failure(
                    "Cannot add party: Maximum parties per army is " + maxGroupsPerArmy +
                            ". This army already has " + currentPartyCount + " parties.");
        }

        return ValidationResult.success();
    }

    public ValidationResult validateArmySettings(int actualPartyCount) {
        GameSettings settings = GameSettings.getInstance();
        int maxGroupsPerArmy = settings.getMaxGroupsPerArmy();

        if (actualPartyCount > maxGroupsPerArmy) {
            return ValidationResult.failure(
                    "Army validation failed: Has " + actualPartyCount +
                            " parties but maximum allowed is " + maxGroupsPerArmy);
        }

        return ValidationResult.success();
    }
}