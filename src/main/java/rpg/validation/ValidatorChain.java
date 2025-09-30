package rpg.validation;

public class ValidatorChain {
    private final Validator firstValidator;

    public ValidatorChain() {
        NameValidator nameValidator = new NameValidator();
        StatsValidator statsValidator = new StatsValidator();
        
        nameValidator.setNext(statsValidator);
        this.firstValidator = nameValidator;
    }

    public ValidationResult validate(ValidationContext context) {
        return firstValidator.validate(context);
    }
}