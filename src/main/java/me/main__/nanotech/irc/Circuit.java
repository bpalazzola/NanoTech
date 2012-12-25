package me.main__.nanotech.irc;

import org.bukkit.Material;

public abstract class Circuit {
    //int FULL_POWER = 15;
    public static final Material MATERIAL = Material.IRON_BLOCK;
    private Input in;
    private Output out;
    private CircuitFactory factory;

    final void init(Input in, Output out, CircuitFactory factory) {
        this.in = in;
        this.out = out;
        this.factory = factory;
        onInit();
    }

    public final CircuitFactory getFactory() {
        return factory;
    }

    protected Input getInput() {
        return in;
    }

    protected Output getOutput() {
        return out;
    }

    protected void onInit() {
    }

    protected void onInputChanged() {
    }

    public interface Input {
        boolean[] get();
    }
    public interface Output {
        void output(boolean[] data);
    }
}
