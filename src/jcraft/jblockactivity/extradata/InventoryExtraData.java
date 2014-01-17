package jcraft.jblockactivity.extradata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class InventoryExtraData implements ExtraData {

    private ItemStack[] content;

    public InventoryExtraData(ItemStack[] content, boolean compress) {
        if (content != null) {
            this.content = compress ? compressInventory(content) : content;
        }
    }

    public InventoryExtraData(String data) {
        final String[] splitter = data.split("\0");
        this.content = new ItemStack[splitter.length];
        for (int i = 0; i < splitter.length; i++) {
            String[] sItem = splitter[i].split(",");
            try {
                this.content[i] = new ItemStack(Integer.parseInt(sItem[0]), Integer.parseInt(sItem[2]), Short.parseShort(sItem[1]));
            } catch (NumberFormatException e) {
                this.content[i] = null;
            }
        }
    }

    public ItemStack[] getContent() {
        return this.content;
    }

    public boolean isEmpty() {
        return this.content == null;
    }

    @Override
    public String getData() {
        final StringBuilder data = new StringBuilder();
        for (ItemStack item : content) {
            data.append(item.getTypeId()).append(',').append(item.getDurability()).append(',').append(item.getAmount()).append('\0');
        }
        return data.toString();
    }

    public void compareInventories(InventoryExtraData invExtraData) {
        final ItemStackComparator comperator = new ItemStackComparator();
        final ArrayList<ItemStack> diff = new ArrayList<ItemStack>();
        final ItemStack[] newContent = invExtraData.getContent();

        final int l1 = this.content.length, l2 = newContent.length;
        int c1 = 0, c2 = 0;
        while (c1 < l1 || c2 < l2) {
            if (c1 >= l1) {
                diff.add(newContent[c2]);
                c2++;
                continue;
            }
            if (c2 >= l2) {
                this.content[c1].setAmount(this.content[c1].getAmount() * -1);
                diff.add(this.content[c1]);
                c1++;
                continue;
            }
            final int comp = comperator.compare(this.content[c1], newContent[c2]);
            if (comp < 0) {
                this.content[c1].setAmount(this.content[c1].getAmount() * -1);
                diff.add(this.content[c1]);
                c1++;
            } else if (comp > 0) {
                diff.add(newContent[c2]);
                c2++;
            } else {
                final int amount = newContent[c2].getAmount() - this.content[c1].getAmount();
                if (amount != 0) {
                    this.content[c1].setAmount(amount);
                    diff.add(this.content[c1]);
                }
                c1++;
                c2++;
            }
        }
        if (diff.isEmpty()) {
            this.content = null;
        } else {
            this.content = diff.toArray(new ItemStack[diff.size()]);
        }
    }

    private ItemStack[] compressInventory(ItemStack[] content) {
        final ArrayList<ItemStack> compressed = new ArrayList<ItemStack>();
        for (final ItemStack iItem : content) {
            if (iItem == null) {
                continue;
            }
            final Material type = iItem.getType();
            final short data = rawData(iItem);
            boolean found = false;
            for (final ItemStack cItem : compressed) {
                if (type == cItem.getType() && data == rawData(cItem)) {
                    cItem.setAmount(cItem.getAmount() + iItem.getAmount());
                    found = true;
                    break;
                }
            }
            if (!found) {
                compressed.add(new ItemStack(type, iItem.getAmount(), data));
            }
        }
        Collections.sort(compressed, new ItemStackComparator());
        return compressed.toArray(new ItemStack[compressed.size()]);
    }

    private short rawData(ItemStack item) {
        return (item.getType() != null) ? (item.getData() != null) ? item.getDurability() : 0 : 0;
    }

    public class ItemStackComparator implements Comparator<ItemStack> {
        @Override
        public int compare(ItemStack a, ItemStack b) {
            final int aType = a.getTypeId(), bType = b.getTypeId();
            if (aType < bType) return -1;
            if (aType > bType) return 1;
            final short aData = rawData(a), bData = rawData(b);
            if (aData < bData) return -1;
            if (aData > bData) return 1;
            return 0;
        }
    }

}
