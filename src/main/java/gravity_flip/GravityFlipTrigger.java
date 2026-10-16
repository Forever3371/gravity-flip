package gravity_flip;

import gravity_flip.api.GravityChangerAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 核心逻辑：玩家完成一个成就的<strong>任意一个 criterion</strong> → 全服所有人的重力随机翻转。
 * <p>
 * 几个关键决定：
 * <ul>
 *     <li><b>criterion 级触发</b>：不等整个成就做完。"吃一个苹果"（均衡饮食的一部分）、
 *         "走进一个新群系"（探索的时光的一部分）都会各自触发一次翻转。</li>
 *     <li><b>按 criterion 去重</b>：去重键是 {@code 成就id#criterion名}。
 *         如果只按成就 id 去重，均衡饮食那 40 种食物就只会翻第一次。</li>
 *     <li><b>不需要处理登录重放</b>：玩家上线时进度是通过私有的
 *         {@code loadProgressMap} 直接塞进去的，<strong>不经过</strong> {@code award}，
 *         所以不会在每次登录时误触发。（这一点是查过字节码确认的，不是猜的。）</li>
 *     <li><b>翻完保持</b>：不设定时器，重力方向一直保持到下一次触发。</li>
 * </ul>
 */
public final class GravityFlipTrigger {

    /** 已经触发过的 {@code 成就id#criterion}，避免同一个 criterion 重复触发。 */
    private static final Set<String> ALREADY_TRIGGERED = new HashSet<>();

    /** 六个方向，随机挑一个。 */
    private static final List<Direction> ALL_DIRECTIONS = List.of(
            Direction.DOWN, Direction.UP,
            Direction.NORTH, Direction.SOUTH,
            Direction.EAST, Direction.WEST);

    private static final Random RANDOM = new Random();

    private GravityFlipTrigger() {
    }

    /**
     * 某个玩家完成了一个成就的某个 criterion。返回是否真的触发了翻转。
     *
     * @param advancementId    成就的资源 id（用于去重与日志）
     * @param criterion        这次完成的 criterion 名（去重键的一部分）
     * @param advancementTitle 成就的显示名（用于聊天框播报）
     * @param progressText     原版给的进度文本（形如 "3/40"）
     * @param wholeDone        这次是否刚好把整个成就做完了
     */
    public static boolean onCriterionCompleted(ServerPlayer player, ResourceLocation advancementId,
                                               String criterion, Component advancementTitle,
                                               Component progressText, boolean wholeDone) {
        if (!GravityFlipConfig.enabled) {
            return false;
        }
        if (!ALREADY_TRIGGERED.add(advancementId + "#" + criterion)) {
            return false; // 这个 criterion 已经触发过了
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }

        Direction direction = pickDirection();
        int affected = applyToEveryone(server, direction);

        if (GravityFlipConfig.showMessage) {
            // 整个成就做完了 vs 只完成了一部分，措辞不一样
            Component headline = wholeDone
                    ? Component.literal(" 完成了成就 ").withStyle(ChatFormatting.WHITE)
                    : Component.literal(" 完成了 ").withStyle(ChatFormatting.WHITE);
            Component tail = wholeDone
                    ? Component.empty()
                    : Component.literal(" 的一部分（").withStyle(ChatFormatting.GRAY)
                            .append(progressText.copy().withStyle(ChatFormatting.GREEN))
                            .append(Component.literal("）").withStyle(ChatFormatting.GRAY));

            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("[重力翻转] ").withStyle(ChatFormatting.AQUA)
                            .append(Component.literal(player.getName().getString()).withStyle(ChatFormatting.WHITE))
                            .append(headline)
                            .append(advancementTitle.copy().withStyle(ChatFormatting.GOLD))
                            .append(tail)
                            .append(Component.literal("，所有人的重力变成了 " + directionName(direction))
                                    .withStyle(ChatFormatting.YELLOW)),
                    false);
        }

        GravityFlip.LOGGER.info("[Gravity Flip] {} 完成了 {} 的 criterion {}（{}），重力方向 -> {}（影响 {} 名玩家）",
                player.getName().getString(), advancementId, criterion,
                progressText.getString(), direction, affected);
        return true;
    }

    /**
     * 把指定方向的重力应用到全服所有玩家，返回受影响人数。
     */
    public static int applyToEveryone(MinecraftServer server, Direction direction) {
        int count = 0;
        for (ServerPlayer target : server.getPlayerList().getPlayers()) {
            if (applyTo(target, direction)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 给单个玩家设置重力方向。返回是否成功。
     */
    public static boolean applyTo(ServerPlayer player, Direction direction) {
        if (player.isSpectator()) {
            return false; // 旁观者不参与
        }
        GravityChangerAPI.setBaseGravityDirection(player, direction);
        return true;
    }

    /** 随机挑一个重力方向。 */
    public static Direction pickDirection() {
        return ALL_DIRECTIONS.get(RANDOM.nextInt(ALL_DIRECTIONS.size()));
    }

    /** 供命令与测试使用：清掉去重记录。 */
    public static void resetTriggered() {
        ALREADY_TRIGGERED.clear();
    }

    /** 供命令查看：已经触发过多少次。 */
    public static int triggeredCount() {
        return ALREADY_TRIGGERED.size();
    }

    /** 方向的中文名，给播报用。 */
    public static String directionName(Direction direction) {
        return switch (direction) {
            case DOWN -> "向下（正常）";
            case UP -> "向上（倒挂）";
            case NORTH -> "向北";
            case SOUTH -> "向南";
            case EAST -> "向东";
            case WEST -> "向西";
        };
    }
}
