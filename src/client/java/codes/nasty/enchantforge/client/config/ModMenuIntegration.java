package codes.nasty.enchantforge.client.config;

import codes.nasty.enchantforge.EnchantForge;
import codes.nasty.enchantforge.ForgeConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.gui.screens.Screen;

/** The config button in Mod Menu's mod list. Mod Menu is optional; without it this class is never loaded. */
public class ModMenuIntegration implements ModMenuApi {
	/** Keys of {@code chancePerTargetLevel}: a target level written the way the forge looks it up ("20", not "020"). */
	private static final Predicate<String> TARGET_LEVEL = key -> key.matches("[1-9][0-9]{0,2}") && Integer.parseInt(key) <= 255;

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return ModMenuIntegration::createScreen;
	}

	public static Screen createScreen(Screen parent) {
		return new ConfigScreen<>(EnchantForge.MOD_ID, parent, ForgeConfig.get().copy(), new ForgeConfig(), ForgeConfig::update)
			.section("levels")
			.integer("maxLevel", 1, 255, c -> c.maxLevel, (c, v) -> c.maxLevel = v)
			.map("maxLevelOverrides", ConfigScreen.ID, text -> level(text, 1, 255), String::valueOf,
				c -> c.maxLevelOverrides, (c, v) -> c.maxLevelOverrides = v)
			.toggle("excludeSingleLevelEnchantments", c -> c.excludeSingleLevelEnchantments, (c, v) -> c.excludeSingleLevelEnchantments = v)
			.toggle("excludeCurses", c -> c.excludeCurses, (c, v) -> c.excludeCurses = v)
			.list("excludedEnchantments", ConfigScreen.ID, c -> c.excludedEnchantments, (c, v) -> c.excludedEnchantments = new ArrayList<>(v))
			.list("excludedItems", ConfigScreen.ID, c -> c.excludedItems, (c, v) -> c.excludedItems = new ArrayList<>(v))
			.toggle("allowEnchantedBooks", c -> c.allowEnchantedBooks, (c, v) -> c.allowEnchantedBooks = v)
			.section("chances")
			.percent("baseChance", c -> c.baseChance, (c, v) -> c.baseChance = v)
			.percent("chanceMultiplierPerLevel", c -> c.chanceMultiplierPerLevel, (c, v) -> c.chanceMultiplierPerLevel = v)
			.percent("minChance", c -> c.minChance, (c, v) -> c.minChance = v)
			.map("chancePerTargetLevel", TARGET_LEVEL, ModMenuIntegration::parsePercent, ModMenuIntegration::formatPercent,
				c -> c.chancePerTargetLevel, (c, v) -> c.chancePerTargetLevel = v)
			.percent("failDowngradeChance", c -> c.failDowngradeChance, (c, v) -> c.failDowngradeChance = v)
			.section("charms")
			.percent("charmChanceBonus", c -> c.charmChanceBonus, (c, v) -> c.charmChanceBonus = v)
			.integer("charmMaxPerAttempt", 0, 64, c -> c.charmMaxPerAttempt, (c, v) -> c.charmMaxPerAttempt = v)
			.percent("charmMaxChance", c -> c.charmMaxChance, (c, v) -> c.charmMaxChance = v)
			.section("costs")
			.decimal("materialCostBase", 0.0, 10_000.0, c -> c.materialCostBase, (c, v) -> c.materialCostBase = v)
			.decimal("materialCostPerLevel", 0.0, 10_000.0, c -> c.materialCostPerLevel, (c, v) -> c.materialCostPerLevel = v)
			.map("materialCostMultipliers", ConfigScreen.ID, ModMenuIntegration::parseFactor, ConfigScreen::formatDecimal,
				c -> c.materialCostMultipliers, (c, v) -> c.materialCostMultipliers = v)
			.integer("xpCostBase", 0, Integer.MAX_VALUE, c -> c.xpCostBase, (c, v) -> c.xpCostBase = v)
			.integer("xpCostPerLevel", 0, Integer.MAX_VALUE, c -> c.xpCostPerLevel, (c, v) -> c.xpCostPerLevel = v)
			.toggle("creativeIgnoresCosts", c -> c.creativeIgnoresCosts, (c, v) -> c.creativeIgnoresCosts = v)
			.section("materials")
			.map("materialOverrides", ConfigScreen.ID, text -> ConfigScreen.ID_OR_TAG.test(text) ? text : null, Function.identity(),
				c -> c.materialOverrides, (c, v) -> c.materialOverrides = new LinkedHashMap<>(v))
			.text("fallbackMaterial", 256, c -> c.fallbackMaterial, (c, v) -> c.fallbackMaterial = v);
	}

	private static Integer level(String text, int min, int max) {
		try {
			int value = Integer.parseInt(text);
			return value >= min && value <= max ? value : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** A chance typed as percentage (7 or 7.5), stored as fraction (0.07). */
	private static Double parsePercent(String text) {
		Double percent = ConfigScreen.parseDecimal(text.endsWith("%") ? text.substring(0, text.length() - 1) : text);
		return percent != null && percent >= 0.0 && percent <= 100.0 ? percent / 100.0 : null;
	}

	/** Exact decimal shift, so 0.07 shows as "7" and not as "7.000000000000001". */
	private static String formatPercent(double fraction) {
		return new BigDecimal(Double.toString(fraction)).movePointRight(2).stripTrailingZeros().toPlainString();
	}

	private static Double parseFactor(String text) {
		Double factor = ConfigScreen.parseDecimal(text);
		return factor != null && factor >= 0.0 && factor <= 1000.0 ? factor : null;
	}
}
