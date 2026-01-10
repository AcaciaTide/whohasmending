package acaciatide.whohasmending.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import acaciatide.whohasmending.Whohasmending;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 村人取引データのJSON読み書きを担当
 */
public class VillagerDataStorage {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    private static final Path CONFIG_DIR = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("whohasmending")
            .resolve("data");

    /**
     * 指定されたワールド/サーバーのデータを読み込む
     * @param worldId ワールドまたはサーバーの識別子
     * @return 村人データのMap (UUID -> VillagerTradeData)
     */
    public static Map<UUID, VillagerTradeData> load(String worldId) {
        Path filePath = getFilePath(worldId);
        
        if (!Files.exists(filePath)) {
            Whohasmending.LOGGER.info("No existing data file for world: {}", worldId);
            return new HashMap<>();
        }

        try {
            String json = Files.readString(filePath);
            Type type = new TypeToken<Map<String, VillagerTradeData>>() {}.getType();
            Map<String, VillagerTradeData> stringKeyMap = GSON.fromJson(json, type);
            
            if (stringKeyMap == null) {
                return new HashMap<>();
            }

            // String キーを UUID キーに変換
            Map<UUID, VillagerTradeData> result = new HashMap<>();
            for (Map.Entry<String, VillagerTradeData> entry : stringKeyMap.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    VillagerTradeData data = entry.getValue();
                    data.setVillagerUuid(uuid);
                    result.put(uuid, data);
                } catch (IllegalArgumentException e) {
                    Whohasmending.LOGGER.warn("Invalid UUID in data file: {}", entry.getKey());
                }
            }
            
            Whohasmending.LOGGER.info("Loaded {} villager records for world: {}", result.size(), worldId);
            return result;
            
        } catch (IOException e) {
            Whohasmending.LOGGER.error("Failed to load data file for world: {}", worldId, e);
            return new HashMap<>();
        }
    }

    /**
     * データをJSONファイルに保存
     * @param worldId ワールドまたはサーバーの識別子
     * @param data 保存するデータ
     */
    public static void save(String worldId, Map<UUID, VillagerTradeData> data) {
        Path filePath = getFilePath(worldId);
        
        try {
            // ディレクトリが存在しない場合は作成
            Files.createDirectories(filePath.getParent());
            
            // UUID キーを String キーに変換（JSONシリアライズ用）
            Map<String, VillagerTradeData> stringKeyMap = new HashMap<>();
            for (Map.Entry<UUID, VillagerTradeData> entry : data.entrySet()) {
                stringKeyMap.put(entry.getKey().toString(), entry.getValue());
            }
            
            String json = GSON.toJson(stringKeyMap);
            Files.writeString(filePath, json);
            
            Whohasmending.LOGGER.info("Saved {} villager records for world: {}", data.size(), worldId);
            
        } catch (IOException e) {
            Whohasmending.LOGGER.error("Failed to save data file for world: {}", worldId, e);
        }
    }

    /**
     * データファイルのパスを取得
     */
    private static Path getFilePath(String worldId) {
        // ファイル名に使えない文字を置換
        String safeWorldId = worldId.replaceAll("[^a-zA-Z0-9._-]", "_");
        return CONFIG_DIR.resolve(safeWorldId + ".json");
    }

    /**
     * データディレクトリを初期化
     */
    public static void ensureDirectoryExists() {
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            Whohasmending.LOGGER.error("Failed to create config directory", e);
        }
    }
}
