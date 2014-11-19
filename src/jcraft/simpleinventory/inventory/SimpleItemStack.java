package jcraft.simpleinventory.inventory;

import jcraft.jblockactivity.config.WorldConfig;
import jcraft.jblockactivity.utils.ActivityUtil;

import org.bukkit.inventory.ItemStack;

public class SimpleItemStack {

    private final Integer T1; // type -> T1
    private final Short D1; // durability -> D1
    private final Integer A1; // amount -> A1
    private final SimpleItemMeta M1; // meta -> M1

    public SimpleItemStack(ItemStack item, WorldConfig config) {
        this.T1 = item.getTypeId();
        this.D1 = item.getDurability();
        this.A1 = item.getAmount();

        if (item.hasItemMeta()) {
            this.M1 = new SimpleItemMeta(item, config);
        } else {
            M1 = null;
        }
    }

    public ItemStack getItem() {
        ItemStack item = new ItemStack((T1 == null) ? 1 : T1, (A1 == null) ? 0 : A1, (D1 == null) ? 1 : D1);

        if (M1 != null) {
            item = M1.setItemMeta(item);
        }

        return item;
    }

    public String toJSON() {
        return ActivityUtil.toJson(this);
    }

}
