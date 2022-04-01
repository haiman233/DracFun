package me.lidan.draconic;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import de.slikey.effectlib.effect.CircleEffect;
import de.slikey.effectlib.effect.SphereEffect;
import de.slikey.effectlib.util.DynamicLocation;
import io.github.mooy1.infinitylib.commands.AddonCommand;
import io.github.mooy1.infinitylib.common.CoolDowns;
import io.github.mooy1.infinitylib.common.Scheduler;
import io.github.mooy1.infinitylib.core.AbstractAddon;
import io.github.mooy1.infinitylib.core.AddonConfig;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.Rechargeable;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.generators.SolarGenerator;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.ChargingBench;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.LoreBuilder;
import me.lidan.draconic.Commands.aiflycmd;
import me.lidan.draconic.Commands.DraconicCmd;
import me.lidan.draconic.Database.Database;
import me.lidan.draconic.Events.Damage;
import me.lidan.draconic.Events.Death;
import me.lidan.draconic.Events.Interact;
import me.lidan.draconic.Every.DraconicHoloLoad;
import me.lidan.draconic.Every.Tick;
import me.lidan.draconic.Fusion.FusionCrafting;
import me.lidan.draconic.Other.ElectroBlock;
import me.lidan.draconic.Other.ErrorFile;
import me.lidan.draconic.Other.Serializer;
import me.lidan.draconic.Other.DraconicArmorPiece;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public final class Draconic extends AbstractAddon {
    // private static Draconic instance = null;
    private static String connectionUrl;
    public static HashMap<Player,BossBar> bars = new HashMap<>();
    // public static HashMap<Location,HashMap<String,Object>> blockdata = new HashMap<>();
    public static HashMap<String,Double> vars = new HashMap<>();
    public static HashMap<String,Object> allvars = new HashMap<>();
    public static Tick everytick = new Tick();
    public static DraconicHoloLoad every5secs = new DraconicHoloLoad();
    public static Serializer DracSerializer = new Serializer();
    // public static BossBar bar = Bukkit.createBossBar("Test", BarColor.BLUE, BarStyle.SOLID);
    public static ItemGroup DraconicGroup;
    public static AddonConfig cfg = null;
    public static EffectManager effectManager;

    public Draconic() {
        super("LidanTheDev", "DracFun", "master", "autoupdate");
    }

    public static Draconic getInstance() {
        final Draconic i = instance();
        if (i == null)
            throw new IllegalStateException("Instance is not created!");
        return i;
    }

    public static String getConnectionUrl() {
        return connectionUrl;
    }

    @Override
    public void enable() {
        // Plugin startup logic
        try {
            cfg = new AddonConfig(this.getDataFolder().getPath() + "/config.yml");
        } catch(Exception error){
            getLogger().severe("Error happened when loading config");
            error.printStackTrace();
        }
        try {
            setup();

        } catch (Exception error){
            getLogger().severe("Error happened when loading items");
            error.printStackTrace();
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays")) {
            getLogger().severe("*** HolographicDisplays is not installed or not enabled. ***");
            getLogger().severe("*** This plugin will be disabled. ***");
            this.setEnabled(false);
            return;
        }
        connectionUrl = "jdbc:sqlite:" + getDataFolder().getAbsolutePath() + "/data.db";
        Database.initDatabase();
        getServer().getPluginManager().registerEvents(new Damage(),this);
        getServer().getPluginManager().registerEvents(new Interact(),this);
        getServer().getPluginManager().registerEvents(new Death(),this);
        getServer().getPluginManager().registerEvents(new FusionCrafting(),this);
        everytick.runTaskTimer(this,0L,5L);
        getCommand("aiflyto").setExecutor(new aiflycmd());
        getCommand("draconic").setExecutor(new DraconicCmd());
        //arry = {core,fused,injector,+8 items} 11 size 10 length
        FusionCrafting.addRecipe("test", new ItemStack[]{new ItemStack(Material.IRON_BLOCK),
                new ItemStack(Material.DIAMOND_BLOCK),FusionCrafting.getItemInjectortier(1),
                new ItemStack(Material.DIAMOND),new ItemStack(Material.DIAMOND),new ItemStack(Material.DIAMOND),new ItemStack(Material.DIAMOND),new ItemStack(Material.DIAMOND),new ItemStack(Material.DIAMOND),new ItemStack(Material.DIAMOND),new ItemStack(Material.DIAMOND)});
        FusionCrafting.addRecipe("test2", new ItemStack[]{new ItemStack(Material.DIAMOND_BLOCK),
                new ItemStack(Material.EMERALD_BLOCK),FusionCrafting.getItemInjectortier(1),
                new ItemStack(Material.EMERALD),new ItemStack(Material.EMERALD),new ItemStack(Material.EMERALD),
                new ItemStack(Material.EMERALD),new ItemStack(Material.EMERALD),new ItemStack(Material.EMERALD),
                new ItemStack(Material.AIR),new ItemStack(Material.AIR)});
        FusionCrafting.addRecipe("test3", new ItemStack[]{new ItemStack(Material.NETHERITE_CHESTPLATE),
                new ItemStack(Material.ELYTRA),FusionCrafting.getItemInjectortier(4),
                new ItemStack(Material.SHULKER_SHELL),new ItemStack(Material.SHULKER_SHELL),
                new ItemStack(Material.LEATHER),
                new ItemStack(Material.LEATHER),new ItemStack(Material.END_CRYSTAL),new ItemStack(Material.END_CRYSTAL),
                new ItemStack(Material.AIR),new ItemStack(Material.AIR)},10000000);
        FusionCrafting.addRecipe("test4", new ItemStack[]{SlimefunItems.NUCLEAR_REACTOR,
                new ItemStack(SlimefunItems.NETHER_STAR_REACTOR),FusionCrafting.getItemInjectortier(4),
                new ItemStack(Material.NETHER_STAR),new ItemStack(Material.NETHER_STAR),
                new ItemStack(Material.NETHER_STAR),
                new ItemStack(Material.NETHER_STAR),new ItemStack(Material.END_CRYSTAL),
                new ItemStack(Material.END_CRYSTAL),
                new ItemStack(Material.SOUL_SAND),new ItemStack(Material.SOUL_SAND)},1000000000);
        FusionCrafting.addRecipe("WYVERN_INJECTOR",
                new ItemStack[]{new ItemStack(SlimefunItem.getById("BASIC_INJECTOR").getItem()),
                new ItemStack(SlimefunItem.getById("WYVERN_INJECTOR").getItem()),FusionCrafting.getItemInjectortier(1),
                new ItemStack(SlimefunItem.getById("WYVERN_CORE").getItem()),new ItemStack(SlimefunItem.getById("DRACONIC_CORE").getItem()),
                new ItemStack(SlimefunItem.getById("DRACONIC_CORE").getItem()),
                new ItemStack(SlimefunItem.getById("DRACONIUM_BLOCK").getItem())
                ,new ItemStack(Material.DIAMOND),new ItemStack(Material.DIAMOND),
                new ItemStack(Material.DIAMOND),new ItemStack(Material.DIAMOND)},25600);

        ItemStack AWAKENED_DRACONIUM_BLOCK_4 = new ItemStack(SlimefunItem.getById("AWAKENED_DRACONIUM_BLOCK").getItem());
        AWAKENED_DRACONIUM_BLOCK_4.setAmount(4);
        ItemStack DRACONIUM_BLOCK_4 = new ItemStack(SlimefunItem.getById("DRACONIUM_BLOCK").getItem());
        DRACONIUM_BLOCK_4.setAmount(4);
        FusionCrafting.addRecipe("AWAKENED_DRACONIUM_BLOCK",
                new ItemStack[]{DRACONIUM_BLOCK_4, AWAKENED_DRACONIUM_BLOCK_4
                ,FusionCrafting.getItemInjectortier(2),
                new ItemStack(SlimefunItem.getById("DRACONIC_CORE").getItem()),new ItemStack(SlimefunItem.getById("DRACONIC_CORE").getItem()),
                new ItemStack(SlimefunItem.getById("DRACONIC_CORE").getItem()),
                new ItemStack(SlimefunItem.getById("DRAGON_HEART").getItem())
                ,new ItemStack(SlimefunItem.getById("DRACONIC_CORE").getItem()),
                new ItemStack(SlimefunItem.getById("DRACONIC_CORE").getItem()),
                new ItemStack(SlimefunItem.getById("DRACONIC_CORE").getItem())
                ,new ItemStack(Material.AIR)},35000000);
        FusionCrafting.addRecipe("AWAKENED_CORE",
                new ItemStack[]{new ItemStack(Material.NETHER_STAR),
                new ItemStack(SlimefunItem.getById("AWAKENED_CORE").getItem())
                ,FusionCrafting.getItemInjectortier(2),
                new ItemStack(SlimefunItem.getById("WYVERN_CORE").getItem()),
                new ItemStack(SlimefunItem.getById("WYVERN_CORE").getItem()),
                new ItemStack(SlimefunItem.getById("AWAKENED_DRACONIUM_INGOT").getItem()),
                new ItemStack(SlimefunItem.getById("AWAKENED_DRACONIUM_INGOT").getItem())
                ,new ItemStack(SlimefunItem.getById("AWAKENED_DRACONIUM_INGOT").getItem()),
                new ItemStack(SlimefunItem.getById("AWAKENED_DRACONIUM_INGOT").getItem()),
                new ItemStack(SlimefunItem.getById("WYVERN_CORE").getItem())
                ,new ItemStack(SlimefunItem.getById("WYVERN_CORE").getItem())},9000000);

        FusionCrafting.addRecipe("DRACONIC_INJECTOR",
                new ItemStack[]{new ItemStack(SlimefunItem.getById("WYVERN_INJECTOR").getItem()),
                        new ItemStack(SlimefunItem.getById("DRACONIC_INJECTOR").getItem())
                        ,FusionCrafting.getItemInjectortier(2),
                        new ItemStack(SlimefunItem.getById("AWAKENED_DRACONIUM_BLOCK").getItem()),
                        new ItemStack(Material.DIAMOND),
                        new ItemStack(Material.DIAMOND),
                        new ItemStack(SlimefunItem.getById("WYVERN_CORE").getItem())
                        ,new ItemStack(SlimefunItem.getById("WYVERN_CORE").getItem()),
                        new ItemStack(Material.DIAMOND),
                        new ItemStack(Material.DIAMOND)
                        ,new ItemStack(Material.AIR)},1792000);

        FusionCrafting.addRecipe("DRACONIC_HELMET",
                new ItemStack[]{SlimefunItem.getById("WYVERN_HELMET").getItem(),
                        new ItemStack(SlimefunItem.getById("DRACONIC_HELMET").getItem())
                        ,FusionCrafting.getItemInjectortier(3),
                        new ItemStack(SlimefunItem.getById("AWAKENED_DRACONIUM_INGOT").getItem()),
                        new ItemStack(Material.AIR),
                        new ItemStack(Material.AIR),
                        new ItemStack(SlimefunItem.getById("AWAKENED_DRACONIUM_INGOT").getItem())
                        ,new ItemStack(SlimefunItem.getById("AWAKENED_CORE").getItem()),
                        new ItemStack(Material.AIR),
                        new ItemStack(Material.AIR)
                        ,new ItemStack(SlimefunItem.getById("DRACONIC_ENERGY_CORE").getItem())},1280000);
        FusionCrafting.addRecipe("DRACONIC_CHESTPLATE",
                new ItemStack[]{SlimefunItem.getById("WYVERN_CHESTPLATE").getItem(),
                        new ItemStack(SlimefunItem.getById("DRACONIC_CHESTPLATE").getItem())
                        ,FusionCrafting.getItemInjectortier(3),
                        new ItemStack(SlimefunItem.getById("AWAKENED_DRACONIUM_INGOT").getItem()),
                        new ItemStack(Material.AIR),
                        new ItemStack(Material.AIR),
                        new ItemStack(SlimefunItem.getById("AWAKENED_DRACONIUM_INGOT").getItem())
                        ,new ItemStack(SlimefunItem.getById("AWAKENED_CORE").getItem()),
                        new ItemStack(Material.AIR),
                        new ItemStack(Material.AIR)
                        ,new ItemStack(SlimefunItem.getById("DRACONIC_ENERGY_CORE").getItem())},1280000);
        FusionCrafting.addRecipe("DRACONIC_LEGGINGS",
                new ItemStack[]{SlimefunItem.getById("WYVERN_LEGGINGS").getItem(),
                        new ItemStack(SlimefunItem.getById("DRACONIC_LEGGINGS").getItem())
                        ,FusionCrafting.getItemInjectortier(3),
                        new ItemStack(SlimefunItem.getById("AWAKENED_DRACONIUM_INGOT").getItem()),
                        new ItemStack(Material.AIR),
                        new ItemStack(Material.AIR),
                        new ItemStack(SlimefunItem.getById("AWAKENED_DRACONIUM_INGOT").getItem())
                        ,new ItemStack(SlimefunItem.getById("AWAKENED_CORE").getItem()),
                        new ItemStack(Material.AIR),
                        new ItemStack(Material.AIR)
                        ,new ItemStack(SlimefunItem.getById("DRACONIC_ENERGY_CORE").getItem())},1280000);
        FusionCrafting.addRecipe("DRACONIC_BOOTS",
                new ItemStack[]{SlimefunItem.getById("WYVERN_BOOTS").getItem(),
                        new ItemStack(SlimefunItem.getById("DRACONIC_BOOTS").getItem())
                        ,FusionCrafting.getItemInjectortier(3),
                        new ItemStack(SlimefunItem.getById("AWAKENED_DRACONIUM_INGOT").getItem()),
                        new ItemStack(Material.AIR),
                        new ItemStack(Material.AIR),
                        new ItemStack(SlimefunItem.getById("AWAKENED_DRACONIUM_INGOT").getItem())
                        ,new ItemStack(SlimefunItem.getById("AWAKENED_CORE").getItem()),
                        new ItemStack(Material.AIR),
                        new ItemStack(Material.AIR)
                        ,new ItemStack(SlimefunItem.getById("DRACONIC_ENERGY_CORE").getItem())},1280000);
        ErrorFile.setup();
        ErrorFile.get().addDefault("A-Number",0);
        // ErrorFile.get().addDefault("Overload-Nerf",0d);
        // ErrorFile.get().set("Bigerror","YES");
        ErrorFile.get().options().copyDefaults(true);
        ErrorFile.save();
        Scheduler.runAsync(new Runnable() {
            @Override
            public void run() {
                Database.selectAllAndDelete();
            }
        });
        every5secs.runTaskLaterAsynchronously(this,100);
        effectManager = new EffectManager(this);
        /*
        if (Slimefun.instance() != null)
            setup();
        else{
            getLogger().log(Level.WARNING,"Error Slimefun instance was null slimefun items didn't load!");
        }
         */

        System.out.println("Draconic Evolution Loaded");
    }

    @Override
    public void disable() {
        // Plugin shutdown logic
        // everytick.cancel();
        Bukkit.getScheduler().cancelTasks(this);
        for(Player p : Bukkit.getOnlinePlayers()){
            if (bars.get(p) != null){
                bars.get(p).removeAll();
            }
        }
        for (Hologram holo: HologramsAPI.getHolograms(this)) {
            holo.delete();
        }
        Slimefun.getRegistry().getAllItemGroups().remove(DraconicGroup);
        System.out.println("Draconic Evolution Unloaded");
    }

    public void setup(){
        NamespacedKey categoryId = new NamespacedKey(this, "Draconic");
        ItemStack categoryItem = new ItemStack(Material.DRAGON_EGG);
        ItemMeta meta = categoryItem.getItemMeta();
        meta.setDisplayName("§6§l龙之研究");
        categoryItem.setItemMeta(meta);
        DraconicGroup = new ItemGroup(categoryId, categoryItem);
        DraconicGroup.register(this);
        ItemStack endcrystal = new ItemStack(Material.END_CRYSTAL);
        Color WyvernColor = Color.fromRGB(227, 3, 252);
        if(SlimefunItem.getById("WYVERN_HELMET") != null){
            Slimefun.getRegistry().getSlimefunItemIds().remove("WYVERN_HELMET");
            Slimefun.getRegistry().getSlimefunItemIds().remove("WYVERN_CHESTPLATE");
            Slimefun.getRegistry().getSlimefunItemIds().remove("WYVERN_LEGGINGS");
            Slimefun.getRegistry().getSlimefunItemIds().remove("WYVERN_BOOTS");
            Slimefun.getRegistry().getSlimefunItemIds().remove("DRACONIC_INJECTOR");
            Slimefun.getRegistry().getSlimefunItemIds().remove("DRACONIC_ENERGY_INFUSER");
            Slimefun.getRegistry().getSlimefunItemIds().remove("CHAOTIC_INJECTOR");
            Slimefun.getRegistry().getSlimefunItemIds().remove("DRACONIC_OP_ENERGY");
            Slimefun.getRegistry().getSlimefunItemIds().remove("DRACONIUM_INGOT");
            Slimefun.getRegistry().getSlimefunItemIds().remove("DRACONIC_CORE");
            Slimefun.getRegistry().getSlimefunItemIds().remove("WYVERN_CORE");
            Slimefun.getRegistry().getSlimefunItemIds().remove("WYVERN_ENERGY_CORE");
            Slimefun.getRegistry().getSlimefunItemIds().remove("DRACONIUM_BLOCK");
            Slimefun.getRegistry().getSlimefunItemIds().remove("DRAGON_HEART");
            Slimefun.getRegistry().getSlimefunItemIds().remove("BASIC_INJECTOR");
            Slimefun.getRegistry().getSlimefunItemIds().remove("FUSION_CORE");
            Slimefun.getRegistry().getSlimefunItemIds().remove("WYVERN_INJECTOR");
            Slimefun.getRegistry().getSlimefunItemIds().remove("AWAKENED_DRACONIUM_BLOCK");
            Slimefun.getRegistry().getSlimefunItemIds().remove("AWAKENED_DRACONIUM_INGOT");
            Slimefun.getRegistry().getSlimefunItemIds().remove("AWAKENED_CORE");
            Slimefun.getRegistry().getSlimefunItemIds().remove("DRACONIC_ENERGY_CORE");
            Slimefun.getRegistry().getSlimefunItemIds().remove("DRACONIC_HELMET");
            Slimefun.getRegistry().getSlimefunItemIds().remove("DRACONIC_CHESTPLATE");
            Slimefun.getRegistry().getSlimefunItemIds().remove("DRACONIC_LEGGINGS");
            Slimefun.getRegistry().getSlimefunItemIds().remove("DRACONIC_BOOTS");
        }


        if(SlimefunItem.getById("DRACONIUM_INGOT") == null){
            SlimefunItemStack draconium_ingot = new SlimefunItemStack("DRACONIUM_INGOT", Material.PURPLE_DYE,
                    "&d龙锭",
                    "&7用于合成龙之研究物品");
            ItemStack[] recipe = {
                    new ItemStack(Material.POPPED_CHORUS_FRUIT),null,null,
                    null,null,null,
                    null,null,null
            };
            SlimefunItem slimeitem = new SlimefunItem(DraconicGroup, draconium_ingot,RecipeType.SMELTERY,
                    recipe);
            slimeitem.register(this);
        }
        SlimefunItemStack DRACONIUM_INGOT = (SlimefunItemStack) SlimefunItem.getById("DRACONIUM_INGOT").getItem();

        if(SlimefunItem.getById("DRACONIUM_BLOCK") == null){
            SlimefunItemStack draconium_block = new SlimefunItemStack("DRACONIUM_BLOCK", Material.PURPUR_BLOCK,
                    "&d龙块",
                    "&7用于合成龙之研究物品");
            ItemStack[] recipe = {
                    DRACONIUM_INGOT, DRACONIUM_INGOT,DRACONIUM_INGOT,
                    DRACONIUM_INGOT,DRACONIUM_INGOT,DRACONIUM_INGOT,
                    DRACONIUM_INGOT,DRACONIUM_INGOT,DRACONIUM_INGOT
            };
            SlimefunItem slimeitem = new SlimefunItem(DraconicGroup, draconium_block,RecipeType.ENHANCED_CRAFTING_TABLE,
                    recipe);
            slimeitem.register(this);
            /*
            SlimefunItem slimeitem2 = new SlimefunItem(DraconicGroup, DRACONIUM_INGOT,RecipeType.ENHANCED_CRAFTING_TABLE,
                    recipe2);
            slimeitem2.register(this);
             */
        }
        SlimefunItemStack DRACONIUM_BLOCK = (SlimefunItemStack) SlimefunItem.getById("DRACONIUM_BLOCK").getItem();
        ItemStack[] recipe2 = {
                DRACONIUM_BLOCK,null,null,
                null,null,null,
                null,null,null
        };
        SlimefunItemStack DRACONIUM_INGOT_9 = (SlimefunItemStack) SlimefunItem.getById("DRACONIUM_INGOT").getItem();
        DRACONIUM_INGOT_9 = (SlimefunItemStack) DRACONIUM_INGOT_9.clone();
        DRACONIUM_INGOT_9.setAmount(9);
        RecipeType.ENHANCED_CRAFTING_TABLE.register(recipe2,DRACONIUM_INGOT_9);

        if(SlimefunItem.getById("DRACONIC_CORE") == null){
            SlimefunItemStack itemStack = new SlimefunItemStack("DRACONIC_CORE", Material.BLUE_DYE,
                    "&b龙芯",
                    "&7用于合成龙之研究物品");
            ItemStack[] recipe = {
                    DRACONIUM_INGOT,new ItemStack(Material.GOLD_INGOT),DRACONIUM_INGOT,
                    new ItemStack(Material.GOLD_INGOT),new ItemStack(Material.DIAMOND),new ItemStack(Material.GOLD_INGOT),
                    DRACONIUM_INGOT,new ItemStack(Material.GOLD_INGOT),DRACONIUM_INGOT
            };
            SlimefunItem slimeitem = new SlimefunItem(DraconicGroup, itemStack,RecipeType.ENHANCED_CRAFTING_TABLE,
                    recipe);
            slimeitem.register(this);
        }
        SlimefunItemStack DRACONIC_CORE = (SlimefunItemStack) SlimefunItem.getById("DRACONIC_CORE").getItem();

        if(SlimefunItem.getById("WYVERN_CORE") == null){
            SlimefunItemStack itemStack = new SlimefunItemStack("WYVERN_CORE", Material.PINK_DYE,
                    "&d双足飞龙核心",
                    "&7用于合成龙之研究物品");
            ItemStack[] recipe = {
                    DRACONIUM_INGOT,DRACONIC_CORE,DRACONIUM_INGOT,
                    DRACONIC_CORE,new ItemStack(Material.NETHER_STAR),DRACONIC_CORE,
                    DRACONIUM_INGOT,DRACONIC_CORE,DRACONIUM_INGOT
            };
            SlimefunItem slimeitem = new SlimefunItem(DraconicGroup, itemStack,RecipeType.ENHANCED_CRAFTING_TABLE,
                    recipe);
            slimeitem.register(this);
        }
        SlimefunItemStack WYVERN_CORE = (SlimefunItemStack) SlimefunItem.getById("WYVERN_CORE").getItem();

        if(SlimefunItem.getById("WYVERN_ENERGY_CORE") == null){
            SlimefunItemStack itemStack = new SlimefunItemStack("WYVERN_ENERGY_CORE", Material.MAGENTA_DYE,
                    "&d飞龙能量核心",
                    "&7能量核心的重要合成材料");
            ItemStack[] recipe = {
                    DRACONIUM_INGOT,new ItemStack(Material.REDSTONE_BLOCK),DRACONIUM_INGOT,
                    new ItemStack(Material.REDSTONE_BLOCK),DRACONIC_CORE,new ItemStack(Material.REDSTONE_BLOCK),
                    DRACONIUM_INGOT,new ItemStack(Material.REDSTONE_BLOCK),DRACONIUM_INGOT
            };
            SlimefunItem slimeitem = new SlimefunItem(DraconicGroup, itemStack,RecipeType.ENHANCED_CRAFTING_TABLE,
                    recipe);
            slimeitem.register(this);
        }
        SlimefunItemStack WYVERN_ENERGY_CORE = (SlimefunItemStack) SlimefunItem.getById("WYVERN_ENERGY_CORE").getItem();

        if(SlimefunItem.getById("FUSION_CORE") == null){
            SlimefunItemStack itemStack = new SlimefunItemStack("FUSION_CORE", Material.GLASS,
                    "&b聚合核心",
                    "&9一种珍奇的材料");
            ItemStack[] recipe = {
                    new ItemStack(Material.LAPIS_BLOCK),new ItemStack(Material.DIAMOND), new ItemStack(Material.LAPIS_BLOCK),
                    new ItemStack(Material.DIAMOND),DRACONIC_CORE,new ItemStack(Material.DIAMOND),
                    new ItemStack(Material.LAPIS_BLOCK),new ItemStack(Material.DIAMOND),new ItemStack(Material.LAPIS_BLOCK)
            };
            ElectroBlock slimeitem = new ElectroBlock(DraconicGroup, itemStack,RecipeType.ENHANCED_CRAFTING_TABLE,
                    recipe,10);
            slimeitem.register(this);
        }

        if(SlimefunItem.getById("DRAGON_HEART") == null){
            SlimefunItemStack itemStack = new SlimefunItemStack("DRAGON_HEART", Material.RED_DYE,
                    "&6龙之心",
                    "&7听说...它十分稀有","&7杀死末影龙掉落");
            ItemStack[] recipe = {
                    null,null,null,
                    null,null,null,
                    null,null,null
            };
            SlimefunItem slimeitem = new SlimefunItem(DraconicGroup, itemStack,RecipeType.NULL,
                    recipe);
            slimeitem.register(this);
        }
        SlimefunItemStack DRAGON_HEART = (SlimefunItemStack) SlimefunItem.getById("DRAGON_HEART").getItem();

        if(SlimefunItem.getById("WYVERN_HELMET") == null){
            SlimefunItemStack itemStack = new SlimefunItemStack("WYVERN_HELMET", Material.LEATHER_HELMET,
                    "&d双足飞龙头盔",
                    "&6充能升级:", "&3电容量 &6-", "&3护盾容量 &6-", "&3护盾回收 &6-","",
                    LoreBuilder.powerCharged(0,400000), "", "&6装备技能:护盾", "&7充电" +
                    "以发动技能", "&7护盾将保护你", "&7免受大部分伤害");
            LeatherArmorMeta lch = (LeatherArmorMeta)itemStack.getItemMeta();
            lch.setColor(WyvernColor);
            lch.setUnbreakable(true);
            itemStack.setItemMeta(lch);
            ItemStack[] recipe = {
                    DRACONIUM_INGOT,WYVERN_CORE,DRACONIUM_INGOT,
                    DRACONIUM_INGOT,new ItemStack(Material.DIAMOND_HELMET),DRACONIUM_INGOT,
                    DRACONIUM_INGOT,WYVERN_ENERGY_CORE,DRACONIUM_INGOT
            };
            DraconicArmorPiece WyvernHelmet = new DraconicArmorPiece(DraconicGroup, itemStack, recipe,400000f,38);
            WyvernHelmet.register(this);
        }

        if(SlimefunItem.getById("WYVERN_CHESTPLATE") == null){
            SlimefunItemStack itemStack = new SlimefunItemStack("WYVERN_CHESTPLATE", Material.LEATHER_CHESTPLATE,
                    "&d双足飞龙胸甲",
                    "&6充能升级:", "&3电容量 &6-", "&3护盾容量 &6-", "&3护盾回收 &6-","",
                    LoreBuilder.powerCharged(0,400000), "", "&6装备技能:护盾", "&7充电" +
                    "以发动技能", "&7护盾将保护你", "&7免受大部分伤害");
            LeatherArmorMeta lch = (LeatherArmorMeta)itemStack.getItemMeta();
            lch.setColor(WyvernColor);
            lch.setUnbreakable(true);
            itemStack.setItemMeta(lch);
            ItemStack[] recipe = {
                    DRACONIUM_INGOT,WYVERN_CORE,DRACONIUM_INGOT,
                    DRACONIUM_INGOT,new ItemStack(Material.DIAMOND_CHESTPLATE),DRACONIUM_INGOT,
                    DRACONIUM_INGOT,WYVERN_ENERGY_CORE,DRACONIUM_INGOT
            };
            DraconicArmorPiece WyvernChestplate = new DraconicArmorPiece(DraconicGroup, itemStack, recipe,400000f,102);
            WyvernChestplate.register(this);
        }
        if(SlimefunItem.getById("WYVERN_LEGGINGS") == null){
            SlimefunItemStack itemStack = new SlimefunItemStack("WYVERN_LEGGINGS", Material.LEATHER_LEGGINGS,
                    "&d双足飞龙护腿",
                    "&6充能升级:", "&3电容量 &6-", "&3护盾容量 &6-", "&3护盾回收 &6-","",
                    LoreBuilder.powerCharged(0,400000), "", "&6装备技能:护盾", "&7充电" +
                    "以发动技能", "&7护盾将保护你", "&7免受大部分伤害");
            LeatherArmorMeta lch = (LeatherArmorMeta)itemStack.getItemMeta();
            lch.setColor(WyvernColor);
            lch.setUnbreakable(true);
            itemStack.setItemMeta(lch);
            ItemStack[] recipe = {
                    DRACONIUM_INGOT,WYVERN_CORE,DRACONIUM_INGOT,
                    DRACONIUM_INGOT,new ItemStack(Material.DIAMOND_LEGGINGS),DRACONIUM_INGOT,
                    DRACONIUM_INGOT,WYVERN_ENERGY_CORE,DRACONIUM_INGOT
            };
            DraconicArmorPiece WyvernLeggings = new DraconicArmorPiece(DraconicGroup, itemStack, recipe,400000f,76);
            WyvernLeggings.register(this);
        }
        if(SlimefunItem.getById("WYVERN_BOOTS") == null){
            SlimefunItemStack itemStack = new SlimefunItemStack("WYVERN_BOOTS", Material.LEATHER_BOOTS,
                    "&d双足飞龙靴子",
                    "&6充能升级:", "&3电容量 &6-", "&3护盾容量 &6-", "&3护盾回收 &6-","",
                    LoreBuilder.powerCharged(0,400000), "", "&6装备技能:护盾", "&7充电" +
                    "以发动技能", "&7护盾将保护你", "&7免受大部分伤害");
            LeatherArmorMeta lch = (LeatherArmorMeta)itemStack.getItemMeta();
            lch.setColor(WyvernColor);
            lch.setUnbreakable(true);
            itemStack.setItemMeta(lch);
            ItemStack[] recipe = {
                    DRACONIUM_INGOT,WYVERN_CORE,DRACONIUM_INGOT,
                    DRACONIUM_INGOT,new ItemStack(Material.DIAMOND_BOOTS),DRACONIUM_INGOT,
                    DRACONIUM_INGOT,WYVERN_ENERGY_CORE,DRACONIUM_INGOT
            };
            DraconicArmorPiece WyvernBoots = new DraconicArmorPiece(DraconicGroup, itemStack, recipe,400000f,38);
            WyvernBoots.register(this);
        }

        if(SlimefunItem.getById("BASIC_INJECTOR") == null){
            SlimefunItemStack itemStack = new SlimefunItemStack("BASIC_INJECTOR", Material.IRON_BLOCK,
                    "&f基础聚合注入器",
                    "&9向龙块中注入能量");
            ItemStack[] recipe = {
                    new ItemStack(Material.DIAMOND),DRACONIC_CORE,new ItemStack(Material.DIAMOND),
                    new ItemStack(Material.STONE),new ItemStack(Material.IRON_BLOCK),new ItemStack(Material.STONE),
                    new ItemStack(Material.STONE),new ItemStack(Material.STONE),new ItemStack(Material.STONE)
            };
            ElectroBlock slimeitem = new ElectroBlock(DraconicGroup, itemStack,RecipeType.ENHANCED_CRAFTING_TABLE,
                    recipe,50000);
            slimeitem.register(this);
        }
        if(SlimefunItem.getById("AWAKENED_DRACONIUM_BLOCK") == null){
            SlimefunItemStack awakened_draconium_block = new SlimefunItemStack("AWAKENED_DRACONIUM_BLOCK",
                    Material.ORANGE_TERRACOTTA,
                    "&6觉醒龙块",
                    "&7龙之研究中的稀有材料");
            ItemStack[] recipe = {
                    null,null,null,
                    null,null,null,
                    null,null,null
            };
            SlimefunItem slimeitem = new SlimefunItem(DraconicGroup, awakened_draconium_block,FusionCrafting.TYPE,
                    recipe);
            slimeitem.register(this);
        }
        SlimefunItemStack AWAKENED_DRACONIUM_BLOCK = (SlimefunItemStack) SlimefunItem.getById("AWAKENED_DRACONIUM_BLOCK").getItem();

        if(SlimefunItem.getById("AWAKENED_DRACONIUM_INGOT") == null){
            SlimefunItemStack awakened_draconium_block = new SlimefunItemStack("AWAKENED_DRACONIUM_INGOT",
                    Material.ORANGE_DYE,
                    "&6觉醒龙锭",
                    "&7用于合成觉醒级别的龙之护甲与工具");
            ItemStack[] recipe = {
                    AWAKENED_DRACONIUM_BLOCK,null,null,
                    null,null,null,
                    null,null,null
            };
            SlimefunItemStack awakened_draconium_block_9 = (SlimefunItemStack) awakened_draconium_block.clone();
            awakened_draconium_block_9.setAmount(9);
            SlimefunItem slimeitem = new SlimefunItem(DraconicGroup, awakened_draconium_block,RecipeType.ENHANCED_CRAFTING_TABLE,
                    recipe,awakened_draconium_block_9);
            slimeitem.register(this);
        }
        SlimefunItemStack AWAKENED_DRACONIUM_INGOT =
                (SlimefunItemStack) SlimefunItem.getById("AWAKENED_DRACONIUM_INGOT").getItem();

        if(SlimefunItem.getById("AWAKENED_CORE") == null){
            SlimefunItemStack awakened_core = new SlimefunItemStack("AWAKENED_CORE",
                    Material.HONEYCOMB,
                    "&6觉醒核心",
                    "&7用于合成觉醒级别的龙之护甲与工具");
            ItemStack[] recipe = {
                    null,null,null,
                    null,null,null,
                    null,null,null
            };
            SlimefunItem slimeitem = new SlimefunItem(DraconicGroup, awakened_core,FusionCrafting.TYPE,
                    recipe);
            slimeitem.register(this);
        }
        SlimefunItemStack AWAKENED_CORE =
                (SlimefunItemStack) SlimefunItem.getById("AWAKENED_CORE").getItem();

        if(SlimefunItem.getById("DRACONIC_ENERGY_CORE") == null){
            SlimefunItemStack draconic_energy_core = new SlimefunItemStack("DRACONIC_ENERGY_CORE",
                    Material.YELLOW_DYE,
                    "&6神龙能量核心",
                    "&7制作觉醒龙套装的必备材料");
            ItemStack[] recipe = {
                    AWAKENED_DRACONIUM_INGOT,WYVERN_ENERGY_CORE,AWAKENED_DRACONIUM_INGOT,
                    WYVERN_ENERGY_CORE,WYVERN_CORE,WYVERN_ENERGY_CORE,
                    AWAKENED_DRACONIUM_INGOT,WYVERN_ENERGY_CORE,AWAKENED_DRACONIUM_INGOT
            };
            SlimefunItem slimeitem = new SlimefunItem(DraconicGroup, draconic_energy_core,RecipeType.ENHANCED_CRAFTING_TABLE,
                    recipe);
            slimeitem.register(this);
        }
        SlimefunItemStack DRACONIC_ENERGY_CORE =
                (SlimefunItemStack) SlimefunItem.getById("DRACONIC_ENERGY_CORE").getItem();

        Color DraconicColor = Color.fromRGB(245, 158, 7);
        if(SlimefunItem.getById("DRACONIC_HELMET") == null){
            SlimefunItemStack itemStack = new SlimefunItemStack("DRACONIC_HELMET", Material.LEATHER_HELMET,
                    "&6神龙头盔",
                    "&6充能升级:", "&3电容量 &6-", "&3护盾容量 &6-", "&3护盾回收 &6-","",
                    LoreBuilder.powerCharged(0,800000), "", "&6装备技能:护盾", "&7充电" +
                    "以发动技能", "&7护盾将保护你", "&7免受大部分伤害");
            LeatherArmorMeta lch = (LeatherArmorMeta)itemStack.getItemMeta();
            lch.setColor(DraconicColor);
            lch.setUnbreakable(true);
            itemStack.setItemMeta(lch);
            ItemStack[] recipe = {
                    null,null,null,
                    null,null,null,
                    null,null,null
            };
            DraconicArmorPiece DraconicHelmet = new DraconicArmorPiece(DraconicGroup, itemStack, recipe,800000f,76,
                    FusionCrafting.TYPE);
            DraconicHelmet.register(this);
        }

        if(SlimefunItem.getById("DRACONIC_CHESTPLATE") == null){
            SlimefunItemStack itemStack = new SlimefunItemStack("DRACONIC_CHESTPLATE", Material.LEATHER_CHESTPLATE,
                    "&6神龙胸甲",
                    "&6充能升级:", "&3电容量 &6-", "&3护盾容量 &6-", "&3护盾回收 &6-","",
                    LoreBuilder.powerCharged(0,800000), "", "&6装备技能:护盾", "&7充电" +
                    "以发动技能", "&7护盾将保护你", "&7免受大部分伤害");
            LeatherArmorMeta lch = (LeatherArmorMeta)itemStack.getItemMeta();
            lch.setColor(DraconicColor);
            lch.setUnbreakable(true);
            itemStack.setItemMeta(lch);
            ItemStack[] recipe = {
                    null,null,null,
                    null,null,null,
                    null,null,null
            };
            DraconicArmorPiece DraconicChestplate = new DraconicArmorPiece(DraconicGroup, itemStack, recipe,800000f,
                    204,FusionCrafting.TYPE);
            DraconicChestplate.register(this);
        }
        if(SlimefunItem.getById("DRACONIC_LEGGINGS") == null){
            SlimefunItemStack itemStack = new SlimefunItemStack("DRACONIC_LEGGINGS", Material.LEATHER_LEGGINGS,
                    "&6神龙护腿",
                    "&6充能升级:", "&3电容量 &6-", "&3护盾容量 &6-", "&3护盾回收 &6-","",
                    LoreBuilder.powerCharged(0,800000), "", "&6装备技能:护盾", "&7充电" +
                    "以发动技能", "&7护盾将保护你", "&7免受大部分伤害");
            LeatherArmorMeta lch = (LeatherArmorMeta)itemStack.getItemMeta();
            lch.setColor(DraconicColor);
            lch.setUnbreakable(true);
            itemStack.setItemMeta(lch);
            ItemStack[] recipe = {
                    null,null,null,
                    null,null,null,
                    null,null,null
            };
            DraconicArmorPiece DraconicLeggings = new DraconicArmorPiece(DraconicGroup, itemStack, recipe,800000f,152,
                    FusionCrafting.TYPE);
            DraconicLeggings.register(this);
        }
        if(SlimefunItem.getById("DRACONIC_BOOTS") == null){
            SlimefunItemStack itemStack = new SlimefunItemStack("DRACONIC_BOOTS", Material.LEATHER_BOOTS,
                    "&6神龙靴子",
                    "&6充能升级:", "&3电容量 &6-", "&3护盾容量 &6-", "&3护盾回收 &6-","",
                    LoreBuilder.powerCharged(0,800000), "", "&6装备技能:护盾", "&7充电" +
                    "以发动技能", "&7护盾将保护你", "&7免受大部分伤害");
            LeatherArmorMeta lch = (LeatherArmorMeta)itemStack.getItemMeta();
            lch.setColor(DraconicColor);
            lch.setUnbreakable(true);
            itemStack.setItemMeta(lch);
            ItemStack[] recipe = {
                    null,null,null,
                    null,null,null,
                    null,null,null
            };
            DraconicArmorPiece DraconicBoots = new DraconicArmorPiece(DraconicGroup, itemStack, recipe,800000f,76,
                    FusionCrafting.TYPE);
            DraconicBoots.register(this);
        }
        
        if(SlimefunItem.getById("BASIC_INJECTOR") == null){
            SlimefunItemStack itemStack = new SlimefunItemStack("BASIC_INJECTOR", Material.IRON_BLOCK,
                    "&F基础聚合注入器",
                    "&9融合器最基本基座");
            ItemStack[] recipe = {
                    endcrystal,endcrystal,endcrystal,
                    endcrystal,new ItemStack(Material.IRON_BLOCK),endcrystal,
                    endcrystal,endcrystal,endcrystal
            };
            ElectroBlock slimeitem = new ElectroBlock(DraconicGroup, itemStack,RecipeType.ENHANCED_CRAFTING_TABLE,
                    recipe,400000);
            slimeitem.register(this);
        }

        if(SlimefunItem.getById("WYVERN_INJECTOR") == null){
            SlimefunItemStack itemStack = new SlimefunItemStack("WYVERN_INJECTOR", Material.PURPLE_TERRACOTTA,
                    "&d飞龙聚合注入器",
                    "&9融合器基座");
            ItemStack[] recipe = {
                    null,null,null,
                    null,null,null,
                    null,null,null
            };
            ElectroBlock slimeitem = new ElectroBlock(DraconicGroup, itemStack,FusionCrafting.TYPE,
                    recipe,250000);
            slimeitem.register(this);
        }
        if(SlimefunItem.getById("DRACONIC_INJECTOR") == null){
            SlimefunItemStack itemStack = new SlimefunItemStack("DRACONIC_INJECTOR", Material.ORANGE_CONCRETE,
                    "&6神龙聚合注入器",
                    "&9融合器基座");
            ItemStack[] recipe = {
                    null,null,null,
                    null,null,null,
                    null,null,null
            };
            ElectroBlock slimeitem = new ElectroBlock(DraconicGroup, itemStack,FusionCrafting.TYPE,
                    recipe,500000);
            slimeitem.register(this);
        }
        if(SlimefunItem.getById("CHAOTIC_INJECTOR") == null){
            SlimefunItemStack itemStack = new SlimefunItemStack("CHAOTIC_INJECTOR", Material.END_PORTAL_FRAME,
                    "&8混沌聚合注入器",
                    "&9融合器基座");
            ItemStack[] recipe = {
                    endcrystal,endcrystal,endcrystal,
                    endcrystal,SlimefunItem.getById("DRACONIC_INJECTOR").getItem(),endcrystal,
                    endcrystal,endcrystal,endcrystal
            };
            ElectroBlock slimeitem = new ElectroBlock(DraconicGroup, itemStack,FusionCrafting.TYPE,
                    recipe,1000000);
            slimeitem.register(this);
        }
        if(SlimefunItem.getById("DRACONIC_ENERGY_INFUSER") == null) {
            SlimefunItemStack DRACONIC_ENERGY_INFUSER = new SlimefunItemStack(
                    "DRACONIC_ENERGY_INFUSER",
                    Material.DRAGON_HEAD,
                    "&6神龙能量注入器",
                    "&7机械里溢满了能量",
                    "&7可快速给物品充电",
                    "&8⇨ &e⚡ &750,000 J/s",
                    "&8⇨ &e⚡ &7能量损耗: &c50%"
            );
            new ChargingBench(DraconicGroup,DRACONIC_ENERGY_INFUSER , RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                    AWAKENED_DRACONIUM_BLOCK, AWAKENED_DRACONIUM_BLOCK, AWAKENED_DRACONIUM_BLOCK,
                    AWAKENED_DRACONIUM_BLOCK, SlimefunItems.CHARGING_BENCH, AWAKENED_DRACONIUM_BLOCK,
                    AWAKENED_DRACONIUM_BLOCK, AWAKENED_DRACONIUM_BLOCK, AWAKENED_DRACONIUM_BLOCK,
            }).setCapacity(10000).setEnergyConsumption(10000).setProcessingSpeed(30).register(this);
        }

        if(SlimefunItem.getById("DRACONIC_OP_ENERGY") == null) {
            SlimefunItemStack DRACONIC_OP_ENERGY = new SlimefunItemStack(
                    "DRACONIC_OP_ENERGY",
                    Material.END_ROD,
                    "&6神龙粒子发生器",
                    "&7释放出巨大的能量",
                    "&8⇨ &e⚡ &7100,000 J/s","&c仅在白天工作!"
            );
            new SolarGenerator(DraconicGroup,100000,0,DRACONIC_OP_ENERGY , RecipeType.ENHANCED_CRAFTING_TABLE,
                    new ItemStack[] {
                            AWAKENED_DRACONIUM_BLOCK, AWAKENED_DRACONIUM_BLOCK, AWAKENED_DRACONIUM_BLOCK,
                            AWAKENED_DRACONIUM_BLOCK, SlimefunItems.SOLAR_GENERATOR_4, endcrystal,
                            AWAKENED_DRACONIUM_BLOCK, AWAKENED_DRACONIUM_BLOCK, AWAKENED_DRACONIUM_BLOCK,
                    }).register(this);
        }
    }

    public static float getArmorEnergy(ItemStack item){
        float energy = 0;
        SlimefunItem slimefunItem = SlimefunItem.getByItem(item);
        if (slimefunItem instanceof Rechargeable) {
            Rechargeable rechargeableItem = (Rechargeable) slimefunItem;
            energy += rechargeableItem.getItemCharge(item);
        }
        return energy;
    }

    public static boolean removeArmorEnergy(ItemStack item,float charge){
        boolean worked = false;
        try {
            SlimefunItem slimefunItem = SlimefunItem.getByItem(item);
            if (slimefunItem instanceof Rechargeable) {
                Rechargeable rechargeableItem = (Rechargeable) slimefunItem;
                worked = rechargeableItem.removeItemCharge(item, charge);
            }
        } catch (IllegalArgumentException e){
            System.out.println("Error in removeArmorEnergy" + item);
            worked = false;
        }
        return worked;
    }

    public static boolean canBuildAt(Player p, Block b, Interaction inter){
        return Slimefun.getProtectionManager().hasPermission(p, b, inter);
    }

    public static float getArmorMaxEnergy(ItemStack item){
        float energy = 0;
        SlimefunItem slimefunItem = SlimefunItem.getByItem(item);
        if (slimefunItem instanceof Rechargeable) {
            Rechargeable rechargeableItem = (Rechargeable) slimefunItem;
            energy += rechargeableItem.getMaxItemCharge(item);
        }
        return energy;
    }

    public static double getArmorMaxShield(ItemStack item){
        double shield = 0;
        SlimefunItem slimefunItem = SlimefunItem.getByItem(item);
        if (slimefunItem instanceof DraconicArmorPiece) {
            DraconicArmorPiece dracpiece = (DraconicArmorPiece) slimefunItem;
            shield += dracpiece.shield;
        }
        return shield;
    }

    public static boolean removeArmorEnergyForPlayer(Player p,float charge){
        if (removeArmorEnergy(p.getInventory().getHelmet(),charge)) return true;
        if (removeArmorEnergy(p.getInventory().getChestplate(),charge)) return true;
        if (removeArmorEnergy(p.getInventory().getLeggings(),charge)) return true;
        if (removeArmorEnergy(p.getInventory().getBoots(),charge)) return true;

        return false;
    }

    public static float getArmorEnergyForPlayer(Player p){
        float energy = 0;
        energy += getArmorEnergy(p.getInventory().getHelmet());
        energy += getArmorEnergy(p.getInventory().getChestplate());
        energy += getArmorEnergy(p.getInventory().getLeggings());
        energy += getArmorEnergy(p.getInventory().getBoots());
        return energy;
    }

    public static float getMaxArmorEnergyForPlayer(Player p){
        float energy = 0;
        energy += getArmorMaxEnergy(p.getInventory().getHelmet());
        energy += getArmorMaxEnergy(p.getInventory().getChestplate());
        energy += getArmorMaxEnergy(p.getInventory().getLeggings());
        energy += getArmorMaxEnergy(p.getInventory().getBoots());
        return energy;
    }

    public static double getMaxShieldForPlayer(Player p){
        double energy = 0;
        energy += getArmorMaxShield(p.getInventory().getHelmet());
        energy += getArmorMaxShield(p.getInventory().getChestplate());
        energy += getArmorMaxShield(p.getInventory().getLeggings());
        energy += getArmorMaxShield(p.getInventory().getBoots());
        return energy;
    }

    public static double getRegenShield(ItemStack item){
        double regen = 0;
        SlimefunItem slimefunItem = SlimefunItem.getByItem(item);
        if (slimefunItem instanceof DraconicArmorPiece) {
            DraconicArmorPiece dracpiece = (DraconicArmorPiece) slimefunItem;
            if (dracpiece.getItemName().contains("Draconic")){
                regen += 10;
            }
            if (dracpiece.getItemName().contains("Wyvern")){
                regen += 5;
            }
        }
        return regen;
    }

    public static double getRegenShieldForPlayer(Player p){
        double regen = 0;
        regen += getRegenShield(p.getInventory().getHelmet());
        regen += getRegenShield(p.getInventory().getChestplate());
        regen += getRegenShield(p.getInventory().getLeggings());
        regen += getRegenShield(p.getInventory().getBoots());
        return regen;
    }

    public static void actionbar(Player p,String msg){
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
    }

    public static void dactionbar(Player p){
        DecimalFormat numberFormat = new DecimalFormat("0.00");
        Double shield = (Double)vars.get("shield::" + p.getName());
        Double mshield = (Double)vars.get("maxshield::" + p.getName());
        Double ov = (Double)vars.get("overload::" + p.getName());
        Double en = (Double)vars.get("energy::" + p.getName());
        Double men = (Double)vars.get("maxenergy::" + p.getName());
        actionbar(p,
                "§b" + numberFormat.format(shield) + "/" + numberFormat.format(mshield) + " §c" + numberFormat.format(ov) + "%" + " " +
                "§a" + (BigNumber(en)) + "/" + (BigNumber(men)) + " J");
    }

    public static void dbossbar(Player p){
        DecimalFormat numberFormat = new DecimalFormat("0.00");
        Double shield = (Double)vars.get("shield::" + p.getName());
        Double mshield = (Double)vars.get("maxshield::" + p.getName());
        Double ov = (Double)vars.get("overload::" + p.getName());
        Double en = (Double)vars.get("energy::" + p.getName());
        Double men = (Double)vars.get("maxenergy::" + p.getName());
        bars.get(p).setTitle(
                "§b" + numberFormat.format(shield) + "/" + numberFormat.format(mshield) + " §c" + numberFormat.format(ov) + "%" + " " +
                        "§a" + (BigNumber(en)) + "/" + (BigNumber(men)) + " J");
        double progress = shield/mshield;
        if (progress >= 0)
            bars.get(p).setProgress(progress);
    }

    public static String CommaNumberFormat(long number) {
        return NumberFormat.getNumberInstance(Locale.US).format(number);
    }

    public static String BigNumber(double number){
        if(number >= 1000000000){
            return String.format("%.2fB", number/ 1000000000.0);
        }

        else if(number >= 1000000){
            return String.format("%.2fM", number/ 1000000.0);
        }

        else if(number >=1000){
            return String.format("%.2fK", number/ 1000.0);
        }
        return String.valueOf(number);
    }

    public static ItemFrame SpawnItemFrame(Location loc, BlockFace face){
        if (face == BlockFace.DOWN){
            loc.setY(loc.getY() - 1);
        }
        else if (face == BlockFace.UP){
            loc.setY(loc.getY() + 1);
        }
        else if (face == BlockFace.SOUTH){
            loc.setZ(loc.getZ() + 1);
        }
        else if (face == BlockFace.NORTH){
            loc.setZ(loc.getZ() - 1);
        }
        else if (face == BlockFace.WEST){
            loc.setX(loc.getX() - 1);
        }
        else if (face == BlockFace.EAST){
            loc.setX(loc.getX() + 1);
        }
        ItemFrame i = (ItemFrame) loc.getWorld().spawn(loc, ItemFrame.class);
        i.setFacingDirection(face);
        return i;
    }

    public static Location getitemfameloc(Location loc, BlockFace face){
        if (face == BlockFace.DOWN){
            loc.setY(loc.getY() - 1);
        }
        else if (face == BlockFace.UP){
            loc.setY(loc.getY() + 1);
        }
        else if (face == BlockFace.SOUTH){
            loc.setZ(loc.getZ() + 1);
        }
        else if (face == BlockFace.NORTH){
            loc.setZ(loc.getZ() - 1);
        }
        else if (face == BlockFace.WEST){
            loc.setX(loc.getX() - 1);
        }
        else if (face == BlockFace.EAST){
            loc.setX(loc.getX() + 1);
        }
        return loc;
    }

    public static Block getTargetBlock(Player p){
        Block b = null;
        RayTraceResult ray = p.getWorld().rayTraceBlocks(p.getEyeLocation(),
                p.getEyeLocation().getDirection(),
                100d);
        if(ray != null){
            b = ray.getHitBlock();
        }
        return b;
    }

    public static ArrayList<Block> loopblockscube(Location center,int size){
        long now = System.currentTimeMillis();
        ArrayList<Block> blocks = new ArrayList<Block>();
        int X = center.getBlockX();
        int Y = center.getBlockY();
        int Z = center.getBlockZ();
        for (int x = X - size; x <= X + size; x++) {
            for (int y = Y - size; y <= size; y++) {
                for (int z = Z - size; z <= Z + size; z++) {
                    blocks.add(center.getWorld().getBlockAt(x, y, z));
                }
            }
        }
        // System.out.println("loopblockscube took " + (System.currentTimeMillis() - now));
        return blocks;
    }

    public static void giveItem(Player p, ItemStack i){
        if (p.getInventory().firstEmpty() == -1){
            p.getWorld().dropItem(p.getLocation(),i);
        }
        else {
            p.getInventory().addItem(i);
        }
    }

    public static void packetDrop(Player p,Location loc,ItemStack item){

    }

    public static void packetHideEntity(Entity e, Player p){
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        PacketContainer packet = manager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        int id = e.getEntityId();
        packet.getIntegers().write(0,1);
        packet.getIntegers().write(1,id);
        for( Player on : Bukkit.getServer().getOnlinePlayers()) {
            try {
                manager.sendServerPacket(on, packet);
            } catch (InvocationTargetException invocationTargetException) {
                invocationTargetException.printStackTrace();
            }
        }
    }

    public static void createCircle(Location loc,float radius,Particle particle,int iter, Color color){
        SphereEffect circle = new SphereEffect(Draconic.effectManager);
        circle.radius = radius;
        circle.particle = particle;
        circle.iterations = iter;
        circle.type = EffectType.REPEATING;
        // circle.setDynamicOrigin(new DynamicLocation(loc));
        circle.color = color;
        circle.setLocation(loc);
        circle.start();
    }

    @Override
    public String getPluginVersion() {
        return null;
    }
    /*
    public static void aiflyto(Entity e, Location loc, long duration) {
        long times = duration/50;
        double x1 = e.getLocation().getX();
        double x2 = loc.getX();

        double y1 = e.getLocation().getY();
        double y2 = loc.getY();

        double z1 = e.getLocation().getZ();
        double z2 = loc.getZ();

        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;

        double mx;
        double my;
        double mz;
        Location tploc;
        for (int i = 0; i < times; i++) {
            mx = x1 + dx / times * i;
            my = y1 + dy / times * i;
            mz = z1 + dz / times * i;
            tploc = new Location(e.getWorld(), mx, my, mz);
            e.teleport(tploc);
        }
         */
    // }
}
