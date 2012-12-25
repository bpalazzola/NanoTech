package me.main__.nanotech.irc.loaders;

import me.main__.nanotech.irc.CircuitFactory;
import me.main__.nanotech.irc.IRCLoader;
import me.main__.nanotech.irc.IRCLoadingException;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JavaCircuitLoader implements IRCLoader {
    @Override
    public CircuitFactory load(final File file) throws IRCLoadingException {
        try {
            final JarFile jar = new JarFile(file);
            final JarEntry descEntry = jar.getJarEntry("irc.yml");
            final InputStream entryStream = jar.getInputStream(descEntry);
            final Configuration desc = YamlConfiguration.loadConfiguration(entryStream);
            entryStream.close();
            jar.close();

            final ClassLoader loader = new URLClassLoader(new URL[] { file.toURI().toURL() },
                    JavaCircuitLoader.class.getClassLoader());
            final Class<?> clazz = loader.loadClass(desc.getString("class"));
            return clazz.asSubclass(CircuitFactory.class).newInstance();
        } catch (Exception e) {
            throw new IRCLoadingException(e);
        }
    }

    @Override
    public FilenameFilter getFilter() {
        return new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".jar");
            }
        };
    }
}
