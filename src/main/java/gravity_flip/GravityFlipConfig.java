package gravity_flip;

import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 极简配置：读写 config/gravity_flip.properties。
 */
public final class GravityFlipConfig {

    /** 总开关。 */
    public static boolean enabled = true;

    /** 是否在聊天框播报是谁完成了什么成就、重力变成了哪个方向。 */
    public static boolean showMessage = true;

    /** 是否只影响和完成成就者同一维度的玩家。 */
    public static boolean sameDimensionOnly = false;

    private static final String FILE_NAME = "gravity_flip.properties";

    private GravityFlipConfig() {
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        Properties props = new Properties();

        try {
            if (Files.notExists(path)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, defaultContent(), StandardCharsets.UTF_8);
                GravityFlip.LOGGER.info("[Gravity Flip] 已生成默认配置：{}", path);
            }
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                props.load(reader);
            }
        } catch (IOException e) {
            GravityFlip.LOGGER.warn("[Gravity Flip] 读取配置失败，使用内置默认值", e);
        }

        enabled = readBoolean(props, "enabled", true);
        showMessage = readBoolean(props, "show_message", true);
        sameDimensionOnly = readBoolean(props, "same_dimension_only", false);
    }

    private static boolean readBoolean(Properties props, String key, boolean fallback) {
        String raw = props.getProperty(key);
        return raw == null ? fallback : Boolean.parseBoolean(raw.trim());
    }

    private static String defaultContent() {
        return """
                # Gravity Flip（成就翻转重力）配置
                # 任何一个玩家完成一个成就，全服所有人的重力方向随机翻转，并一直保持到下一次成就。

                # 总开关
                enabled=true

                # 是否在聊天框播报是谁完成了什么成就、重力变成了哪个方向
                show_message=true

                # 是否只影响和完成成就者同一维度的玩家
                # false = 全服所有人（含其它维度）
                same_dimension_only=false
                """;
    }
}
