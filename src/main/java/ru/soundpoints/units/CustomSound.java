package ru.soundpoints.units;

import org.bukkit.Sound;

public class CustomSound {
    private Sound sound;
    private float tonality;
    private float volume;
    private float hearBlocks;

    public CustomSound(Sound sound, float tonality, float volume, float hearBlocks) {
        this.sound = sound;
        this.tonality = tonality;
        this.volume = volume;
        this.hearBlocks = hearBlocks;
    }

    public Sound getSound() { return sound; }
    public float getTonality() { return tonality; }
    public float getVolume() { return volume / 100.0f; }
    public float getHearBlocks() { return hearBlocks; }
}