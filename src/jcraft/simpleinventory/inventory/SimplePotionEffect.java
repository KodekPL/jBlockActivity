package jcraft.simpleinventory.inventory;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SimplePotionEffect {

    private final String T3; // type -> T3
    private final Integer A3; // amplifier -> A3
    private final Integer D2; // duration -> D2
    private final Boolean A4; // ambient -> A4

    public SimplePotionEffect(PotionEffect effect) {
        T3 = effect.getType().getName();
        A3 = effect.getAmplifier();
        D2 = effect.getDuration();
        A4 = effect.isAmbient();
    }

    public String getType() {
        return T3;
    }

    public Integer getAmplifier() {
        return A3;
    }

    public Integer getDuration() {
        return D2;
    }

    public Boolean isAmbient() {
        return A4;
    }

    public PotionEffect getPotionEffect() {
        return new PotionEffect(PotionEffectType.getByName(T3), D2, A3, A4);
    }

}
