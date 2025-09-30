package rpg.validation;

public class NameValidator extends BaseValidator {
    @Override
    protected ValidationResult validateSpecific(ValidationContext context) {
        String name = context.getName();
        if (name == null || name.trim().isEmpty()) {
            return ValidationResult.failure("Name cannot be empty");
        }
        if (name.length() > 50) {
            return ValidationResult.failure("Name too long (max 50 characters)");
        }
        return ValidationResult.success();
    }
}