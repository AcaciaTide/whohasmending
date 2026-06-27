package acaciatide.whohasmending.mixin.client;

import acaciatide.whohasmending.data.VillagerDataManager;
import acaciatide.whohasmending.data.VillagerTradeData;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobRenderer.class)
public abstract class MobRendererMixin<T extends Mob> {

    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
    private void onShouldShowName(T livingEntity, CallbackInfoReturnable<Boolean> cir) {
        if (livingEntity instanceof Villager villager) {
            if (VillagerDataManager.getInstance().isDisplayEnabled()) {
                // キャッシュ対応のgetVillagerDataを呼び出す
                VillagerTradeData data = VillagerDataManager.getInstance().getVillagerData(villager.getUUID());
                if (data != null && data.getDisplayName() != null && !data.getDisplayName().isEmpty()) {
                    cir.setReturnValue(true);
                }
            }
        }
    }
}
