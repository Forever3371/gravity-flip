package gravity_flip;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gravity Flip（成就翻转重力）
 * <p>
 * 任何人完成成就的<strong>任意一个进度</strong>（criterion），全服所有人的重力方向随机翻转，
 * 翻完一直保持到下一次触发。
 * <p>
 * <strong>默认不启用</strong>：加载后不会碰任何人的重力，需要 OP 在游戏内执行
 * {@code /gravityflip on} 才开始工作。
 * <p>
 * 重力本体借用了 Gravity Changer（MIT，原作者 Gaider10 / Fusion Flux / qouteall /
 * CyborgCabbage / Mortimer Kerman 等），本模组只在其上加了"成就触发 + 随机方向 + 开关"这一层。
 */
public class GravityFlip implements ModInitializer {

    public static final String MOD_ID = "gravity_flip";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        GravityFlipConfig.load();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                GravityFlipCommand.register(dispatcher));

        // 玩家死亡重生会换一个新实体，挂在身上的重力方向会丢，
        // 所以重生后把当前方向重新贴回去（否则"死一次重力就恢复正常了"）。
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                GravityFlipTrigger.reapplyCurrentGravity(newPlayer));

        LOGGER.info("[Gravity Flip] 已加载：完成成就会让全服重力随机翻转。当前状态：{}",
                GravityFlipConfig.enabled
                        ? "已启用（配置里 enabled=true）"
                        : "未启用 —— 在游戏内执行 /gravityflip on 才会开始工作");
    }
}
