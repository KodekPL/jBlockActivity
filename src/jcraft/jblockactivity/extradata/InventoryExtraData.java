package jcraft.jblockactivity.extradata;

import static jcraft.jblockactivity.utils.ActivityUtil.toJson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.config.WorldConfig;
import jcraft.jblockactivity.extradata.ExtraLoggingTypes.ItemMetaType;

import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.World;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class InventoryExtraData implements ExtraData {

    private WorldConfig config;
    private ItemStack[] content;

    private String[] sContent;
    private SimpleItemMeta[] meta;

    public InventoryExtraData(ItemStack[] content, boolean compress, World world) {
        this(content, compress, BlockActivity.getWorldConfig(world.getName()));
    }

    public InventoryExtraData(ItemStack[] content, boolean compress, WorldConfig config) {
        this.config = config;
        if (config != null && content != null) {
            this.content = compress ? compressInventory(content) : content;
        }
    }

    public InventoryExtraData(String[] sContent, SimpleItemMeta[] meta, World world) {
        this.config = BlockActivity.getWorldConfig(world.getName());
        this.sContent = sContent;
        this.meta = meta;
    }

    public ItemStack[] getContent() {
        if (content == null && sContent != null) {
            content = new ItemStack[sContent.length];
            for (int i = 0; i < sContent.length; i++) {
                if (sContent[i] == null || sContent[i].equals("")) {
                    continue;
                }
                String[] sItem = sContent[i].split(",");
                try {
                    content[i] = new ItemStack(Integer.parseInt(sItem[0]), Integer.parseInt(sItem[2]), Short.parseShort(sItem[1]));
                } catch (NumberFormatException e) {
                    content[i] = null;
                }
            }
            if (meta != null) {
                for (SimpleItemMeta itemMeta : meta) {
                    ItemStack item = content[itemMeta.getId()];
                    if (item != null) {
                        itemMeta.setItemMeta(item);
                    }
                }
            }
        }
        return content;
    }

    public void fillData() {
        if (sContent != null) {
            return;
        }
        if (content == null) {
            return;
        }
        this.sContent = new String[content.length];
        final List<SimpleItemMeta> meta = new ArrayList<SimpleItemMeta>();
        for (int i = 0; i < content.length; i++) {
            ItemStack item = content[i];
            if (item != null) {
                sContent[i] = item.getTypeId() + "," + item.getDurability() + "," + item.getAmount();
                if (item.hasItemMeta()) {
                    SimpleItemMeta itemMeta = new SimpleItemMeta(i, item.getItemMeta());
                    meta.add(itemMeta);
                }
            }
        }
        if (!meta.isEmpty()) {
            this.meta = meta.toArray(new SimpleItemMeta[meta.size()]);
        }
    }

    public String[] getStringContent() {
        fillData();
        return sContent;
    }

    public SimpleItemMeta[] getSimpleItemMeta() {
        fillData();
        return meta;
    }

    public boolean isEmpty() {
        return content == null;
    }

    @Override
    public String getData() {
        fillData();
        this.config = null;
        this.content = null;
        return toJson(this);
    }

    public void compareInventories(InventoryExtraData invExtraData) {
        final ItemStackComparator comperator = new ItemStackComparator();
        final ArrayList<ItemStack> diff = new ArrayList<ItemStack>();
        final ItemStack[] newContent = invExtraData.getContent();

        final int l1 = content.length, l2 = newContent.length;
        int c1 = 0, c2 = 0;
        while (c1 < l1 || c2 < l2) {
            if (c1 >= l1) {
                diff.add(newContent[c2]);
                c2++;
                continue;
            }
            if (c2 >= l2) {
                content[c1].setAmount(content[c1].getAmount() * -1);
                diff.add(content[c1]);
                c1++;
                continue;
            }
            final int comp = comperator.compare(content[c1], newContent[c2], config.isLoggingExtraItemMeta());
            if (comp < 0) {
                content[c1].setAmount(content[c1].getAmount() * -1);
                diff.add(content[c1]);
                c1++;
            } else if (comp > 0) {
                diff.add(newContent[c2]);
                c2++;
            } else {
                final int amount = newContent[c2].getAmount() - content[c1].getAmount();
                if (amount != 0) {
                    content[c1].setAmount(amount);
                    diff.add(content[c1]);
                }
                c1++;
                c2++;
            }
        }
        if (diff.isEmpty()) {
            content = null;
        } else {
            content = diff.toArray(new ItemStack[diff.size()]);
        }
    }

    public class ItemStackComparator implements Comparator<ItemStack> {

        public int compare(ItemStack a, ItemStack b) {
            return compare(a, b, false);
        }

        public int compare(ItemStack a, ItemStack b, boolean checkMeta) {
            final int aType = a.getTypeId(), bType = b.getTypeId();
            if (aType < bType) return -1;
            if (aType > bType) return 1;
            final short aData = a.getDurability(), bData = b.getDurability();
            if (aData < bData) return -1;
            if (aData > bData) return 1;
            if (checkMeta) {
                final int aMeta = a.getItemMeta().hashCode(), bMeta = b.getItemMeta().hashCode();
                if (aMeta < bMeta) return -1;
                if (aMeta > bMeta) return 1;
            }
            return 0;
        }
    }

    private ItemStack[] compressInventory(ItemStack[] content) {
        final ArrayList<ItemStack> compressed = new ArrayList<ItemStack>();
        for (final ItemStack iItem : content) {
            if (iItem == null) {
                continue;
            }
            boolean found = false;
            for (final ItemStack cItem : compressed) {
                if (isSimilar(iItem, cItem, config.isLoggingExtraItemMeta())) {
                    cItem.setAmount(cItem.getAmount() + iItem.getAmount());
                    found = true;
                    break;
                }
            }
            if (!found) {
                ItemStack compressedItem = new ItemStack(iItem.getType(), iItem.getAmount(), iItem.getDurability());
                if (iItem.hasItemMeta()) {
                    compressedItem.setItemMeta(iItem.getItemMeta());
                }
                compressed.add(compressedItem);
            }
        }
        Collections.sort(compressed, new ItemStackComparator());
        return compressed.toArray(new ItemStack[compressed.size()]);
    }

    private boolean isSimilar(ItemStack item1, ItemStack item2, boolean checkItemMeta) {
        if (checkItemMeta) {
            return item1.isSimilar(item2);
        } else {
            return item1.getType() == item2.getType() && item1.getDurability() == item2.getDurability();
        }
    }

    public class SimpleItemMeta {
        private final int id;
        private String D1;
        private String[] L1;
        private Map<String, Integer> E1;
        private Map<String, Integer> SE1;
        private String T1;
        private String A1;
        private String[] P1;
        private Integer C1;
        private Integer R1;
        private String O1;
        private Boolean S1;
        private SimplePotionEffect[] P2;
        private SimpleFireworkEffect[] E2;
        private Integer P3;

        public void setItemMeta(ItemStack item) {
            final ItemMeta meta = item.getItemMeta();
            if (getDisplayName() != null) meta.setDisplayName(getDisplayName());
            if (getLore() != null) meta.setLore(Arrays.asList(getLore()));
            final Map<Enchantment, Integer> enchants = getEnchants();
            if (enchants != null) {
                for (Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                    meta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
            }
            final Repairable repairMeta = (Repairable) meta;
            if (getRepairCost() != null) repairMeta.setRepairCost(getRepairCost());

            if (item.getType() == Material.WRITTEN_BOOK || item.getType() == Material.BOOK_AND_QUILL) {
                final BookMeta bookMeta = (BookMeta) meta;
                if (getBookTitle() != null) bookMeta.setTitle(getBookTitle());
                if (getBookAuthor() != null) bookMeta.setAuthor(getBookAuthor());
                if (getBookPages() != null) bookMeta.setPages(getBookPages());
            }
            if (item.getType() == Material.ENCHANTED_BOOK) {
                final EnchantmentStorageMeta enchantStore = (EnchantmentStorageMeta) meta;
                final Map<Enchantment, Integer> bookEnchants = getStoreEnchants();
                if (bookEnchants != null) {
                    for (Entry<Enchantment, Integer> entry : bookEnchants.entrySet()) {
                        enchantStore.addStoredEnchant(entry.getKey(), entry.getValue(), true);
                    }
                }
            }
            if (item.getType() == Material.LEATHER_HELMET || item.getType() == Material.LEATHER_CHESTPLATE
                    || item.getType() == Material.LEATHER_LEGGINGS || item.getType() == Material.LEATHER_BOOTS) {
                final LeatherArmorMeta leatherMeta = (LeatherArmorMeta) meta;
                if (getLeatherColor() != null) leatherMeta.setColor(org.bukkit.Color.fromRGB(getLeatherColor()));
            }
            if (item.getType() == Material.SKULL_ITEM) {
                final SkullMeta skullMeta = (SkullMeta) meta;
                if (getSkullOwner() != null) skullMeta.setOwner(getSkullOwner());
            }
            if (item.getType() == Material.MAP) {
                final MapMeta mapMeta = (MapMeta) meta;
                if (isMapScaling() != null) mapMeta.setScaling(isMapScaling());
            }
            if (item.getType() == Material.POTION) {
                final PotionMeta potMeta = (PotionMeta) meta;
                if (getPotionEffects() != null) {
                    for (SimplePotionEffect effect : getPotionEffects()) {
                        potMeta.addCustomEffect(effect.getPotionEffect(), true);
                    }
                }
            }
            if (item.getType() == Material.FIREWORK) {
                final FireworkMeta fireworkMeta = (FireworkMeta) meta;
                if (getFireworkEffects() != null) {
                    for (SimpleFireworkEffect effect : getFireworkEffects()) {
                        fireworkMeta.addEffects(effect.getFireworkEffect());
                    }
                }
                if (getFireworkPower() != null) fireworkMeta.setPower(getFireworkPower());
            }
            if (item.getType() == Material.FIREWORK_CHARGE) {
                final FireworkEffectMeta fireworkMeta = (FireworkEffectMeta) meta;
                if (getFireworkEffects() != null) fireworkMeta.setEffect(getFireworkEffects()[0].getFireworkEffect());
            }

            item.setItemMeta(meta);
        }

        public SimpleItemMeta(int id, ItemMeta meta) {
            this.id = id;
            if (config.isLoggingExtraItemMeta(ItemMetaType.name) && meta.hasDisplayName()) D1 = meta.getDisplayName();
            if (config.isLoggingExtraItemMeta(ItemMetaType.lore) && meta.hasLore()) L1 = meta.getLore().toArray(new String[meta.getLore().size()]);
            if (config.isLoggingExtraItemMeta(ItemMetaType.enchants) && meta.hasEnchants()) {
                E1 = new HashMap<String, Integer>();
                for (Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                    E1.put(entry.getKey().getName(), entry.getValue());
                }
            }

            if (meta instanceof BookMeta) {
                final BookMeta bookMeta = (BookMeta) meta;
                if (config.isLoggingExtraItemMeta(ItemMetaType.booktitle) && bookMeta.hasTitle()) T1 = bookMeta.getTitle();
                if (config.isLoggingExtraItemMeta(ItemMetaType.bookauthor) && bookMeta.hasAuthor()) A1 = bookMeta.getAuthor();
                if (config.isLoggingExtraItemMeta(ItemMetaType.bookpage) && bookMeta.hasPages()) P1 = bookMeta.getPages().toArray(
                        new String[bookMeta.getPages().size()]);
            }
            if (meta instanceof EnchantmentStorageMeta) {
                final EnchantmentStorageMeta enchantStore = (EnchantmentStorageMeta) meta;
                if (config.isLoggingExtraItemMeta(ItemMetaType.bookenchant) && enchantStore.hasStoredEnchants()) {
                    E1 = new HashMap<String, Integer>();
                    for (Entry<Enchantment, Integer> entry : enchantStore.getStoredEnchants().entrySet()) {
                        E1.put(entry.getKey().getName(), entry.getValue());
                    }
                }
            }
            if (meta instanceof LeatherArmorMeta) {
                final LeatherArmorMeta leatherMeta = (LeatherArmorMeta) meta;
                if (config.isLoggingExtraItemMeta(ItemMetaType.leathercolor) && leatherMeta.getColor() != null) C1 = leatherMeta.getColor().asRGB();
            }
            if (meta instanceof Repairable) {
                final Repairable repairMeta = (Repairable) meta;
                if (config.isLoggingExtraItemMeta(ItemMetaType.repair) && repairMeta.hasRepairCost()) {
                    R1 = repairMeta.getRepairCost();
                }
            }
            if (meta instanceof SkullMeta) {
                final SkullMeta skullMeta = (SkullMeta) meta;
                if (config.isLoggingExtraItemMeta(ItemMetaType.skull) && skullMeta.hasOwner()) O1 = skullMeta.getOwner();
            }
            if (meta instanceof MapMeta) {
                final MapMeta mapMeta = (MapMeta) meta;
                if (config.isLoggingExtraItemMeta(ItemMetaType.map)) S1 = mapMeta.isScaling();
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
                    E2 = new SimpleFireworkEffect[fireworkMeta.getEffects().size()];
                    for (int i = 0; i < E2.length; i++) {
                        E2[i] = new SimpleFireworkEffect(fireworkMeta.getEffects().get(i));
                    }
                    P3 = fireworkMeta.getPower();
                }
            }
            if (meta instanceof FireworkEffectMeta) {
                final FireworkEffectMeta fireworkMeta = (FireworkEffectMeta) meta;
                if (config.isLoggingExtraItemMeta(ItemMetaType.firework) && fireworkMeta.hasEffect()) E2 = new SimpleFireworkEffect[] { new SimpleFireworkEffect(
                        fireworkMeta.getEffect()) };
            }
        }

        public int getId() {
            return id;
        }

        public String getDisplayName() {
            return D1;
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

        public Map<Enchantment, Integer> getStoreEnchants() {
            if (SE1 == null) {
                return null;
            }
            final Map<Enchantment, Integer> enchants = new HashMap<Enchantment, Integer>();
            for (Entry<String, Integer> entry : SE1.entrySet()) {
                enchants.put(Enchantment.getByName(entry.getKey()), entry.getValue());
            }
            return enchants;
        }

        public String getBookTitle() {
            return T1;
        }

        public String getBookAuthor() {
            return A1;
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
            return O1;
        }

        public Boolean isMapScaling() {
            return S1;
        }

        public SimplePotionEffect[] getPotionEffects() {
            return P2;
        }

        public SimpleFireworkEffect[] getFireworkEffects() {
            return E2;
        }

        public Integer getFireworkPower() {
            return P3;
        }

    }

    public class SimpleFireworkEffect {
        private final Boolean flicker;
        private final Boolean trail;
        private final Integer[] colors;
        private final Integer[] fadeColors;
        private final String type;

        public SimpleFireworkEffect(FireworkEffect effect) {
            flicker = effect.hasFlicker();
            trail = effect.hasTrail();
            colors = new Integer[effect.getColors().size()];
            for (int i = 0; i < effect.getColors().size(); i++) {
                colors[i] = effect.getColors().get(i).asRGB();
            }
            fadeColors = new Integer[effect.getFadeColors().size()];
            for (int i = 0; i < effect.getFadeColors().size(); i++) {
                fadeColors[i] = effect.getFadeColors().get(i).asRGB();
            }
            type = effect.getType().name();
        }

        public boolean hasFlicker() {
            return flicker;
        }

        public boolean hasTrail() {
            return trail;
        }

        public Integer[] getColors() {
            return colors;
        }

        public org.bukkit.Color[] getBukkitColors() {
            final org.bukkit.Color[] colors = new org.bukkit.Color[getColors().length];
            for (int i = 0; i < colors.length; i++) {
                colors[i] = org.bukkit.Color.fromRGB(this.colors[i]);
            }
            return colors;
        }

        public Integer[] getFadeColors() {
            return fadeColors;
        }

        public org.bukkit.Color[] getBukkitFadeColors() {
            final org.bukkit.Color[] fadeColors = new org.bukkit.Color[getFadeColors().length];
            for (int i = 0; i < fadeColors.length; i++) {
                fadeColors[i] = org.bukkit.Color.fromRGB(this.fadeColors[i]);
            }
            return fadeColors;
        }

        public org.bukkit.FireworkEffect.Type getType() {
            return org.bukkit.FireworkEffect.Type.valueOf(type);
        }

        public FireworkEffect getFireworkEffect() {
            return FireworkEffect.builder().flicker(flicker).trail(trail).withColor(getBukkitColors()).withFade(getBukkitFadeColors())
                    .with(getType()).build();
        }
    }

    public class SimplePotionEffect {
        private final Integer amplifier;
        private final Integer duration;
        private final String type;
        private final Boolean ambient;

        public SimplePotionEffect(PotionEffect effect) {
            amplifier = effect.getAmplifier();
            duration = effect.getDuration();
            type = effect.getType().getName();
            ambient = effect.isAmbient();
        }

        public int getAmplifier() {
            return amplifier;
        }

        public int getDuration() {
            return duration;
        }

        public PotionEffectType getType() {
            return PotionEffectType.getByName(type);
        }

        public boolean isAmbient() {
            return ambient;
        }

        public PotionEffect getPotionEffect() {
            return new PotionEffect(getType(), duration, amplifier, ambient);
        }
    }

}
