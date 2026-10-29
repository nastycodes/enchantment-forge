package codes.nasty.enchantforge;

import codes.nasty.enchantforge.block.EnchantmentForgeBlock;
import codes.nasty.enchantforge.menu.EnchantmentForgeMenu;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnchantForge implements ModInitializer {
	public static final String MOD_ID = "enchantforge";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Identifier FORGE_ID = id("enchantment_forge");
	private static final ResourceKey<Block> FORGE_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, FORGE_ID);
	private static final ResourceKey<Item> FORGE_ITEM_KEY = ResourceKey.create(Registries.ITEM, FORGE_ID);

	public static final Block ENCHANTMENT_FORGE = Registry.register(
		BuiltInRegistries.BLOCK,
		FORGE_BLOCK_KEY,
		new EnchantmentForgeBlock(BlockBehaviour.Properties.of()
			.setId(FORGE_BLOCK_KEY)
			.mapColor(MapColor.COLOR_PURPLE)
			.requiresCorrectToolForDrops()
			.strength(5.0F, 1200.0F)
			.sound(SoundType.METAL)
			.lightLevel(state -> 7))
	);

	public static final Item ENCHANTMENT_FORGE_ITEM = Registry.register(
		BuiltInRegistries.ITEM,
		FORGE_ITEM_KEY,
		new BlockItem(ENCHANTMENT_FORGE, new Item.Properties().setId(FORGE_ITEM_KEY).useBlockDescriptionPrefix())
	);

	private static final ResourceKey<Item> LUCK_CHARM_KEY = ResourceKey.create(Registries.ITEM, id("luck_charm"));

	/** Raises the forge's success chance; the more of them sit in the charm slot, the higher it goes. */
	public static final Item LUCK_CHARM = Registry.register(
		BuiltInRegistries.ITEM,
		LUCK_CHARM_KEY,
		new Item(new Item.Properties()
			.setId(LUCK_CHARM_KEY)
			.rarity(Rarity.UNCOMMON)
			.component(DataComponents.LORE, new ItemLore(List.of(
				Component.translatable("item.enchantforge.luck_charm.desc").withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false))
			))))
	);

	public static final MenuType<EnchantmentForgeMenu> ENCHANTMENT_FORGE_MENU = Registry.register(
		BuiltInRegistries.MENU,
		FORGE_ID,
		new MenuType<>(EnchantmentForgeMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ForgeConfig.load();

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
			.register(entries -> entries.addAfter(Items.ENCHANTING_TABLE, ENCHANTMENT_FORGE_ITEM));
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
			.register(entries -> entries.accept(LUCK_CHARM));

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
			Commands.literal(MOD_ID)
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("reload").executes(context -> {
					try {
						ForgeConfig.reload();
					} catch (Exception e) {
						context.getSource().sendFailure(Component.translatable("enchantforge.command.reload_failed", e.getMessage()));
						return 0;
					}
					context.getSource().sendSuccess(() -> Component.translatable("enchantforge.command.reloaded"), true);
					return 1;
				}))
		));
	}
}
