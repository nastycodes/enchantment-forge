package codes.nasty.enchantforge.client;

import codes.nasty.enchantforge.EnchantForge;
import codes.nasty.enchantforge.menu.EnchantmentForgeMenu;
import codes.nasty.enchantforge.menu.EnchantmentForgeMenu.Entry;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Repairable;

public class EnchantmentForgeScreen extends AbstractContainerScreen<EnchantmentForgeMenu> {
	private static final Identifier TEXTURE = EnchantForge.id("textures/gui/container/enchantment_forge.png");

	private static final int LIST_X = 42;
	private static final int LIST_Y = 18;
	private static final int LIST_WIDTH = 112;
	private static final int ROW_HEIGHT = 20;
	private static final int VISIBLE_ROWS = 5;

	private static final int SCROLLER_X = 157;
	private static final int SCROLLER_Y = 18;
	private static final int SCROLLER_TRACK = 100;
	private static final int SCROLLER_WIDTH = 12;
	private static final int SCROLLER_HEIGHT = 15;

	private static final int MATERIAL_X = 17;
	private static final int MATERIAL_Y = 60;
	private static final int CHARM_X = 17;
	private static final int CHARM_Y = 98;
	/** Slot background colours of the texture, used to fade the ghost items. */
	private static final int GHOST_OVERLAY = 0xA08B8B8B;
	private static final int CHARM_GHOST_OVERLAY = 0xA0867A9C;
	private static final int CHARM_COLOR = 0x78CD5F;

	private static final int TEXT_MUTED = 0xFFA8A0B8;
	private static final int TEXT_DISABLED = 0xFF8A8A8A;

	private int scrollRow;
	private boolean scrolling;

	public EnchantmentForgeScreen(EnchantmentForgeMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = 222;
		this.inventoryLabelY = this.imageHeight - 94;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		this.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		this.scrollRow = Mth.clamp(this.scrollRow, 0, this.maxScroll());
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
		this.renderScroller(graphics, mouseX, mouseY);
		this.renderMaterialGhost(graphics);
		this.renderCharmGhost(graphics);

		List<Entry> entries = this.entries();
		if (this.menu.getStatus() != EnchantmentForgeMenu.STATUS_READY || entries.isEmpty()) {
			this.renderStatusMessage(graphics);
			return;
		}

		int hovered = this.rowAt(mouseX, mouseY);
		for (int row = 0; row < VISIBLE_ROWS; row++) {
			int index = this.scrollRow + row;
			if (index >= entries.size()) {
				break;
			}
			int x = this.leftPos + LIST_X;
			int y = this.topPos + LIST_Y + row * ROW_HEIGHT;
			this.renderRow(graphics, entries.get(index), index, x, y, index == hovered);
		}
	}

	private void renderScroller(GuiGraphics graphics, int mouseX, int mouseY) {
		int maxScroll = this.maxScroll();
		int offset = maxScroll > 0 ? Math.round((SCROLLER_TRACK - SCROLLER_HEIGHT) * this.scrollRow / (float) maxScroll) : 0;
		int x = this.leftPos + SCROLLER_X;
		int y = this.topPos + SCROLLER_Y + offset;
		float u = maxScroll > 0 ? 176.0F : 188.0F;
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, u, 0.0F, SCROLLER_WIDTH, SCROLLER_HEIGHT, 256, 256);
		if (maxScroll > 0 && mouseX >= x && mouseX < x + SCROLLER_WIDTH && mouseY >= y && mouseY < y + SCROLLER_HEIGHT) {
			graphics.requestCursor(this.scrolling ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
		}
	}

	/** Shows the required material faded inside the empty material slot. */
	private void renderMaterialGhost(GuiGraphics graphics) {
		if (this.menu.getStatus() != EnchantmentForgeMenu.STATUS_READY || !this.menu.getMaterialStack().isEmpty()) {
			return;
		}
		Item material = this.displayedMaterial();
		if (material == Items.AIR) {
			return;
		}
		int x = this.leftPos + MATERIAL_X;
		int y = this.topPos + MATERIAL_Y;
		graphics.renderFakeItem(new ItemStack(material), x, y);
		graphics.fill(x, y, x + 16, y + 16, GHOST_OVERLAY);
	}

	private void renderCharmGhost(GuiGraphics graphics) {
		if (!this.menu.getCharmStack().isEmpty()) {
			return;
		}
		int x = this.leftPos + CHARM_X;
		int y = this.topPos + CHARM_Y;
		graphics.renderFakeItem(new ItemStack(EnchantForge.LUCK_CHARM), x, y);
		graphics.fill(x, y, x + 16, y + 16, CHARM_GHOST_OVERLAY);
	}

	/** Cycles through all accepted repair materials (e.g. every kind of planks), like vanilla's template ghosts. */
	private Item displayedMaterial() {
		Item material = this.menu.getMaterialItem();
		Repairable repairable = this.menu.getInputItem().get(DataComponents.REPAIRABLE);
		if (repairable != null && repairable.items().size() > 1 && repairable.items().contains(BuiltInRegistries.ITEM.wrapAsHolder(material))) {
			HolderSet<Item> items = repairable.items();
			return items.get((int) (Util.getMillis() / 1000L % items.size())).value();
		}
		return material;
	}

	private void renderStatusMessage(GuiGraphics graphics) {
		String key = switch (this.menu.getStatus()) {
			case EnchantmentForgeMenu.STATUS_NOT_ALLOWED -> "enchantforge.gui.not_allowed";
			case EnchantmentForgeMenu.STATUS_NO_ENCHANTMENTS -> "enchantforge.gui.no_enchantments";
			default -> "enchantforge.gui.empty";
		};
		List<FormattedCharSequence> lines = this.font.split(Component.translatable(key), LIST_WIDTH - 12);
		int centerX = this.leftPos + LIST_X + LIST_WIDTH / 2;
		int y = this.topPos + LIST_Y + (VISIBLE_ROWS * ROW_HEIGHT - lines.size() * 10) / 2;
		for (FormattedCharSequence line : lines) {
			graphics.drawCenteredString(this.font, line, centerX, y, TEXT_MUTED);
			y += 10;
		}
	}

	private void renderRow(GuiGraphics graphics, Entry entry, int index, int x, int y, boolean hovered) {
		int state = this.menu.getField(index, EnchantmentForgeMenu.FIELD_STATE);
		boolean upgradeable = state == EnchantmentForgeMenu.STATE_UPGRADEABLE;
		boolean clickable = this.canUpgrade(index);

		int background;
		if (upgradeable) {
			background = clickable ? (hovered ? 0xFF6E54A6 : 0xFF4D3B6B) : (hovered ? 0xFF5E3C55 : 0xFF4A3043);
		} else if (state == EnchantmentForgeMenu.STATE_MAXED) {
			background = 0xFF2E4A3B;
		} else {
			background = 0xFF3A3A40;
		}
		graphics.fill(x, y, x + LIST_WIDTH, y + ROW_HEIGHT - 1, background);
		graphics.fill(x, y, x + LIST_WIDTH, y + 1, 0x30FFFFFF);
		graphics.fill(x, y + ROW_HEIGHT - 2, x + LIST_WIDTH, y + ROW_HEIGHT - 1, 0x40000000);
		if (hovered && clickable) {
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}

		String levels = upgradeable
			? EnchantmentForgeMenu.levelName(entry.level()).getString() + " → " + EnchantmentForgeMenu.levelName(entry.level() + 1).getString()
			: EnchantmentForgeMenu.levelName(entry.level()).getString();
		int levelsWidth = this.font.width(levels);
		String name = entry.enchantment().value().description().getString();
		int nameSpace = LIST_WIDTH - 6 - levelsWidth - 4;
		if (this.font.width(name) > nameSpace) {
			name = this.font.plainSubstrByWidth(name, nameSpace - this.font.width("…")) + "…";
		}
		int nameColor = upgradeable ? 0xFFFFFFFF : 0xFFB0B0B0;
		graphics.drawString(this.font, name, x + 3, y + 2, nameColor);
		graphics.drawString(this.font, levels, x + LIST_WIDTH - 3 - levelsWidth, y + 2, upgradeable ? 0xFFE0C8FF : 0xFFB0B0B0);

		Component info;
		if (upgradeable) {
			int materialCost = this.menu.getField(index, EnchantmentForgeMenu.FIELD_MATERIAL_COST);
			int xpCost = this.menu.getField(index, EnchantmentForgeMenu.FIELD_XP_COST);
			info = Component.translatable(
				"enchantforge.gui.row_info",
				this.chanceComponent(index),
				Component.literal(Integer.toString(materialCost)).withColor(this.hasMaterial(materialCost) ? 0xC0C0C0 : 0xFF6060),
				Component.literal(Integer.toString(xpCost)).withColor(this.hasXp(xpCost) ? 0xC0C0C0 : 0xFF6060)
			);
		} else if (state == EnchantmentForgeMenu.STATE_MAXED) {
			info = Component.translatable("enchantforge.gui.maxed").withColor(0x7FD47F);
		} else {
			info = Component.translatable("enchantforge.gui.excluded");
		}
		graphics.drawString(this.font, info, x + 3, y + 11, upgradeable ? TEXT_MUTED : TEXT_DISABLED);
	}

	@Override
	protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
		super.renderTooltip(graphics, mouseX, mouseY);

		if (this.menu.getStatus() == EnchantmentForgeMenu.STATUS_READY
			&& this.menu.getMaterialStack().isEmpty()
			&& this.isHovering(MATERIAL_X, MATERIAL_Y, 16, 16, mouseX, mouseY)) {
			Component material = new ItemStack(this.displayedMaterial()).getHoverName();
			graphics.setTooltipForNextFrame(this.font, Component.translatable("enchantforge.tooltip.required_material", material), mouseX, mouseY);
			return;
		}

		if (this.menu.getCharmStack().isEmpty() && this.isHovering(CHARM_X, CHARM_Y, 16, 16, mouseX, mouseY)) {
			graphics.setComponentTooltipForNextFrame(this.font, List.of(
				Component.translatable("enchantforge.tooltip.charm_slot").withColor(CHARM_COLOR),
				Component.translatable("enchantforge.tooltip.charm_slot.bonus", formatChance(this.menu.getCharmBonus())).withStyle(ChatFormatting.GRAY),
				Component.translatable("enchantforge.tooltip.charm_slot.max", this.menu.getCharmMaxPerAttempt()).withStyle(ChatFormatting.GRAY),
				Component.translatable("enchantforge.tooltip.charm_slot.consumed").withStyle(ChatFormatting.DARK_GRAY)
			), mouseX, mouseY);
			return;
		}

		int index = this.rowAt(mouseX, mouseY);
		if (index >= 0) {
			graphics.setComponentTooltipForNextFrame(this.font, this.rowTooltip(this.entries().get(index), index), mouseX, mouseY);
		}
	}

	private List<Component> rowTooltip(Entry entry, int index) {
		List<Component> lines = new ArrayList<>();
		int state = this.menu.getField(index, EnchantmentForgeMenu.FIELD_STATE);
		if (state != EnchantmentForgeMenu.STATE_UPGRADEABLE) {
			lines.add(EnchantmentForgeMenu.enchantmentName(entry.enchantment(), entry.level()).withStyle(ChatFormatting.AQUA));
			String reason = state == EnchantmentForgeMenu.STATE_MAXED ? "enchantforge.tooltip.maxed" : "enchantforge.tooltip.excluded";
			lines.add(Component.translatable(reason).withStyle(ChatFormatting.GRAY));
			return lines;
		}

		int materialCost = this.menu.getField(index, EnchantmentForgeMenu.FIELD_MATERIAL_COST);
		int xpCost = this.menu.getField(index, EnchantmentForgeMenu.FIELD_XP_COST);
		int maxLevel = this.menu.getField(index, EnchantmentForgeMenu.FIELD_MAX_LEVEL);
		lines.add(Component.translatable(
			"enchantforge.tooltip.upgrade",
			EnchantmentForgeMenu.enchantmentName(entry.enchantment(), entry.level()),
			EnchantmentForgeMenu.levelName(entry.level() + 1)
		).withStyle(ChatFormatting.AQUA));
		lines.add(Component.translatable("enchantforge.tooltip.chance", this.chanceComponent(index)).withStyle(ChatFormatting.GRAY));
		int charms = this.menu.getField(index, EnchantmentForgeMenu.FIELD_CHARMS_USED);
		if (charms > 0) {
			int baseChance = this.menu.getField(index, EnchantmentForgeMenu.FIELD_BASE_CHANCE);
			int bonus = this.menu.getField(index, EnchantmentForgeMenu.FIELD_CHANCE) - baseChance;
			lines.add(Component.literal("  ").append(Component.translatable(
				"enchantforge.tooltip.charm_bonus",
				formatChance(baseChance),
				formatChance(bonus),
				charms
			)).withColor(CHARM_COLOR));
		}
		lines.add(Component.translatable(
			"enchantforge.tooltip.material",
			materialCost,
			new ItemStack(this.menu.getMaterialItem()).getHoverName()
		).withStyle(this.hasMaterial(materialCost) ? ChatFormatting.GRAY : ChatFormatting.RED));
		lines.add(Component.translatable("enchantforge.tooltip.xp", xpCost).withStyle(this.hasXp(xpCost) ? ChatFormatting.GRAY : ChatFormatting.RED));
		lines.add(Component.translatable("enchantforge.tooltip.max_level", EnchantmentForgeMenu.levelName(maxLevel)).withStyle(ChatFormatting.GRAY));
		if (this.menu.getDowngradeChance() > 0) {
			lines.add(Component.translatable("enchantforge.tooltip.downgrade", formatChance(this.menu.getDowngradeChance())).withStyle(ChatFormatting.DARK_RED));
		}

		if (this.canUpgrade(index)) {
			lines.add(Component.translatable("enchantforge.tooltip.click").withStyle(ChatFormatting.YELLOW));
		} else if (!this.menu.isMaterialValid() && !this.menu.getMaterialStack().isEmpty()) {
			lines.add(Component.translatable("enchantforge.tooltip.wrong_material").withStyle(ChatFormatting.RED));
		} else if (!this.hasMaterial(materialCost)) {
			lines.add(Component.translatable("enchantforge.tooltip.not_enough_material").withStyle(ChatFormatting.RED));
		} else {
			lines.add(Component.translatable("enchantforge.tooltip.not_enough_xp").withStyle(ChatFormatting.RED));
		}
		return lines;
	}

	private MutableComponent chanceComponent(int index) {
		int chance = this.menu.getField(index, EnchantmentForgeMenu.FIELD_CHANCE);
		int color;
		if (chance >= 7500) {
			color = 0x55FF55;
		} else if (chance >= 4000) {
			color = 0xFFFF55;
		} else if (chance >= 1500) {
			color = 0xFFAA00;
		} else {
			color = 0xFF5555;
		}
		MutableComponent text = Component.literal(formatChance(chance)).withColor(color);
		if (this.menu.getField(index, EnchantmentForgeMenu.FIELD_CHARMS_USED) > 0) {
			text.append(Component.literal("♣").withColor(CHARM_COLOR));
		}
		return text;
	}

	private static String formatChance(int basisPoints) {
		double percent = basisPoints * 100.0 / EnchantmentForgeMenu.CHANCE_SCALE;
		if (percent >= 10.0 || percent == Math.floor(percent)) {
			return String.format(Locale.ROOT, "%.0f%%", percent);
		}
		return String.format(Locale.ROOT, "%.1f%%", percent);
	}

	private boolean costsWaived() {
		Player player = this.minecraft.player;
		return player != null && this.menu.costsWaivedFor(player);
	}

	private boolean hasMaterial(int cost) {
		return this.costsWaived() || (this.menu.isMaterialValid() && this.menu.getMaterialStack().getCount() >= cost);
	}

	private boolean hasXp(int cost) {
		Player player = this.minecraft.player;
		return this.costsWaived() || (player != null && player.experienceLevel >= cost);
	}

	private boolean canUpgrade(int index) {
		if (this.menu.getField(index, EnchantmentForgeMenu.FIELD_STATE) != EnchantmentForgeMenu.STATE_UPGRADEABLE) {
			return false;
		}
		return this.hasMaterial(this.menu.getField(index, EnchantmentForgeMenu.FIELD_MATERIAL_COST))
			&& this.hasXp(this.menu.getField(index, EnchantmentForgeMenu.FIELD_XP_COST));
	}

	private List<Entry> entries() {
		return EnchantmentForgeMenu.entries(this.menu.getInputItem());
	}

	private int maxScroll() {
		return Math.max(0, this.entries().size() - VISIBLE_ROWS);
	}

	/** Index of the entry under the mouse, or -1. */
	private int rowAt(double mouseX, double mouseY) {
		if (this.menu.getStatus() != EnchantmentForgeMenu.STATUS_READY) {
			return -1;
		}
		double relX = mouseX - (this.leftPos + LIST_X);
		double relY = mouseY - (this.topPos + LIST_Y);
		if (relX < 0 || relX >= LIST_WIDTH || relY < 0 || relY >= VISIBLE_ROWS * ROW_HEIGHT) {
			return -1;
		}
		int index = this.scrollRow + (int) (relY / ROW_HEIGHT);
		return index < this.entries().size() ? index : -1;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			int index = this.rowAt(event.x(), event.y());
			if (index >= 0 && this.canUpgrade(index)) {
				this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
				return true;
			}
			double relX = event.x() - (this.leftPos + SCROLLER_X);
			double relY = event.y() - (this.topPos + SCROLLER_Y);
			if (this.maxScroll() > 0 && relX >= 0 && relX < SCROLLER_WIDTH && relY >= 0 && relY < SCROLLER_TRACK) {
				this.scrolling = true;
				this.scrollTo(event.y());
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.scrolling) {
			this.scrollTo(event.y());
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		this.scrolling = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
			return true;
		}
		int maxScroll = this.maxScroll();
		if (maxScroll > 0 && scrollY != 0) {
			this.scrollRow = Mth.clamp(this.scrollRow - (int) Math.signum(scrollY), 0, maxScroll);
			return true;
		}
		return false;
	}

	private void scrollTo(double mouseY) {
		float progress = (float) (mouseY - (this.topPos + SCROLLER_Y) - SCROLLER_HEIGHT / 2.0) / (SCROLLER_TRACK - SCROLLER_HEIGHT);
		this.scrollRow = Math.round(Mth.clamp(progress, 0.0F, 1.0F) * this.maxScroll());
	}
}
