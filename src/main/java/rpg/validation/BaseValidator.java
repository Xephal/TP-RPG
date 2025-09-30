package rpg.validation;

public abstract class BaseValidator implements Validator {
    private Validator next;

    @Override
    public void setNext(Validator next) {
        this.next = next;
    }

    @Override
    public ValidationResult validate(ValidationContext context) {
        ValidationResult result = validateSpecific(context);
        if (!result.isValid()) {
            return result;
        }
        if (next != null) {
            return next.validate(context);
        }
        return ValidationResult.success();
    }

    protected abstract ValidationResult validateSpecific(ValidationContext context);
}