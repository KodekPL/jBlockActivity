package jcraft.simpleinventory.inventory;

import org.bukkit.FireworkEffect;

public class SimpleFireworkEffect {

    private final String T4; // type -> T4
    private final Boolean F2; // flicker -> F2
    private final Boolean T5; // trail -> T5
    private final Integer[] C2; // colors -> C2
    private final Integer[] C3; // fadeColors -> C3

    public SimpleFireworkEffect(FireworkEffect effect) {
        F2 = effect.hasFlicker();
        T5 = effect.hasTrail();
        C2 = new Integer[effect.getColors().size()];
        for (int i = 0; i < effect.getColors().size(); i++) {
            C2[i] = effect.getColors().get(i).asRGB();
        }
        C3 = new Integer[effect.getFadeColors().size()];
        for (int i = 0; i < effect.getFadeColors().size(); i++) {
            C3[i] = effect.getFadeColors().get(i).asRGB();
        }
        T4 = effect.getType().name();
    }

    public String getType() {
        return T4;
    }

    public Boolean hasFlicker() {
        return F2;
    }

    public Boolean hasTrail() {
        return T5;
    }

    public Integer[] getColors() {
        return C2;
    }

    public org.bukkit.Color[] getBukkitColors() {
        final org.bukkit.Color[] colors = new org.bukkit.Color[getColors().length];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = org.bukkit.Color.fromRGB(this.C2[i]);
        }
        return colors;
    }

    public Integer[] getFadeColors() {
        return C3;
    }

    public org.bukkit.Color[] getBukkitFadeColors() {
        final org.bukkit.Color[] fadeColors = new org.bukkit.Color[getFadeColors().length];
        for (int i = 0; i < fadeColors.length; i++) {
            fadeColors[i] = org.bukkit.Color.fromRGB(this.C3[i]);
        }
        return fadeColors;
    }

    public FireworkEffect getFireworkEffect() {
        return FireworkEffect.builder().flicker(F2).trail(T5).withColor(getBukkitColors()).withFade(getBukkitFadeColors())
                .with(org.bukkit.FireworkEffect.Type.valueOf(T4)).build();
    }

}