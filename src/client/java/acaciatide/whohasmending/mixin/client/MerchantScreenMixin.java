package acaciatide.whohasmending.mixin.client;

import acaciatide.whohasmending.Whohasmending;
import acaciatide.whohasmending.capture.VillagerTradeCapture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * MerchantScreenにフックして取引情報をキャプチャするMixin
 */
@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin {

    // 既にキャプチャしたかどうかのフラグ（1回だけ実行）
    @Unique
    private boolean whohasmending_captured = false;

    // 対象の村人を保持
    @Unique
    private Villager whohasmending_targetVillager = null;

    /**
     * 取引画面の初期化時に村人を特定
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        whohasmending_captured = false;
        whohasmending_targetVillager = null;
        
        try {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.level == null) {
                return;
            }

            // 対話中の村人を探す
            Entity targetEntity = findInteractingVillager(client);
            
            if (targetEntity instanceof Villager villager) {
                whohasmending_targetVillager = villager;
                Whohasmending.LOGGER.info("MerchantScreen.init() - Found villager: {}", villager.getUUID());
            }
            
        } catch (Exception e) {
            Whohasmending.LOGGER.error("Error finding villager on init", e);
        }
    }

    /**
     * 画面描画時にオファーをキャプチャ（最初の1回のみ）
     */
    @Inject(method = "extractContents", at = @At("HEAD"))
    private void onRenderMain(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        // 既にキャプチャ済みの場合はスキップ
        if (whohasmending_captured) {
            return;
        }

        try {
            MerchantScreen screen = (MerchantScreen) (Object) this;
            MerchantMenu handler = screen.getMenu();
            
            if (handler == null) {
                return;
            }

            MerchantOffers offers = handler.getOffers();
            
            // オファーがまだ空の場合は次のフレームで再試行
            if (offers == null || offers.isEmpty()) {
                return;
            }

            Whohasmending.LOGGER.info("renderMain - {} offers available, capturing...", offers.size());

            if (whohasmending_targetVillager != null) {
                UUID villagerUuid = whohasmending_targetVillager.getUUID();
                
                // 職業IDを取得
                String professionId = whohasmending_targetVillager.getVillagerData().profession().unwrapKey()
                        .map(k -> k.identifier().getPath())
                        .orElse("none");

                // 司書以外は記録しない
                if (!"librarian".equals(professionId)) {
                    // キャプチャ済みフラグを立てて、この村人に対して再試行しないようにする
                    whohasmending_captured = true;
                    return;
                }
                
                // 職業名を取得（現在の言語設定で翻訳）
                String professionName = getProfessionDisplayName(whohasmending_targetVillager.getVillagerData().profession());
                
                // 取引情報をキャプチャ
                VillagerTradeCapture.captureFromOffers(offers, villagerUuid, professionName);
                whohasmending_captured = true;
                
            } else {
                // フォールバック: 改めて村人を探す
                Minecraft client = Minecraft.getInstance();
                Entity targetEntity = findInteractingVillager(client);
                
                if (targetEntity instanceof Villager villager) {
                    UUID villagerUuid = villager.getUUID();
                    
                    // 職業IDを取得
                    String professionId = villager.getVillagerData().profession().unwrapKey()
                            .map(k -> k.identifier().getPath())
                            .orElse("none");

                    // 司書以外は記録しない
                    if (!"librarian".equals(professionId)) {
                        whohasmending_captured = true;
                        return;
                    }

                    String professionName = getProfessionDisplayName(villager.getVillagerData().profession());
                    
                    VillagerTradeCapture.captureFromOffers(offers, villagerUuid, professionName);
                    whohasmending_captured = true;
                } else {
                    Whohasmending.LOGGER.warn("Could not find villager for trade capture");
                }
            }
            
        } catch (Exception e) {
            Whohasmending.LOGGER.error("Error capturing trade data on renderMain", e);
        }
    }

    /**
     * 現在対話中の村人を探す
     */
    @Unique
    private Entity findInteractingVillager(Minecraft client) {
        // クロスヘアのEntityHitResultから取得を試みる
        if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) client.hitResult;
            Entity entity = entityHit.getEntity();
            if (entity instanceof Villager) {
                return entity;
            }
        }

        // フォールバック: プレイヤーの近くにいる村人を検索
        if (client.player != null && client.level != null) {
            double searchRadius = 5.0;
            return client.level.getEntitiesOfClass(
                    Villager.class,
                    client.player.getBoundingBox().inflate(searchRadius),
                    villager -> villager.getTradingPlayer() == client.player
            ).stream().findFirst().orElse(null);
        }

        return null;
    }

    /**
     * 職業の表示名を取得
     */
    @Unique
    private String getProfessionDisplayName(Holder<VillagerProfession> profession) {
        // 現在の言語設定に従って翻訳された名前を取得
        return net.minecraft.network.chat.Component.translatable("entity.minecraft.villager." + 
                profession.unwrapKey().map(k -> k.identifier().getPath()).orElse("none")).getString();
    }
}
