package net.urbanmc.treasurehunter;

import com.earth2me.essentials.Essentials;
import net.urbanmc.randomtp.RandomTP;
import net.urbanmc.treasurehunter.command.THCommand;
import net.urbanmc.treasurehunter.listener.CompassListener;
import net.urbanmc.treasurehunter.listener.FlyListener;
import net.urbanmc.treasurehunter.listener.GodListener;
import net.urbanmc.treasurehunter.listener.InteractListener;
import net.urbanmc.treasurehunter.manager.ConfigManager;
import net.urbanmc.treasurehunter.manager.ItemManager;
import net.urbanmc.treasurehunter.runnable.StartTask;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import javax.xml.bind.Marshaller;

public class TreasureHunter extends JavaPlugin {

	private static Essentials essentials;
	private boolean isError;

	public static Essentials getEssentials() {
		return essentials;
	}

	@Override
	public void onEnable() {
		if (!checkDependencies())
			return;

		initializeManagers();

		registerListeners();
		registerCommand();

		if (isError) { // The manager class should print the error reason
			getLogger().info("Cannot start task due to errors.");
		} else {
			start();
		}
	}

	@Override
	public void onDisable() {

	}

	/*
	 * This method initializes the managers before registering/starting so that we know if there are errors.
	 * If there are any errors, we will not start the task.
	 */
	private void initializeManagers() {
		isError = false;

		ItemManager.getInstance().checkError(this);
		ConfigManager.checkError(this);
	}

	public void error() {
		isError = true;
	}

	private void registerListeners() {
		registerListener(new CompassListener());
		registerListener(new FlyListener());
		registerListener(new GodListener());
		registerListener(new InteractListener());
	}

	private void registerListener(Listener l) {
		Bukkit.getPluginManager().registerEvents(l, this);
	}

	private void registerCommand() {
		getCommand("treasurehunter").setExecutor(new THCommand(this));
	}

	private void start() {
		new StartTask(this);
	}

	private boolean checkDependencies() {
		essentials = getPlugin(Essentials.class);
		Plugin temp = getPlugin(RandomTP.class);

		if (essentials == null || temp == null) {
			getLogger().severe("Essentials and RandomTP are needed for Treasure Hunter! Disabling plugin..");
			setEnabled(false);

			return false;
		} else {
			return true;
		}
	}
}
