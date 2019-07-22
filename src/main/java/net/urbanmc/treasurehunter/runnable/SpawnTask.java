package net.urbanmc.treasurehunter.runnable;

import net.urbanmc.treasurehunter.TreasureHunter;
import net.urbanmc.treasurehunter.manager.*;
import net.urbanmc.treasurehunter.object.TreasureChest;
import net.urbanmc.treasurehunter.object.TreasureChest.TreasureChestType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;


public class SpawnTask extends BukkitRunnable {

	private static SpawnTask instance = new SpawnTask();

	private SpawnTask() {
	}

	public static SpawnTask getInstance() {
		return instance;
	}

	static void start() {
		if (instance.hasBeenScheduled()) {
			instance = new SpawnTask();
		}

		instance.runTaskTimer(TreasureHunter.getInstance(), 0, 72000);
	}

	@Override
	public void run() {
		//Get the world synchronously
		World world = Bukkit.getWorld(ConfigManager.getConfig().getString("world"));

		if (world == null) {
			Bukkit.getLogger().severe("[TreasureHunter] Invalid world for spawn location!");
			return;
		}

		//Run the async generation
		Bukkit.getScheduler().runTaskAsynchronously(TreasureHunter.getInstance(), () -> {
			generateChestAndItems(world);
		});

		/* TreasureChestManager.getInstance().removeCurrentChest();

		TreasureChestType type = randomType();

		Location loc = randomLocation(type);

		if (loc == null) {
			Bukkit.getLogger().severe("[TreasureHunter] Error generating location for chest!");
			return;
		}

		Block b = loc.getBlock();

		TreasureChest chest = new TreasureChest(type, b);

		TreasureChestManager.getInstance().setCurrentChest(chest);

		b.setType(Material.CHEST, true);


		if (!(b.getState() instanceof Chest)) {
			Bukkit.getLogger().severe("[TreasureHunter] Error spawning chest. Blockstate is not a chest! Location: " + loc.toString());
			return;
		}

		Chest c = (Chest) b.getState();

		List<ItemStack> items = getItems(type);

		ItemStack[] itemArray = new ItemStack[items.size()];
		itemArray = items.toArray(itemArray);

		c.getBlockInventory().addItem(itemArray);

		String typeName = type.getDisplayName().toLowerCase();

		typeName = typeName.substring(0,2) + ChatColor.BOLD + typeName.substring(2);

		Bukkit.broadcastMessage(Messages.getString("broadcast.start", type.equals(TreasureChestType.EPIC) ? "n" : "", typeName));

		checkSpawnedTime(); */
	}

	private TreasureChestType randomType() {
		return ItemManager.getInstance().randomChestType();
	}

	private List<ItemStack> getItems(TreasureChestType type) {
		return ItemManager.getInstance().getRandomItemsForChestType(type);
	}

	private boolean hasBeenScheduled() {
		try {
			instance.getTaskId();
			return true;
		} catch (IllegalStateException ex) {
			return false;
		}
	}

	/*
	 * @return true if successfully; false if error (such as never started)
	 */
	private boolean cancelTask() {
		try {
			cancel();
			return true;
		} catch (IllegalStateException ex) {
			return false;
		}
	}

	public void forceSpawn() {
		cancelTask();

		instance = new SpawnTask();

		instance.runTask(TreasureHunter.getInstance());
	}

	private void checkSpawnedTime() {
		GregorianCalendar cal = new GregorianCalendar();

		int minute = cal.get(Calendar.MINUTE);

		if (minute < 3) return;

		new StartTask();

		cancelTask();
	}


	private void generateChestAndItems(World world) {
		//Get type
		TreasureChestType type = randomType();

		//Get x and z cords
		int[] xZ = SpawnManager.getInstance().generateLocationAsync(world, type);

		//Get items list
		List<ItemStack> items = getItems(type);

		//Get the items array
		ItemStack[] itemArray = items.toArray(new ItemStack[0]);

		//Run the sync spawn task
		Bukkit.getScheduler().runTask(TreasureHunter.getInstance(), () -> runSyncTask(world, xZ[0], xZ[1], type, itemArray));
	}

	private void runSyncTask(World world, int x, int z, TreasureChestType type, ItemStack[] items) {
		int y;

		// Get highest block using world method with a fail-safe if that throws an NPE.
		try {
		 y = world.getHighestBlockYAt(x, z);
		} catch (NullPointerException ex) {
			y = 256;
			for (Material mat = world.getBlockAt(x, y, z).getType();
				 mat.equals(Material.AIR) || mat.equals(Material.VOID_AIR) || mat.equals(Material.CAVE_AIR);
				 mat = world.getBlockAt(x, y, z).getType()) {
				y -= 1;
			}

			y++;
		}

		//Get the location from the cords and world
		Location loc = new Location(world, x, y, z);

		if (loc == null) {
			Bukkit.getLogger().severe("[TreasureHunter] Error generating location for chest!");
			return;
		}

		TreasureChestManager.getInstance().removeCurrentChest();

		Block b = loc.getBlock();

		TreasureChest chest = new TreasureChest(type, b);

		TreasureChestManager.getInstance().setCurrentChest(chest);

		b.setType(Material.CHEST, true);

		if (!(b.getState() instanceof Chest)) {
			Bukkit.getLogger().severe("[TreasureHunter] Error spawning chest. Blockstate is not a chest! Location: " + loc.toString());
			return;
		}

		Chest c = (Chest) b.getState();

		c.getBlockInventory().addItem(items);

		String typeName = type.getDisplayName().toLowerCase();

		typeName = typeName.substring(0,2) + ChatColor.BOLD + typeName.substring(2);

		Bukkit.broadcastMessage(Messages.getString("broadcast.start", type.equals(TreasureChestType.EPIC) ? "n" : "", typeName));

		checkSpawnedTime();
	}
}
