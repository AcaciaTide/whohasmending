package acaciatide.whohasmending.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import acaciatide.whohasmending.Whohasmending;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

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

    private static final int MAX_BACKUPS = 3;

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
            
            // 空ファイルかチェック
            if (json == null || json.trim().isEmpty()) {
                Whohasmending.LOGGER.warn("Data file is empty for world: {}", worldId);
                return handleCorruptedFile(filePath, worldId);
            }
            
            Type type = new TypeToken<Map<String, VillagerTradeData>>() {}.getType();
            Map<String, VillagerTradeData> stringKeyMap = GSON.fromJson(json, type);
            
            if (stringKeyMap == null) {
                return new HashMap<>();
            }

            // String キーを UUID キーに変換し、バリデーション適用
            Map<UUID, VillagerTradeData> result = new HashMap<>();
            int sanitizedCount = 0;
            
            for (Map.Entry<String, VillagerTradeData> entry : stringKeyMap.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    VillagerTradeData data = entry.getValue();
                    
                    if (data != null) {
                        data.setVillagerUuid(uuid);
                        
                        // データの検証とサニタイズ
                        if (!data.isValid()) {
                            data.sanitize();
                            sanitizedCount++;
                        }
                        
                        result.put(uuid, data);
                    }
                } catch (IllegalArgumentException e) {
                    Whohasmending.LOGGER.warn("Invalid UUID in data file, skipping: {}", entry.getKey());
                }
            }
            
            if (sanitizedCount > 0) {
                Whohasmending.LOGGER.info("Sanitized {} invalid entries for world: {}", sanitizedCount, worldId);
            }
            
            Whohasmending.LOGGER.info("Loaded {} villager records for world: {}", result.size(), worldId);
            return result;
            
        } catch (JsonParseException e) {
            Whohasmending.LOGGER.error("JSON parse error (possibly corrupted) for world: {}", worldId, e);
            return handleCorruptedFile(filePath, worldId);
        } catch (IOException e) {
            Whohasmending.LOGGER.error("Failed to load data file for world: {}", worldId, e);
            return new HashMap<>();
        }
    }

    /**
     * 破損したファイルを処理し、バックアップからの復元を試みる
     */
    private static Map<UUID, VillagerTradeData> handleCorruptedFile(Path filePath, String worldId) {
        // 破損したファイルを移動
        Path corruptedPath = filePath.resolveSibling(
            filePath.getFileName() + ".corrupted_" + System.currentTimeMillis()
        );
        
        try {
            if (Files.exists(filePath)) {
                Files.move(filePath, corruptedPath);
                Whohasmending.LOGGER.warn("Moved corrupted file to: {}", corruptedPath);
            }
        } catch (IOException moveError) {
            Whohasmending.LOGGER.error("Failed to move corrupted file", moveError);
        }
        
        // バックアップからの復元を試す
        return tryRestoreFromBackup(filePath, worldId);
    }

    /**
     * バックアップからデータを復元する
     */
    private static Map<UUID, VillagerTradeData> tryRestoreFromBackup(Path filePath, String worldId) {
        Path backupDir = filePath.getParent().resolve("backups");
        
        if (!Files.exists(backupDir)) {
            Whohasmending.LOGGER.info("No backup directory exists for restoration");
            return new HashMap<>();
        }
        
        try (Stream<Path> backupFiles = Files.list(backupDir)) {
            String filePrefix = filePath.getFileName().toString() + ".backup_";
            
            Optional<Path> latestBackup = backupFiles
                .filter(p -> p.getFileName().toString().startsWith(filePrefix))
                .max(Comparator.comparingLong(p -> {
                    try {
                        return Files.getLastModifiedTime(p).toMillis();
                    } catch (IOException e) {
                        return 0L;
                    }
                }));
            
            if (latestBackup.isPresent()) {
                Whohasmending.LOGGER.info("Attempting to restore from backup: {}", latestBackup.get());
                
                String json = Files.readString(latestBackup.get());
                Type type = new TypeToken<Map<String, VillagerTradeData>>() {}.getType();
                Map<String, VillagerTradeData> stringKeyMap = GSON.fromJson(json, type);
                
                if (stringKeyMap != null) {
                    Map<UUID, VillagerTradeData> result = new HashMap<>();
                    for (Map.Entry<String, VillagerTradeData> entry : stringKeyMap.entrySet()) {
                        try {
                            UUID uuid = UUID.fromString(entry.getKey());
                            VillagerTradeData data = entry.getValue();
                            if (data != null) {
                                data.setVillagerUuid(uuid);
                                data.sanitize();
                                result.put(uuid, data);
                            }
                        } catch (IllegalArgumentException e) {
                            Whohasmending.LOGGER.warn("Invalid UUID in backup, skipping: {}", entry.getKey());
                        }
                    }
                    
                    Whohasmending.LOGGER.info("Successfully restored {} records from backup", result.size());
                    return result;
                }
            } else {
                Whohasmending.LOGGER.info("No backup files found for: {}", worldId);
            }
        } catch (Exception e) {
            Whohasmending.LOGGER.error("Failed to restore from backup", e);
        }
        
        return new HashMap<>();
    }

    /**
     * 最新のバックアップからデータを復元（外部呼び出し用）
     */
    public static Map<UUID, VillagerTradeData> restoreFromBackup(String worldId) {
        Path filePath = getFilePath(worldId);
        return tryRestoreFromBackup(filePath, worldId);
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
            
            // バックアップはワールド離脱時のみ作成（saveWithBackupメソッドを使用）
            
            // UUID キーを String キーに変換（JSONシリアライズ用）
            Map<String, VillagerTradeData> stringKeyMap = new HashMap<>();
            for (Map.Entry<UUID, VillagerTradeData> entry : data.entrySet()) {
                stringKeyMap.put(entry.getKey().toString(), entry.getValue());
            }
            
            // アトミック書き込み（一時ファイル経由）
            Path tempFile = filePath.resolveSibling(filePath.getFileName() + ".tmp");
            String json = GSON.toJson(stringKeyMap);
            Files.writeString(tempFile, json);
            Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
            
            Whohasmending.LOGGER.info("Saved {} villager records for world: {}", data.size(), worldId);
            
        } catch (IOException e) {
            Whohasmending.LOGGER.error("Failed to save data file for world: {}", worldId, e);
        }
    }

    /**
     * バックアップを作成
     */
    private static void createBackup(Path filePath) {
        try {
            Path backupDir = filePath.getParent().resolve("backups");
            Files.createDirectories(backupDir);
            
            String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path backupPath = backupDir.resolve(
                filePath.getFileName() + ".backup_" + timestamp
            );
            
            Files.copy(filePath, backupPath);
            Whohasmending.LOGGER.debug("Created backup: {}", backupPath);
            
            // 古いバックアップを削除
            cleanupOldBackups(backupDir, filePath.getFileName().toString());
            
        } catch (IOException e) {
            Whohasmending.LOGGER.warn("Failed to create backup", e);
        }
    }

    /**
     * 手動でバックアップを作成（外部呼び出し用）
     */
    public static void createManualBackup(String worldId) {
        Path filePath = getFilePath(worldId);
        if (Files.exists(filePath)) {
            createBackup(filePath);
            Whohasmending.LOGGER.info("Manual backup created for world: {}", worldId);
        } else {
            Whohasmending.LOGGER.warn("No data file exists to backup for world: {}", worldId);
        }
    }

    /**
     * 古いバックアップを削除（最新のMAX_BACKUPS個を保持）
     */
    private static void cleanupOldBackups(Path backupDir, String baseFileName) {
        try (Stream<Path> backupFiles = Files.list(backupDir)) {
            String prefix = baseFileName + ".backup_";
            
            backupFiles
                .filter(p -> p.getFileName().toString().startsWith(prefix))
                .sorted(Comparator.comparingLong((Path p) -> {
                    try {
                        return Files.getLastModifiedTime(p).toMillis();
                    } catch (IOException e) {
                        return 0L;
                    }
                }).reversed())
                .skip(MAX_BACKUPS)
                .forEach(p -> {
                    try {
                        Files.delete(p);
                        Whohasmending.LOGGER.debug("Deleted old backup: {}", p);
                    } catch (IOException e) {
                        Whohasmending.LOGGER.warn("Failed to delete old backup: {}", p);
                    }
                });
        } catch (IOException e) {
            Whohasmending.LOGGER.warn("Failed to cleanup old backups", e);
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
