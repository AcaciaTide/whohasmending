package acaciatide.whohasmending.mixin.client;

import acaciatide.whohasmending.data.VillagerDataManager;
import acaciatide.whohasmending.data.VillagerTradeData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    @ModifyVariable(method = "renderNameTag", at = @At("HEAD"), argsOnly = true)
    private Component modifyNameTag(Component formattedName, T entity) {
        if (entity instanceof Villager villager) {
            if (VillagerDataManager.getInstance().isDisplayEnabled()) {
                VillagerTradeData data = VillagerDataManager.getInstance().getVillagerData(villager.getUUID());
                if (data != null && data.getDisplayName() != null && !data.getDisplayName().isEmpty()) {
                    String displayName = data.getDisplayName();
                    if (villager.hasCustomName() && formattedName != null) {
                        return Component.empty().append(formattedName).append(" ").append(Component.nullToEmpty(displayName));
                    } else {
                        return Component.nullToEmpty(displayName);
                    }
                }
            }
        }
        return formattedName;
    }
}
