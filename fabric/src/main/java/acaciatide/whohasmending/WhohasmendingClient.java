package acaciatide.whohasmending;

import acaciatide.whohasmending.data.VillagerDataManager;
import acaciatide.whohasmending.data.VillagerDataStorage;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Who Has Mending クライアントエントリーポイント
 */
public class WhohasmendingClient implements ClientModInitializer {
    

    
    // 前回のワールド状態
    private boolean wasInWorld = false;

    @Override
    public void onInitializeClient() {
        Whohasmending.LOGGER.info("Initializing Who Has Mending client...");
        
        // キーバインドの登録
        registerKeyBindings();
        
        // コマンドの登録
        registerCommands();
        
        // イベントリスナーの登録
        registerEventListeners();
        
        Whohasmending.LOGGER.info("Who Has Mending client initialized successfully");
    }

    /**
     * キーバインドを登録
     */
    private void registerKeyBindings() {
        // MISCカテゴリにキーバインドを登録
        Whohasmending.toggleDisplayKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.whohasmending.toggle_display",
                com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                KeyMapping.Category.MISC
        ));
        
        Whohasmending.LOGGER.info("Registered key binding for toggle display");
    }

    /**
     * イベントリスナーを登録
     */
    private void registerEventListeners() {
        // クライアントティックイベント（キーバインド処理 & ワールド状態監視）
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // キーバインドの処理
            while (Whohasmending.toggleDisplayKey.consumeClick()) {
                VillagerDataManager.getInstance().toggleDisplay();
                
                // フィードバックメッセージ
                boolean enabled = VillagerDataManager.getInstance().isDisplayEnabled();
                if (client.player != null) {
                    String message = enabled ? "§aWho Has Mending?: Trade Display ON" : "§cWho Has Mending?: Trade Display OFF";
                    client.gui.setOverlayMessage(Component.literal(message), false);
                }
            }
            
            // ワールド参加/離脱の検出
            handleWorldStateChange(client);
        });

        // クライアント停止時にデータを保存
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            Whohasmending.LOGGER.info("Client stopping, saving data...");
            VillagerDataManager.getInstance().onWorldLeave();
        });
        
        Whohasmending.LOGGER.info("Registered event listeners");
    }

    /**
     * コマンドを登録
     */
    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // /whohasmending reset
            dispatcher.register(ClientCommands.literal("whohasmending")
                .then(ClientCommands.literal("reset")
                    .executes(context -> {
                        VillagerDataManager.getInstance().clearCurrentWorldData();
                        context.getSource().sendFeedback(Component.literal("§c[WhoHasMending] Current world data has been reset/cleared."));
                        return 1;
                    })
                )
                .then(ClientCommands.literal("restore")
                    .executes(context -> {
                        boolean success = VillagerDataManager.getInstance().restoreFromBackup();
                        String message = success 
                            ? "§a[WhoHasMending] Data restored from backup successfully."
                            : "§c[WhoHasMending] Failed to restore: no backup found.";
                        context.getSource().sendFeedback(Component.literal(message));
                        return success ? 1 : 0;
                    })
                )
                .then(ClientCommands.literal("backup")
                    .executes(context -> {
                        VillagerDataManager.getInstance().createManualBackup();
                        context.getSource().sendFeedback(Component.literal("§a[WhoHasMending] Backup created successfully."));
                        return 1;
                    })
                )
                .then(ClientCommands.literal("validate")
                    .executes(context -> {
                        acaciatide.whohasmending.data.ValidationResult result = VillagerDataManager.getInstance().validateData();
                        context.getSource().sendFeedback(Component.literal(result.getMessage()));
                        return result.isValid() ? 1 : 0;
                    })
                )
            );

            // ショートカット: /whm サブコマンド
            dispatcher.register(ClientCommands.literal("whm")
                .then(ClientCommands.literal("reset")
                    .executes(context -> {
                        VillagerDataManager.getInstance().clearCurrentWorldData();
                        context.getSource().sendFeedback(Component.literal("§c[WhoHasMending] Current world data has been reset/cleared."));
                        return 1;
                    })
                )
                .then(ClientCommands.literal("restore")
                    .executes(context -> {
                        boolean success = VillagerDataManager.getInstance().restoreFromBackup();
                        String message = success 
                            ? "§a[WhoHasMending] Data restored from backup successfully."
                            : "§c[WhoHasMending] Failed to restore: no backup found.";
                        context.getSource().sendFeedback(Component.literal(message));
                        return success ? 1 : 0;
                    })
                )
                .then(ClientCommands.literal("backup")
                    .executes(context -> {
                        VillagerDataManager.getInstance().createManualBackup();
                        context.getSource().sendFeedback(Component.literal("§a[WhoHasMending] Backup created successfully."));
                        return 1;
                    })
                )
                .then(ClientCommands.literal("validate")
                    .executes(context -> {
                        acaciatide.whohasmending.data.ValidationResult result = VillagerDataManager.getInstance().validateData();
                        context.getSource().sendFeedback(Component.literal(result.getMessage()));
                        return result.isValid() ? 1 : 0;
                    })
                )
            );
        });
    }

    /**
     * ワールド状態の変化を検出して処理
     */
    private void handleWorldStateChange(Minecraft client) {
        boolean inWorld = client.level != null && client.player != null;
        
        if (inWorld && !wasInWorld) {
            // ワールドに参加した
            Whohasmending.LOGGER.info("Detected world join");
            VillagerDataManager.getInstance().onWorldJoin();
        } else if (!inWorld && wasInWorld) {
            // ワールドから離脱した
            Whohasmending.LOGGER.info("Detected world leave");
            VillagerDataManager.getInstance().onWorldLeave();
        }
        
        wasInWorld = inWorld;
    }
}