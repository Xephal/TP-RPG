package rpg.validation;

/**
 * Classe de base abstraite pour les validateurs.
 * Impl\u00e9mente le pattern Chain of Responsibility avec accumulation des erreurs.
 */
public abstract class BaseValidator implements Validator {
    private Validator next;

    @Override
    public void setNext(Validator next) {
        this.next = next;
    }

    @Override
    public ValidationResult validate(ValidationContext context) {
        ValidationResult result = validateSpecific(context);
        
        if (next != null) {
            ValidationResult nextResult = next.validate(context);
            // Combine les r\u00e9sultats pour accumuler les erreurs
            return result.combine(nextResult);
        }
        
        return result;
    }

    protected abstract ValidationResult validateSpecific(ValidationContext context);
}