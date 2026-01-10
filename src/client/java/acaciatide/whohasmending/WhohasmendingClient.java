package acaciatide.whohasmending;

import acaciatide.whohasmending.data.VillagerDataManager;
import acaciatide.whohasmending.data.VillagerDataStorage;
import acaciatide.whohasmending.render.VillagerNametagRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

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
        
        // イベントリスナーの登録
        registerEventListeners();
        
        // レンダラーの登録
        VillagerNametagRenderer.register();
        
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
        
        Whohasmending.LOGGER.info("Registered key binding: H to toggle display");
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
                    String message = enabled ? "§aWho Has Mending: 表示ON" : "§cWho Has Mending: 表示OFF";
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