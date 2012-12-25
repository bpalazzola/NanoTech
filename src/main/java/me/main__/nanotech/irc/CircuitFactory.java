package me.main__.nanotech.irc;

public interface CircuitFactory {
    Circuit build();
    String getId();
    int getInputs();
    int getOutputs();
}
