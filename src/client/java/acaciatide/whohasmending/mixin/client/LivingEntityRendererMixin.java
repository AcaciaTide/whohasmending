package acaciatide.whohasmending.mixin.client;

import acaciatide.whohasmending.Whohasmending;
import acaciatide.whohasmending.data.VillagerDataManager;
import acaciatide.whohasmending.data.VillagerTradeData;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.EntityAttachmentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
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

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void onUpdateRenderState(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (!(entity instanceof VillagerEntity villager)) {
            return;
        }

        if (!VillagerDataManager.getInstance().isDisplayEnabled()) {
            return;
        }

        VillagerTradeData data = VillagerDataManager.getInstance().getVillagerData(villager.getUuid());
        if (data != null && data.getDisplayName() != null && !data.getDisplayName().isEmpty()) {
            Text tradeText = Text.of(data.getDisplayName());
            
            // 既存の名前があれば "既存の名前 トレード内容" にする
            if (state.displayName != null) {
                state.displayName = Text.empty().append(state.displayName).append(" ").append(tradeText);
            } else {
                state.displayName = tradeText;
            }
            
            // 重要: 名前表示位置(nameLabelPos)がnullの場合、計算して設定する
            // 村人はデフォルトで名前表示がないため、バニラの処理で計算されない可能性がある
            if (state.nameLabelPos == null) {
                state.nameLabelPos = villager.getAttachments().getPointNullable(EntityAttachmentType.NAME_TAG, 0, villager.getYaw());
            }


        }
    }
}
