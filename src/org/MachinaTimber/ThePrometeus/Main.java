package org.MachinaTimber.ThePrometeus;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private final List<Material> woodBlocks     = new ArrayList<>();
    private final List<Material> leafBlocks     = new ArrayList<>();
    private List<String> axeItems               = new ArrayList<>();
    private String type;
    
    /**
     * Set default configuration settings
     */
    private void setDefaults() {
        axeItems.add("IRON_AXE");
        axeItems.add("GOLDEN_AXE");
        axeItems.add("DIAMOND_AXE");

        getConfig().addDefault("axes", axeItems);
        getConfig().addDefault("mode", "full");
        
        getConfig().options().copyDefaults(true);
    }
    
    /**
     * Register the plugin event listener
     */
    private void setEventListener() {
        // Register new event listener
        getServer().getPluginManager().registerEvents(new Listener() {
            // On block break
            @EventHandler
            public void timberIt(BlockBreakEvent event) {
                // Get player item in hand
                final ItemStack ih              = event.getPlayer().getInventory().getItemInMainHand();

                if (woodBlocks.contains(event.getBlock().getType()) && (axeItems.contains(ih.getType().name()))) {
                    // Get world
                    final World tw              = event.getPlayer().getWorld();

                    // Get block coordinates
                    final int x                 = event.getBlock().getX();
                    final int y                 = event.getBlock().getY();
                    final int z                 = event.getBlock().getZ();

                    Bukkit.getScheduler().scheduleSyncDelayedTask(Main.this, new Runnable() {
                        @Override
                        public void run() {
                            int count           = Main.this.breakChain(tw, x, y, z);
                            ih.setDurability((short) ((short) (ih.getDurability() - count) * -1 - ih.getType().getMaxDurability())); 
                        }
                    }, 1);
                }
            }
        }, this);
    }

    @Override
    public void onEnable() {
        super.onEnable();

        // Reset breakable list to prevent issues on reload
        woodBlocks.clear();
        leafBlocks.clear();

        // Set config defaults
        setDefaults();

        // Save the config
        saveConfig();
        
        getLogger().info("Configuration file loaded");
        
        // Get axe items
        axeItems                                = getConfig().getStringList("axes");
        
        // Get all available materials
        Material[] materials                    = Material.values();
        
        getLogger().log(Level.INFO, "Generating breakable blocks from {0} total blocks...", materials.length);

        // Iterate over all materials
        for (Material material: materials) {
            // Check if name contains _LOG
            if (material.name().contains("LOG")) {
                // Add it to breakable log list
                woodBlocks.add(material);
            } else // Check if name contains _LEAVES
            if (material.name().contains("LEAVES")) {
                // Add it to breakable leaves list
                leafBlocks.add(material);
            }
        }

        // Load default type
        type                                    = getConfig().getString("mode");        

        // Register plugin events
        setEventListener();

        getLogger().log(Level.INFO, "{0} is ready", this.getName());
    }

    /**
     * Get block at desired World and position
     * @param world World
     * @param x int
     * @param y int
     * @param z int
     * @return 
     */
    private Material getBlockAt(World world, int x, int y, int z) {
        return world.getBlockAt(x, y, z).getType();
    }

    /**
     * Do a break chain checking the surroundings
     * @param w World
     * @param x int
     * @param y int
     * @param z int
     * @param blockList List<Material>
     * @return int
     */
    private int doBreakChain(World w, int x, int y, int z, List<Material> blockList) {
        int count                       = 0;

        // Y line
        for(int cy = y - 1; cy <= y + 1; cy++) {
            // Z line
            for(int cz = z - 1; cz <= z + 1; cz++) {
                // X line
                for(int cx = x - 1; cx <= x + 1; cx++) {
                    Material b           = getBlockAt(w, cx, cy, cz);

                    if (blockList.contains(b)) {
                        count           += breakChain(w, cx, cy, cz);
                    }
                }
            }
        }
        
        return count;
    }

    /**
     * Starts a break chain
     * @param w Wiorld
     * @param x int
     * @param y int
     * @param z int
     * @return int
     */
    public int breakChain(World w, int x, int y, int z) {        
        int count                       = 1;

        // Break base block
        w.getBlockAt(x, y, z).breakNaturally();

        // Check if upper block is breakable
        if (woodBlocks.contains(getBlockAt(w, x, y + 1, z))) {
            // Break it
            count                       += breakChain(w, x, y + 1, z);
        }

        // If needs to remove leaves
        if (type.equalsIgnoreCase("full") || type.equalsIgnoreCase("classic-leaves"))  {
            count                       += doBreakChain(w, x, y, z, leafBlocks);
        }
        
        // If needs to remove logs
        if (type.equalsIgnoreCase("full") || type.equalsIgnoreCase("full-noleaves")) {
            count                       += doBreakChain(w, x, y, z, woodBlocks);
        }
        
        return count;
    }
}
