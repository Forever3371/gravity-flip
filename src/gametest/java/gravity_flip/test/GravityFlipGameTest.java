package gravity_flip.test;

import gravity_flip.GravityFlip;
import gravity_flip.GravityFlipConfig;
import gravity_flip.GravityFlipTrigger;
import gravity_flip.api.GravityChangerAPI;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

/**
 * 自动化测试：在无头服务端里真的设置重力方向，读回来验证。
 * 运行：./gradlew runGameTest
 */
public class GravityFlipGameTest implements FabricGameTest {

    private static final Set<Direction> ALL_SIX = Set.of(
            Direction.DOWN, Direction.UP,
            Direction.NORTH, Direction.SOUTH,
            Direction.EAST, Direction.WEST);

    // ---------------------------------------------------------- 重力本身

    /** 设置重力方向后，读回来必须一致（说明借用的重力实现真的生效了） */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void applyToChangesGravity(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        GravityFlipTrigger.applyTo(player, Direction.UP);
        check(GravityChangerAPI.getGravityDirection(player) == Direction.UP,
                "重力方向应当是 UP，实际 " + GravityChangerAPI.getGravityDirection(player));

        GravityFlipTrigger.applyTo(player, Direction.EAST);
        check(GravityChangerAPI.getGravityDirection(player) == Direction.EAST,
                "重力方向应当是 EAST，实际 " + GravityChangerAPI.getGravityDirection(player));

        helper.succeed();
    }

    /** 随机方向必须是六个方向之一 */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void randomDirectionIsOneOfSix(GameTestHelper helper) {
        for (int i = 0; i < 100; i++) {
            Direction direction = GravityFlipTrigger.pickDirection();
            check(ALL_SIX.contains(direction), "随机方向不合法：" + direction);
        }
        helper.succeed();
    }

    // ---------------------------------------------------------- 成就触发

    /** 完成成就 → 所有人重力翻转 */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void completingAdvancementFlipsGravity(GameTestHelper helper) {
        GravityFlipTrigger.resetTriggered();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Direction before = GravityChangerAPI.getGravityDirection(player);

        boolean triggered = GravityFlipTrigger.onAdvancementCompleted(
                player, ResourceLocation.withDefaultNamespace("test/advancement"));

        check(triggered, "完成成就应当触发翻转");
        Direction after = GravityChangerAPI.getGravityDirection(player);
        check(after != null, "翻转后重力方向不该是 null");
        check(ALL_SIX.contains(after), "翻转后重力方向不合法：" + after);
        // 注意：随机方向有可能正好抽到翻转前的方向，所以这里不断言 after != before
        GravityFlip.LOGGER.info("[测试] 完成成就触发翻转：{} -> {}", before, after);
        helper.succeed();
    }

    /** 同一个成就只触发一次（一个成就有多个 criterion，必须去重） */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void sameAdvancementTriggersOnlyOnce(GameTestHelper helper) {
        GravityFlipTrigger.resetTriggered();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ResourceLocation id = ResourceLocation.withDefaultNamespace("test/only_once");

        check(GravityFlipTrigger.onAdvancementCompleted(player, id), "第一次完成应当触发");
        check(!GravityFlipTrigger.onAdvancementCompleted(player, id), "同一个成就不该重复触发");
        check(GravityFlipTrigger.triggeredCount() == 1,
                "触发记录应当只有 1 条，实际 " + GravityFlipTrigger.triggeredCount());

        GravityFlipTrigger.resetTriggered();
        helper.succeed();
    }

    /** 关掉开关后不再触发 */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void disabledConfigBlocksTrigger(GameTestHelper helper) {
        GravityFlipTrigger.resetTriggered();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        GravityFlipConfig.enabled = false;
        try {
            check(!GravityFlipTrigger.onAdvancementCompleted(
                            player, ResourceLocation.withDefaultNamespace("test/disabled")),
                    "关掉开关后不该触发");
        } finally {
            GravityFlipConfig.enabled = true;
            GravityFlipTrigger.resetTriggered();
        }
        helper.succeed();
    }

    // ---------------------------------------------------------- 辅助

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }
}
