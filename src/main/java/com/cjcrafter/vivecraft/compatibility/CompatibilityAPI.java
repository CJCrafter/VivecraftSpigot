package com.cjcrafter.vivecraft.compatibility;

import com.cjcrafter.foliascheduler.util.MinecraftVersions;
import org.bukkit.Bukkit;
import com.cjcrafter.vivecraft.VSE;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CompatibilityAPI {

    private static VivecraftCompatibility compatibility;

    public static VivecraftCompatibility getCompatibility() {

        // When compatibility has not yet been setup
        if (compatibility == null) {

            // Get the version string like 'v1_19_R2' for 1.19.3
            String version = MinecraftVersions.getCurrent().toProtocolString();
            try {

                // If a class exists for this minecraft protocol version, then
                // we should cache and instance of it to be used as the compatibility
                Class<?> clazz = Class.forName("com.cjcrafter.vivecraft.compatibility.Vivecraft_" + version);
                Object instance = clazz.getDeclaredConstructor().newInstance();
                compatibility = (VivecraftCompatibility) instance;

            } catch (ReflectiveOperationException ex) {
                Logger log = VSE.me.getLogger();
                log.log(Level.WARNING, "Your version '" + version + "' is not fully supported");
                log.log(Level.WARNING, "Check Spigot for a list of supported Minecraft versions");
                log.log(Level.WARNING, "If you just updated your server to the newest version of Minecraft, make sure you update VivecraftSpigot as well!");
                log.log(Level.WARNING, "https://www.spigotmc.org/resources/111303/");
                log.log(Level.WARNING, "The following features will now be disabled: ");
                log.log(Level.WARNING, "  - CreeperRadius");
                log.log(Level.WARNING, "  - Climbing");
                log.log(Level.WARNING, "  - Teleporting");
                log.log(Level.WARNING, "  - Enderman Staring");
                log.log(Level.WARNING, "  - More features then this may be disabled.");

                // Basic support for all versions
                compatibility = new UnknownVivecraftCompatibility();
            }
        }

        return compatibility;
    }
}
