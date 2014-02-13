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
        if (content != null) {
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
                    if (itemMeta.hasItemMeta()) {
                        meta.add(itemMeta);
                    }
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

    public enum MetaType {
        BOOK, ENCHANT_STORAGE, LEATHER, REPAIR, SKULL, MAP, POTION, FIREWORK, FIREWORK_EFFECT;
    }

    public class SimpleItemMeta {
        private final int id;
        private String D1;
        private String[] L1;
        private Map<String, Integer> E1;
        private MetaType type;
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
        private SimpleFireworkEffect E3;

        public void setItemMeta(ItemStack item) {
            final ItemMeta meta = item.getItemMeta();
            if (getDisplayName() != null) meta.setDisplayName(getDisplayName());
            if (getLore() != null) meta.setLore(Arrays.asList(getLore()));
            if (type != null && type != MetaType.ENCHANT_STORAGE) {
                final Map<Enchantment, Integer> enchants = getEnchants();
                if (enchants != null) {
                    for (Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                        meta.addEnchant(entry.getKey(), entry.getValue(), true);
                    }
                }
            }

            if (type != null) {
                if (type == MetaType.BOOK) {
                    final BookMeta bookMeta = (BookMeta) meta;
                    if (getBookTitle() != null) bookMeta.setTitle(getBookTitle());
                    if (getBookAuthor() != null) bookMeta.setAuthor(getBookAuthor());
                    if (getBookPages() != null) bookMeta.setPages(getBookPages());
                } else if (type == MetaType.ENCHANT_STORAGE) {
                    final EnchantmentStorageMeta enchantStore = (EnchantmentStorageMeta) meta;
                    final Map<Enchantment, Integer> bookEnchants = getEnchants();
                    if (bookEnchants != null) {
                        for (Entry<Enchantment, Integer> entry : bookEnchants.entrySet()) {
                            enchantStore.addStoredEnchant(entry.getKey(), entry.getValue(), true);
                        }
                    }
                } else if (type == MetaType.LEATHER) {
                    final LeatherArmorMeta leatherMeta = (LeatherArmorMeta) meta;
                    if (getLeatherColor() != null) leatherMeta.setColor(org.bukkit.Color.fromRGB(getLeatherColor()));
                } else if (type == MetaType.REPAIR) {
                    final Repairable repairMeta = (Repairable) meta;
                    if (getRepairCost() != null) repairMeta.setRepairCost(getRepairCost());
                } else if (type == MetaType.SKULL) {
                    final SkullMeta skullMeta = (SkullMeta) meta;
                    if (getSkullOwner() != null) skullMeta.setOwner(getSkullOwner());
                } else if (type == MetaType.MAP) {
                    final MapMeta mapMeta = (MapMeta) meta;
                    if (isMapScaling() != null) mapMeta.setScaling(isMapScaling());
                } else if (type == MetaType.POTION) {
                    final PotionMeta potMeta = (PotionMeta) meta;
                    if (getPotionEffects() != null) {
                        for (SimplePotionEffect effect : getPotionEffects()) {
                            potMeta.addCustomEffect(effect.getPotionEffect(), true);
                        }
                    }
                } else if (type == MetaType.FIREWORK) {
                    final FireworkMeta fireworkMeta = (FireworkMeta) meta;
                    if (getFireworkEffects() != null) {
                        for (SimpleFireworkEffect effect : getFireworkEffects()) {
                            fireworkMeta.addEffects(effect.getFireworkEffect());
                        }
                    }
                    if (getFireworkPower() != null) fireworkMeta.setPower(getFireworkPower());
                } else if (type == MetaType.FIREWORK_EFFECT) {
                    final FireworkEffectMeta fireworkMeta = (FireworkEffectMeta) meta;
                    if (getFireworkEffect() != null) fireworkMeta.setEffect(getFireworkEffect().getFireworkEffect());
                }
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
                type = MetaType.BOOK;
                final BookMeta bookMeta = (BookMeta) meta;
                if (config.isLoggingExtraItemMeta(ItemMetaType.booktitle) && bookMeta.hasTitle()) T1 = bookMeta.getTitle();
                if (config.isLoggingExtraItemMeta(ItemMetaType.bookauthor) && bookMeta.hasAuthor()) A1 = bookMeta.getAuthor();
                if (config.isLoggingExtraItemMeta(ItemMetaType.bookpage) && bookMeta.hasPages()) P1 = bookMeta.getPages().toArray(
                        new String[meta.getLore().size()]);
            } else if (meta instanceof EnchantmentStorageMeta) {
                type = MetaType.ENCHANT_STORAGE;
                final EnchantmentStorageMeta enchantStore = (EnchantmentStorageMeta) meta;
                if (config.isLoggingExtraItemMeta(ItemMetaType.bookenchant) && enchantStore.hasStoredEnchants()) {
                    E1 = new HashMap<String, Integer>();
                    for (Entry<Enchantment, Integer> entry : enchantStore.getStoredEnchants().entrySet()) {
                        E1.put(entry.getKey().getName(), entry.getValue());
                    }
                }
            } else if (meta instanceof LeatherArmorMeta) {
                type = MetaType.LEATHER;
                final LeatherArmorMeta leatherMeta = (LeatherArmorMeta) meta;
                if (config.isLoggingExtraItemMeta(ItemMetaType.leathercolor) && leatherMeta.getColor() != null) C1 = leatherMeta.getColor().asRGB();
            } else if (meta instanceof Repairable) {
                final Repairable repairMeta = (Repairable) meta;
                if (config.isLoggingExtraItemMeta(ItemMetaType.repair) && repairMeta.hasRepairCost()) {
                    type = MetaType.REPAIR; // Special case
                    C1 = repairMeta.getRepairCost();
                }
            } else if (meta instanceof SkullMeta) {
                type = MetaType.SKULL;
                final SkullMeta skullMeta = (SkullMeta) meta;
                if (config.isLoggingExtraItemMeta(ItemMetaType.skull) && skullMeta.hasOwner()) O1 = skullMeta.getOwner();
            } else if (meta instanceof MapMeta) {
                type = MetaType.MAP;
                final MapMeta mapMeta = (MapMeta) meta;
                if (config.isLoggingExtraItemMeta(ItemMetaType.map)) S1 = mapMeta.isScaling();
            } else if (meta instanceof PotionMeta) {
                type = MetaType.POTION;
                final PotionMeta potMeta = (PotionMeta) meta;
                if (config.isLoggingExtraItemMeta(ItemMetaType.potion) && potMeta.hasCustomEffects()) {
                    P2 = new SimplePotionEffect[potMeta.getCustomEffects().size()];
                    for (int i = 0; i < P2.length; i++) {
                        P2[i] = new SimplePotionEffect(potMeta.getCustomEffects().get(i));
                    }
                }
            } else if (meta instanceof FireworkMeta) {
                type = MetaType.FIREWORK;
                final FireworkMeta fireworkMeta = (FireworkMeta) meta;
                if (config.isLoggingExtraItemMeta(ItemMetaType.firework) && fireworkMeta.hasEffects()) {
                    E2 = new SimpleFireworkEffect[fireworkMeta.getEffects().size()];
                    for (int i = 0; i < E2.length; i++) {
                        E2[i] = new SimpleFireworkEffect(fireworkMeta.getEffects().get(i));
                    }
                    P3 = fireworkMeta.getPower();
                }
            } else if (meta instanceof FireworkEffectMeta) {
                type = MetaType.FIREWORK_EFFECT;
                final FireworkEffectMeta fireworkMeta = (FireworkEffectMeta) meta;
                if (config.isLoggingExtraItemMeta(ItemMetaType.firework) && fireworkMeta.hasEffect()) E3 = new SimpleFireworkEffect(
                        fireworkMeta.getEffect());
            }
        }

        public boolean hasItemMeta() {
            return getType() != null || getDisplayName() != null || getLore() != null || getEnchants() != null;
        }

        public int getId() {
            return id;
        }

        public MetaType getType() {
            return type;
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

        public SimpleFireworkEffect getFireworkEffect() {
            return E3;
        }

    }

    public class SimpleFireworkEffect {
        private final boolean flicker;
        private final boolean trail;
        private final Integer[] colors;
        private final Integer[] fadeColors;
        private final org.bukkit.FireworkEffect.Type type;

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
            type = effect.getType();
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
            return type;
        }

        public FireworkEffect getFireworkEffect() {
            return FireworkEffect.builder().flicker(flicker).trail(trail).withColor(getBukkitColors()).withFade(getBukkitFadeColors()).with(type)
                    .build();
        }
    }

    public class SimplePotionEffect {
        private final int amplifier;
        private final int duration;
        private final PotionEffectType type;
        private final boolean ambient;

        public SimplePotionEffect(PotionEffect effect) {
            amplifier = effect.getAmplifier();
            duration = effect.getDuration();
            type = effect.getType();
            ambient = effect.isAmbient();
        }

        public int getAmplifier() {
            return amplifier;
        }

        public int getDuration() {
            return duration;
        }

        public PotionEffectType getType() {
            return type;
        }

        public boolean isAmbient() {
            return ambient;
        }

        public PotionEffect getPotionEffect() {
            return new PotionEffect(type, duration, amplifier, ambient);
        }
    }

}
