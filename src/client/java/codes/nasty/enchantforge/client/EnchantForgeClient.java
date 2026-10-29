package codes.nasty.enchantforge.client;

import codes.nasty.enchantforge.EnchantForge;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class EnchantForgeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(EnchantForge.ENCHANTMENT_FORGE_MENU, EnchantmentForgeScreen::new);
	}
}
