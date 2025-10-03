package rpg.validation;

import rpg.settings.GameSettings;

public class PartyValidator extends BaseValidator {
    private final int currentCharacterCount;

    public PartyValidator() {
        this.currentCharacterCount = 0;
    }

    public PartyValidator(int currentCharacterCount) {
        this.currentCharacterCount = currentCharacterCount;
    }

    @Override
    protected ValidationResult validateSpecific(ValidationContext context) {
        GameSettings settings = GameSettings.getInstance();
        int maxCharactersPerGroup = settings.getMaxCharactersPerGroup();

        if (currentCharacterCount >= maxCharactersPerGroup) {
            return ValidationResult.failure(
                    "Cannot add character: Maximum characters per party is " + maxCharactersPerGroup +
                            ". This party already has " + currentCharacterCount + " characters.");
        }

        return ValidationResult.success();
    }

    public ValidationResult validatePartySettings(int actualCharacterCount) {
        GameSettings settings = GameSettings.getInstance();
        int maxCharactersPerGroup = settings.getMaxCharactersPerGroup();

        if (actualCharacterCount > maxCharactersPerGroup) {
            return ValidationResult.failure(
                    "Party validation failed: Has " + actualCharacterCount +
                            " characters but maximum allowed is " + maxCharactersPerGroup);
        }

        return ValidationResult.success();
    }
}