package me.main__.nanotech.irc;

import java.io.File;
import java.io.FilenameFilter;

public interface IRCLoader {
    CircuitFactory load(File file) throws IRCLoadingException;
    FilenameFilter getFilter();
}
