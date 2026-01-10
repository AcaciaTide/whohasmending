package acaciatide.whohasmending.capture;

import acaciatide.whohasmending.Whohasmending;
import acaciatide.whohasmending.data.TradeEntry;
import acaciatide.whohasmending.data.VillagerDataManager;
import acaciatide.whohasmending.data.VillagerTradeData;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

import java.util.UUID;

/**
 * 取引画面から村人の取引情報を抽出するクラス
 */
public class VillagerTradeCapture {

    /**
     * Merchantから取引情報をキャプチャして保存
     * @param merchant 村人（Merchantインターフェース）
     * @param villagerUuid 村人のUUID
     * @param profession 村人の職業名
     */
    public static void captureFromMerchant(Merchant merchant, UUID villagerUuid, String profession) {
        if (merchant == null || villagerUuid == null) {
            Whohasmending.LOGGER.warn("Cannot capture: merchant or UUID is null");
            return;
        }

        TradeOfferList offers = merchant.getOffers();
        if (offers == null || offers.isEmpty()) {
            Whohasmending.LOGGER.info("No trade offers available for villager: {}", villagerUuid);
            return;
        }

        VillagerTradeData data = new VillagerTradeData(villagerUuid, profession);
        
        for (TradeOffer offer : offers) {
            TradeEntry entry = extractTradeEntry(offer);
            if (entry != null) {
                data.addTrade(entry);
            }
        }

        data.updateDisplayName();
        
        VillagerDataManager.getInstance().putVillagerData(villagerUuid, data);
        
        Whohasmending.LOGGER.info("Captured {} trades for villager {} ({}): display='{}'", 
                data.getTrades().size(), villagerUuid, profession, data.getDisplayName());
    }

    /**
     * TradeOfferListから取引情報をキャプチャして保存
     * クライアント側で安全に使用可能
     * @param offers 取引オファーリスト
     * @param villagerUuid 村人のUUID
     * @param profession 村人の職業名
     */
    public static void captureFromOffers(TradeOfferList offers, UUID villagerUuid, String profession) {
        if (offers == null || villagerUuid == null) {
            Whohasmending.LOGGER.warn("Cannot capture: offers or UUID is null");
            return;
        }

        if (offers.isEmpty()) {
            Whohasmending.LOGGER.info("No trade offers available for villager: {}", villagerUuid);
            return;
        }

        VillagerTradeData data = new VillagerTradeData(villagerUuid, profession);
        
        for (TradeOffer offer : offers) {
            TradeEntry entry = extractTradeEntry(offer);
            if (entry != null) {
                data.addTrade(entry);
            }
        }

        data.updateDisplayName();
        
        VillagerDataManager.getInstance().putVillagerData(villagerUuid, data);
        
        Whohasmending.LOGGER.info("Captured {} trades for villager {} ({}): display='{}'", 
                data.getTrades().size(), villagerUuid, profession, data.getDisplayName());
    }

    /**
     * TradeOfferからTradeEntryを抽出
     */
    private static TradeEntry extractTradeEntry(TradeOffer offer) {
        ItemStack sellItem = offer.getSellItem();

        // エンチャント本以外は無視（記録しない）
        if (!sellItem.isOf(Items.ENCHANTED_BOOK)) {
            return null;
        }

        ItemStack buyItem = offer.getDisplayedFirstBuyItem();
        
        String itemName = sellItem.getName().getString();
        String enchantmentName = null;
        int enchantmentLevel = 0;
        int emeraldCost = buyItem.getCount();
        int priority = 100; // デフォルト優先度（低い）

        // エンチャント本の場合、エンチャント情報を抽出
        if (sellItem.isOf(Items.ENCHANTED_BOOK)) {
            ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(sellItem);
            
            for (var entry : enchantments.getEnchantmentEntries()) {
                RegistryEntry<Enchantment> enchantment = entry.getKey();
                int level = entry.getIntValue();
                
                // エンチャント名を取得
                Text nameText = Enchantment.getName(enchantment, level);
                enchantmentName = extractEnchantmentBaseName(enchantment);
                enchantmentLevel = level;
                
                // 優先度を計算
                priority = calculatePriority(enchantment, level);
                
                // 最初のエンチャントのみ使用（通常は1つだけ）
                break;
            }
        }

        return new TradeEntry(itemName, enchantmentName, enchantmentLevel, emeraldCost, priority);
    }

    /**
     * エンチャントの基本名を抽出（レベル表記なし）
     */
    private static String extractEnchantmentBaseName(RegistryEntry<Enchantment> enchantment) {
        // 現在の言語設定に従って翻訳された名前を取得
        return enchantment.value().description().getString();
    }
    /**
     * エンチャントの優先度を計算
     * 1 = 最高優先度（修繕）
     * 100 = 最低優先度
     */
    private static int calculatePriority(RegistryEntry<Enchantment> enchantment, int level) {
        String key = enchantment.getKey()
                .map(k -> k.getValue().getPath())
                .orElse("");

        // Tier 1: 最重要（修繕）
        if ("mending".equals(key)) {
            return 1;
        }

        // Tier 2: 高優先度（高レベルエンチャント）
        if (isHighPriorityEnchantment(key, level)) {
            return 2;
        }

        // Tier 3: 中優先度（有用なエンチャント）
        if (isMediumPriorityEnchantment(key, level)) {
            return 3;
        }

        // Tier 4: その他
        return 4;
    }

    private static boolean isHighPriorityEnchantment(String key, int level) {
        return switch (key) {
            case "sharpness", "efficiency", "power" -> level >= 5;
            case "protection" -> level >= 4;
            case "unbreaking" -> level >= 3;
            default -> false;
        };
    }

    private static boolean isMediumPriorityEnchantment(String key, int level) {
        return switch (key) {
            case "silk_touch", "infinity" -> true;
            case "fortune", "looting" -> level >= 3;
            case "fire_aspect" -> level >= 2;
            default -> false;
        };
    }
}
