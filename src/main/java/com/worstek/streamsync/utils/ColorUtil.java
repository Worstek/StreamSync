package com.worstek.streamsync.utils;

import net.md_5.bungee.api.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    private static final Pattern HEX_HASH_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_TAG_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private static final Pattern HEX_BRACKET_PATTERN = Pattern.compile("\\{#([A-Fa-f0-9]{6})\\}");

    public static String colorize(String text) {
        if (text == null) return "";

        Matcher matcher1 = HEX_HASH_PATTERN.matcher(text);
        StringBuffer buffer1 = new StringBuffer();
        while (matcher1.find()) {
            String hex = matcher1.group(1);
            try {
                matcher1.appendReplacement(buffer1, ChatColor.of("#" + hex).toString());
            } catch (NoSuchMethodError | Exception e) {
                matcher1.appendReplacement(buffer1, "");
            }
        }
        matcher1.appendTail(buffer1);
        text = buffer1.toString();

        Matcher matcher2 = HEX_TAG_PATTERN.matcher(text);
        StringBuffer buffer2 = new StringBuffer();
        while (matcher2.find()) {
            String hex = matcher2.group(1);
            try {
                matcher2.appendReplacement(buffer2, ChatColor.of("#" + hex).toString());
            } catch (NoSuchMethodError | Exception e) {
                matcher2.appendReplacement(buffer2, "");
            }
        }
        matcher2.appendTail(buffer2);
        text = buffer2.toString();

        Matcher matcher3 = HEX_BRACKET_PATTERN.matcher(text);
        StringBuffer buffer3 = new StringBuffer();
        while (matcher3.find()) {
            String hex = matcher3.group(1);
            try {
                matcher3.appendReplacement(buffer3, ChatColor.of("#" + hex).toString());
            } catch (NoSuchMethodError | Exception e) {
                matcher3.appendReplacement(buffer3, "");
            }
        }
        matcher3.appendTail(buffer3);
        text = buffer3.toString();

        return ChatColor.translateAlternateColorCodes('&', text);
    }
}