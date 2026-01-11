package acaciatide.whohasmending;

import acaciatide.whohasmending.data.VillagerDataManager;
import acaciatide.whohasmending.data.VillagerDataStorage;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

/**
 * Who Has Mending クライアントエントリーポイント
 */
public class WhohasmendingClient implements ClientModInitializer {
    
    // キーバインド: 表示切り替え（Hキー）
    private static KeyBinding toggleDisplayKey;
    
    // 前回のワールド状態
    private boolean wasInWorld = false;

    @Override
    public void onInitializeClient() {
        Whohasmending.LOGGER.info("Initializing Who Has Mending client...");
        
        // データディレクトリの初期化
        VillagerDataStorage.ensureDirectoryExists();
        
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
        toggleDisplayKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.whohasmending.toggle_display",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(),
                KeyBinding.Category.MISC
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
            while (toggleDisplayKey.wasPressed()) {
                VillagerDataManager.getInstance().toggleDisplay();
                
                // フィードバックメッセージ
                boolean enabled = VillagerDataManager.getInstance().isDisplayEnabled();
                if (client.player != null) {
                    String message = enabled ? "§aWho Has Mending?: Trade Display ON" : "§cWho Has Mending?: Trade Display OFF";
                    client.player.sendMessage(net.minecraft.text.Text.of(message), true);
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
        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // /whohasmending reset
            dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("whohasmending")
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("reset")
                    .executes(context -> {
                        VillagerDataManager.getInstance().clearCurrentWorldData();
                        context.getSource().sendFeedback(net.minecraft.text.Text.literal("§c[WhoHasMending] Current world data has been reset/cleared."));
                        return 1;
                    })
                )
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("restore")
                    .executes(context -> {
                        boolean success = VillagerDataManager.getInstance().restoreFromBackup();
                        String message = success 
                            ? "§a[WhoHasMending] Data restored from backup successfully."
                            : "§c[WhoHasMending] Failed to restore: no backup found.";
                        context.getSource().sendFeedback(net.minecraft.text.Text.literal(message));
                        return success ? 1 : 0;
                    })
                )
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("backup")
                    .executes(context -> {
                        VillagerDataManager.getInstance().createManualBackup();
                        context.getSource().sendFeedback(net.minecraft.text.Text.literal("§a[WhoHasMending] Backup created successfully."));
                        return 1;
                    })
                )
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("validate")
                    .executes(context -> {
                        acaciatide.whohasmending.data.ValidationResult result = VillagerDataManager.getInstance().validateData();
                        context.getSource().sendFeedback(net.minecraft.text.Text.literal(result.getMessage()));
                        return result.isValid() ? 1 : 0;
                    })
                )
            );

            // ショートカット: /whm サブコマンド
            dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("whm")
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("reset")
                    .executes(context -> {
                        VillagerDataManager.getInstance().clearCurrentWorldData();
                        context.getSource().sendFeedback(net.minecraft.text.Text.literal("§c[WhoHasMending] Current world data has been reset/cleared."));
                        return 1;
                    })
                )
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("restore")
                    .executes(context -> {
                        boolean success = VillagerDataManager.getInstance().restoreFromBackup();
                        String message = success 
                            ? "§a[WhoHasMending] Data restored from backup successfully."
                            : "§c[WhoHasMending] Failed to restore: no backup found.";
                        context.getSource().sendFeedback(net.minecraft.text.Text.literal(message));
                        return success ? 1 : 0;
                    })
                )
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("backup")
                    .executes(context -> {
                        VillagerDataManager.getInstance().createManualBackup();
                        context.getSource().sendFeedback(net.minecraft.text.Text.literal("§a[WhoHasMending] Backup created successfully."));
                        return 1;
                    })
                )
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("validate")
                    .executes(context -> {
                        acaciatide.whohasmending.data.ValidationResult result = VillagerDataManager.getInstance().validateData();
                        context.getSource().sendFeedback(net.minecraft.text.Text.literal(result.getMessage()));
                        return result.isValid() ? 1 : 0;
                    })
                )
            );
        });
    }

    /**
     * ワールド状態の変化を検出して処理
     */
    private void handleWorldStateChange(MinecraftClient client) {
        boolean inWorld = client.world != null && client.player != null;
        
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