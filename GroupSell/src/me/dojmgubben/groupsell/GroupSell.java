package me.dojmgubben.groupsell;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class GroupSell extends JavaPlugin implements Listener {

	public static Economy economy = null;
	private static final Logger log = Logger.getLogger("Minecraft");

	File configurationConfig;
	public FileConfiguration config;

	ArrayList<String> inShop = new ArrayList<String>();

	public void onEnable() {
		setupEconomy();
		if (!setupEconomy()) {
			log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		configurationConfig = new File(getDataFolder(), "config.yml");
		config = YamlConfiguration.loadConfiguration(configurationConfig);
		loadConfig();
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
	}

	public void savec() {
		try {
			config.save(configurationConfig);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loadConfig() {

		config.addDefault("sign.line.1", "&1[SELL]");
		config.addDefault("sign.line.2", "&bSpawn Shop");
		config.addDefault("sign.line.3", "");
		config.addDefault("sign.line.4", "");
		config.addDefault("sign.detect", "sell");
		config.addDefault("messages.noperm", "&4You don't have enough permissions.");
		config.addDefault("messages.sign.create", "&aSell sign created.");
		config.addDefault("messages.sell.failure", "&4An error occured. No items sold.");
		config.addDefault("messages.sell.success", "&bYou sold %i for %p..");
		config.addDefault("items.STONE", 5.00);
		config.addDefault("items.GRASS", 5.00);
		config.options().copyDefaults(true);
		savec();
	}

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		economy = rsp.getProvider();
		return economy != null;
	}

	@EventHandler
	public void onSignChange(SignChangeEvent event) {
		Player player = event.getPlayer();
		if (event.getLine(0).equals(config.getString("sign.detect"))) {
			if (player.hasPermission("groupsell.setup")) {
				event.setLine(0, ChatColor.translateAlternateColorCodes('&', config.getString("sign.line.1")));
				if (config.getString("sign.line.2") != null) {
					event.setLine(1, ChatColor.translateAlternateColorCodes('&', config.getString("sign.line.2")));
				}
				if (config.getString("sign.line.3") != null) {
					event.setLine(2, ChatColor.translateAlternateColorCodes('&', config.getString("sign.line.3")));
				}
				if (config.getString("sign.line.4") != null) {
					event.setLine(3, ChatColor.translateAlternateColorCodes('&', config.getString("sign.line.4")));
				}
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.sign.create")));
			} else {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.noperm")));
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (event.getClickedBlock().getType() == Material.WALL_SIGN || event.getClickedBlock().getType() == Material.SIGN_POST) {
				Sign s = (Sign) event.getClickedBlock().getState();
				if (s.getLine(0).equals(ChatColor.translateAlternateColorCodes('&', config.getString("sign.line.1")))) {
					if (player.hasPermission("groupsell.use")) {
						Inventory shop = Bukkit.getServer().createInventory(player, 36, "Shop");
						ItemStack redwool = new ItemStack(Material.WOOL, 1, DyeColor.RED.getData());
						ItemStack greenwool = new ItemStack(Material.WOOL, 1, DyeColor.GREEN.getData());
						ItemMeta greenwoolmeta = greenwool.getItemMeta();
						ItemMeta redwoolmeta = redwool.getItemMeta();
						greenwoolmeta.setDisplayName(ChatColor.GREEN + "Finish");
						redwoolmeta.setDisplayName(ChatColor.RED + "Cancel");
						greenwool.setItemMeta(greenwoolmeta);
						redwool.setItemMeta(redwoolmeta);
						shop.setItem(35, greenwool);
						shop.setItem(27, redwool);
						player.openInventory(shop);
						inShop.add(player.getName());
						event.setCancelled(true);
					} else {
						player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.noperm")));
						event.setCancelled(true);
					}
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		if (inShop.contains(player.getName())) {
			if (event.getInventory().getTitle().equalsIgnoreCase("shop")) {
				ItemStack is = event.getCurrentItem();
				if (is.hasItemMeta()) {
					if (is.getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.RED + "cancel")) {
						event.setCancelled(true);
						for (int i = 0; i < event.getInventory().getSize(); i++) {
							ItemStack item = event.getInventory().getItem(i);
							if (item != null) {
								if (item.hasItemMeta()) {
									if (item.getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.RED + "cancel")) {

									} else if (item.getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.GREEN + "finish")) {

									} else {
										player.getInventory().addItem(item);
										event.getInventory().getItem(i).setType(Material.AIR);
										player.updateInventory();
									}
								} else {
									player.getInventory().addItem(item);
									event.getInventory().setItem(i, new ItemStack(Material.AIR));
									player.updateInventory();
								}
							}
						}
					} else if (is.getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.GREEN + "finish")) {
						event.setCancelled(true);
						for (int i = 0; i < event.getInventory().getSize(); i++) {
							ItemStack item = event.getInventory().getItem(i);
							if (item != null) {
								if (item.hasItemMeta()) {
									if (!item.getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.RED + "cancel")
											|| item.getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.GREEN + "finish")) {
										int itemAmount = item.getAmount();
										if (config.contains("items." + item.getType().toString())) {
											double price = config.getDouble("items." + item.getType().toString()) * itemAmount;
											EconomyResponse r = economy.withdrawPlayer(player, price);
											if (r.transactionSuccess()) {
												player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.sell.success").replace("%i", item.getType().toString())
														.replace("%p", price + "")));
												event.getInventory().setItem(i, new ItemStack(Material.AIR));
												player.updateInventory();
											} else {
												player.getInventory().addItem(item);
												event.getInventory().setItem(i, new ItemStack(Material.AIR));
												player.updateInventory();
												player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.sell.failure")));
											}
										}
									}
								} else {
									int itemAmount = item.getAmount();
									if (config.contains("items." + item.getType().toString())) {
										double price = config.getDouble("items." + item.getType().toString()) * itemAmount;
										EconomyResponse r = economy.withdrawPlayer(player, price);
										if (r.transactionSuccess()) {
											player.sendMessage(ChatColor.translateAlternateColorCodes('&',
													config.getString("messages.sell.success").replace("%i", item.getType().toString()).replace("%p", price + "")));
											event.getInventory().setItem(i, new ItemStack(Material.AIR));
											player.updateInventory();
										} else {
											player.getInventory().addItem(item);
											event.getInventory().setItem(i, new ItemStack(Material.AIR));
											player.updateInventory();
											player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.sell.failure")));
										}
									}
								}
							}
						}
					}
				}
			}
		}

	}
}
