package codes.nasty.enchantforge.block;

import codes.nasty.enchantforge.menu.EnchantmentForgeMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class EnchantmentForgeBlock extends Block {
	public static final MapCodec<EnchantmentForgeBlock> CODEC = simpleCodec(EnchantmentForgeBlock::new);
	private static final Component CONTAINER_TITLE = Component.translatable("container.enchantforge.enchantment_forge");

	public EnchantmentForgeBlock(Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<EnchantmentForgeBlock> codec() {
		return CODEC;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide()) {
			player.openMenu(state.getMenuProvider(level, pos));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
		return new SimpleMenuProvider(
			(containerId, inventory, player) -> new EnchantmentForgeMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)),
			CONTAINER_TITLE
		);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(3) != 0) {
			return;
		}
		// Enchant glyphs fly from the offset back to the spawn point, so they gather on the top face.
		level.addParticle(
			ParticleTypes.ENCHANT,
			pos.getX() + 0.5,
			pos.getY() + 1.1,
			pos.getZ() + 0.5,
			random.nextFloat() * 2.0F - 1.0F,
			random.nextFloat() * 0.8F + 0.2F,
			random.nextFloat() * 2.0F - 1.0F
		);
	}
}
