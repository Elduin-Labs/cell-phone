package com.elduin.cell_phone;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * The phone. Everything a phone does — ringing, talking, the call list — is on the screen in
 * front of you, so right-clicking only matters on the client.
 */
public class CellPhoneItem extends Item {

	/**
	 * Set by the client entrypoint. Kept as a hook so this class never names a client-only
	 * class, which would crash a dedicated server.
	 */
	public static Consumer<Player> openOnClient = player -> {
	};

	public CellPhoneItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			openOnClient.accept(player);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
	                            Consumer<Component> lines, TooltipFlag flag) {
		lines.accept(Component.translatable("item.cell_phone.cell_phone.tip").withStyle(ChatFormatting.GRAY));
	}
}
