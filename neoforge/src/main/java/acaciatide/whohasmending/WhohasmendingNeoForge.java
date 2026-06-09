package acaciatide.whohasmending;

import acaciatide.whohasmending.data.VillagerDataManager;
import acaciatide.whohasmending.data.VillagerDataStorage;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(Whohasmending.MOD_ID)
public class WhohasmendingNeoForge {

    public WhohasmendingNeoForge(IEventBus modEventBus) {
        Whohasmending.init();

        // Modイベントバスへの登録（キーバインド等用）
        modEventBus.register(ClientModEvents.class);

        // ゲームイベントバスへの登録
        NeoForge.EVENT_BUS.register(ForgeClientEvents.class);
    }

    // キーバインドなどの登録用ハンドラ（Mod Event Bus）
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            Whohasmending.toggleDisplayKey = new KeyMapping(
                    "key.whohasmending.toggle_display",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_H,
                    KeyMapping.Category.MISC
            );
            event.register(Whohasmending.toggleDisplayKey);
            Whohasmending.LOGGER.info("Registered key binding for toggle display (NeoForge)");
        }
    }

    // ゲーム内処理のハンドラ（Game Event Bus）
    public static class ForgeClientEvents {
        
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null && Whohasmending.toggleDisplayKey != null) {
                while (Whohasmending.toggleDisplayKey.consumeClick()) {
                    VillagerDataManager.getInstance().toggleDisplay();
                    boolean enabled = VillagerDataManager.getInstance().isDisplayEnabled();
                    String message = enabled ? "§aWho Has Mending?: Trade Display ON" : "§cWho Has Mending?: Trade Display OFF";
                    client.gui.setOverlayMessage(Component.literal(message), false);
                }
            }
        }

        @SubscribeEvent
        public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
            Whohasmending.LOGGER.info("Detected world join (NeoForge)");
            VillagerDataManager.getInstance().onWorldJoin();
        }

        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            Whohasmending.LOGGER.info("Detected world leave (NeoForge)");
            VillagerDataManager.getInstance().onWorldLeave();
        }

        @SubscribeEvent
        public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
            // コマンド /whohasmending を登録
            event.getDispatcher().register(Commands.literal("whohasmending")
                .then(Commands.literal("reset")
                    .executes(context -> {
                        VillagerDataManager.getInstance().clearCurrentWorldData();
                        context.getSource().sendSystemMessage(Component.literal("§c[WhoHasMending] Current world data has been reset/cleared."));
                        return 1;
                    })
                )
                .then(Commands.literal("restore")
                    .executes(context -> {
                        boolean success = VillagerDataManager.getInstance().restoreFromBackup();
                        String message = success 
                            ? "§a[WhoHasMending] Data restored from backup successfully."
                            : "§c[WhoHasMending] Failed to restore: no backup found.";
                        context.getSource().sendSystemMessage(Component.literal(message));
                        return success ? 1 : 0;
                    })
                )
                .then(Commands.literal("backup")
                    .executes(context -> {
                        VillagerDataManager.getInstance().createManualBackup();
                        context.getSource().sendSystemMessage(Component.literal("§a[WhoHasMending] Backup created successfully."));
                        return 1;
                    })
                )
                .then(Commands.literal("validate")
                    .executes(context -> {
                        acaciatide.whohasmending.data.ValidationResult result = VillagerDataManager.getInstance().validateData();
                        context.getSource().sendSystemMessage(Component.literal(result.getMessage()));
                        return result.isValid() ? 1 : 0;
                    })
                )
            );

            // ショートカットコマンド /whm を登録
            event.getDispatcher().register(Commands.literal("whm")
                .then(Commands.literal("reset")
                    .executes(context -> {
                        VillagerDataManager.getInstance().clearCurrentWorldData();
                        context.getSource().sendSystemMessage(Component.literal("§c[WhoHasMending] Current world data has been reset/cleared."));
                        return 1;
                    })
                )
                .then(Commands.literal("restore")
                    .executes(context -> {
                        boolean success = VillagerDataManager.getInstance().restoreFromBackup();
                        String message = success 
                            ? "§a[WhoHasMending] Data restored from backup successfully."
                            : "§c[WhoHasMending] Failed to restore: no backup found.";
                        context.getSource().sendSystemMessage(Component.literal(message));
                        return success ? 1 : 0;
                    })
                )
                .then(Commands.literal("backup")
                    .executes(context -> {
                        VillagerDataManager.getInstance().createManualBackup();
                        context.getSource().sendSystemMessage(Component.literal("§a[WhoHasMending] Backup created successfully."));
                        return 1;
                    })
                )
                .then(Commands.literal("validate")
                    .executes(context -> {
                        acaciatide.whohasmending.data.ValidationResult result = VillagerDataManager.getInstance().validateData();
                        context.getSource().sendSystemMessage(Component.literal(result.getMessage()));
                        return result.isValid() ? 1 : 0;
                    })
                )
            );
            Whohasmending.LOGGER.info("Registered client commands (NeoForge)");
        }
    }
}
