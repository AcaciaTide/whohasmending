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

import java.util.UUID;

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
            UUID uuid = villager.getUUID();
            VillagerDataManager.CachedTag cached = VillagerDataManager.getInstance().getCachedTag(uuid);
            Component baseTag = state.nameTag;
            String displayName = data.getDisplayName();

            // キャッシュが存在し、ベースの名前タグと取引テキストが一致しているか判定
            if (cached != null && 
                ((baseTag == null && cached.baseTag == null) || (baseTag != null && baseTag.equals(cached.baseTag))) && 
                displayName.equals(cached.displayName)) {
                state.nameTag = cached.resultTag;
            } else {
                // 一致しない場合は新しくComponentを生成してキャッシュに保存
                Component tradeText = Component.nullToEmpty(displayName);
                Component resultTag;
                if (baseTag != null) {
                    resultTag = Component.empty().append(baseTag).append(" ").append(tradeText);
                } else {
                    resultTag = tradeText;
                }
                VillagerDataManager.getInstance().putCachedTag(uuid, new VillagerDataManager.CachedTag(baseTag, displayName, resultTag));
                state.nameTag = resultTag;
            }
            
            // 重要: 名前表示位置(nameLabelPos)がnullの場合、計算して設定する
            // 村人はデフォルトで名前表示がないため、バニラの処理で計算されない可能性がある
            if (state.nameTagAttachment == null) {
                state.nameTagAttachment = villager.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, villager.getYRot());
            }
        }
    }
}
