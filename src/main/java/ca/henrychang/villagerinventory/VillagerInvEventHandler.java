package ca.henrychang.villagerinventory;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class VillagerInvEventHandler implements Listener {
    VillagerInventory plugin;

    public VillagerInvEventHandler(VillagerInventory plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onVillagerDied(EntityDeathEvent e) {
        if (!plugin.dropOnDeath)
            return;

        if (!(e.getEntity() instanceof Villager v))
            return;

        for (ItemStack is : v.getInventory().getContents())
            if (is != null)
                v.getWorld().dropItemNaturally(v.getLocation(), is);
    }

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND)
            return;

        Player player = e.getPlayer();
        Material hand = player.getInventory().getItemInMainHand().getType();

        if (hand != plugin.interactMaterial)
            return;

        if (!(e.getRightClicked() instanceof Villager v))
            return;

        if (v.isLeashed())
            return;

        e.setCancelled(true);

        Inventory villagerInv = v.getInventory();

        String title = (v.getCustomName() != null)
                ? v.getName() + "'s Inventory (" + v.getProfession() + ")"
                : v.getName() + "'s Inventory";

        Inventory customInv = Bukkit.createInventory(
                new VillagerInventoryHolder(),
                9,
                title
        );

        for (ItemStack item : villagerInv.getContents()) {
            if (item != null)
                customInv.addItem(item);
        }

        customInv.setItem(8, new ItemStack(Material.BARRIER));

        if (v.isSleeping())
            v.wakeup();

        player.openInventory(customInv);
        plugin.invMap.put(player.getUniqueId(), v);
    }

    @EventHandler
    public void onClick(InventoryClickEvent evt) {
        if (!(evt.getWhoClicked() instanceof Player player))
            return;

        Inventory inv = evt.getInventory();

        if (!(inv.getHolder() instanceof VillagerInventoryHolder))
            return;

        if (inv.getSize() != 9 || inv.getType() != InventoryType.CHEST)
            return;

        if (!plugin.invMap.containsKey(player.getUniqueId()))
            return;

        int slot = evt.getSlot();
        if (slot < 0 || slot > 8)
            return;

        Villager villager = plugin.invMap.get(player.getUniqueId());
        if (villager == null)
            return;

        ItemStack clicked = evt.getCurrentItem();
        if (clicked != null && clicked.getType() == Material.BARRIER) {
            evt.setCancelled(true);
            return;
        }

        Bukkit.getScheduler().runTask(plugin,
                new InvUpdater(inv, villager.getInventory(), player));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent evt) {
        if (!(evt.getWhoClicked() instanceof Player player))
            return;

        Inventory inv = evt.getInventory();

        if (!(inv.getHolder() instanceof VillagerInventoryHolder))
            return;

        if (inv.getSize() != 9 || inv.getType() != InventoryType.CHEST)
            return;

        Villager villager = plugin.invMap.get(player.getUniqueId());
        if (villager == null)
            return;

        Bukkit.getScheduler().runTask(plugin,
                new InvUpdater(inv, villager.getInventory(), player));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent evt) {
        if (!(evt.getPlayer() instanceof Player player))
            return;

        Inventory inv = evt.getInventory();

        if (!(inv.getHolder() instanceof VillagerInventoryHolder))
            return;

        if (inv.getSize() != 9 || inv.getType() != InventoryType.CHEST)
            return;

        Villager villager = plugin.invMap.get(player.getUniqueId());
        if (villager == null)
            return;

        Bukkit.getScheduler().runTask(plugin,
                new InvUpdater(inv, villager.getInventory(), player));

        plugin.invMap.remove(player.getUniqueId());
    }

    private class InvUpdater implements Runnable {
        Inventory sourceInv, targetInv;
        boolean needReStack;
        Player player;

        public InvUpdater(Inventory sourceInv, Inventory targetInv, Player player) {
            this(sourceInv, targetInv, player, true);
        }

        public InvUpdater(Inventory sourceInv, Inventory targetInv, Player player, boolean needReStack) {
            this.sourceInv = sourceInv;
            this.targetInv = targetInv;
            this.needReStack = needReStack;
            this.player = player;
        }

        @Override
        public void run() {
            for (int i = 0; i < 8; i++)
                targetInv.setItem(i, sourceInv.getItem(i));

            if (needReStack) {
                restackInv(targetInv);
                Bukkit.getScheduler().runTask(plugin,
                        new InvUpdater(targetInv, sourceInv, player, false));
            }
        }

        private void restackInv(Inventory inv) {
            boolean changed = false;

            loop1:
            for (int i = 0; i < 8; i++) {
                ItemStack cur = inv.getItem(i);

                if (cur == null) {
                    for (int j = i + 1; j < 8; j++)
                        if (inv.getItem(j) != null) {
                            inv.setItem(i, inv.getItem(j));
                            inv.setItem(j, null);
                            changed = true;
                            continue loop1;
                        }
                    continue;
                }

                if (cur.getAmount() < cur.getMaxStackSize())
                    for (int j = i + 1; j < 8; j++) {
                        ItemStack other = inv.getItem(j);

                        if (other != null && other.isSimilar(cur)) {
                            int newSize = cur.getAmount() + other.getAmount();

                            if (newSize > cur.getMaxStackSize()) {
                                other.setAmount(newSize - cur.getMaxStackSize());
                                cur.setAmount(cur.getMaxStackSize());
                                changed = true;
                                break;
                            } else {
                                cur.setAmount(newSize);
                                inv.setItem(j, null);
                                changed = true;
                            }
                        }
                    }
            }

            if (changed)
                restackInv(inv);
        }
    }
}
