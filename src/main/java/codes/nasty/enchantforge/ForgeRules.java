package codes.nasty.enchantforge;

import java.util.Iterator;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Repairable;
import org.jspecify.annotations.Nullable;

/** Server-side rules of the forge, all driven by {@link ForgeConfig}. */
public final class ForgeRules {
	private ForgeRules() {
	}

	/** What has to go into the material slot. {@code display} is the item shown in the GUI. */
	public record MaterialRequirement(Item display, Predicate<ItemStack> matcher) {
		public boolean matches(ItemStack stack) {
			return !stack.isEmpty() && this.matcher.test(stack);
		}
	}

	public static boolean isItemAllowed(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		ForgeConfig config = ForgeConfig.get();
		if (config.excludedItems.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
			return false;
		}
		return !stack.is(Items.ENCHANTED_BOOK) || config.allowEnchantedBooks;
	}

	public static boolean isExcluded(Holder<Enchantment> enchantment) {
		ForgeConfig config = ForgeConfig.get();
		String id = enchantment.getRegisteredName();
		if (config.excludedEnchantments.contains(id)) {
			return true;
		}
		if (config.excludeCurses && enchantment.is(EnchantmentTags.CURSE)) {
			return true;
		}
		return config.excludeSingleLevelEnchantments
			&& enchantment.value().getMaxLevel() <= 1
			&& !config.maxLevelOverrides.containsKey(id);
	}

	public static int maxLevel(Holder<Enchantment> enchantment) {
		ForgeConfig config = ForgeConfig.get();
		return config.maxLevelOverrides.getOrDefault(enchantment.getRegisteredName(), config.maxLevel);
	}

	/** Success chance (0-1) for raising an enchantment to {@code targetLevel}; lower the higher the target. */
	public static double chance(int targetLevel) {
		ForgeConfig config = ForgeConfig.get();
		Double fixed = config.chancePerTargetLevel.get(Integer.toString(targetLevel));
		if (fixed != null) {
			return fixed;
		}
		double chance = config.baseChance * Math.pow(config.chanceMultiplierPerLevel, Math.max(0, targetLevel - 2));
		return Mth.clamp(chance, Math.min(config.minChance, config.baseChance), 1.0);
	}

	/** How many of the {@code available} Luck Charms an attempt with {@code baseChance} uses: only as many as raise the chance. */
	public static int charmsUsed(double baseChance, int available) {
		ForgeConfig config = ForgeConfig.get();
		double headroom = config.charmMaxChance - baseChance;
		if (config.charmChanceBonus <= 0.0 || headroom <= 0.0 || available <= 0) {
			return 0;
		}
		// The epsilon keeps e.g. 0.05 / 0.01 = 5.000000000000004 from rounding up to 6.
		int needed = (int) Math.ceil(headroom / config.charmChanceBonus - 1.0E-9);
		return Math.min(needed, Math.min(available, config.charmMaxPerAttempt));
	}

	public static double chanceWithCharms(double baseChance, int charms) {
		if (charms <= 0) {
			return baseChance;
		}
		ForgeConfig config = ForgeConfig.get();
		return Math.max(baseChance, Math.min(config.charmMaxChance, baseChance + charms * config.charmChanceBonus));
	}

	public static int materialCost(int currentLevel, Item material) {
		ForgeConfig config = ForgeConfig.get();
		double factor = config.materialCostMultipliers.getOrDefault(BuiltInRegistries.ITEM.getKey(material).toString(), 1.0);
		int cost = (int) Math.ceil((config.materialCostBase + config.materialCostPerLevel * currentLevel) * factor);
		return Mth.clamp(cost, 1, Math.max(1, material.getDefaultMaxStackSize()));
	}

	public static int xpCost(int currentLevel) {
		ForgeConfig config = ForgeConfig.get();
		return config.xpCostBase + config.xpCostPerLevel * currentLevel;
	}

	/**
	 * The material is taken from the item itself: config override first, then the vanilla repair
	 * material (diamond gear needs diamonds, netherite gear netherite ingots, ...), then the fallback.
	 */
	public static @Nullable MaterialRequirement material(ItemStack stack) {
		ForgeConfig config = ForgeConfig.get();
		String override = config.materialOverrides.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
		if (override != null) {
			return parseMaterial(override);
		}

		Repairable repairable = stack.get(DataComponents.REPAIRABLE);
		if (repairable != null && repairable.items().size() > 0) {
			HolderSet<Item> items = repairable.items();
			return new MaterialRequirement(items.get(0).value(), repairable::isValidRepairItem);
		}

		return parseMaterial(config.fallbackMaterial);
	}

	private static @Nullable MaterialRequirement parseMaterial(String spec) {
		if (spec.startsWith("#")) {
			Identifier tagId = Identifier.tryParse(spec.substring(1));
			if (tagId == null) {
				return null;
			}
			TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
			Iterator<Holder<Item>> members = BuiltInRegistries.ITEM.getTagOrEmpty(tag).iterator();
			if (!members.hasNext()) {
				return null;
			}
			return new MaterialRequirement(members.next().value(), stack -> stack.is(tag));
		}

		Identifier itemId = Identifier.tryParse(spec);
		if (itemId == null) {
			return null;
		}
		return BuiltInRegistries.ITEM.getOptional(itemId)
			.filter(item -> item != Items.AIR)
			.map(item -> new MaterialRequirement(item, stack -> stack.is(item)))
			.orElse(null);
	}
}
