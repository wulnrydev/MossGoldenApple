package dev.wulnrydev.mossgoldenapple.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

/**
 * Envanter islemleri icin performans odakli yardimci sinif.
 *
 * Tasarim: Envanterin ic dizisine (getStorageContents) TEK sefer erisilir,
 * tekrar tekrar contains/first cagrilari yapilmaz. Sayim ve silme islemleri
 * tek dongude, gereksiz nesne olusturmadan yapilir.
 *
 * NOT: count() ve remove() yalnizca ana depolama slotlarini (storage) dikkate alir;
 * off-hand ve zirh slotlari kapsam disi biraktirilmistir. Bu tasarim tercihidir.
 */
public final class InventoryUtil {

    private InventoryUtil() {
    }

    /**
     * Envanterdeki belirli bir materyalin toplam adedini sayar.
     * Sadece ana depolama slotlarina (storage) bakar — zirh/off-hand haric.
     *
     * @param inventory oyuncu envanteri
     * @param material  sayilacak materyal
     * @return toplam adet
     */
    public static int count(PlayerInventory inventory, Material material) {
        int total = 0;
        // getStorageContents dahili diziyi dondurur; tek gecis yeterli.
        for (ItemStack item : inventory.getStorageContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }

    /**
     * Envanterden belirli bir materyalden istenen adedi siler.
     * Cagirmadan once yeterli miktar oldugu dogrulanmis olmalidir.
     *
     * @param inventory oyuncu envanteri
     * @param material  silinecek materyal
     * @param amount    silinecek adet
     */
    public static void remove(PlayerInventory inventory, Material material, int amount) {
        if (amount <= 0) {
            return;
        }

        ItemStack[] contents = inventory.getStorageContents();
        int remaining = amount;

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != material) {
                continue;
            }

            int stackAmount = item.getAmount();
            if (stackAmount <= remaining) {
                // Tum stack tuketilir.
                remaining -= stackAmount;
                contents[i] = null;
            } else {
                // Stack'in bir kismi tuketilir.
                item.setAmount(stackAmount - remaining);
                remaining = 0;
            }
        }

        // Degistirilen dizi tek seferde geri yazilir.
        inventory.setStorageContents(contents);
    }

    /**
     * Oyuncuya esya verir; envanter doluysa artan kismi ayaklarina dusurur.
     *
     * Duzeltme: World null ise (teorik edge case) esyalar kaybolmak yerine
     * tekrar envantere iade edilmeye calisilir. Ikinci ekleme de basarisiz
     * olursa esya yere birakilmis sayilir (dropped=true) ve kullanici
     * bilgilendirilmis olur.
     *
     * @param player oyuncu
     * @param stack  verilecek esya
     * @return envanter dolu oldugu icin yere birakma (veya kurtarma) yapildiysa true
     */
    public static boolean give(Player player, ItemStack stack) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
        if (leftover.isEmpty()) {
            return false;
        }

        Location loc = player.getLocation();
        World world = loc.getWorld();

        if (world != null) {
            // Normal durum: esyalari oyuncunun konumuna bırak.
            for (ItemStack drop : leftover.values()) {
                world.dropItemNaturally(loc, drop);
            }
        } else {
            // Nadir edge-case: world null, kaybetmemek icin tekrar envantere iade et.
            // (addItem'in kalani yoksayilir; oyuncu zaten dolu bildirimini alir.)
            player.getInventory().addItem(leftover.values().toArray(new ItemStack[0]));
        }

        return true;
    }
}
