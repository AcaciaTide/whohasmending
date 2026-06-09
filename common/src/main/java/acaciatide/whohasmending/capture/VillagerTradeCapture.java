package acaciatide.whohasmending.capture;

import acaciatide.whohasmending.Whohasmending;
import acaciatide.whohasmending.data.TradeEntry;
import acaciatide.whohasmending.data.VillagerDataManager;
import acaciatide.whohasmending.data.VillagerTradeData;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * 取引画面から村人の取引情報を抽出するクラス
 */
public class VillagerTradeCapture {

    /**
     * TradeOfferListから取引情報をキャプチャして保存
     * @param offers 取引オファーリスト
     * @param villagerUuid 村人のUUID
     * @param profession 村人の職業名
     */
    public static void captureFromOffers(MerchantOffers offers, UUID villagerUuid, String profession) {
        if (offers == null || villagerUuid == null) {
            Whohasmending.LOGGER.warn("Cannot capture: offers or UUID is null");
            return;
        }

        if (offers.isEmpty()) {
            Whohasmending.LOGGER.info("No trade offers available for villager: {}", villagerUuid);
            return;
        }

        VillagerTradeData data = new VillagerTradeData(villagerUuid, profession);
        
        for (MerchantOffer offer : offers) {
            TradeEntry entry = extractTradeEntry(offer);
            if (entry != null) {
                data.addTrade(entry);
                break; // 最初の1つ（一番上のエンチャント本）を見つけたら終了
            }
        }

        data.updateDisplayName();
        
        VillagerDataManager.getInstance().putVillagerData(villagerUuid, data);
        
        Whohasmending.LOGGER.info("Captured trade for villager {} ({}): display='{}'", 
                villagerUuid, profession, data.getDisplayName());
    }

    /**
     * TradeOfferからTradeEntryを抽出
     */
    private static TradeEntry extractTradeEntry(MerchantOffer offer) {
        ItemStack sellItem = offer.getResult();

        // エンチャント本以外は無視（記録しない）
        if (!sellItem.is(Items.ENCHANTED_BOOK)) {
            return null;
        }

        ItemStack buyItem = offer.getCostA();
        
        String itemName = sellItem.getHoverName().getString();
        String enchantmentName = null;
        int enchantmentLevel = 0;
        int emeraldCost = buyItem.getCount();
        // エンチャント情報を抽出
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(sellItem);
        
        for (var entry : enchantments.entrySet()) {
            var enchantment = entry.getKey();
            int level = entry.getIntValue();
            
            // エンチャント名を取得
            enchantmentName = extractEnchantmentBaseName(enchantment);
            enchantmentLevel = level;
            
            // 最初のエンチャントのみ使用（通常は1つだけ）
            break;
        }

        return new TradeEntry(itemName, enchantmentName, enchantmentLevel, emeraldCost);
    }

    /**
     * エンチャントの基本名を抽出（レベル表記なし）
     */
    private static String extractEnchantmentBaseName(Holder<Enchantment> enchantment) {
        // 現在の言語設定に従って翻訳された名前を取得
        return enchantment.value().description().getString();
    }
}
