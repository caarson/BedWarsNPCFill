package com.andrei1058.spigot.sidebar;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PAPIAdapter implements PAPISupport {

    public PAPIAdapter() {}

    @Override
    public String replacePlaceholders(Player p, String s) {
        return PlaceholderAPI.setPlaceholders(p, s);
    }

    @Override
    public boolean hasPlaceholders(@NotNull String s) {
        Pattern pattern =  Pattern.compile("%([^%]+)%");
        Matcher matcher = pattern.matcher(s);
        return matcher.find();
    }
}
