package gravity_flip;

import gravity_flip.api.GravityChangerAPI;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 核心逻辑：玩家完成一个成就 → 全服所有人的重力随机翻转。
 * <p>
 * 几个关键决定：
 * <ul>
 *     <li><b>只在"整个成就完成"时触发</b>：一个成就有多个 criterion，
 *         {@code grantCriterion} 会被调用多次。判断 {@code getProgress(...).isDone()}
 *         才是"这个成就真的做完了"。</li>
 *     <li><b>不需要处理登录重放</b>：玩家上线时进度是通过私有的
 *         {@code loadProgressMap} 直接塞进去的，<strong>不经过</strong> {@code grantCriterion}，
 *         所以不会在每次登录时误触发。（这一点是查过字节码确认的，不是猜的。）</li>
 *     <li><b>额外去重</b>：万一同一个成就在同一条命里被反复判定完成，用集合挡一下。</li>
 *     <li><b>翻完保持</b>：不设定时器，重力方向一直保持到下一次成就。</li>
 * </ul>
 */
public final class GravityFlipTrigger {

    /** 已经触发过的成就 id，避免重复触发。 */
    private static final Set<ResourceLocation> ALREADY_TRIGGERED = new HashSet<>();

    /** 六个方向，随机挑一个。 */
    private static final List<Direction> ALL_DIRECTIONS = List.of(
            Direction.DOWN, Direction.UP,
            Direction.NORTH, Direction.SOUTH,
            Direction.EAST, Direction.WEST);

    private static final Random RANDOM = new Random();

    private GravityFlipTrigger() {
    }

    /**
     * 某个玩家完成了一个成就。返回是否真的触发了翻转。
     */
    public static boolean onAdvancementCompleted(ServerPlayer player, ResourceLocation advancementId) {
        if (!GravityFlipConfig.enabled) {
            return false;
        }
        if (!ALREADY_TRIGGERED.add(advancementId)) {
            return false; // 同一个成就只触发一次
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }

        Direction direction = pickDirection();
        int affected = applyToEveryone(server, direction);

        if (GravityFlipConfig.showMessage) {
            server.getPlayerList().broadcastSystemMessage(
                    net.minecraft.network.chat.Component.literal("[重力翻转] ")
                            .withStyle(net.minecraft.ChatFormatting.AQUA)
                            .append(net.minecraft.network.chat.Component.literal(
                                            player.getName().getString() + " 完成了成就 " + advancementId.getPath())
                                    .withStyle(net.minecraft.ChatFormatting.WHITE))
                            .append(net.minecraft.network.chat.Component.literal(
                                            "，所有人的重力变成了 " + directionName(direction))
                                    .withStyle(net.minecraft.ChatFormatting.YELLOW)),
                    false);
        }

        GravityFlip.LOGGER.info("[Gravity Flip] {} 完成了成就 {}，重力方向 -> {}（影响 {} 名玩家）",
                player.getName().getString(), advancementId, direction, affected);
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

    /** 供命令与测试使用：清掉去重记录，好让同一个成就能再触发一次。 */
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
