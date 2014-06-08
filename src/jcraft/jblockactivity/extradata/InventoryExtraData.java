package jcraft.jblockactivity.extradata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.config.WorldConfig;
import jcraft.jblockactivity.utils.ActivityUtil;
import jcraft.simpleinventory.inventory.SimpleItemStack;

import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

public class InventoryExtraData implements ExtraData {

    private WorldConfig config;

    private ItemStack[] content;
    private SimpleItemStack[] sInv;

    public InventoryExtraData(ItemStack[] content, boolean compress, World world) {
        this(content, compress, BlockActivity.getWorldConfig(world.getName()));
    }

    public InventoryExtraData(ItemStack[] content, boolean compress, WorldConfig config) {
        this.config = config;
        if (config != null && content != null) {
            this.content = compress ? compressInventory(content) : content;
        }
    }

    public InventoryExtraData(SimpleItemStack[] content, World world) {
        this.config = BlockActivity.getWorldConfig(world.getName());
        this.sInv = content;
    }

    public ItemStack[] getContent() {
        if (content == null && sInv != null) {
            content = new ItemStack[sInv.length];
            for (int i = 0; i < sInv.length; i++) {
                content[i] = sInv[i].getItem();
            }
        }
        return content;
    }

    public void fillData() {
        if (sInv != null || content == null) {
            return;
        }
        this.sInv = new SimpleItemStack[content.length];
        for (int i = 0; i < content.length; i++) {
            sInv[i] = new SimpleItemStack(content[i], config);
        }
    }

    public SimpleItemStack[] getSimpleContent() {
        fillData();
        return sInv;
    }

    public boolean isEmpty() {
        return getContent() == null;
    }

    @Override
    public String getData() {
        fillData();
        this.config = null;
        this.content = null;
        return ActivityUtil.toJson(this);
    }

    @Override
    public boolean isNull() {
        return isEmpty();
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

}
