package me.main__.nanotech.irc;

import me.main__.nanotech.NanoTech;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class Storage {
    private static Storage INSTANCE;

    private final NanoTech plugin;
    private final Collection<? extends IRCLoader> loaders;
    private final Map<String, CircuitFactory> circuitMap;

    public Storage(final NanoTech plugin, final Collection<? extends IRCLoader> loaders) {
        INSTANCE = this;

        this.plugin = plugin;
        this.loaders = loaders;
        this.circuitMap = new HashMap<String, CircuitFactory>();
    }

    public void load(File storageDir) {
        storageDir.mkdirs();

        for (IRCLoader loader : loaders) {
            for (File file : storageDir.listFiles(loader.getFilter())) {
                try {
                    CircuitFactory irc = loader.load(file);
                    if (circuitMap.containsKey(irc.getId()))
                        continue; // skip
                    circuitMap.put(irc.getId(), irc);
                } catch (IRCLoadingException e) {
                    plugin.getLogger().log(Level.INFO, "Failed to load IRC from file " + file, e);
                }
            }
        }
    }

    public CircuitFactory getIRC(String id) {
        return circuitMap.get(id);
    }

    public static CircuitFactory getIRC_static(final String id) {
        // UGLY!
        return new CircuitFactory() {
            private CircuitFactory lazy = null;

            protected CircuitFactory getLazy() {
                if (lazy == null)
                    lazy = INSTANCE.getIRC(id);
                return lazy;
            }

            @Override
            public Circuit build() {
                return getLazy().build();
            }

            @Override
            public String getId() {
                return getLazy().getId();
            }

            @Override
            public int getInputs() {
                return getLazy().getInputs();
            }

            @Override
            public int getOutputs() {
                return getLazy().getOutputs();
            }
        };
    }

    public List<String> matchStart(final String start) {
        List<String> matches = new LinkedList<String>();
        for (Map.Entry<String, CircuitFactory> entry : circuitMap.entrySet())
            if (entry.getKey().startsWith(start))
                matches.add(entry.getKey());
        return matches;
    }
}
