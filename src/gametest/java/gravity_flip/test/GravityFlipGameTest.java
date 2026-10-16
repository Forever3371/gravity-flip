package gravity_flip.test;

import gravity_flip.GravityFlip;
import gravity_flip.GravityFlipConfig;
import gravity_flip.GravityFlipTrigger;
import gravity_flip.api.GravityChangerAPI;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 自动化测试：在无头服务端里真的设置重力方向、真的 award 成就，然后读回来验证。
 * 运行：./gradlew runGameTest
 * <p>
 * 注意：模组默认 {@code enabled=false}（要 OP 在游戏内 /gravityflip on 才启用），
 * 所以每个需要触发的测试都<strong>自己先把开关打开</strong>，
 * 不能依赖测试之间的执行顺序。
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
        enableAndReset();
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
        enableAndReset();
        for (int i = 0; i < 100; i++) {
            Direction direction = GravityFlipTrigger.pickDirection();
            check(ALL_SIX.contains(direction), "随机方向不合法：" + direction);
        }
        helper.succeed();
    }

    // ---------------------------------------------------------- 端到端：真的完成成就

    /**
     * 端到端：真的调用 award 完成一个真实成就的 criterion，
     * 验证 Mixin 注入生效、并且重力真的被翻了。
     * <p>
     * 用"均衡饮食"（吃 40 种食物）——它的每个 criterion 对应一种食物，
     * 正好对应"吃一次苹果也要翻一次"这个需求。
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void realCriterionAwardFlipsGravity(GameTestHelper helper) {
        enableAndReset();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        MinecraftServer server = helper.getLevel().getServer();

        AdvancementHolder advancement = server.getAdvancements()
                .get(ResourceLocation.withDefaultNamespace("husbandry/balanced_diet"));
        check(advancement != null, "找不到成就 husbandry/balanced_diet");

        List<String> criteria = new ArrayList<>(advancement.value().criteria().keySet());
        check(!criteria.isEmpty(), "均衡饮食居然没有 criterion");

        Direction before = GravityChangerAPI.getGravityDirection(player);

        // 完成第一个 criterion（相当于"吃了一个苹果"）
        boolean awarded = player.getAdvancements().award(advancement, criteria.get(0));
        check(awarded, "award 应当返回 true");

        // 触发是通过 Mixin 自动发生的，这里验证它确实发生了
        check(GravityFlipTrigger.triggeredCount() == 1,
                "完成一个 criterion 应当触发 1 次翻转，实际 " + GravityFlipTrigger.triggeredCount());
        Direction after = GravityChangerAPI.getGravityDirection(player);
        check(ALL_SIX.contains(after), "翻转后重力方向不合法：" + after);
        GravityFlip.LOGGER.info("[测试] 真实 award 触发翻转：{} -> {}", before, after);

        // 再完成第二个 criterion（相当于"又吃了一个面包"），应该再翻一次
        if (criteria.size() > 1) {
            boolean second = player.getAdvancements().award(advancement, criteria.get(1));
            check(second, "第二个 criterion 的 award 也应当返回 true");
            check(GravityFlipTrigger.triggeredCount() == 2,
                    "第二个 criterion 应当再触发一次，实际 " + GravityFlipTrigger.triggeredCount());
        }

        // 重复 award 同一个 criterion 不该再触发
        player.getAdvancements().award(advancement, criteria.get(0));
        int expected = Math.min(2, criteria.size());
        check(GravityFlipTrigger.triggeredCount() == expected,
                "重复 award 同一个 criterion 不该增加触发次数，实际 " + GravityFlipTrigger.triggeredCount());

        helper.succeed();
    }

    /** 整个成就做完（所有 criterion 都完成）时也要翻 */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void finishingWholeAdvancementAlsoFlips(GameTestHelper helper) {
        enableAndReset();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        MinecraftServer server = helper.getLevel().getServer();

        AdvancementHolder advancement = server.getAdvancements()
                .get(ResourceLocation.withDefaultNamespace("story/root"));
        check(advancement != null, "找不到成就 story/root");

        List<String> criteria = new ArrayList<>(advancement.value().criteria().keySet());
        for (String criterion : criteria) {
            player.getAdvancements().award(advancement, criterion);
        }

        check(GravityFlipTrigger.triggeredCount() >= 1,
                "完成成就的 criterion 应当触发翻转，实际 " + GravityFlipTrigger.triggeredCount());
        check(player.getAdvancements().getOrStartProgress(advancement).isDone(),
                "story/root 应当已经完成");

        helper.succeed();
    }

    /** 默认（未启用）时不该触发——这是"要指令才启用"这个需求的核心保障 */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void disabledByDefaultBlocksTrigger(GameTestHelper helper) {
        GravityFlipConfig.enabled = false;
        GravityFlipTrigger.resetTriggered();

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        MinecraftServer server = helper.getLevel().getServer();

        AdvancementHolder advancement = server.getAdvancements()
                .get(ResourceLocation.withDefaultNamespace("husbandry/balanced_diet"));
        check(advancement != null, "找不到成就 husbandry/balanced_diet");
        List<String> criteria = new ArrayList<>(advancement.value().criteria().keySet());

        Direction before = GravityChangerAPI.getGravityDirection(player);
        player.getAdvancements().award(advancement, criteria.get(0));
        Direction after = GravityChangerAPI.getGravityDirection(player);

        check(GravityFlipTrigger.triggeredCount() == 0,
                "没启用时不该触发，实际 " + GravityFlipTrigger.triggeredCount());
        check(before == after, "没启用时重力方向不该改变：" + before + " -> " + after);

        enableAndReset();
        helper.succeed();
    }

    // ---------------------------------------------------------- 辅助

    /** 每个测试自己把开关打开并清空去重记录，不依赖测试之间的顺序。 */
    private static void enableAndReset() {
        GravityFlipConfig.enabled = true;
        GravityFlipTrigger.resetTriggered();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }
}
