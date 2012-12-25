package me.main__.nanotech.irc;

public abstract class SimpleIOCircuit extends Circuit {
    @Override
    protected void onInputChanged() {
        getOutput().output(run(getInput().get()));
    }

    protected abstract boolean[] run(boolean[] in);
}
