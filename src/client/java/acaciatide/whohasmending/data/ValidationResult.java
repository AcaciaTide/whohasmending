package acaciatide.whohasmending.data;

/**
 * データバリデーションの結果を保持するクラス
 */
public class ValidationResult {
    private final boolean valid;
    private final int totalRecords;
    private final int validRecords;
    private final int invalidRecords;
    private final String message;

    public ValidationResult(boolean valid, int totalRecords, int validRecords, int invalidRecords, String message) {
        this.valid = valid;
        this.totalRecords = totalRecords;
        this.validRecords = validRecords;
        this.invalidRecords = invalidRecords;
        this.message = message;
    }

    public boolean isValid() {
        return valid;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public int getValidRecords() {
        return validRecords;
    }

    public int getInvalidRecords() {
        return invalidRecords;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 成功結果を作成
     */
    public static ValidationResult success(int totalRecords) {
        return new ValidationResult(true, totalRecords, totalRecords, 0,
            "§a[WhoHasMending] All " + totalRecords + " records are valid.");
    }

    /**
     * 一部無効な結果を作成
     */
    public static ValidationResult partial(int totalRecords, int validRecords, int invalidRecords) {
        return new ValidationResult(false, totalRecords, validRecords, invalidRecords,
            "§e[WhoHasMending] Found " + invalidRecords + " invalid entries out of " + totalRecords + " records.");
    }

    /**
     * データなしの結果を作成
     */
    public static ValidationResult empty() {
        return new ValidationResult(true, 0, 0, 0,
            "§7[WhoHasMending] No data to validate.");
    }
}
