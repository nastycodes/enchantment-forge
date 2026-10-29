package codes.nasty.enchantforge.menu;

import codes.nasty.enchantforge.EnchantForge;
import codes.nasty.enchantforge.ForgeConfig;
import codes.nasty.enchantforge.ForgeRules;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

/**
 * Three input slots (enchanted item, material, Luck Charms) plus the player inventory. The client picks an
 * enchantment by its index in {@link #entries(ItemStack)} and sends it as a menu button click;
 * everything the GUI needs to show (chances, costs, limits) is computed here and synced through
 * {@link ContainerData}, so the config only has to exist on the server.
 */
public class EnchantmentForgeMenu extends AbstractContainerMenu {
	public static final int ITEM_SLOT = 0;
	public static final int MATERIAL_SLOT = 1;
	public static final int CHARM_SLOT = 2;
	public static final int INV_START = 3;
	public static final int HOTBAR_START = 30;
	private static final int HOTBAR_END = 39;

	public static final int MAX_ENTRIES = 16;

	public static final int STATUS_EMPTY = 0;
	public static final int STATUS_READY = 1;
	public static final int STATUS_NOT_ALLOWED = 2;
	public static final int STATUS_NO_ENCHANTMENTS = 3;

	public static final int STATE_UNKNOWN = 0;
	public static final int STATE_UPGRADEABLE = 1;
	public static final int STATE_MAXED = 2;
	public static final int STATE_EXCLUDED = 3;

	private static final int DATA_STATUS = 0;
	private static final int DATA_MATERIAL = 1;
	private static final int DATA_MATERIAL_OK = 2;
	private static final int DATA_DOWNGRADE_CHANCE = 3;
	private static final int DATA_CREATIVE_FREE = 4;
	private static final int DATA_CHARM_BONUS = 5;
	private static final int DATA_CHARM_MAX = 6;
	private static final int HEADER_SIZE = 7;

	public static final int FIELD_STATE = 0;
	public static final int FIELD_CHANCE = 1;
	public static final int FIELD_MATERIAL_COST = 2;
	public static final int FIELD_XP_COST = 3;
	public static final int FIELD_MAX_LEVEL = 4;
	/** Chance without Luck Charms; {@link #FIELD_CHANCE} includes their bonus. */
	public static final int FIELD_BASE_CHANCE = 5;
	public static final int FIELD_CHARMS_USED = 6;
	private static final int FIELD_COUNT = 7;

	/** Chances travel as basis points because container data is synced as shorts. */
	public static final int CHANCE_SCALE = 10_000;

	public record Entry(Holder<Enchantment> enchantment, int level) {
	}

	private final ContainerLevelAccess access;
	private final Player player;
	private final ContainerData data = new SimpleContainerData(HEADER_SIZE + MAX_ENTRIES * FIELD_COUNT);
	private final Container forgeSlots = new SimpleContainer(3) {
		@Override
		public void setChanged() {
			super.setChanged();
			EnchantmentForgeMenu.this.slotsChanged(this);
		}
	};

	public EnchantmentForgeMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, ContainerLevelAccess.NULL);
	}

	public EnchantmentForgeMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(EnchantForge.ENCHANTMENT_FORGE_MENU, containerId);
		this.access = access;
		this.player = inventory.player;

		this.addSlot(new Slot(this.forgeSlots, ITEM_SLOT, 17, 24) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return hasEnchantments(stack);
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});
		this.addSlot(new Slot(this.forgeSlots, MATERIAL_SLOT, 17, 60));
		this.addSlot(new Slot(this.forgeSlots, CHARM_SLOT, 17, 98) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(EnchantForge.LUCK_CHARM);
			}
		});
		this.addStandardInventorySlots(inventory, 8, 140);
		this.addDataSlots(this.data);

		if (!this.player.level().isClientSide()) {
			this.updateData();
		}
	}

	public static boolean hasEnchantments(ItemStack stack) {
		return !EnchantmentHelper.getEnchantmentsForCrafting(stack).isEmpty();
	}

	/** Enchantments of the stack in a stable order, so client and server agree on the indices. */
	public static List<Entry> entries(ItemStack stack) {
		List<Entry> entries = new ArrayList<>();
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
			entries.add(new Entry(entry.getKey(), entry.getIntValue()));
		}
		entries.sort(Comparator.comparing(entry -> entry.enchantment().getRegisteredName()));
		return entries;
	}

	public static MutableComponent enchantmentName(Holder<Enchantment> enchantment, int level) {
		MutableComponent name = enchantment.value().description().copy();
		if (level != 1 || enchantment.value().getMaxLevel() != 1) {
			name.append(CommonComponents.SPACE).append(levelName(level));
		}
		return name;
	}

	public static Component levelName(int level) {
		return Component.translatable("enchantment.level." + level);
	}

	@Override
	public void slotsChanged(Container container) {
		super.slotsChanged(container);
		if (container == this.forgeSlots && !this.player.level().isClientSide()) {
			this.updateData();
		}
	}

	private void updateData() {
		for (int i = 0; i < this.data.getCount(); i++) {
			this.data.set(i, 0);
		}
		ForgeConfig config = ForgeConfig.get();
		this.data.set(DATA_DOWNGRADE_CHANCE, toBasisPoints(config.failDowngradeChance));
		this.data.set(DATA_CREATIVE_FREE, config.creativeIgnoresCosts ? 1 : 0);
		this.data.set(DATA_CHARM_BONUS, toBasisPoints(config.charmChanceBonus));
		this.data.set(DATA_CHARM_MAX, config.charmMaxPerAttempt);

		ItemStack stack = this.forgeSlots.getItem(ITEM_SLOT);
		if (stack.isEmpty()) {
			this.data.set(DATA_STATUS, STATUS_EMPTY);
			return;
		}
		ForgeRules.MaterialRequirement material = ForgeRules.isItemAllowed(stack) ? ForgeRules.material(stack) : null;
		if (material == null) {
			this.data.set(DATA_STATUS, STATUS_NOT_ALLOWED);
			return;
		}
		List<Entry> entries = entries(stack);
		if (entries.isEmpty()) {
			this.data.set(DATA_STATUS, STATUS_NO_ENCHANTMENTS);
			return;
		}

		this.data.set(DATA_STATUS, STATUS_READY);
		this.data.set(DATA_MATERIAL, BuiltInRegistries.ITEM.getId(material.display()) + 1);
		this.data.set(DATA_MATERIAL_OK, material.matches(this.forgeSlots.getItem(MATERIAL_SLOT)) ? 1 : 0);

		for (int i = 0; i < Math.min(entries.size(), MAX_ENTRIES); i++) {
			Entry entry = entries.get(i);
			int maxLevel = ForgeRules.maxLevel(entry.enchantment());
			int state;
			if (ForgeRules.isExcluded(entry.enchantment())) {
				state = STATE_EXCLUDED;
			} else if (entry.level() >= maxLevel) {
				state = STATE_MAXED;
			} else {
				state = STATE_UPGRADEABLE;
			}
			this.setField(i, FIELD_STATE, state);
			this.setField(i, FIELD_MAX_LEVEL, maxLevel);
			if (state == STATE_UPGRADEABLE) {
				double baseChance = ForgeRules.chance(entry.level() + 1);
				int charms = ForgeRules.charmsUsed(baseChance, this.availableCharms());
				this.setField(i, FIELD_BASE_CHANCE, toBasisPoints(baseChance));
				this.setField(i, FIELD_CHARMS_USED, charms);
				this.setField(i, FIELD_CHANCE, toBasisPoints(ForgeRules.chanceWithCharms(baseChance, charms)));
				this.setField(i, FIELD_MATERIAL_COST, ForgeRules.materialCost(entry.level(), material.display()));
				this.setField(i, FIELD_XP_COST, Math.min(Short.MAX_VALUE, ForgeRules.xpCost(entry.level())));
			}
		}
	}

	private int availableCharms() {
		ItemStack charms = this.forgeSlots.getItem(CHARM_SLOT);
		return charms.is(EnchantForge.LUCK_CHARM) ? charms.getCount() : 0;
	}

	private static int toBasisPoints(double chance) {
		return (int) Math.round(chance * CHANCE_SCALE);
	}

	private void setField(int entry, int field, int value) {
		this.data.set(HEADER_SIZE + entry * FIELD_COUNT + field, value);
	}

	public int getField(int entry, int field) {
		return entry < MAX_ENTRIES ? this.data.get(HEADER_SIZE + entry * FIELD_COUNT + field) : 0;
	}

	public int getStatus() {
		return this.data.get(DATA_STATUS);
	}

	public Item getMaterialItem() {
		int id = this.data.get(DATA_MATERIAL);
		return id > 0 ? BuiltInRegistries.ITEM.byId(id - 1) : Items.AIR;
	}

	public boolean isMaterialValid() {
		return this.data.get(DATA_MATERIAL_OK) == 1;
	}

	public int getDowngradeChance() {
		return this.data.get(DATA_DOWNGRADE_CHANCE);
	}

	public boolean costsWaivedFor(Player player) {
		return this.data.get(DATA_CREATIVE_FREE) == 1 && player.hasInfiniteMaterials();
	}

	/** Chance bonus per Luck Charm in basis points. */
	public int getCharmBonus() {
		return this.data.get(DATA_CHARM_BONUS);
	}

	public int getCharmMaxPerAttempt() {
		return this.data.get(DATA_CHARM_MAX);
	}

	public ItemStack getInputItem() {
		return this.forgeSlots.getItem(ITEM_SLOT);
	}

	public ItemStack getMaterialStack() {
		return this.forgeSlots.getItem(MATERIAL_SLOT);
	}

	public ItemStack getCharmStack() {
		return this.forgeSlots.getItem(CHARM_SLOT);
	}

	/** Called on the server with the index of the enchantment the player clicked. */
	@Override
	public boolean clickMenuButton(Player player, int index) {
		ItemStack stack = this.forgeSlots.getItem(ITEM_SLOT);
		if (index < 0 || index >= MAX_ENTRIES || !ForgeRules.isItemAllowed(stack)) {
			return false;
		}
		List<Entry> entries = entries(stack);
		ForgeRules.MaterialRequirement material = ForgeRules.material(stack);
		if (index >= entries.size() || material == null) {
			return false;
		}
		Entry entry = entries.get(index);
		if (ForgeRules.isExcluded(entry.enchantment()) || entry.level() >= ForgeRules.maxLevel(entry.enchantment())) {
			return false;
		}

		ForgeConfig config = ForgeConfig.get();
		int targetLevel = entry.level() + 1;
		double baseChance = ForgeRules.chance(targetLevel);
		int charms = ForgeRules.charmsUsed(baseChance, this.availableCharms());
		if (!(config.creativeIgnoresCosts && player.hasInfiniteMaterials())) {
			ItemStack materialStack = this.forgeSlots.getItem(MATERIAL_SLOT);
			int materialCost = ForgeRules.materialCost(entry.level(), material.display());
			int xpCost = ForgeRules.xpCost(entry.level());
			if (!material.matches(materialStack) || materialStack.getCount() < materialCost || player.experienceLevel < xpCost) {
				return false;
			}
			materialStack.shrink(materialCost);
			this.forgeSlots.getItem(CHARM_SLOT).shrink(charms);
			player.giveExperienceLevels(-xpCost);
		}

		boolean success = player.getRandom().nextDouble() < ForgeRules.chanceWithCharms(baseChance, charms);
		if (success) {
			EnchantmentHelper.updateEnchantments(stack, enchantments -> enchantments.set(entry.enchantment(), targetLevel));
			player.displayClientMessage(Component.translatable(
				"enchantforge.message.success",
				enchantmentName(entry.enchantment(), targetLevel).withStyle(ChatFormatting.AQUA)
			).withStyle(ChatFormatting.GREEN), true);
		} else if (entry.level() > 1 && player.getRandom().nextDouble() < config.failDowngradeChance) {
			int lowerLevel = entry.level() - 1;
			EnchantmentHelper.updateEnchantments(stack, enchantments -> enchantments.set(entry.enchantment(), lowerLevel));
			player.displayClientMessage(Component.translatable(
				"enchantforge.message.downgrade",
				enchantmentName(entry.enchantment(), lowerLevel)
			).withStyle(ChatFormatting.RED), true);
		} else {
			player.displayClientMessage(Component.translatable("enchantforge.message.failure").withStyle(ChatFormatting.RED), true);
		}

		this.access.execute((level, pos) -> playEffects(level, pos, success));
		this.forgeSlots.setChanged();
		return true;
	}

	private static void playEffects(Level level, BlockPos pos, boolean success) {
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 1.1;
		double z = pos.getZ() + 0.5;
		if (success) {
			level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.3F);
			if (level instanceof ServerLevel serverLevel) {
				serverLevel.sendParticles(ParticleTypes.ENCHANT, x, y + 0.3, z, 60, 0.3, 0.3, 0.3, 1.0);
				serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 8, 0.35, 0.15, 0.35, 0.0);
			}
		} else {
			level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 0.9F);
			if (level instanceof ServerLevel serverLevel) {
				serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 10, 0.25, 0.05, 0.25, 0.01);
			}
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(this.access, player, EnchantForge.ENCHANTMENT_FORGE);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.access.execute((level, pos) -> this.clearContainer(player, this.forgeSlots));
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();

		if (index < INV_START) {
			if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (stack.is(EnchantForge.LUCK_CHARM)) {
			if (!this.moveItemStackTo(stack, CHARM_SLOT, CHARM_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (hasEnchantments(stack)) {
			if (!this.moveItemStackTo(stack, ITEM_SLOT, ITEM_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (this.moveItemStackTo(stack, MATERIAL_SLOT, MATERIAL_SLOT + 1, false)) {
			// moved into the material slot
		} else if (index < HOTBAR_START) {
			if (!this.moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.moveItemStackTo(stack, INV_START, HOTBAR_START, false)) {
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, stack);
		return original;
	}
}
