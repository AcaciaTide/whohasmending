package acaciatide.whohasmending;

import net.minecraft.client.KeyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Whohasmending {
	public static final String MOD_ID = "whohasmending";

	// コンソールやログファイルにテキストを出力するためのロガー
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// キーバインドの実体を共通定義として保持
	public static KeyMapping toggleDisplayKey;

	public static void init() {
		LOGGER.info("Who Has Mending mod initialized (common)");
	}
}