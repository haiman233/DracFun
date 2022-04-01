package me.lidan.draconic.Fusion;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.ItemLine;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import io.github.mooy1.infinitylib.common.CoolDowns;
import io.github.mooy1.infinitylib.common.Scheduler;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.guide.GuideHistory;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideImplementation;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNet;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import me.lidan.draconic.Database.Database;
import me.lidan.draconic.Draconic;
import me.lidan.draconic.Other.EnergyBreaker;
import me.lidan.draconic.Other.aiflyholo;
import me.lidan.draconic.Other.aiflyrun;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class FusionCrafting implements Listener, CommandExecutor{

    public static final RecipeType TYPE = new RecipeType(Draconic.createKey("Fusion_Crafting"),SlimefunItem.getById(
            "FUSION_CORE").getItem());

    public static HashMap<String, ItemStack[]> recipes = new HashMap<String, ItemStack[]>();
    public static HashMap<String, Integer> recipepower = new HashMap<>();

    public static HashMap<Player, Long> cooldowns = new HashMap<>();

    public final static Integer DEFAULT_ENERGY_COST = 100000;
    public static void addRecipe(String recipe,ItemStack[] items){
        addRecipe(recipe,items,DEFAULT_ENERGY_COST);
    }

    public static void addRecipe(String recipe,ItemStack[] items, Integer energy){
        recipes.put(recipe,items);
        recipepower.put(recipe,energy);
    }

    public static void removeRecipe(String recipe){
        recipes.remove(recipe);
    }

    public static void removeRecipe(String recipe,ItemStack[] items){
        recipes.remove(recipe,items);
    }

    public static ItemStack[] getRecipe(String recipe){
        return recipes.get(recipe);
    }

    public static HashMap<Location, Double> lockedBlocks = new HashMap<>();
    /* lockedBlocks
       -1 = locked
       0 = open
       0-100 charging
       100-200 crafting
       200 finish
     */

    public static HashMap<Location, Location> connectedInjectors = new HashMap<>();

    public static boolean viewRecipe(Player p,ItemStack item){
        for (String recipe: recipes.keySet()) {
            ItemStack result = recipes.get(recipe)[1];
            if (result.isSimilar(item)){
                 p.sendMessage("Found Recipe! " + recipe);
                if (viewRecipe(p,recipe)){
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean viewRecipe(Player p,String recipe){
        if (recipes.get(recipe) == null) return false;
        ItemStack[] items = recipes.get(recipe);
        Inventory inv = Bukkit.createInventory(p,54,"§b注入器配方");
        for (int i = 0; i < 54; i++){
            inv.setItem(i,new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        }

        inv.setItem(22, items[0]);
        inv.setItem(40, items[1]);
        inv.setItem(4,items[2]);

        inv.setItem(9,items[3]);
        inv.setItem(17,items[4]);
        inv.setItem(18,items[5]);
        inv.setItem(26,items[6]);
        inv.setItem(27,items[7]);
        inv.setItem(35,items[8]);
        inv.setItem(36,items[9]);
        inv.setItem(44,items[10]);
        ItemStack energyNeeded = new ItemStack(Material.DRAGON_EGG);
        ItemMeta meta = energyNeeded.getItemMeta();
        meta.setDisplayName("§a能量所需: " + Draconic.CommaNumberFormat(recipepower.get(recipe)) + " §7J ⚡");
        energyNeeded.setItemMeta(meta);
        inv.setItem(31,energyNeeded);
        ItemStack goBack = new ItemStack(Material.BARRIER);
        ItemMeta goBackMeta = energyNeeded.getItemMeta();
        goBackMeta.setDisplayName("§c返回");
        goBack.setItemMeta(goBackMeta);
        inv.setItem(49,goBack);
        p.openInventory(inv);
        return true;
    }

    public static HashMap<String, ItemStack[]> getRecipes() {
        return recipes;
    }

    public static void setRecipes(HashMap<String, ItemStack[]> recipes) {
        FusionCrafting.recipes = recipes;
    }

    public static ItemStack getItemInjector(String tier){
        ItemStack injector = new ItemStack(Material.END_PORTAL_FRAME);
        injector.getItemMeta().setDisplayName(tier + "聚合注入器");
        return injector;
    }

    public static ItemStack getItemInjectortier(int tier){
        ItemStack injector = new ItemStack(Material.END_PORTAL_FRAME);
        ItemMeta meta = injector.getItemMeta();
        SlimefunItem item = SlimefunItem.getById("DRACONIC_INJECTOR");
        if (item == null)
        {
            if(tier == 1) meta.setDisplayName("§f基础聚合注入器");
            if(tier == 2) meta.setDisplayName("§d飞龙聚合注入器");
            if(tier == 3) meta.setDisplayName("§6神龙聚合注入器");
            if(tier == 4) meta.setDisplayName("§8混沌聚合注入器");
            injector.setItemMeta(meta);
        }
        else{
            if(tier == 1) injector = SlimefunItem.getById("BASIC_INJECTOR").getItem();
            if(tier == 2) injector = SlimefunItem.getById("WYVERN_INJECTOR").getItem();
            if(tier == 3) injector = SlimefunItem.getById("DRACONIC_INJECTOR").getItem();
            if(tier == 4) injector = SlimefunItem.getById("DRACONIC_INJECTOR").getItem();
        }
        return injector;
    }

    public static int getItemInjectorTier(ItemStack injector){
        int tier = 1;
        ItemMeta meta = injector.getItemMeta();
        if (meta.getDisplayName().contains("Basic")) tier = 1;
        if (meta.getDisplayName().contains("Wyvern")) tier = 2;
        if (meta.getDisplayName().contains("Draconic")) tier = 3;
        if (meta.getDisplayName().contains("Chaotic")) tier = 4;
        return tier;
    }

    public static int getItemInjectorTier(String injector){
        int tier = 1;
        if (injector.contains("Basic")) tier = 1;
        if (injector.contains("Wyvern")) tier = 2;
        if (injector.contains("Draconic")) tier = 3;
        if (injector.contains("Chaotic")) tier = 4;
        return tier;
    }


     @EventHandler(priority = EventPriority.MONITOR)
     public void onInteract(PlayerInteractEvent e){
         Player p = e.getPlayer();
         if (e.useInteractedBlock() == Event.Result.DENY){
             return;
         }
         if (!p.getName().contains("LidanTheGamer"))
         {
             return;
         }
         if (cooldowns.get(p) == null){
             cooldowns.put(p,System.currentTimeMillis() - 200);
         }
         if (System.currentTimeMillis() - cooldowns.get(p) <= 200) {
              p.sendMessage("Click cooldown " + (System.currentTimeMillis() - cooldowns.get(p)));
             return;
         }
         if (e.getAction() == Action.RIGHT_CLICK_BLOCK){
             p.sendMessage("useInteractedBlock + " + e.useInteractedBlock());
             p.sendMessage("useItemInHand + " + e.useItemInHand());
             p.sendMessage("isCancelled + " + e.isCancelled());
             Block block = e.getClickedBlock();
             ItemStack tool = p.getInventory().getItemInMainHand();
              p.sendMessage("Interact 1");
              p.sendMessage(Draconic.blockdata.get(block.getLocation()));
             HashMap<String,Object> blockdata = Database.select(block.getLocation());
              p.sendMessage("Interact 2");
             if (blockdata.size() == 0) return;
             e.setCancelled(true);
             lockedBlocks.putIfAbsent(e.getClickedBlock().getLocation(), 0d);
             if (lockedBlocks.get(e.getClickedBlock().getLocation()) == -1d) {
                 p.sendMessage("This block is locked " + block.getLocation());
                 return;
             }
             if(blockdata.get("type").toString().contains("Core")){
                 if (System.currentTimeMillis() - cooldowns.get(p) <= 2000) return;
                 //TODO: fix stupid error with locking core [for now disabled]
                 
                 if (lockedBlocks.get(e.getClickedBlock().getLocation()) == 0) {
                     p.sendMessage("click on core at " + e.getClickedBlock().getLocation());
                     lockedBlocks.put(e.getClickedBlock().getLocation(), -1d);
                 }
                  
                 if (lockedBlocks.get(e.getClickedBlock().getLocation()) > 0){
                     p.sendMessage("click on locked core at " + e.getClickedBlock().getLocation());
                 }
                 Draconic.allvars.put("openinv::" + p.getName(),e.getClickedBlock().getLocation().clone());
                 openInventory1(p, block.getLocation());
                 p.sendMessage("Interact 3");
             }
             else if(blockdata.get("type").toString().contains("Injector")){
                 ItemStack item = (ItemStack) blockdata.get("item");
                  p.sendMessage("Interact 3");
                 if (item.getType() != Material.AIR){
                     blockdata.put("item",new ItemStack(Material.AIR));
                     Draconic.giveItem(p,item);
                 }
                 if(tool.getType() != Material.AIR){
                     e.setCancelled(true);
    
                     if (tool.getAmount() > 1) {
                         tool.setAmount(tool.getAmount() - 1);
                         p.getInventory().setItemInMainHand(tool);
                     }
                     else{
                         p.getInventory().removeItem(tool);
                     }
                     tool.setAmount(1);
                     blockdata.put("item",tool);
                 }
                 Database.setblock(block.getLocation(),blockdata);
                 createFusionHolo(block.getLocation(),2);
             }
    
              p.getOpenInventory().setItem();
              p.getInventory();
    
         }
         cooldowns.put(p,System.currentTimeMillis());
         // TODO: finish fusion
     }

    public static void openInventory1(Player p, Location location){
        Inventory inv = Bukkit.createInventory(p,54,"§b注入器合成");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        }
         p.sendMessage("openinv 1");
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                Location blockloc = (Location) Draconic.allvars.get("openinv::" + p.getName());
                HashMap<String,Object> blockdata = Database.select(blockloc);
                 p.sendMessage("prepare in " + (System.currentTimeMillis() - now));
                ItemStack[] items = getCloseInjectors(location);
                 p.sendMessage("close injectors " + (System.currentTimeMillis() - now));
                 p.sendMessage("item in core is " + (ItemStack) blockdata.get("item") + "at " + blockloc);
                inv.setItem(22, (ItemStack) blockdata.get("item"));
                inv.setItem(9,items[0]);
                inv.setItem(17,items[1]);
                inv.setItem(18,items[2]);
                inv.setItem(26,items[3]);
                inv.setItem(27,items[4]);
                inv.setItem(35,items[5]);
                inv.setItem(36,items[6]);
                inv.setItem(44,items[7]);
                inv.setItem(4,items[8]);
                inv.setItem(40, (ItemStack) Draconic.DracSerializer.deserialize("rO0ABXNyABpvcmcuYnVra2l0LnV0aWwuaW8uV3JhcHBlcvJQR+zxEm8FAgABTAADbWFwdAAPTGphdmEvdXRpbC9NYXA7eHBzcgA1Y29tLmdvb2dsZS5jb21tb24uY29sbGVjdC5JbW11dGFibGVNYXAkU2VyaWFsaXplZEZvcm0AAAAAAAAAAAIAAlsABGtleXN0ABNbTGphdmEvbGFuZy9PYmplY3Q7WwAGdmFsdWVzcQB+AAR4cHVyABNbTGphdmEubGFuZy5PYmplY3Q7kM5YnxBzKWwCAAB4cAAAAAR0AAI9PXQAAXZ0AAR0eXBldAAEbWV0YXVxAH4ABgAAAAR0AB5vcmcuYnVra2l0LmludmVudG9yeS5JdGVtU3RhY2tzcgARamF2YS5sYW5nLkludGVnZXIS4qCk94GHOAIAAUkABXZhbHVleHIAEGphdmEubGFuZy5OdW1iZXKGrJUdC5TgiwIAAHhwAAAKGnQACkRSQUdPTl9FR0dzcQB+AABzcQB+AAN1cQB+AAYAAAADcQB+AAh0AAltZXRhLXR5cGV0AAxkaXNwbGF5LW5hbWV1cQB+AAYAAAADdAAISXRlbU1ldGF0AApVTlNQRUNJRklDdACbeyJleHRyYSI6W3siYm9sZCI6dHJ1ZSwiaXRhbGljIjpmYWxzZSwidW5kZXJsaW5lZCI6ZmFsc2UsInN0cmlrZXRocm91Z2giOmZhbHNlLCJvYmZ1c2NhdGVkIjpmYWxzZSwiY29sb3IiOiJsaWdodF9wdXJwbGUiLCJ0ZXh0IjoiU3RhcnQgRnVzaW9uIn1dLCJ0ZXh0IjoiIn0="));
                 p.sendMessage("inventory make " + (System.currentTimeMillis() - now));
                new BukkitRunnable() {

                    @Override
                    public void run() {
                        p.openInventory(inv);
                         p.sendMessage("open fusion core finished in " + (System.currentTimeMillis() - now));
                    }
                }.runTask(Draconic.getInstance());
            }
        }.runTaskAsynchronously(Draconic.getInstance());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void breakBlock(BlockBreakEvent e) {
        Player p = (Player) e.getPlayer();
        Location blockloc = e.getBlock().getLocation();
        HashMap<String,Object> blockdata = Database.select(blockloc);
        if (blockdata.size() == 0) {return;}
        if (!Slimefun.getProtectionManager().hasPermission(p, blockloc, Interaction.BREAK_BLOCK)) return;
        if (!e.isCancelled()) {
            ItemStack item = (ItemStack) blockdata.get("item");
            if (item == null) {
                item = new ItemStack(Material.AIR);
            }
            if (lockedBlocks.get(blockloc) != 0d){
                p.sendMessage("§c你无法破坏上锁的方块");
                e.setCancelled(true);
                return;
            }
            Draconic.giveItem(p,item);
            Database.delete(blockloc);
            for (Hologram holo : HologramsAPI.getHolograms(Draconic.getInstance())) {
                double distance = holo.getLocation().distance(e.getBlock().getLocation());
                if (distance < 2.5d) {
                    holo.delete();
                    break;
                }
                
                else {
                    p.sendMessage("holo at " + holo.getLocation() + " distance=" + distance);
                }
                 
            }
        }
    }

    @EventHandler
    public void dragInv(InventoryDragEvent e){
        Player p = (Player) e.getWhoClicked();
        String invname = p.getOpenInventory().getTitle();
        if (invname.contains("§b注入器")){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void clickInventory1(InventoryClickEvent e){
        Player p = (Player) e.getWhoClicked();
        String invname = p.getOpenInventory().getTitle();
        Inventory inv = e.getInventory();
        if (invname.contains("§b注入器配方")){
            e.setCancelled(true);
            PlayerProfile profile = PlayerProfile.find(p).get();
            if (e.getRawSlot() == 49){
                profile.getGuideHistory().goBack(Slimefun.getRegistry().getSlimefunGuide(SlimefunGuideMode.SURVIVAL_MODE));
                 SlimefunGuide.openMainMenu(profile,SlimefunGuideMode.SURVIVAL_MODE, 1);
                 SlimefunGuide.openMainMenuAsync(p, SlimefunGuideMode.SURVIVAL_MODE, 1);
            }
            else if(!e.getCurrentItem().equals(new ItemStack(Material.BLACK_STAINED_GLASS_PANE))){
                SlimefunGuide.displayItem(profile,e.getCurrentItem(),true);
            }
        }
        else if (invname.contains("§b注入器合成")){
            Location blockloc = ((Location) Draconic.allvars.get("openinv::" + p.getName())).clone();
             p.sendMessage("invname = " + invname + " rawslot " + e.getRawSlot() +
                    " slot " + e.getSlot());
            ItemStack olditem = p.getOpenInventory().getItem(22);

            if (e.getRawSlot() >= 0 && e.getRawSlot() <= 53) { // clicked inside inventory
                if (e.getRawSlot() != 22) {
                    e.setCancelled(true);
                    if (e.getRawSlot() == 40){
                        ItemStack[] items = new ItemStack[11];
                        ItemStack[] invitems = inv.getContents();
                        items[0] = invitems[22];
                        items[1] = new ItemStack(Material.AIR);
                        items[2] = invitems[4];
                        items[3] = invitems[9];
                        items[4] = invitems[17];
                        items[5] = invitems[18];
                        items[6] = invitems[26];
                        items[7] = invitems[27];
                        items[8] = invitems[35];
                        items[9] = invitems[36];
                        items[10] = invitems[44];
                        p.closeInventory();
                        boolean fusionWorks = false;
                        for (String recipe: recipes.keySet()) {
                            if (FuseRecipe(p, blockloc, items, recipe)){
                                fusionWorks = true;
                            }

                        }
                        // arry = {core,fused,injector,+8 items} 11 size 10 length
                        if (fusionWorks == false){
                            lockedBlocks.put(blockloc,0d);
                            unlockConnectedInjectors(blockloc);
                            p.sendMessage("§c此配方无效，请在粘液科技书中查看所有配方!");
                        }
                    }
                } else {

                }
            }
            if (e.getClick() == ClickType.DOUBLE_CLICK || e.getClick().isShiftClick()) {
                e.setCancelled(true);
            }
        }
    }

    public boolean FuseRecipe(Player p, Location oblockloc, ItemStack[] items, String recipename) {
        Location blockloc = oblockloc.clone();
        Location hololoc = oblockloc.clone();
        blockloc.add(0.5,1,0.5);
        hololoc.add(0.5,2,0.5);
        lockConnectedInjectors(oblockloc);
        lockedBlocks.put(oblockloc,-1d);
        HashMap<String,Object> blockdata = Database.select(oblockloc);
        if (checkRecipe(items,recipename)){
             p.sendMessage("Recipe work! " + recipename);
            EnergyBreaker breaker = new EnergyBreaker();
            Hologram craftingHologram = HologramsAPI.createHologram(Draconic.getInstance(), hololoc);
            TextLine line1 = craftingHologram.appendTextLine("&6Charging %");
            line1.setText("&6充能 0%");
            TextLine line2 = craftingHologram.appendTextLine("&6EN/MAXEN ");
            line2.setText("&a0/1M &7J &e⚡");
            new BukkitRunnable(){
                int energyGot = 0;
                int particle = 0;
                @Override
                public void run() {
                    int totalEnergy = recipepower.get(recipename);
                    int energyPerTick = totalEnergy / 100 / 8;
                    double percentloaded = 0d;
                    particle++;

                    for (HashMap.Entry<Location, Location> entry : connectedInjectors.entrySet()) {
                        if (entry.getValue().equals(oblockloc)) {
                            int charge = breaker.getCharge(entry.getKey());
                            if (charge > 0) {
                                if (charge >= energyPerTick) {
                                    charge = energyPerTick;
                                }
                                energyGot += charge;
                                breaker.removeCharge(entry.getKey(), charge);
                                 p.sendMessage("more energy added " + charge);
                            }
                        }
                    }
                    percentloaded = (double)energyGot/totalEnergy*100d;
                    String percentloadeds = String.format("%.2f",percentloaded);
                    line1.setText("&6充能 " + percentloadeds + "%");
                    line2.setText("&a" + Draconic.BigNumber(energyGot) +"/" + Draconic.BigNumber(totalEnergy) + " &7J &e⚡");
                    if (particle % 2 == 0){
                        oblockloc.getWorld().playSound(oblockloc, Sound.ITEM_FLINTANDSTEEL_USE,0.7f,1.3f);
                        oblockloc.getWorld().spawnParticle(Particle.CRIT_MAGIC,blockloc,5);
                    }
                     p.sendMessage("Energy: " + energyGot + "/" + totalEnergy);
                    if (percentloaded != 0){
                        lockedBlocks.put(oblockloc,percentloaded);
                    }
                    if (energyGot >= totalEnergy) {
                        craftingHologram.delete();
                        for (Hologram holo : HologramsAPI.getHolograms(Draconic.getInstance())) {
                            if (holo.getLocation().distance(blockloc) < 5) {
                                 holo.getLocation().subtract();
                                new aiflyholo(holo, blockloc, 5000).runTaskTimer(Draconic.getInstance(), 0, 1);
                                 new (e,p.getLocation(),1000).runTaskTimer(Draconic.getInstance(),0,1);
                            }
                        }
                        new BukkitRunnable() {
                            int particle = 0;
                            @Override
                            public void run() {
                                if (particle > 100){
                                    cancel();
                                }
                                particle++;
                                oblockloc.getWorld().playSound(oblockloc, Sound.ITEM_FLINTANDSTEEL_USE,0.7f,1.3f);
                                oblockloc.getWorld().spawnParticle(Particle.CRIT_MAGIC,blockloc,5);
                            }
                        }.runTaskTimer(Draconic.getInstance(),0,1);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                for (Hologram holo : HologramsAPI.getHolograms(Draconic.getInstance())) {
                                    if (holo.getLocation().distance(oblockloc) < 5) {
                                        holo.delete();
                                         new (e,p.getLocation(),1000).runTaskTimer(Draconic.getInstance(),0,1);
                                    }
                                }
                                for (HashMap.Entry<Location, Location> entry : connectedInjectors.entrySet()) {
                                     System.out.println("Injectors " + entry.getValue() + " Core " + oblockloc);
                                    if (entry.getValue().equals(oblockloc)) {
                                         System.out.println("scam " + entry.getKey());
                                        HashMap<String, Object> blockdata = Database.select(entry.getKey());
                                        ItemStack itemscam = (ItemStack) blockdata.get("item");
                                        itemscam.setAmount(itemscam.getAmount() - 1);
                                        blockdata.put("item", new ItemStack(Material.AIR));
                                        Database.setblock(entry.getKey(), blockdata);
                                    }
                                }
                                oblockloc.getWorld().playSound(blockloc, Sound.ENTITY_GENERIC_EXPLODE,1,1);
                                oblockloc.getWorld().spawnParticle(Particle.CRIT_MAGIC,blockloc,100);

                                lockedBlocks.put(oblockloc,0d);
                                 Bukkit.getPlayer("LidanTheGamer_").sendMessage("Finish Fusion");
                                unlockConnectedInjectors(oblockloc);
                                blockdata.put("item", getRecipe(recipename)[1]);
                                Database.setblock(oblockloc, blockdata);
                                createFusionHolo(oblockloc, 1);
                            }
                        }.runTaskLater(Draconic.getInstance(), 101L);
                        cancel();
                    }
                }
            }.runTaskTimer(Draconic.getInstance(),0L,1L);
            return true;
        }
        else{
             p.sendMessage("Recipe not work!");
            return false;
        }
    }

    public static void lockConnectedInjectors(Location location) {
        for (HashMap.Entry<Location, Location> entry : connectedInjectors.entrySet()){
            if (entry.getValue().equals(location)){
                 Bukkit.getPlayer("LidanTheGamer_").sendMessage("Lock injector at " + entry.getKey());
                lockedBlocks.put(entry.getKey(),-1d);
            }
        }
    }

    public static void unlockConnectedInjectors(Location location) {
        for (HashMap.Entry<Location, Location> entry : connectedInjectors.entrySet()){
            if (entry.getValue().equals(location)){
                 Bukkit.getPlayer("LidanTheGamer_").sendMessage("Unlock injector at " + entry.getKey());
                lockedBlocks.put(entry.getKey(),0d);
            }
        }
    }

    @EventHandler
    public static void onInvClose(InventoryCloseEvent e){
        Player p = (Player) e.getPlayer();
        String invname = e.getView().getTitle();
        if (invname.contains("§b注入器合成")){
            ItemStack newitem = e.getInventory().getItem(22);
            Location blockloc = (Location) Draconic.allvars.get("openinv::" + p.getName());
            if (blockloc == null) {
                p.sendMessage("error block doesn't exists");
                return;
            }
            HashMap<String,Object> blockdata = Database.select(blockloc);
            blockdata.put("item",newitem);
            Database.setblock(blockloc,blockdata);
             p.sendMessage("saved item " + newitem + " at " + blockloc);
            createFusionHolo(blockloc,1);
            lockedBlocks.putIfAbsent(blockloc, 0d);
             p.sendMessage("locked " + lockedBlocks.get(blockloc));
        }
    }

    public static ItemStack[] getCloseInjectors(Location center,int size){
         System.out.println("GetCloseInjectors " + center + " size " + size);
        ArrayList<Block> injectors = new ArrayList<>();
        for (HashMap.Entry<Location, Location> entry : connectedInjectors.entrySet()){
            if (injectors.size() == 8) break;
            if (entry.getValue().equals(center)) {
               ("connectedinjector found");
                injectors.add(center.getWorld().getBlockAt(entry.getKey()));
            }
            HashMap<String,Object> blockdata = Database.select(entry.getKey());
            if (blockdata.size() == 0){
                connectedInjectors.remove(entry.getKey());
                injectors.remove(center.getWorld().getBlockAt(entry.getKey()));
            }
        }
        int itemIndex = 0;
        ItemStack air = new ItemStack(Material.AIR);
        ItemStack[] items = {air,air,air,air,air,air,air,air,getItemInjectortier(4)};
        ArrayList<Block> blocks = new ArrayList<>();
        if (injectors.size() >= 8){
             Bukkit.getPlayer("LidanTheGamer_").sendMessage("close injectors!");
            blocks = injectors;
        }
        else{
             Bukkit.getPlayer("LidanTheGamer_").sendMessage("non close injectors! " + center + " " + size);
            blocks = Draconic.loopblockscube(center,size);
             Bukkit.getPlayer("LidanTheGamer_").sendMessage("non close injectors! 2 " + blocks);
        }
        for (Block block: blocks) {
             Bukkit.getPlayer("LidanTheGamer_").sendMessage("block found " + block.getLocation());
            HashMap<String,Object> blockdata = Database.select(block.getLocation());
            if (itemIndex >= 8) break;
             Bukkit.getPlayer("LidanTheGamer_").sendMessage("injector found " + block.getLocation());
            if(blockdata.size() > 0 && ((String) blockdata.get("type")).contains("Injector")){
                if (!(connectedInjectors.get(block.getLocation()) == null || connectedInjectors.get(block.getLocation()).equals(center))){
                     Bukkit.getPlayer("LidanTheGamer_").sendMessage("injector already connected " + block
                     .getLocation());
                    continue;
                }
                connectedInjectors.put(block.getLocation(), center);
                String type = (String) blockdata.get("type");
                int tier = getItemInjectorTier(type);
                if (tier < getItemInjectorTier(items[8])){
                    items[8] = getItemInjectortier(tier);
                }
                items[itemIndex] = (ItemStack) blockdata.get("item");
                 Bukkit.getPlayer("LidanTheGamer_").sendMessage("getinject" + blockdata.get("item") + " at: " +
                 block.getLocation());
                itemIndex++;
            }
        }
        return items;
    }

    public static ItemStack[] getCloseInjectors(Location center) {
        return getCloseInjectors(center,4);
    }

    public static void createFusionHolo(Location loc,double above){
        HashMap<String,Object> blockdata = Database.select(loc);
        if (blockdata.size() == 0) return;
        loc.add(0.5, above,0.5);
        for (Hologram holo: HologramsAPI.getHolograms(Draconic.getInstance())) {
            if (holo.getLocation().distance(loc) < 1)
            {
                holo.delete();
                break;
            }
        }
        Hologram hologram = HologramsAPI.createHologram(Draconic.getInstance(), loc);
         TextLine textLine = hologram.appendTextLine("Injector");
        if(blockdata.get("item") == null){
            return;
        }
        ItemStack item = (ItemStack) blockdata.get("item");
        if (item.getAmount() > 0) {
            ItemLine itemline = hologram.appendItemLine(item);
        }
    }

    public static boolean checkRecipe(ItemStack[] items,String recipename){
        try {

            ItemStack[] recipe = getRecipe(recipename);
            ArrayList<ItemStack> recipeal = new ArrayList<>(Arrays.asList(recipe));
            ArrayList<ItemStack> itemal = new ArrayList<>(Arrays.asList(items));
             System.out.println("Fusion Crafting items phase 1 " + itemal.size());
            for (int i = 0; i < itemal.size(); i++) {
                 System.out.println("" + i + " " + itemal.get(i) + " " + recipeal.get(i));
                if (itemal.get(i) == null)
                    itemal.set(i,new ItemStack(Material.AIR));
                if (i != 1) {
                    if (recipeal.get(i) instanceof SlimefunItemStack){
                        String id1 = SlimefunItem.getByItem(itemal.get(i)).getId();
                        String id2 = ((SlimefunItemStack) recipeal.get(i)).getItemId();
                        if (id1.equals(id2)){
                            ItemStack fix = new ItemStack(Material.DRAGON_EGG);
                            ItemMeta meta = fix.getItemMeta();
                            meta.setDisplayName("SlimeFunItem(" + id1 + ")");
                            fix.setItemMeta(meta);
                            recipeal.set(i,fix);
                            itemal.set(i,fix);
                        }
                    }
                }
            }
            if (recipeal.get(0).isSimilar(itemal.get(0))) {
                if (recipeal.get(0).getAmount() == itemal.get(0).getAmount()) {
                     System.out.println("IF 1 WORK");
                    recipeal.remove(0);
                    itemal.remove(0);
                    recipeal.remove(0);
                    itemal.remove(0);
                }
            }
            System.out.println("Fusion Crafting items phase 2 " + itemal.size());
            for (int i = 0; i < itemal.size(); i++) {
                System.out.println("" + i + " " + itemal.get(i) + " " + recipeal.get(i));
            }
            if (getItemInjectorTier(itemal.get(0)) > getItemInjectorTier(recipeal.get(0))) {
                recipeal.remove(0);
                itemal.remove(0);
            }
            System.out.println("Fusion Crafting items phase 3 " + itemal.size());
            for (int i = 0; i < itemal.size(); i++) {
                System.out.println("" + i + " " + itemal.get(i) + " " + recipeal.get(i));
            }
             System.out.println("Fusion Crafting Try");
            for (ItemStack i : recipeal) {
                if (!itemal.contains(i)) {
                     System.out.println("item not in recipe! " + i);
                    return false;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
        
        inv.setItem(22, items[0]);
        inv.setItem(40, items[1]);
        inv.setItem(4,items[2]);

        inv.setItem(9,items[3]);
        inv.setItem(17,items[4]);
        inv.setItem(18,items[5]);
        inv.setItem(26,items[6]);
        inv.setItem(27,items[7]);
        inv.setItem(35,items[8]);
        inv.setItem(36,items[9]);
        inv.setItem(44,items[10]);
         
    }
    @EventHandler
    public static void onInvOpen(InventoryOpenEvent e){
        Player p = (Player) e.getPlayer();
        InventoryView view = e.getView();
        if (view.getTitle().contains("Slimefun Guide")){
             p.sendMessage("Slime fun guide!");
             p.sendMessage("slot 10 " + view.getItem(10));
             p.sendMessage("core " + SlimefunItem.getById("FUSION_CORE").getItem());
            SlimefunItem slot10 = SlimefunItem.getByItem(view.getItem(10));
            if (slot10.equals(SlimefunItem.getById("FUSION_CORE"))){
                 p.sendMessage("Checking Fusion!");
                Scheduler.run(2, new Runnable() {
                    @Override
                    public void run() {
                        viewRecipe(p,view.getItem(16));
                    }
                });
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }
}
