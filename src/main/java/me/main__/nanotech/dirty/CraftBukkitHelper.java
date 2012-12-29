package me.main__.nanotech.dirty;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import me.main__.nanotech.NanoTech;
import me.main__.nanotech.TickGetter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CraftBukkitHelper {
    private static final class PatchInformation {
        private final String nmsPackage;
        private final String diodeCanStayMethod;
        private final String diodeCanStayDesc;
        private final String canStayBody;
        private final String serverClass;
        private final String currentTickField;

        private PatchInformation(final String nmsPackage, final String diodeCanStayMethod, final String diodeCanStayDesc, final String canStayBody, final String serverClass, final String currentTickField) {
            this.nmsPackage = nmsPackage;
            this.diodeCanStayMethod = diodeCanStayMethod;
            this.diodeCanStayDesc = diodeCanStayDesc;
            this.canStayBody = canStayBody;
            this.serverClass = serverClass;
            this.currentTickField = currentTickField;
        }
    }

    private interface PIBuilder {
        PatchInformation build();
        int getMajor();
        int getMinor();
        int getRev();
        PIBuilder tryCompatibilityHack(int major, int minor, int rev);
    }

    private static final class PIBuilder_v1 implements PIBuilder {
        private int major;
        private int minor;
        private int rev;
        private String canStayName;

        private PIBuilder_v1(final int major, final int minor, final int rev, final String canStayName) {
            this.major = major;
            this.minor = minor;
            this.rev = rev;
            this.canStayName = canStayName;
        }

        @Override
        public PatchInformation build() {
            return new PatchInformation(String.format("net.minecraft.server.v%d_%d_%d.", major, minor, rev),
                    canStayName, String.format("(Lnet/minecraft/server/v%d_%d_%d/World;III)Z", major, minor, rev),
                    "{" +
                            "boolean b = super.d($1, $2, $3, $4);" +
                            "return b ? b : me.main__.nanotech.dirty.CraftBukkitHelper.canStay($1.getWorld(), $2, $3, $4);" +
                    "}", "MinecraftServer", "currentTick");
        }

        @Override
        public PIBuilder_v1 tryCompatibilityHack(int major, int minor, int rev) {
            return new PIBuilder_v1(major, minor, rev, this.canStayName);
        }

        @Override
        public int getMajor() {
            return major;
        }

        @Override
        public int getMinor() {
            return minor;
        }

        @Override
        public int getRev() {
            return rev;
        }
    }

    private static final Pattern CB_VERSION = Pattern.compile("git-Bukkit-\\d\\.\\d\\.\\d-R\\d\\.\\d-[\\d]+-g[\\dabcdef]+(-b[\\d]+jnks)? \\(MC: (?<major>[\\d]+)\\.(?<minor>[\\d]+)\\.(?<rev>[\\d]+)\\)");
    private static final PIBuilder[][][] SUPPORTED_VERSIONS = {
            null, // 0.X
            { // 1.X
                    null, // 1.0
                    null, // 1.1
                    null, // 1.2
                    null, // 1.3
                    { // 1.4
                            null, // 1.4.0
                            null, // 1.4.1
                            null, // 1.4.2
                            null, // 1.4.3
                            null, // 1.4.4
                            null, // 1.4.5
                            new PIBuilder_v1(1, 4, 6, "d"),
                    }
            }
    };

    private static NanoTech plugin;

    public static boolean run(final NanoTech plugin, final boolean force) {
        CraftBukkitHelper.plugin = plugin;

        if (!Bukkit.getName().equals("CraftBukkit"))
            return false; // nope.

        Matcher matcher = CB_VERSION.matcher(Bukkit.getVersion());

        if (!matcher.matches())
            return false; // nope.

        int major = Integer.parseInt(matcher.group("major"));
        int minor = Integer.parseInt(matcher.group("minor"));
        int rev = Integer.parseInt(matcher.group("rev"));

        try {
            PIBuilder[] revs = SUPPORTED_VERSIONS[major][minor];
            if (revs != null) {
                PIBuilder pi = (revs.length > rev) ? revs[rev] : null;

                if (pi == null && force) {
                    // we might be INSANELY lucky
                    // the last version might also work with this one
                    for (int i = Math.min(revs.length - 1, rev); i >= 0; i--) {
                        if (revs[i] != null) {
                            plugin.getLogger().warning(String.format("Your FORCE makes me use code for MC %d.%d.%d",
                                    revs[i].getMajor(), revs[i].getMinor(), revs[i].getRev()));
                            plugin.getLogger().warning("Remember, you have been warned.");
                            pi = revs[i].tryCompatibilityHack(major, minor, rev);
                            break;
                        }
                    }
                }

                if (pi != null) return doIt(plugin, pi.build());
                else {
                    plugin.getLogger().warning("CraftBukkit injection failed: Unsupported revision!");
                    if (!force) {
                        plugin.getLogger().warning("Enabling force will either fix this or screw up your server.");
                        plugin.getLogger().warning("You have been warned.");
                    } else plugin.getLogger().warning("Force didn't help.");
                    return false;
                }
            }
        } catch (NullPointerException e) {
            // discard
        } catch (ArrayIndexOutOfBoundsException e) {
            // discard
        }
        // illegal major/minor. nope.
        plugin.getLogger().warning("CraftBukkit injection failed: Unsupported major/minor version!");
        return false;
    }

    private static void USE_DA_FORCE(Field f, Object obj, Object val) throws Exception {
        // Meta-Reflection. Enough said.
        Field ff = Field.class.getDeclaredField("modifiers");
        ff.setAccessible(true);
        ff.set(f, ((Integer)ff.get(f)) & ~Modifier.FINAL);
        f.setAccessible(true);
        f.set(obj, val);
    }

    @Deprecated // only called from javassist proxy code
    public static boolean canStay(World w, int x, int y, int z) {
        return plugin.getIRCController().handleEnvironmentalBlockBreak(new Location(w, x, y, z));
    }

    public static TickGetter TICK_GETTER;

    private static boolean doIt(final NanoTech plugin, final PatchInformation pi) {
        try {
            // set up javassist
            ClassPool pool = ClassPool.getDefault();
            pool.appendClassPath(new LoaderClassPath(CraftBukkitHelper.class.getClassLoader()));
            pool.get(CraftBukkitHelper.class.getName());

            // create a fake/proxy diode with an overridden canStay method and inject it
            CtClass diodeClass = pool.get(pi.nmsPackage + "BlockDiode");
            CtClass proxyDiode = pool.makeClass("me.main__.nanotech.dirty.proxy.BlockDiode", diodeClass);
            CtMethod realCanStay = diodeClass.getMethod(pi.diodeCanStayMethod, pi.diodeCanStayDesc);
            CtMethod newCanStay = new CtMethod(realCanStay, proxyDiode, null);
            newCanStay.setBody(pi.canStayBody, "super", pi.diodeCanStayMethod);
            proxyDiode.addMethod(newCanStay);

            Class<?> clazz = proxyDiode.toClass();
            Constructor<?> ctor = clazz.getDeclaredConstructor(int.class, boolean.class);
            ctor.setAccessible(true);
            // we have to clear them out of the blocks list before we can instantiate them
            Class<?> blockClass = Class.forName(pi.nmsPackage + "Block");
            Field blocksListField = blockClass.getField("byId");
            Object blocksList = blocksListField.get(null);
            Array.set(blocksList, 93, null);
            Array.set(blocksList, 94, null);
            Object repeaterOff = ctor.newInstance(93, false);
            Object repeaterOn = ctor.newInstance(94, true);

            USE_DA_FORCE(blockClass.getField("DIODE_OFF"), null, repeaterOff);
            USE_DA_FORCE(blockClass.getField("DIODE_ON"), null, repeaterOn);

            final Class<?> server = Class.forName(pi.nmsPackage + pi.serverClass);
            final Field tickField = server.getField(pi.currentTickField);
            // create the TickGetter
            TICK_GETTER = new TickGetter() {
                @Override
                public int getCurrentTick() {
                    try {
                        return tickField.getInt(null);
                    } catch (IllegalAccessException e) {
                        throw new Error(e);
                    }
                }
            };

            // fix classloader. this totally isn't already dirty enough... -.-
            ClassLoader nmsLoader = blockClass.getClassLoader();
            Field parentField = ClassLoader.class.getDeclaredField("parent");
            parentField.setAccessible(true);
            USE_DA_FORCE(parentField, nmsLoader, new InterceptingClassLoader(
                    (ClassLoader)parentField.get(nmsLoader), CraftBukkitHelper.class));

            plugin.getLogger().log(Level.INFO, "CraftBukkit injection successfully completed.");
            return true;
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "CraftBukkit injection failed.", t);
            return false;
        }
    }
}
