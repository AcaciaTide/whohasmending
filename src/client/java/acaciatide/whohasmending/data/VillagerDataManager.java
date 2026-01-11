package acaciatide.whohasmending.data;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.server.integrated.IntegratedServer;
import acaciatide.whohasmending.Whohasmending;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 村人取引データのメモリキャッシュを管理
 */
public class VillagerDataManager {
    private static VillagerDataManager instance;
    
    private Map<UUID, VillagerTradeData> villagerData;
    private String currentWorldId;
    private boolean displayEnabled;
    private boolean isDirty;

    private VillagerDataManager() {
        this.villagerData = new HashMap<>();
        this.displayEnabled = true;
        this.isDirty = false;
    }

    public static VillagerDataManager getInstance() {
        if (instance == null) {
            instance = new VillagerDataManager();
        }
        return instance;
    }

    /**
     * ワールドまたはサーバーに参加した時に呼び出す
     */
    public void onWorldJoin() {
        String worldId = getWorldIdentifier();
        if (worldId == null) {
            Whohasmending.LOGGER.warn("Could not determine world identifier");
            return;
        }

        // 前のワールドのデータが未保存なら保存
        if (currentWorldId != null && isDirty) {
            saveCurrentWorld();
        }

        this.currentWorldId = worldId;
        this.villagerData = VillagerDataStorage.load(worldId);
        this.isDirty = false;
        
        Whohasmending.LOGGER.info("Joined world: {} with {} villager records", worldId, villagerData.size());
    }

    /**
     * ワールドまたはサーバーから離脱した時に呼び出す
     */
    public void onWorldLeave() {
        if (currentWorldId != null && !villagerData.isEmpty()) {
            // ワールド離脱時にバックアップを作成
            VillagerDataStorage.createManualBackup(currentWorldId);
        }
        
        if (currentWorldId != null && isDirty) {
            saveCurrentWorld();
        }
        
        this.villagerData.clear();
        this.currentWorldId = null;
        this.isDirty = false;
        
        Whohasmending.LOGGER.info("Left world, data cleared");
    }

    /**
     * 現在のワールドのデータを保存
     */
    public void saveCurrentWorld() {
        Whohasmending.LOGGER.info("saveCurrentWorld called: currentWorldId={}, dataSize={}", currentWorldId, villagerData.size());
        
        if (currentWorldId == null) {
            Whohasmending.LOGGER.warn("Cannot save: currentWorldId is null");
            return;
        }
        
        if (!villagerData.isEmpty()) {
            VillagerDataStorage.save(currentWorldId, villagerData);
            isDirty = false;
        } else {
            Whohasmending.LOGGER.info("Skipping save: no villager data to save");
        }
    }

    /**
     * 村人データを取得
     */
    public VillagerTradeData getVillagerData(UUID villagerUuid) {
        return villagerData.get(villagerUuid);
    }

    /**
     * 村人データを登録または更新
     */
    public void putVillagerData(UUID villagerUuid, VillagerTradeData data) {
        villagerData.put(villagerUuid, data);
        isDirty = true;
        
        // 毎回の更新で保存（MVP版ではシンプルに即時保存）
        saveCurrentWorld();
    }

    /**
     * 村人データを削除
     */
    public void removeVillagerData(UUID villagerUuid) {
        if (villagerData.remove(villagerUuid) != null) {
            isDirty = true;
            saveCurrentWorld();
        }
    }

    /**
     * 現在のワールドの全データを削除
     */
    public void clearCurrentWorldData() {
        if (villagerData.isEmpty()) {
            return;
        }
        
        villagerData.clear();
        isDirty = false; // クリア時はここでフラグを落とす
        
        // saveCurrentWorldはisEmptyの場合スキップするので、直接saveを呼んで空データを書き込む
        if (currentWorldId != null) {
            VillagerDataStorage.save(currentWorldId, new HashMap<>());
            Whohasmending.LOGGER.info("Cleared all villager data for current world: {}", currentWorldId);
        }
    }

    /**
     * バックアップからデータを復元
     * @return 復元に成功した場合true
     */
    public boolean restoreFromBackup() {
        if (currentWorldId == null) {
            Whohasmending.LOGGER.warn("Cannot restore: not in a world");
            return false;
        }
        
        Map<UUID, VillagerTradeData> restored = VillagerDataStorage.restoreFromBackup(currentWorldId);
        
        if (!restored.isEmpty()) {
            this.villagerData = restored;
            this.isDirty = true;
            saveCurrentWorld();
            Whohasmending.LOGGER.info("Restored {} villager records from backup", restored.size());
            return true;
        }
        
        Whohasmending.LOGGER.warn("No backup data found to restore");
        return false;
    }

    /**
     * 手動でバックアップを作成
     */
    public void createManualBackup() {
        if (currentWorldId == null) {
            Whohasmending.LOGGER.warn("Cannot create backup: not in a world");
            return;
        }
        
        // 現在のデータを保存してからバックアップ
        if (isDirty) {
            saveCurrentWorld();
        }
        
        VillagerDataStorage.createManualBackup(currentWorldId);
    }

    /**
     * 現在のデータをバリデーション
     * @return バリデーション結果
     */
    public ValidationResult validateData() {
        if (villagerData.isEmpty()) {
            return ValidationResult.empty();
        }
        
        int totalRecords = villagerData.size();
        int validRecords = 0;
        int invalidRecords = 0;
        
        for (VillagerTradeData data : villagerData.values()) {
            if (data.isValid()) {
                validRecords++;
            } else {
                invalidRecords++;
            }
        }
        
        if (invalidRecords == 0) {
            return ValidationResult.success(totalRecords);
        } else {
            return ValidationResult.partial(totalRecords, validRecords, invalidRecords);
        }
    }

    /**
     * 全村人データを取得（レンダリング用）
     */
    public Map<UUID, VillagerTradeData> getAllVillagerData() {
        return villagerData;
    }

    /**
     * 表示の有効/無効を切り替え
     */
    public void toggleDisplay() {
        this.displayEnabled = !this.displayEnabled;
        Whohasmending.LOGGER.info("Display toggled: {}", displayEnabled);
    }

    /**
     * 表示が有効かどうか
     */
    public boolean isDisplayEnabled() {
        return displayEnabled;
    }

    /**
     * 現在のワールド/サーバーの識別子を取得
     */
    private String getWorldIdentifier() {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // シングルプレイの場合
        IntegratedServer integratedServer = client.getServer();
        if (integratedServer != null) {
            String worldName = integratedServer.getSaveProperties().getLevelName();
            return "world_" + worldName;
        }
        
        // マルチプレイの場合
        ServerInfo serverInfo = client.getCurrentServerEntry();
        if (serverInfo != null) {
            return "server_" + serverInfo.address;
        }
        
        // フォールバック
        if (client.world != null) {
            return "unknown_" + System.currentTimeMillis();
        }
        
        return null;
    }

    /**
     * データの件数を取得
     */
    public int getRecordCount() {
        return villagerData.size();
    }

    /**
     * 現在のワールドIDを取得
     */
    public String getCurrentWorldId() {
        return currentWorldId;
    }
}
