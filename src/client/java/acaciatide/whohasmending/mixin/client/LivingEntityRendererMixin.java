package acaciatide.whohasmending.mixin.client;

import acaciatide.whohasmending.data.VillagerDataManager;
import acaciatide.whohasmending.data.VillagerTradeData;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LivingEntityRendererにフックして取引情報をネームタグとして設定するMixin
 * VillagerEntityRendererではなく親クラスをフックすることで確実に実行させる
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void onUpdateRenderState(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (!(entity instanceof Villager villager)) {
            return;
        }

        if (!VillagerDataManager.getInstance().isDisplayEnabled()) {
            return;
        }

        VillagerTradeData data = VillagerDataManager.getInstance().getVillagerData(villager.getUUID());
        if (data != null && data.getDisplayName() != null && !data.getDisplayName().isEmpty()) {
            Component tradeText = Component.nullToEmpty(data.getDisplayName());
            
            // 既存の名前があれば "既存の名前 トレード内容" にする
            if (state.nameTag != null) {
                state.nameTag = Component.empty().append(state.nameTag).append(" ").append(tradeText);
            } else {
                state.nameTag = tradeText;
            }
            
            // 重要: 名前表示位置(nameLabelPos)がnullの場合、計算して設定する
            // 村人はデフォルトで名前表示がないため、バニラの処理で計算されない可能性がある
            if (state.nameTagAttachment == null) {
                state.nameTagAttachment = villager.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, villager.getYRot());
            }
        }
    }
}
