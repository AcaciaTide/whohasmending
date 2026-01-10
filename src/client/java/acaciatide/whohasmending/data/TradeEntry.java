package acaciatide.whohasmending.data;

/**
 * 個別の取引エントリを表すデータクラス
 */
public class TradeEntry {
    private transient String itemName; // JSON保存対象外
    private String enchantmentName;
    private int enchantmentLevel;
    private int emeraldCost;

    public TradeEntry() {
    }

    public TradeEntry(String itemName, String enchantmentName, int enchantmentLevel, int emeraldCost) {
        this.itemName = itemName;
        this.enchantmentName = enchantmentName;
        this.enchantmentLevel = enchantmentLevel;
        this.emeraldCost = emeraldCost;
    }

    // Getters
    public String getItemName() {
        return itemName;
    }

    public String getEnchantmentName() {
        return enchantmentName;
    }

    public int getEnchantmentLevel() {
        return enchantmentLevel;
    }

    public int getEmeraldCost() {
        return emeraldCost;
    }



    // Setters
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void setEnchantmentName(String enchantmentName) {
        this.enchantmentName = enchantmentName;
    }

    public void setEnchantmentLevel(int enchantmentLevel) {
        this.enchantmentLevel = enchantmentLevel;
    }

    public void setEmeraldCost(int emeraldCost) {
        this.emeraldCost = emeraldCost;
    }



    /**
     * 表示用テキストを生成
     * 例: "修繕 [10e]" または "ダメージ増加V [64e]"
     */
    public String getDisplayText() {
        StringBuilder sb = new StringBuilder();
        
        // [コスト] を先頭に
        sb.append("[").append(emeraldCost).append("]");
        
        if (enchantmentName != null && !enchantmentName.isEmpty()) {
            sb.append(enchantmentName);
            if (enchantmentLevel > 1) {
                // ローマ数字の前にスペースを入れる "Sharpness V"
                sb.append(" ").append(toRoman(enchantmentLevel));
            }
        } else {
            sb.append(itemName);
        }
        
        return sb.toString();
    }

    /**
     * 数値をローマ数字に変換
     */
    private String toRoman(int num) {
        if (num <= 0 || num > 10) {
            return String.valueOf(num);
        }
        
        String[] romans = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return romans[num];
    }

    /**
     * エンチャント本かどうかを判定
     */
    public boolean isEnchantedBook() {
        return enchantmentName != null && !enchantmentName.isEmpty();
    }
}
