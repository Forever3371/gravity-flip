package gravity_flip;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

/**
 * /gravityflip 命令。
 * <p>
 * 本模组<strong>默认不启用</strong>：加载后不会碰任何人的重力，
 * 必须由 OP 在游戏内执行 {@code /gravityflip on} 才会开始工作。
 * <p>
 * 这个启用状态只存在于本次运行中，<strong>不会写回配置</strong>——
 * 所以每次重启服务器/游戏都要重新开一次，符合"不要开局就启用"的要求。
 * 如果你反过来希望开局就启用，把配置里的 {@code enabled} 改成 true 即可。
 */
public final class GravityFlipCommand {

    private GravityFlipCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("gravityflip")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("on")
                        .executes(ctx -> setEnabled(ctx.getSource(), true)))
                .then(Commands.literal("off")
                        .executes(ctx -> setEnabled(ctx.getSource(), false)))
                .then(Commands.literal("trigger")
                        .executes(ctx -> trigger(ctx.getSource())))
                .then(Commands.literal("status")
                        .executes(ctx -> status(ctx.getSource()))));
    }

    private static int setEnabled(CommandSourceStack source, boolean value) {
        GravityFlipConfig.enabled = value;
        GravityFlip.LOGGER.info("[Gravity Flip] 被 {} 设为 {}", source.getTextName(), value ? "启用" : "禁用");
        source.sendSuccess(() -> Component.literal(value
                        ? "[重力翻转] 已启用：从现在起，任何人完成成就的任意一个进度都会翻转全体重力"
                        : "[重力翻转] 已禁用：不再响应成就")
                .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.YELLOW), true);
        return 1;
    }

    /** 手动翻一次，方便不开成就就能看效果。 */
    private static int trigger(CommandSourceStack source) {
        Direction direction = GravityFlipTrigger.pickDirection();
        int affected = GravityFlipTrigger.applyToEveryone(source.getServer(), direction);
        source.sendSuccess(() -> Component.literal("[重力翻转] 手动触发：把 " + affected
                        + " 名玩家的重力变成了 " + GravityFlipTrigger.directionName(direction))
                .withStyle(ChatFormatting.AQUA), true);
        return affected > 0 ? 1 : 0;
    }

    private static int status(CommandSourceStack source) {
        boolean on = GravityFlipConfig.enabled;
        source.sendSuccess(() -> Component.literal("===== 重力翻转 =====").withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("状态：" + (on ? "已启用" : "未启用（默认）"))
                .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        if (!on) {
            source.sendSuccess(() -> Component.literal(
                            "用 /gravityflip on 启用；/gravityflip trigger 可以手动翻一次看看效果")
                    .withStyle(ChatFormatting.GRAY), false);
        }
        source.sendSuccess(() -> Component.literal("已触发过的成就进度：" + GravityFlipTrigger.triggeredCount() + " 个")
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("聊天播报：" + (GravityFlipConfig.showMessage ? "开" : "关"))
                .withStyle(ChatFormatting.GRAY), false);
        return 1;
    }
}
