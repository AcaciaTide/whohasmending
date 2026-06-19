package acaciatide.whohasmending.mixin.client;

import acaciatide.whohasmending.Whohasmending;
import acaciatide.whohasmending.data.VillagerDataManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Screenクラスにフックして画面が閉じられた時のイベントを監視するMixin
 */
@Mixin(Screen.class)
public abstract class ScreenMixin {

    /**
     * 画面が閉じられたときに実行（取引画面が閉じられたらデータをセーブ）
     */
    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        // 対象の画面がMerchantScreenである場合のみデータを保存する
        if ((Object) this instanceof MerchantScreen) {
            try {
                VillagerDataManager.getInstance().saveCurrentWorld();
                Whohasmending.LOGGER.info("MerchantScreen closed (detected via ScreenMixin) - Saved data");
            } catch (Exception e) {
                Whohasmending.LOGGER.error("Error saving data on MerchantScreen close", e);
            }
        }
    }
}
