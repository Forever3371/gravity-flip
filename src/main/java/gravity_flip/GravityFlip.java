package gravity_flip;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gravity Flip（成就翻转重力）
 * <p>
 * 任何一个玩家完成一个成就，<strong>全服所有人的重力方向随机翻转</strong>，翻完一直保持到下一次成就。
 * <p>
 * 重力本体借用了 Gravity Changer（MIT，原作者 Gaider10 / Fusion Flux / qouteall /
 * CyborgCabbage / Mortimer Kerman 等），本模组只在其上加了"成就触发 + 随机方向"这一层。
 */
public class GravityFlip implements ModInitializer {

    public static final String MOD_ID = "gravity_flip";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        GravityFlipConfig.load();

        LOGGER.info("[Gravity Flip] 已加载：完成成就会让全服重力随机翻转（{}）",
                GravityFlipConfig.enabled ? "已启用" : "已禁用");
    }
}
