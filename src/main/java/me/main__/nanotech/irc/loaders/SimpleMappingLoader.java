package me.main__.nanotech.irc.loaders;

import me.main__.nanotech.irc.Circuit;
import me.main__.nanotech.irc.CircuitFactory;
import me.main__.nanotech.irc.IRCLoader;
import me.main__.nanotech.irc.IRCLoadingException;
import me.main__.nanotech.irc.SimpleIOCircuit;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SimpleMappingLoader implements IRCLoader {
    @Override
    public CircuitFactory load(final File file) throws IRCLoadingException {
        try {
            List<String> lines = Files.readAllLines(file.toPath(), Charset.defaultCharset());
            if (lines.isEmpty())
                throw new IRCLoadingException("The file is empty!");
            final List<String> sinput = new ArrayList<String>();
            final List<String> soutput = new ArrayList<String>();
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).isEmpty())
                    continue;
                String[] parts = lines.get(i).split("=");
                if (parts.length != 2)
                    throw new IRCLoadingException("Parsing error at line " + i);
                sinput.add(parts[0]);
                soutput.add(parts[1]);
            }
            if (sinput.size() != new HashSet<String>(sinput).size())
                throw new IRCLoadingException("Ambigous inputs!");
            final List<boolean[]> input = new ArrayList<boolean[]>();
            final List<boolean[]> output = new ArrayList<boolean[]>();
            for (int i = 0; i < sinput.size(); i++) {
                boolean[] in = toBools(sinput.get(i));
                boolean[] out = toBools(soutput.get(i));
                if ((in == null) || (out == null))
                    throw new IRCLoadingException("Parsing error at line " + i);
                input.add(in);
                output.add(out);
            }
            final int inputs = input.get(0).length;
            final int outputs = output.get(0).length;
            for (int i = 1; i < input.size(); i++)
                if ((input.get(i).length != inputs) || (output.get(i).length != outputs))
                    throw new IRCLoadingException("Inputs/Outputs inconsistency detected!");
            int possibleInputs = 1 << inputs; // possibleInputs = 2 ^ inputs
            if (input.size() != possibleInputs)
                throw new IRCLoadingException(String.format("An IRC with %d inputs has %d combinations, not %d!",
                        inputs, possibleInputs, input.size()));
            final String id = file.getName().substring(0, file.getName().length() - 10);
            return new CircuitFactory() {
                @Override
                public Circuit build() {
                    return new SimpleIOCircuit() {
                        final boolean[][] inputCombinations = input.toArray(new boolean[input.size()][]);
                        final boolean[][] outputCombinations = output.toArray(new boolean[output.size()][]);
                        @Override
                        public boolean[] run(final boolean[] input) {
                            int index = -1;
                            for (int i = 0; i < inputCombinations.length; i++) {
                                boolean[] combination = inputCombinations[i];
                                boolean matches = true;
                                for (int x = 0; x < input.length; x++) {
                                    if (input[x] != combination[x]) {
                                        matches = false;
                                        break;
                                    }
                                }
                                if (matches) {
                                    index = i;
                                    break;
                                }
                            }

                            if (index == -1)
                                throw new Error("Congratulations, the universe just collapsed.");

                            return outputCombinations[index];
                        }
                    };
                }

                @Override
                public int getOutputs() {
                    return outputs;
                }

                @Override
                public int getInputs() {
                    return inputs;
                }

                @Override
                public String getId() {
                    return id;
                }
            };
        } catch (IOException e) {
            throw new IRCLoadingException(e);
        }
    }

    private static boolean[] toBools(String s) {
        boolean[] arr = new boolean[s.length()];
        for (int i = 0; i < s.length(); i++)
            if (s.charAt(i) == '0')
                arr[i] = false;
            else if (s.charAt(i) == '1')
                arr[i] = true;
            else
                return null;
        return arr;
    }

    @Override
    public FilenameFilter getFilter() {
        return new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".simpleirc");
            }
        };
    }
}
