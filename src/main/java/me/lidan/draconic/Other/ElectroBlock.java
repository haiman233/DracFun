package me.lidan.draconic.Other;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSetting;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import me.lidan.draconic.Database.Database;
import me.lidan.draconic.Draconic;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Optional;

import static me.lidan.draconic.Fusion.FusionCrafting.*;

public class ElectroBlock extends SlimefunItem implements EnergyNetComponent {
    int capacity;

    public ElectroBlock(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe,
                        int capacity) {
        super(itemGroup, item, recipeType, recipe);
        this.capacity = capacity;
    }

    @Override
    public EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public void preRegister() {
        BlockUseHandler blockUseHandler = this::onBlockRightClick;
        addItemHandler(blockUseHandler);
    }

    private void onBlockRightClick(PlayerRightClickEvent e) {
        Player p = e.getPlayer();
        if (cooldowns.get(p) == null) {
            cooldowns.put(p, System.currentTimeMillis() - 3000);
        }
        if (System.currentTimeMillis() - cooldowns.get(p) <= 200) {
            // p.sendMessage("Click cooldown " + (System.currentTimeMillis() - cooldowns.get(p)));
            return;
        }
        Block block = e.getClickedBlock().get();
        if (!Slimefun.getProtectionManager().hasPermission(p, block, Interaction.INTERACT_BLOCK)) return;
        ItemStack tool = p.getInventory().getItemInMainHand();
        // p.sendMessage("Interact 1");
        // p.sendMessage(Draconic.blockdata.get(block.getLocation()));
        HashMap<String, Object> blockdata = Database.select(block.getLocation());
        // p.sendMessage("Interact 2");
        if (blockdata.size() == 0) return;
        e.cancel();
        lockedBlocks.putIfAbsent(block.getLocation(), 0d);
        if (lockedBlocks.get(block.getLocation()) == -1d) {
            p.sendMessage("§c该方块已上锁");
            return;
        }
        // p.sendMessage("[DEBUG] " + lockedBlocks.get(block.getLocation()));
        if (blockdata.get("type").toString().contains("Core")) {
            if (System.currentTimeMillis() - cooldowns.get(p) <= 2000) return;
            //TODO: fix stupid error with locking core [for now disabled]
                /*
                if (lockedBlocks.get(block.getLocation()) == 0) {
                    p.sendMessage("click on core at " + block.getLocation());
                    lockedBlocks.put(block.getLocation(), -1d);
                }
                 */
            if (lockedBlocks.get(block.getLocation()) > 0) {
                p.sendMessage("§c该核心已上锁");
                return;
            }
            Draconic.allvars.put("openinv::" + p.getName(), block.getLocation().clone());
            Location oblockloc = block.getLocation().clone();
            for (Player loop_player: Bukkit.getOnlinePlayers()) {
                if (Draconic.allvars.get("openinv::" + loop_player.getName()) != null){
                    Location value = (Location) Draconic.allvars.get("openinv::" + loop_player.getName());
                    if (value.equals(oblockloc)) {
                        loop_player.closeInventory();
                    }
                }
            }
            openInventory1(p, block.getLocation());
            // p.sendMessage("Interact 3");
        } else if (blockdata.get("type").toString().contains("Injector")) {
            ItemStack item = (ItemStack) blockdata.get("item");
            // p.sendMessage("Interact 3");
            if (item.getType() != Material.AIR) {
                blockdata.put("item", new ItemStack(Material.AIR));
                Draconic.giveItem(p, item);
            }
            if (tool.getType() != Material.AIR) {
                e.cancel();

                if (tool.getAmount() > 1) {
                    tool.setAmount(tool.getAmount() - 1);
                    p.getInventory().setItemInMainHand(tool);
                } else {
                    p.getInventory().removeItem(tool);
                }
                tool.setAmount(1);
                blockdata.put("item", tool);
            }
            Database.setblock(block.getLocation(), blockdata);
            createFusionHolo(block.getLocation(), 2);
        }
            /*
             p.getOpenInventory().setItem();
             p.getInventory();
            */
        cooldowns.put(p, System.currentTimeMillis());
    }

    @Override
    public int getCapacity() {
        return capacity;
    }
}
