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
import net.minecraft.network.chat.Component;
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

    /** 只完成"一部分"（一个 criterion）就要触发，不用等整个成就做完 */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void completingOneCriterionFlipsGravity(GameTestHelper helper) {
        GravityFlipTrigger.resetTriggered();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Direction before = GravityChangerAPI.getGravityDirection(player);

        // 1/40 —— 相当于"均衡饮食"里只吃了一种食物
        boolean triggered = trigger(player, "test/balanced_diet", "apple", "1/40", false);

        check(triggered, "只完成一个 criterion 也应当触发翻转");
        Direction after = GravityChangerAPI.getGravityDirection(player);
        check(after != null, "翻转后重力方向不该是 null");
        check(ALL_SIX.contains(after), "翻转后重力方向不合法：" + after);
        GravityFlip.LOGGER.info("[测试] 单个 criterion 触发翻转：{} -> {}", before, after);
        helper.succeed();
    }

    /** 同一个成就的不同 criterion 都要能各自触发一次（这是"吃苹果"和"吃面包"各翻一次的关键） */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void differentCriteriaBothTrigger(GameTestHelper helper) {
        GravityFlipTrigger.resetTriggered();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        String advancement = "test/balanced_diet";

        check(trigger(player, advancement, "apple", "1/40", false), "第一个 criterion 应当触发");
        check(trigger(player, advancement, "bread", "2/40", false), "同一个成就的第二个 criterion 也应当触发");
        check(GravityFlipTrigger.triggeredCount() == 2,
                "应当记录 2 次触发，实际 " + GravityFlipTrigger.triggeredCount());

        GravityFlipTrigger.resetTriggered();
        helper.succeed();
    }

    /** 同一个 criterion 只触发一次（成就系统会重复调用 award） */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void sameCriterionTriggersOnlyOnce(GameTestHelper helper) {
        GravityFlipTrigger.resetTriggered();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        check(trigger(player, "test/only_once", "c1", "1/3", false), "第一次应当触发");
        check(!trigger(player, "test/only_once", "c1", "1/3", false), "同一个 criterion 不该重复触发");
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
            check(!trigger(player, "test/disabled", "c1", "1/1", false), "关掉开关后不该触发");
        } finally {
            GravityFlipConfig.enabled = true;
            GravityFlipTrigger.resetTriggered();
        }
        helper.succeed();
    }

    // ---------------------------------------------------------- 辅助

    private static boolean trigger(ServerPlayer player, String advancementPath, String criterion,
                                   String progressText, boolean wholeDone) {
        ResourceLocation id = ResourceLocation.withDefaultNamespace(advancementPath);
        return GravityFlipTrigger.onCriterionCompleted(
                player, id, criterion, Component.literal("测试成就"),
                Component.literal(progressText), wholeDone);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }
}
