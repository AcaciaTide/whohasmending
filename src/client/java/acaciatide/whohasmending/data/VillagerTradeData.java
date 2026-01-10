package acaciatide.whohasmending.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 村人1体分の取引情報を保持するデータクラス
 */
public class VillagerTradeData {
    private UUID villagerUuid;
    private String displayName;
    private transient String profession; // JSON保存対象外
    private transient long lastUpdated;  // JSON保存対象外
    private List<TradeEntry> trades;

    public VillagerTradeData() {
        this.trades = new ArrayList<>();
    }

    public VillagerTradeData(UUID villagerUuid, String profession) {
        this.villagerUuid = villagerUuid;
        this.profession = profession;
        this.lastUpdated = System.currentTimeMillis();
        this.trades = new ArrayList<>();
    }

    // Getters
    public UUID getVillagerUuid() {
        return villagerUuid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getProfession() {
        return profession;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public List<TradeEntry> getTrades() {
        return trades;
    }

    // Setters
    public void setVillagerUuid(UUID villagerUuid) {
        this.villagerUuid = villagerUuid;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setTrades(List<TradeEntry> trades) {
        this.trades = trades;
    }

    /**
     * 取引リストを更新し、最も優先度の高いアイテムを表示名に設定
     */
    public void updateDisplayName() {
        if (trades.isEmpty()) {
            this.displayName = null; // 取引がない（エンチャント本がない）場合は何も表示しない
            return;
        }

        // 優先度が最も高い（数値が小さい）取引を探す
        TradeEntry bestTrade = trades.stream()
                .min((a, b) -> Integer.compare(a.getPriority(), b.getPriority()))
                .orElse(null);

        if (bestTrade != null) {
            this.displayName = bestTrade.getDisplayText();
        } else {
            this.displayName = profession;
        }
        
        this.lastUpdated = System.currentTimeMillis();
    }

    /**
     * 取引エントリを追加
     */
    public void addTrade(TradeEntry trade) {
        this.trades.add(trade);
    }

    /**
     * 取引リストをクリアして再構築用に準備
     */
    public void clearTrades() {
        this.trades.clear();
    }
}
