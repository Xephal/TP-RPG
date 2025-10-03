package rpg.builder;

import java.util.Collections;
import java.util.List;

/**
 * Exception levée lorsqu'un personnage ne peut pas être créé en raison d'erreurs de validation.
 * Supporte l'accumulation de plusieurs erreurs de validation.
 */
public class InvalidCharacterException extends RuntimeException {
    private final List<String> errors;

    /**
     * Constructeur avec un message d'erreur unique.
     * @param message le message d'erreur
     */
    public InvalidCharacterException(String message) {
        super(message);
        this.errors = Collections.singletonList(message);
    }

    /**
     * Constructeur avec plusieurs messages d'erreur.
     * @param errors la liste des erreurs de validation
     */
    public InvalidCharacterException(List<String> errors) {
        super(formatErrors(errors));
        this.errors = Collections.unmodifiableList(errors);
    }

    /**
     * @return la liste de toutes les erreurs de validation
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * @return le nombre d'erreurs de validation
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * Formate une liste d'erreurs en un seul message.
     */
    private static String formatErrors(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return "Invalid character";
        }
        if (errors.size() == 1) {
            return errors.get(0);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Multiple validation errors (").append(errors.size()).append("):").append("\n");
        for (int i = 0; i < errors.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(errors.get(i));
            if (i < errors.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
