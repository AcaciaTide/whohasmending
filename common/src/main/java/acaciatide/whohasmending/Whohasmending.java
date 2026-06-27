package acaciatide.whohasmending;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Whohasmending {
	public static final String MOD_ID = "whohasmending";

	// コンソールやログファイルにテキストを出力するためのロガー
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static void init() {
		LOGGER.info("Who Has Mending mod initialized (common)");
	}
}