package codes.nasty.enchantforge.test;

import codes.nasty.enchantforge.EnchantForge;
import codes.nasty.enchantforge.ForgeConfig;
import codes.nasty.enchantforge.client.EnchantmentForgeScreen;
import codes.nasty.enchantforge.client.config.ConfigScreen;
import codes.nasty.enchantforge.menu.EnchantmentForgeMenu;
import com.mojang.blaze3d.platform.InputConstants;
import com.terraformersmc.modmenu.ModMenu;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;

/**
 * End-to-end check of the forge in a real client (English, 1920x1080): opens the GUI,
 * inserts a sword, diamonds and Luck Charms, clicks enchantment rows with the mouse and verifies the results on the
 * server, then changes settings through the config screen behind Mod Menu's config button.
 * Run with {@code gradlew runClientGameTest}; screenshots land in build/run/clientGameTest/screenshots.
 */
public class EnchantForgeClientGameTest implements FabricClientGameTest {
	// Menu slot indices of the first hotbar slots.
	private static final int HOTBAR_0 = EnchantmentForgeMenu.HOTBAR_START;
	private static final int HOTBAR_1 = EnchantmentForgeMenu.HOTBAR_START + 1;
	private static final int HOTBAR_2 = EnchantmentForgeMenu.HOTBAR_START + 2;

	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(1920, 1080);
		context.runOnClient(client -> client.getLanguageManager().setSelected("en_us"));
		context.runOnClient(client -> client.reloadResourcePacks());
		context.waitFor(client -> client.getOverlay() == null);

		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getClientWorld().waitForChunksRender();
			TestServerContext server = singleplayer.getServer();

			server.runOnServer(EnchantForgeClientGameTest::checkData);
			// Unlock what the diamonds would unlock, so no toast or chat line covers the screenshots.
			server.runCommand("recipe give @a *");
			server.runCommand("advancement grant @a only minecraft:story/mine_diamond");
			context.waitTicks(5);
			context.runOnClient(client -> {
				client.getToastManager().clear();
				client.gui.getChat().clearMessages(false);
			});
			server.runOnServer(EnchantForgeClientGameTest::prepare);
			context.waitForScreen(EnchantmentForgeScreen.class);
			context.waitTicks(5);
			context.takeScreenshot("enchantforge_01_empty");

			quickMove(context, HOTBAR_0);
			context.waitTicks(5);
			hoverGui(context, 17 + 8, 98 + 8);
			context.waitTicks(3);
			context.takeScreenshot("enchantforge_02_sword_ghosts_charm_tooltip");

			quickMove(context, HOTBAR_1);
			context.waitTicks(5);
			int materialBefore = server.computeOnServer(s -> forgeMenu(s).getMaterialStack().getCount());
			assertEquals(64, materialBefore, "diamonds in the material slot");

			// Rows are sorted by enchantment id, so row 0 is Fire Aspect II.
			hoverRow(context, 0);
			context.waitTicks(3);
			context.takeScreenshot("enchantforge_03_tooltip");

			int clicks = 0;
			while (clicks < 15 && server.computeOnServer(s -> levelOf(s, Enchantments.FIRE_ASPECT)) < 3) {
				context.getInput().pressMouse(0);
				context.waitTicks(3);
				clicks++;
			}
			int fireAspect = server.computeOnServer(s -> levelOf(s, Enchantments.FIRE_ASPECT));
			int materialAfter = server.computeOnServer(s -> forgeMenu(s).getMaterialStack().getCount());
			int xpAfter = server.computeOnServer(s -> player(s).experienceLevel);
			EnchantForge.LOGGER.info("[gametest] {} click(s): fire aspect {}, diamonds {} -> {}, xp levels {}", clicks, fireAspect, materialBefore, materialAfter, xpAfter);
			assertTrue(fireAspect == 3, "Fire Aspect should reach III, got " + fireAspect);
			assertEquals(64 - clicks * 3, materialAfter, "diamonds after " + clicks + " attempt(s) at 3 each");
			assertEquals(100 - clicks * 3, xpAfter, "xp levels after " + clicks + " attempt(s) at 3 each");
			context.takeScreenshot("enchantforge_04_after_upgrade");

			// Luck Charms: Knockback II -> III has 80.75 % base chance, so 20 of the 32 charms lift it to 100 %.
			quickMove(context, HOTBAR_2);
			context.waitTicks(5);
			assertEquals(32, server.computeOnServer(s -> forgeMenu(s).getCharmStack().getCount()), "charms in the charm slot");
			assertEquals(8075, server.computeOnServer(s -> forgeMenu(s).getField(1, EnchantmentForgeMenu.FIELD_BASE_CHANCE)), "knockback base chance");
			assertEquals(20, server.computeOnServer(s -> forgeMenu(s).getField(1, EnchantmentForgeMenu.FIELD_CHARMS_USED)), "charms used for knockback");
			assertEquals(10000, server.computeOnServer(s -> forgeMenu(s).getField(1, EnchantmentForgeMenu.FIELD_CHANCE)), "knockback chance with charms");
			hoverRow(context, 1);
			context.waitTicks(3);
			context.takeScreenshot("enchantforge_05_charms");
			context.getInput().pressMouse(0);
			context.waitTicks(3);
			assertEquals(3, server.computeOnServer(s -> levelOf(s, Enchantments.KNOCKBACK)), "Knockback after the charm-backed attempt");
			assertEquals(12, server.computeOnServer(s -> forgeMenu(s).getCharmStack().getCount()), "charms left after the attempt");
			materialAfter -= 3;
			assertEquals(materialAfter, server.computeOnServer(s -> forgeMenu(s).getMaterialStack().getCount()), "diamonds after the charm-backed attempt");
			// Fire Aspect III -> IV (68.6 % base) would now take all 12 remaining charms: 80.6 %.
			assertEquals(12, server.computeOnServer(s -> forgeMenu(s).getField(0, EnchantmentForgeMenu.FIELD_CHARMS_USED)), "charms offered to fire aspect");
			assertEquals(8064, server.computeOnServer(s -> forgeMenu(s).getField(0, EnchantmentForgeMenu.FIELD_CHANCE)), "fire aspect chance with 12 charms");

			// Scrolled to the bottom, row 4 is the Curse of Vanishing, which must not react to clicks.
			for (int i = 0; i < 3; i++) {
				context.getInput().scroll(-1);
				context.waitTicks(1);
			}
			context.waitTicks(2);
			context.takeScreenshot("enchantforge_06_scrolled");
			hoverRow(context, 4);
			context.getInput().pressMouse(0);
			context.waitTicks(3);
			assertEquals(1, server.computeOnServer(s -> levelOf(s, Enchantments.VANISHING_CURSE)), "curse level");
			assertEquals(materialAfter, server.computeOnServer(s -> forgeMenu(s).getMaterialStack().getCount()), "diamonds after clicking the curse");

			// Closing the GUI hands the items back.
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitForScreen(null);
			context.waitTicks(5);
			ItemStack returned = server.computeOnServer(s -> player(s).getInventory().getItem(0).copy());
			assertTrue(returned.is(Items.DIAMOND_SWORD), "sword returned to the inventory, got " + returned);
			int returnedLevel = server.computeOnServer(s -> EnchantmentHelper.getItemEnchantmentLevel(
				s.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FIRE_ASPECT), returned));
			assertEquals(3, returnedLevel, "Fire Aspect on the returned sword");

			// Look at the placed block for a screenshot of the model and its particles.
			server.runCommand("time set noon");
			server.runOnServer(s -> {
				ServerPlayer player = player(s);
				player.connection.teleport(player.getX(), player.getY(), player.getZ(), -90.0F, 35.0F);
			});
			context.waitTicks(40);
			context.takeScreenshot("enchantforge_07_block");

			checkConfigScreen(context, server);
			EnchantForge.LOGGER.info("[gametest] all checks passed");
		}
	}

	/** Opens the settings the way Mod Menu's config button does, edits numbers, a percentage and a map, saves. */
	private static void checkConfigScreen(ClientGameTestContext context, TestServerContext server) {
		context.setScreen(() -> ModMenu.getConfigScreen(EnchantForge.MOD_ID, null));
		context.waitForScreen(ConfigScreen.class);
		context.waitTicks(5);
		EditBox baseChance = widget(context, EditBox.class, "config.enchantforge.baseChance");
		assertTrue(context.computeOnClient(client -> baseChance.getValue()).equals("95"), "chances are shown as percentages");
		EditBox maxLevel = widget(context, EditBox.class, "config.enchantforge.maxLevel");
		hoverWidget(context, maxLevel);
		context.takeScreenshot("enchantforge_08_config");

		// Out of range turns the field red and blocks saving.
		context.runOnClient(client -> maxLevel.setValue("300"));
		context.waitTicks(2);
		assertTrue(!widget(context, Button.class, "gui.done").active, "Done is blocked while a field is invalid");
		context.runOnClient(client -> maxLevel.setValue("25"));
		context.runOnClient(client -> baseChance.setValue("90"));
		context.waitTicks(2);
		assertTrue(widget(context, Button.class, "gui.done").active, "Done works again with valid values");

		// Edit buttons in list order: maxLevelOverrides, excludedEnchantments, excludedItems, chancePerTargetLevel, materialCostMultipliers, materialOverrides.
		List<Button> editButtons = widgets(context, Button.class, "config.enchantforge.ui.edit");
		assertEquals(6, editButtons.size(), "list and map options");

		// The material map shows its defaults one per line; look and leave without changes.
		Button materials = editButtons.get(5);
		scrollTo(context, materials);
		clickWidget(context, materials);
		context.waitFor(client -> !(client.screen instanceof ConfigScreen));
		context.waitTicks(3);
		context.takeScreenshot("enchantforge_09_config_map");
		context.clickScreenButton("gui.cancel");
		context.waitForScreen(ConfigScreen.class);

		// A fixed chance for level XX: a bad value is refused, "1,5" (percent, comma) is accepted.
		Button fixedChances = editButtons.get(3);
		scrollTo(context, fixedChances);
		clickWidget(context, fixedChances);
		context.waitFor(client -> !(client.screen instanceof ConfigScreen));
		context.getInput().typeChars("20 = abc");
		context.waitTicks(2);
		assertTrue(!widget(context, Button.class, "gui.done").active, "the map editor refuses a bad value");
		for (int i = 0; i < 3; i++) {
			context.getInput().pressKey(InputConstants.KEY_BACKSPACE);
		}
		context.getInput().typeChars("1,5");
		context.waitTicks(2);
		assertTrue(widget(context, Button.class, "gui.done").active, "the map editor accepts the fixed value");
		context.clickScreenButton("gui.done");
		context.waitForScreen(ConfigScreen.class);
		context.waitTicks(3);
		context.clickScreenButton("gui.done");
		context.waitForScreen(null);
		context.waitTicks(10);

		assertEquals(25, server.computeOnServer(s -> ForgeConfig.get().maxLevel), "max level saved through the screen");
		assertTrue(server.computeOnServer(s -> ForgeConfig.get().baseChance) == 0.9, "base chance 90 % saved as 0.9");
		assertTrue(server.computeOnServer(s -> ForgeConfig.get().chancePerTargetLevel.get("20")) == 0.015, "fixed chance 1.5 % for level 20");
		String file = readConfigFile();
		assertTrue(file.contains("\"maxLevel\": 25") && file.contains("\"20\": 0.015"), "the screen wrote config/enchantforge.json");

		// Esc with unsaved changes asks first; discarding keeps the saved values.
		context.setScreen(() -> ModMenu.getConfigScreen(EnchantForge.MOD_ID, null));
		context.waitForScreen(ConfigScreen.class);
		EditBox reopened = widget(context, EditBox.class, "config.enchantforge.maxLevel");
		assertTrue(context.computeOnClient(client -> reopened.getValue()).equals("25"), "the screen shows the saved value");
		context.runOnClient(client -> reopened.setValue("30"));
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(ConfirmScreen.class);
		context.clickScreenButton("config.enchantforge.ui.discard.yes");
		context.waitForScreen(null);
		context.waitTicks(5);
		assertEquals(25, server.computeOnServer(s -> ForgeConfig.get().maxLevel), "discarded changes are not saved");

		server.runOnServer(s -> ForgeConfig.update(new ForgeConfig()));
	}

	private static String readConfigFile() {
		try {
			return Files.readString(FabricLoader.getInstance().getConfigDir().resolve(EnchantForge.MOD_ID + ".json"));
		} catch (IOException e) {
			throw new AssertionError("config file not readable", e);
		}
	}

	/** Finds a widget of the current screen, also inside lists, by the translation key of its message. */
	private static <T extends AbstractWidget> T widget(ClientGameTestContext context, Class<T> type, String translationKey) {
		List<T> found = widgets(context, type, translationKey);
		if (found.isEmpty()) {
			throw new AssertionError("no " + type.getSimpleName() + " with message " + translationKey);
		}
		return found.getFirst();
	}

	/** All widgets of the current screen with this message, in the order they are shown. */
	private static <T extends AbstractWidget> List<T> widgets(ClientGameTestContext context, Class<T> type, String translationKey) {
		return context.computeOnClient(client -> {
			List<T> found = new ArrayList<>();
			collect(client.screen.children(), type, translationKey, found);
			return found;
		});
	}

	private static <T extends AbstractWidget> void collect(List<? extends GuiEventListener> listeners, Class<T> type, String translationKey, List<T> found) {
		for (GuiEventListener listener : listeners) {
			if (type.isInstance(listener) && ((AbstractWidget) listener).getMessage().getContents() instanceof TranslatableContents contents
				&& contents.getKey().equals(translationKey)) {
				found.add(type.cast(listener));
			}
			if (listener instanceof ContainerEventHandler container) {
				collect(container.children(), type, translationKey, found);
			}
		}
	}

	/**
	 * Scrolls the config list with the mouse wheel until the row of the widget is fully visible. Widgets only get their
	 * position when their row is drawn, but the list keeps the row positions up to date, so the row decides.
	 */
	private static void scrollTo(ClientGameTestContext context, AbstractWidget widget) {
		double[] center = context.computeOnClient(client -> new double[] {client.getWindow().getScreenWidth() / 2.0, client.getWindow().getScreenHeight() / 2.0});
		context.getInput().setCursorPos(center[0], center[1]);
		for (int i = 0; i < 80; i++) {
			int direction = context.computeOnClient(client -> {
				LayoutElement row = rowOf(client.screen.children(), widget);
				int bottom = client.getWindow().getGuiScaledHeight() - 40;
				return row.getY() < 40 ? 1 : row.getY() + row.getHeight() > bottom ? -1 : 0;
			});
			if (direction == 0) break;
			context.getInput().scroll(direction);
			context.waitTicks(1);
		}
		context.waitTicks(2);
	}

	private static LayoutElement rowOf(List<? extends GuiEventListener> listeners, AbstractWidget widget) {
		for (GuiEventListener listener : listeners) {
			if (listener instanceof AbstractSelectionList<?> list) {
				for (Object row : list.children()) {
					if (row instanceof ContainerEventHandler container && container.children().contains(widget)) {
						return (LayoutElement) row;
					}
				}
			}
		}
		throw new AssertionError("widget is not in a list row");
	}

	private static void hoverWidget(ClientGameTestContext context, AbstractWidget widget) {
		double[] cursor = context.computeOnClient(client -> {
			double scale = (double) client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth();
			return new double[] {(widget.getX() + widget.getWidth() / 2.0) * scale, (widget.getY() + widget.getHeight() / 2.0) * scale};
		});
		context.getInput().setCursorPos(cursor[0], cursor[1]);
		context.waitTicks(3);
	}

	private static void clickWidget(ClientGameTestContext context, AbstractWidget widget) {
		hoverWidget(context, widget);
		context.getInput().pressMouse(0);
		context.waitTicks(3);
	}

	private static void checkData(MinecraftServer server) {
		ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> recipe = ResourceKey.create(Registries.RECIPE, EnchantForge.FORGE_ID);
		assertTrue(server.getRecipeManager().byKey(recipe).isPresent(), "crafting recipe is loaded");
		ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> charmRecipe = ResourceKey.create(Registries.RECIPE, EnchantForge.id("luck_charm"));
		assertTrue(server.getRecipeManager().byKey(charmRecipe).isPresent(), "luck charm recipe is loaded");
		var lootTable = ResourceKey.create(Registries.LOOT_TABLE, EnchantForge.id("blocks/enchantment_forge"));
		assertTrue(server.reloadableRegistries().getLootTable(lootTable) != net.minecraft.world.level.storage.loot.LootTable.EMPTY, "block loot table is loaded");
	}

	private static void prepare(MinecraftServer server) {
		ServerPlayer player = player(server);
		player.setGameMode(GameType.SURVIVAL);
		player.giveExperienceLevels(100);

		Registry<Enchantment> enchantments = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
		for (var entry : List.of(
			new Object[] {Enchantments.SHARPNESS, 5},
			new Object[] {Enchantments.FIRE_ASPECT, 2},
			new Object[] {Enchantments.KNOCKBACK, 2},
			new Object[] {Enchantments.LOOTING, 3},
			new Object[] {Enchantments.SWEEPING_EDGE, 3},
			new Object[] {Enchantments.UNBREAKING, 3},
			new Object[] {Enchantments.MENDING, 1},
			new Object[] {Enchantments.VANISHING_CURSE, 1}
		)) {
			@SuppressWarnings("unchecked")
			Holder<Enchantment> holder = enchantments.getOrThrow((ResourceKey<Enchantment>) entry[0]);
			sword.enchant(holder, (Integer) entry[1]);
		}
		player.getInventory().setItem(0, sword);
		player.getInventory().setItem(1, new ItemStack(Items.DIAMOND, 64));
		player.getInventory().setItem(2, new ItemStack(EnchantForge.LUCK_CHARM, 32));

		BlockPos pos = player.blockPosition().east(2);
		player.level().setBlockAndUpdate(pos, EnchantForge.ENCHANTMENT_FORGE.defaultBlockState());
		player.openMenu(player.level().getBlockState(pos).getMenuProvider(player.level(), pos));
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static EnchantmentForgeMenu forgeMenu(MinecraftServer server) {
		return (EnchantmentForgeMenu) player(server).containerMenu;
	}

	private static int levelOf(MinecraftServer server, ResourceKey<Enchantment> key) {
		ItemStack stack = forgeMenu(server).getInputItem();
		Holder<Enchantment> holder = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
		return EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
	}

	/** Shift-clicks a menu slot through the same path a real click takes. */
	private static void quickMove(ClientGameTestContext context, int slot) {
		context.runOnClient(client -> client.gameMode.handleInventoryMouseClick(
			client.player.containerMenu.containerId, slot, 0, ClickType.QUICK_MOVE, client.player));
	}

	/** Puts the cursor in the middle of a visible enchantment row of the forge GUI. */
	private static void hoverRow(ClientGameTestContext context, int row) {
		hoverGui(context, 42 + 56, 18 + row * 20 + 10);
	}

	/** Puts the cursor on a point given in the forge GUI's own coordinates. */
	private static void hoverGui(ClientGameTestContext context, int guiX, int guiY) {
		double[] pos = context.computeOnClient(client -> {
			var window = client.getWindow();
			double scale = (double) window.getScreenWidth() / window.getGuiScaledWidth();
			int left = (window.getGuiScaledWidth() - 176) / 2;
			int top = (window.getGuiScaledHeight() - 222) / 2;
			return new double[] {(left + guiX) * scale, (top + guiY) * scale};
		});
		context.getInput().setCursorPos(pos[0], pos[1]);
	}

	private static void assertTrue(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private static void assertEquals(int expected, int actual, String what) {
		if (expected != actual) {
			throw new AssertionError(what + ": expected " + expected + ", got " + actual);
		}
	}
}
