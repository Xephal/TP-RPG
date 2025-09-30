package rpg.validation;

public interface Validator {
    ValidationResult validate(ValidationContext context);
    void setNext(Validator next);
}