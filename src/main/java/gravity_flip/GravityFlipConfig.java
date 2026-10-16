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

    /**
     * 是否启用。
     * <p>
     * <strong>默认 false</strong>：模组加载后不会碰任何人的重力，
     * 必须由 OP 在游戏内执行 {@code /gravityflip on} 才开始工作。
     * <p>
     * 命令设置的启用状态<strong>不会写回这个文件</strong>，所以每次重启都要重新开一次。
     * 想让它开局就启用，把这个值改成 true。
     */
    public static boolean enabled = false;

    /** 是否在聊天框播报是谁完成了什么成就、重力变成了哪个方向。 */
    public static boolean showMessage = true;

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

        enabled = readBoolean(props, "enabled", false);
        showMessage = readBoolean(props, "show_message", true);
    }

    private static boolean readBoolean(Properties props, String key, boolean fallback) {
        String raw = props.getProperty(key);
        return raw == null ? fallback : Boolean.parseBoolean(raw.trim());
    }

    private static String defaultContent() {
        return """
                # Gravity Flip（成就翻转重力）配置
                # 任何人完成成就的任意一个进度（criterion），全服所有人的重力方向就随机翻一次，
                # 并一直保持到下一次触发。

                # 是否开局就启用
                # 默认 false：加载后不会碰任何人的重力，
                #           需要 OP 在游戏内执行 /gravityflip on 才开始工作。
                # 注意：命令设置的启用状态不会被写回这里，所以每次重启都要重新执行一次 on。
                #      如果你希望重启后自动启用，把这个值改成 true。
                enabled=false

                # 是否在聊天框播报是谁完成了什么成就、重力变成了哪个方向
                show_message=true
                """;
    }
}
