package acaciatide.whohasmending.render;

import acaciatide.whohasmending.Whohasmending;

/**
 * 村人の頭上にカスタムネームタグを描画するレンダラー
 * 実際の描画はVillagerEntityRendererMixinで行う
 */
public class VillagerNametagRenderer {
    
    /**
     * レンダリングイベントを登録（現在はMixinベースなので空）
     */
    public static void register() {
        Whohasmending.LOGGER.info("VillagerNametagRenderer initialized (using Mixin-based rendering)");
    }
}
