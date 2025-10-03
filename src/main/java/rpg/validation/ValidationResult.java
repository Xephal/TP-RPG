package rpg.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Représente le résultat d'une validation avec accumulation d'erreurs multiples.
 * Permet de collecter toutes les erreurs de validation au lieu de s'arrêter à la première.
 */
public class ValidationResult {
    private final boolean valid;
    private final List<String> errors;

    /**
     * Constructeur privé pour contrôler la création via les factory methods.
     */
    private ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = new ArrayList<>(errors);
    }

    /**
     * @return true si la validation a réussi (aucune erreur), false sinon
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * @return le message d'erreur unique (première erreur) pour compatibilité
     * @deprecated Utiliser getErrors() pour obtenir toutes les erreurs
     */
    @Deprecated
    public String getMessage() {
        if (errors.isEmpty()) {
            return "";
        }
        return errors.get(0);
    }

    /**
     * @return une liste immuable de toutes les erreurs de validation
     */
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * @return un message formaté contenant toutes les erreurs, séparées par des retours à la ligne
     */
    public String getAllErrorsMessage() {
        if (errors.isEmpty()) {
            return "";
        }
        if (errors.size() == 1) {
            return errors.get(0);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Multiple validation errors:\n");
        for (int i = 0; i < errors.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(errors.get(i));
            if (i < errors.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * @return le nombre d'erreurs accumulées
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * Crée un résultat de validation réussi (sans erreur).
     */
    public static ValidationResult success() {
        return new ValidationResult(true, Collections.emptyList());
    }

    /**
     * Crée un résultat de validation échoué avec une seule erreur.
     */
    public static ValidationResult failure(String message) {
        List<String> errors = new ArrayList<>();
        errors.add(message);
        return new ValidationResult(false, errors);
    }

    /**
     * Crée un résultat de validation échoué avec plusieurs erreurs.
     */
    public static ValidationResult failure(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            throw new IllegalArgumentException("Cannot create failure result without errors");
        }
        return new ValidationResult(false, errors);
    }

    /**
     * Combine ce résultat avec un autre résultat.
     * Si l'un des deux est invalide, le résultat combiné sera invalide.
     * Les erreurs sont accumulées.
     */
    public ValidationResult combine(ValidationResult other) {
        if (this.valid && other.valid) {
            return success();
        }
        List<String> combinedErrors = new ArrayList<>();
        combinedErrors.addAll(this.errors);
        combinedErrors.addAll(other.errors);
        return new ValidationResult(false, combinedErrors);
    }

    /**
     * Ajoute une erreur à ce résultat.
     * Retourne un nouveau ValidationResult avec l'erreur ajoutée.
     */
    public ValidationResult addError(String error) {
        List<String> newErrors = new ArrayList<>(this.errors);
        newErrors.add(error);
        return new ValidationResult(false, newErrors);
    }
}