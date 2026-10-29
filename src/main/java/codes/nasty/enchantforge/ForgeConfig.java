package codes.nasty.enchantforge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Server-side settings, stored in {@code config/enchantforge.json}.
 * Unknown keys are ignored and missing keys fall back to the defaults below,
 * so the file is rewritten on every successful load to pick up new options.
 */
public final class ForgeConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve(EnchantForge.MOD_ID + ".json");
	private static ForgeConfig instance = new ForgeConfig();

	/** Highest level any enchantment can be forged to (1-255). */
	public int maxLevel = 20;
	/** Per-enchantment caps that replace {@link #maxLevel}. Listing a single-level enchantment here also makes it upgradeable. */
	public Map<String, Integer> maxLevelOverrides = new LinkedHashMap<>(Map.of(
		// Lure above V makes fish stop biting entirely.
		"minecraft:lure", 5
	));
	/** Skip enchantments whose vanilla maximum is I (Silk Touch, Mending, Infinity, Multishot, ...). */
	public boolean excludeSingleLevelEnchantments = true;
	public boolean excludeCurses = true;
	/** Enchantments that can never be upgraded, regardless of the other settings. */
	public List<String> excludedEnchantments = new ArrayList<>(List.of(
		"minecraft:silk_touch",
		"minecraft:mending",
		"minecraft:infinity"
	));
	/** Item ids that are rejected by the forge. */
	public List<String> excludedItems = new ArrayList<>();
	public boolean allowEnchantedBooks = false;

	/** Chance for the first upgrade step (level I to II). */
	public double baseChance = 0.95;
	/** Each further target level multiplies the chance by this factor. */
	public double chanceMultiplierPerLevel = 0.85;
	/** The chance never drops below this value. */
	public double minChance = 0.02;
	/** Fixed chances for specific target levels, e.g. {"20": 0.01}. They win over the formula. */
	public Map<String, Double> chancePerTargetLevel = new LinkedHashMap<>();
	/** Chance that a failed attempt also takes one level away. */
	public double failDowngradeChance = 0.0;

	/** Each Luck Charm in the charm slot adds this much success chance (0.01 = +1 percentage point). */
	public double charmChanceBonus = 0.01;
	/** Most Luck Charms a single attempt uses. An attempt never uses more than it needs to reach {@link #charmMaxChance}. */
	public int charmMaxPerAttempt = 64;
	/** Luck Charms never push the chance above this value. */
	public double charmMaxChance = 1.0;

	/** Material cost = ceil((materialCostBase + materialCostPerLevel * currentLevel) * multiplier), capped at the material's stack size. */
	public double materialCostBase = 1.0;
	public double materialCostPerLevel = 1.0;
	/** Cost multipliers per material item, so rare materials can cost less. */
	public Map<String, Double> materialCostMultipliers = new LinkedHashMap<>(Map.of(
		"minecraft:netherite_ingot", 0.25
	));
	/** Experience levels = xpCostBase + xpCostPerLevel * currentLevel. */
	public int xpCostBase = 1;
	public int xpCostPerLevel = 1;
	public boolean creativeIgnoresCosts = true;

	/**
	 * Upgrade material for specific items. Values are item ids or item tags ("#minecraft:planks").
	 * Items not listed here use their vanilla repair material (diamond sword: diamonds, elytra: phantom membranes, ...).
	 */
	public Map<String, String> materialOverrides = new LinkedHashMap<>();
	{
		materialOverrides.put("minecraft:bow", "minecraft:string");
		materialOverrides.put("minecraft:crossbow", "minecraft:iron_ingot");
		materialOverrides.put("minecraft:trident", "minecraft:prismarine_shard");
		materialOverrides.put("minecraft:fishing_rod", "minecraft:string");
		materialOverrides.put("minecraft:shears", "minecraft:iron_ingot");
		materialOverrides.put("minecraft:flint_and_steel", "minecraft:iron_ingot");
		materialOverrides.put("minecraft:brush", "minecraft:copper_ingot");
		materialOverrides.put("minecraft:carrot_on_a_stick", "minecraft:carrot");
		materialOverrides.put("minecraft:warped_fungus_on_a_stick", "minecraft:warped_fungus");
		materialOverrides.put("minecraft:enchanted_book", "minecraft:lapis_lazuli");
	}
	/** Used for enchanted items that have neither an override nor a vanilla repair material. */
	public String fallbackMaterial = "minecraft:diamond";

	public static ForgeConfig get() {
		return instance;
	}

	/** Loads the config at startup; a broken file is reported and the defaults are used instead. */
	public static void load() {
		try {
			reload();
		} catch (IOException | JsonParseException e) {
			EnchantForge.LOGGER.error("Could not read {}, using default settings", PATH, e);
			instance = new ForgeConfig();
		}
	}

	public static void reload() throws IOException {
		ForgeConfig config = new ForgeConfig();
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
				ForgeConfig parsed = GSON.fromJson(reader, ForgeConfig.class);
				if (parsed != null) {
					config = parsed;
				}
			}
		}
		config.sanitize();
		instance = config;
		save(config);
	}

	/** Activates settings changed in the config screen and writes them to the file. */
	public static void update(ForgeConfig config) {
		config.sanitize();
		instance = config;
		save(config);
	}

	/** A deep copy, for the config screen to change. */
	public ForgeConfig copy() {
		return GSON.fromJson(GSON.toJsonTree(this), ForgeConfig.class);
	}

	private static void save(ForgeConfig config) {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException e) {
			EnchantForge.LOGGER.error("Could not write {}", PATH, e);
		}
	}

	private void sanitize() {
		maxLevel = clamp(maxLevel, 1, 255);
		if (maxLevelOverrides == null) maxLevelOverrides = new LinkedHashMap<>();
		maxLevelOverrides.replaceAll((id, level) -> clamp(level == null ? maxLevel : level, 1, 255));
		if (excludedEnchantments == null) excludedEnchantments = new ArrayList<>();
		if (excludedItems == null) excludedItems = new ArrayList<>();
		baseChance = clamp(baseChance, 0.0, 1.0);
		chanceMultiplierPerLevel = clamp(chanceMultiplierPerLevel, 0.0, 1.0);
		minChance = clamp(minChance, 0.0, 1.0);
		if (chancePerTargetLevel == null) chancePerTargetLevel = new LinkedHashMap<>();
		chancePerTargetLevel.replaceAll((level, chance) -> clamp(chance == null ? 0.0 : chance, 0.0, 1.0));
		failDowngradeChance = clamp(failDowngradeChance, 0.0, 1.0);
		charmChanceBonus = clamp(charmChanceBonus, 0.0, 1.0);
		charmMaxPerAttempt = clamp(charmMaxPerAttempt, 0, 64);
		charmMaxChance = clamp(charmMaxChance, 0.0, 1.0);
		materialCostBase = Math.max(0.0, materialCostBase);
		materialCostPerLevel = Math.max(0.0, materialCostPerLevel);
		if (materialCostMultipliers == null) materialCostMultipliers = new LinkedHashMap<>();
		materialCostMultipliers.replaceAll((id, factor) -> Math.max(0.0, factor == null ? 1.0 : factor));
		xpCostBase = Math.max(0, xpCostBase);
		xpCostPerLevel = Math.max(0, xpCostPerLevel);
		if (materialOverrides == null) materialOverrides = new LinkedHashMap<>();
		if (fallbackMaterial == null) fallbackMaterial = "";
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static double clamp(double value, double min, double max) {
		return Double.isNaN(value) ? min : Math.max(min, Math.min(max, value));
	}
}
