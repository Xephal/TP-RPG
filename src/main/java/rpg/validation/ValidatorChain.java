package rpg.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Chaîne de validateurs impl\u00e9mentant le pattern Chain of Responsibility.
 * Offre une API fluide pour construire la chaîne: ValidatorChain.start().add(...).add(...)
 */
public class ValidatorChain {
    private final List<Validator> validators;
    private Validator firstValidator;

    /**
     * Constructeur priv\u00e9 interne.
     */
    private ValidatorChain(boolean useDefaultValidators) {
        this.validators = new ArrayList<>();
        
        // Si pas fluent API, cr\u00e9er la cha\u00eene par d\u00e9faut
        if (useDefaultValidators) {
            NameValidator nameValidator = new NameValidator();
            StatsValidator statsValidator = new StatsValidator();
            
            nameValidator.setNext(statsValidator);
            this.firstValidator = nameValidator;
            this.validators.add(nameValidator);
            this.validators.add(statsValidator);
        }
    }

    /**
     * Constructeur par d\u00e9faut pour compatibilit\u00e9 ascendante.
     * Cr\u00e9e une cha\u00eene avec NameValidator -> StatsValidator.
     */
    public ValidatorChain() {
        this(true);
    }

    /**
     * D\u00e9marre une nouvelle cha\u00eene de validateurs (API fluide).
     * @return une nouvelle instance de ValidatorChain vide
     */
    public static ValidatorChain start() {
        return new ValidatorChain(false);
    }

    /**
     * Ajoute un validateur \u00e0 la cha\u00eene (API fluide).
     * @param validator le validateur \u00e0 ajouter
     * @return cette instance pour permettre le cha\u00eenage
     */
    public ValidatorChain add(Validator validator) {
        if (validator == null) {
            throw new IllegalArgumentException("Validator cannot be null");
        }
        validators.add(validator);
        rebuildChain();
        return this;
    }

    /**
     * Reconstruit la cha\u00eene en reliant tous les validateurs.
     */
    private void rebuildChain() {
        if (validators.isEmpty()) {
            firstValidator = null;
            return;
        }
        
        firstValidator = validators.get(0);
        for (int i = 0; i < validators.size() - 1; i++) {
            validators.get(i).setNext(validators.get(i + 1));
        }
        // Le dernier validateur n'a pas de next
        validators.get(validators.size() - 1).setNext(null);
    }

    /**
     * Ex\u00e9cute la validation en parcourant tous les validateurs de la cha\u00eene.
     * @param context le contexte de validation
     * @return le r\u00e9sultat de validation (accumule toutes les erreurs)
     */
    public ValidationResult validate(ValidationContext context) {
        if (firstValidator == null) {
            return ValidationResult.success();
        }
        return firstValidator.validate(context);
    }

    /**
     * @return le nombre de validateurs dans la cha\u00eene
     */
    public int size() {
        return validators.size();
    }
}