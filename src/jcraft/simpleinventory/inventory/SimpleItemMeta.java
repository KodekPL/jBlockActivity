package jcraft.simpleinventory.inventory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import jcraft.jblockactivity.config.WorldConfig;
import jcraft.jblockactivity.extradata.ExtraLoggingTypes.ItemMetaType;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.meta.SkullMeta;

public class SimpleItemMeta {

    private String N1; // displayName -> N1
    private String[] L1; // lore -> L1
    private Map<String, Integer> E1; // enchantments -> E1
    private Map<String, Integer> E2; // storageEnchantments -> E2
    private String T2; // bookTitle -> T2
    private String A2; // bookAuthor -> A2
    private String[] P1; // bookPages -> P1
    private Integer C1; // leatherColor -> C1
    private Integer R1; // repairCost -> R1
    private String S1; // skullOwner -> S1
    private Boolean M2; // isMapScaled -> M2
    private SimplePotionEffect[] P2; // potionEffects -> P2
    private SimpleFireworkEffect[] F1; // fireworkEffects -> F1
    private Integer P3; // fireworkPower -> P3

    public ItemStack setItemMeta(ItemStack item) {
        final ItemMeta meta = item.getItemMeta();

        if (getDisplayName() != null) {
            meta.setDisplayName(getDisplayName());
        }

        if (getLore() != null) {
            meta.setLore(Arrays.asList(getLore()));

        }

        final Map<Enchantment, Integer> enchants = getEnchants();

        if (enchants != null) {
            for (Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
        }

        // Repair Cost
        final Repairable repairMeta = (Repairable) meta;

        if (getRepairCost() != null) {
            repairMeta.setRepairCost(getRepairCost());
        }

        // Book
        if (item.getType() == Material.BOOK_AND_QUILL || item.getType() == Material.WRITTEN_BOOK) {
            final BookMeta bookMeta = (BookMeta) meta;
            if (getBookTitle() != null) {
                bookMeta.setTitle(getBookTitle());
            }

            if (getBookAuthor() != null) {
                bookMeta.setAuthor(getBookAuthor());
            }

            if (getBookPages() != null) {
                bookMeta.setPages(getBookPages());
            }
        }

        // Enchantment Storage
        if (item.getType() == Material.ENCHANTED_BOOK) {
            final EnchantmentStorageMeta enchantStore = (EnchantmentStorageMeta) meta;
            final Map<Enchantment, Integer> bookEnchants = getStorageEnchants();

            if (bookEnchants != null) {
                for (Entry<Enchantment, Integer> entry : bookEnchants.entrySet()) {
                    enchantStore.addStoredEnchant(entry.getKey(), entry.getValue(), true);
                }
            }
        }

        // Leather Color
        if (item.getType() == Material.LEATHER_HELMET || item.getType() == Material.LEATHER_CHESTPLATE || item.getType() == Material.LEATHER_LEGGINGS
                || item.getType() == Material.LEATHER_BOOTS) {
            final LeatherArmorMeta leatherMeta = (LeatherArmorMeta) meta;

            if (getLeatherColor() != null) {
                leatherMeta.setColor(org.bukkit.Color.fromRGB(getLeatherColor()));
            }
        }

        // Skull
        if (item.getType() == Material.SKULL_ITEM) {
            final SkullMeta skullMeta = (SkullMeta) meta;

            if (getSkullOwner() != null) {
                skullMeta.setOwner(getSkullOwner());
            }
        }

        // Map
        if (item.getType() == Material.MAP) {
            final MapMeta mapMeta = (MapMeta) meta;

            if (isMapScaled() != null) {
                mapMeta.setScaling(isMapScaled());
            }
        }

        // Potion Effects
        if (item.getType() == Material.POTION) {
            final PotionMeta potMeta = (PotionMeta) meta;

            if (getPotionEffects() != null) {
                for (SimplePotionEffect effect : getPotionEffects()) {
                    potMeta.addCustomEffect(effect.getPotionEffect(), true);
                }
            }
        }

        // Firework
        if (item.getType() == Material.FIREWORK) {
            final FireworkMeta fireworkMeta = (FireworkMeta) meta;

            if (getFireworkEffects() != null) {
                for (SimpleFireworkEffect effect : getFireworkEffects()) {
                    fireworkMeta.addEffects(effect.getFireworkEffect());
                }
            }

            if (getFireworkPower() != null) {
                fireworkMeta.setPower(getFireworkPower());
            }
        }

        // Firework effect
        if (item.getType() == Material.FIREWORK_CHARGE) {
            final FireworkEffectMeta fireworEffectMeta = (FireworkEffectMeta) meta;

            if (getFireworkEffects() != null) {
                fireworEffectMeta.setEffect(getFireworkEffects()[0].getFireworkEffect());
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    public SimpleItemMeta(ItemStack item, WorldConfig config) {
        final ItemMeta meta = item.getItemMeta();

        if (config.isLoggingExtraItemMeta(ItemMetaType.name) && meta.hasDisplayName()) {
            N1 = meta.getDisplayName();
        }

        if (config.isLoggingExtraItemMeta(ItemMetaType.lore) && meta.hasLore()) {
            L1 = meta.getLore().toArray(new String[meta.getLore().size()]);
        }

        if (config.isLoggingExtraItemMeta(ItemMetaType.enchants) && meta.hasEnchants()) {
            E1 = new HashMap<String, Integer>();

            for (Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                E1.put(entry.getKey().getName(), entry.getValue());
            }
        }

        if (meta instanceof BookMeta) {
            final BookMeta bookMeta = (BookMeta) meta;

            if (config.isLoggingExtraItemMeta(ItemMetaType.booktitle) && bookMeta.hasTitle()) {
                T2 = bookMeta.getTitle();
            }

            if (config.isLoggingExtraItemMeta(ItemMetaType.bookauthor) && bookMeta.hasAuthor()) {
                A2 = bookMeta.getAuthor();
            }

            if (config.isLoggingExtraItemMeta(ItemMetaType.bookpage) && bookMeta.hasPages()) {
                P1 = bookMeta.getPages().toArray(new String[bookMeta.getPages().size()]);
            }

        }

        if (meta instanceof EnchantmentStorageMeta) {
            final EnchantmentStorageMeta enchantStore = (EnchantmentStorageMeta) meta;

            if (config.isLoggingExtraItemMeta(ItemMetaType.bookenchant) && enchantStore.hasStoredEnchants()) {
                E2 = new HashMap<String, Integer>();

                for (Entry<Enchantment, Integer> entry : enchantStore.getStoredEnchants().entrySet()) {
                    E2.put(entry.getKey().getName(), entry.getValue());
                }
            }
        }

        if (meta instanceof LeatherArmorMeta) {
            final LeatherArmorMeta leatherMeta = (LeatherArmorMeta) meta;

            if (config.isLoggingExtraItemMeta(ItemMetaType.leathercolor) && leatherMeta.getColor() != null) {
                C1 = leatherMeta.getColor().asRGB();
            }
        }

        if (meta instanceof Repairable) {
            final Repairable repairMeta = (Repairable) meta;

            if (config.isLoggingExtraItemMeta(ItemMetaType.repair) && repairMeta.hasRepairCost()) {
                R1 = repairMeta.getRepairCost();
            }
        }

        if (meta instanceof SkullMeta) {
            final SkullMeta skullMeta = (SkullMeta) meta;

            if (config.isLoggingExtraItemMeta(ItemMetaType.skull) && skullMeta.hasOwner()) {
                S1 = skullMeta.getOwner();
            }
        }

        if (meta instanceof MapMeta && config.isLoggingExtraItemMeta(ItemMetaType.map)) {
            final MapMeta mapMeta = (MapMeta) meta;

            M2 = mapMeta.isScaling();
        }

        if (meta instanceof PotionMeta) {
            final PotionMeta potMeta = (PotionMeta) meta;

            if (config.isLoggingExtraItemMeta(ItemMetaType.potion) && potMeta.hasCustomEffects()) {
                P2 = new SimplePotionEffect[potMeta.getCustomEffects().size()];

                for (int i = 0; i < P2.length; i++) {
                    P2[i] = new SimplePotionEffect(potMeta.getCustomEffects().get(i));
                }
            }
        }
        if (meta instanceof FireworkMeta) {
            final FireworkMeta fireworkMeta = (FireworkMeta) meta;

            if (config.isLoggingExtraItemMeta(ItemMetaType.firework) && fireworkMeta.hasEffects()) {
                F1 = new SimpleFireworkEffect[fireworkMeta.getEffects().size()];

                for (int i = 0; i < F1.length; i++) {
                    F1[i] = new SimpleFireworkEffect(fireworkMeta.getEffects().get(i));
                }

                P3 = fireworkMeta.getPower();
            }
        }
        if (meta instanceof FireworkEffectMeta) {
            final FireworkEffectMeta fireworkMeta = (FireworkEffectMeta) meta;

            if (config.isLoggingExtraItemMeta(ItemMetaType.firework) && fireworkMeta.hasEffect()) {
                F1 = new SimpleFireworkEffect[] { new SimpleFireworkEffect(fireworkMeta.getEffect()) };
            }
        }
    }

    public String getDisplayName() {
        return N1;
    }

    public String[] getLore() {
        return L1;
    }

    public Map<Enchantment, Integer> getEnchants() {
        if (E1 == null) {
            return null;
        }

        final Map<Enchantment, Integer> enchants = new HashMap<Enchantment, Integer>();

        for (Entry<String, Integer> entry : E1.entrySet()) {
            enchants.put(Enchantment.getByName(entry.getKey()), entry.getValue());
        }

        return enchants;
    }

    public Map<Enchantment, Integer> getStorageEnchants() {
        if (E2 == null) {
            return null;
        }

        final Map<Enchantment, Integer> enchants = new HashMap<Enchantment, Integer>();

        for (Entry<String, Integer> entry : E2.entrySet()) {
            enchants.put(Enchantment.getByName(entry.getKey()), entry.getValue());
        }

        return enchants;
    }

    public String getBookTitle() {
        return T2;
    }

    public String getBookAuthor() {
        return A2;
    }

    public String[] getBookPages() {
        return P1;
    }

    public Integer getLeatherColor() {
        return C1;
    }

    public Integer getRepairCost() {
        return R1;
    }

    public String getSkullOwner() {
        return S1;
    }

    public Boolean isMapScaled() {
        return M2;
    }

    public SimplePotionEffect[] getPotionEffects() {
        return P2;
    }

    public SimpleFireworkEffect[] getFireworkEffects() {
        return F1;
    }

    public Integer getFireworkPower() {
        return P3;
    }

}
