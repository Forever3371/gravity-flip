package gravity_flip.item;

import gravity_flip.GravityChangerMod;
import gravity_flip.api.GravityChangerAPI;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GravityChangerItem extends Item {
    public static final Item gravity_flip_DOWN = new GravityChangerItem(new Properties().stacksTo(1), Direction.DOWN);
    public static final Item gravity_flip_UP = new GravityChangerItem(new Properties().stacksTo(1), Direction.UP);
    public static final Item gravity_flip_NORTH = new GravityChangerItem(new Properties().stacksTo(1), Direction.NORTH);
    public static final Item gravity_flip_SOUTH = new GravityChangerItem(new Properties().stacksTo(1), Direction.SOUTH);
    public static final Item gravity_flip_WEST = new GravityChangerItem(new Properties().stacksTo(1), Direction.WEST);
    public static final Item gravity_flip_EAST = new GravityChangerItem(new Properties().stacksTo(1), Direction.EAST);
    
    public final Direction gravityDirection;
    
    public GravityChangerItem(Properties settings, Direction _gravityDirection) {
        super(settings);
        gravityDirection = _gravityDirection;
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        if (!world.isClientSide())
            GravityChangerAPI.setBaseGravityDirection(user, gravityDirection);
        return InteractionResultHolder.success(user.getItemInHand(hand));
    }
    
    @Environment(EnvType.CLIENT)
    @Override
    public void appendHoverText(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, tooltipContext, tooltip, tooltipFlag);
        tooltip.add(
            Component.translatable("gravity_flip.gravity_flip.tooltip.0")
                .withStyle(ChatFormatting.GRAY)
        );
        tooltip.add(
            Component.translatable("gravity_flip.gravity_flip.tooltip.1")
                .withStyle(ChatFormatting.GRAY)
        );
    }
    
    public static void init() {
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(GravityChangerMod.NAMESPACE, "gravity_flip_down"), gravity_flip_DOWN);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(GravityChangerMod.NAMESPACE, "gravity_flip_up"), gravity_flip_UP);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(GravityChangerMod.NAMESPACE, "gravity_flip_north"), gravity_flip_NORTH);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(GravityChangerMod.NAMESPACE, "gravity_flip_south"), gravity_flip_SOUTH);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(GravityChangerMod.NAMESPACE, "gravity_flip_west"), gravity_flip_WEST);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(GravityChangerMod.NAMESPACE, "gravity_flip_east"), gravity_flip_EAST);
    }
    
}
