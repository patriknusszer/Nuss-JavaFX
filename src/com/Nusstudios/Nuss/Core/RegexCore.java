package com.Nusstudios.Nuss.Core;

import java.util.ArrayList;
import java.util.List;

public class RegexCore {
    public static final String VideoPageStreamConfigRegex = "ytplayer\\.config\\s*=\\s*(\\{[\u0000-\uFFFF]*?})\\s*;\\s*ytplayer\\.load";

    public static List<String> getPlaylistPageStreamConfigPatterns() {
        List<String> playlistPageStreamConfigPatterns = new ArrayList<String>();
        playlistPageStreamConfigPatterns.add("window\\s*\\[\\s*\"\\s*ytInitialData\\s*\"\\]\\s*=\\s*(\\{.*\\});");
        // playlistPageStreamConfigPatterns.add("ytInitialData[\\u0000-\\uFFFF]*?=[\\u0000-\\uFFFF]*?(\\{[\\u0000-\\uFFFF]*?\\});");
        return playlistPageStreamConfigPatterns;
    }

    public static final String PlaylistPageHeadersConfigRegex = "ytcfg\\s*\\.\\s*set\\(\\s*(\\{[\\u0000-\\uFFFF]*?\\})\\s*\\);";

    public static List<String> getDecryptionFunctionNamePatterns() {
        List<String> decryptionFunctionNamePatterns = new ArrayList<String>();
        decryptionFunctionNamePatterns.add("\\.\\s*sig\\s*\\|\\|\\s*(?:(?:encodeURIComponent\\s*\\(\\s*(\\w+)\\s*\\((?:\\s*decodeURIComponent)?)|(?:(\\w+)\\s*\\())");
        decryptionFunctionNamePatterns.add("\\.\\s*set\\s*\\(\\s*\"signature\"\\s*,\\s*(?:(?:encodeURIComponent\\s*\\(\\s*(\\w+)\\s*\\((?:\\s*decodeURIComponent)?)|(?:(\\w+?)\\s*\\())");
        decryptionFunctionNamePatterns.add("\\.\\s*set\\s*\\(\\s*\"signature\"\\s*,\\s*(?:(?:encodeURIComponent\\s*\\(\\s*(\\w+)\\s*\\((?:\\s*decodeURIComponent)?)|(?:(\\w+?)\\s*\\())");

        // At the time writing there was a player release which matched all these 3 regular expressions
        decryptionFunctionNamePatterns.add("\\s*signature\\s*\"\\s*\\).+,.+\\.\\s*set\\s*\\(.+,\\s*(?:(?:encodeURIComponent\\s*\\(\\s*(\\w+?)\\s*\\((\\s*decodeURIComponent)?)|(?:(\\w+?)\\s*\\())");
        decryptionFunctionNamePatterns.add("\"\\s*signature\\s*\"\\s*,\\s*(?:(?:encodeURIComponent\\s*\\(\\s*(\\w+)\\s*\\(\\s*decodeURIComponent\\s*\\(\\s*\\w+?\\s*\\.\\s*s)|(?:(\\w+?)\\s*\\(\\s*\\w+?\\s*\\.\\s*s))");
        decryptionFunctionNamePatterns.add("\"\\s*signature\\s*\\\"\\s*:\\s*\"\\s*sig\\s*\"\\s*,.+=\\s*(?:(?:encodeURIComponent\\s*\\(\\s*(\\w+)\\s*\\((?:\\s*decodeURIComponent)?)|(?:(\\w+?)\\s*\\())");

        // These will match up the same function name - f.set(k.sp,HK(k.s)) or f.set(k.sp,encodeURIComponent(HK(decodeURIComponent(k.s))))
        decryptionFunctionNamePatterns.add("(\\w+)\\s*(?:(?:\\(decodeURIComponent\\s*\\(\\s*\\w+\\s*\\.\\s*s\\s*\\))|(?:\\(\\s*\\w*\\s*\\.\\s*s\\s*\\)))\\)");
        decryptionFunctionNamePatterns.add("\\w+\\s*\\.\\s*set\\s*\\(\\s*\\w+\\s*\\.\\s*sp\\s*,\\s*(?:(?:encodeURIComponent\\s*\\(\\s*(\\w+)\\s*\\(\\s*decodeURIComponent\\s*\\(s*\\w+\\s*\\.\\s*s\\s*\\)\\)\\))|(?:(\\w+)\\s*\\(s*\\w*\\s*\\.\\s*s\\s*\\)))\\)");

        // These were not found just yet anywhere, but they are good tries as last resort
        decryptionFunctionNamePatterns.add("(?:(?:(\\w+)\\s*\\(\\s*decodeURIComponent\\(\\s*\\w+\\s*\\.\\s*sig)|(?:(\\w+)\\s*\\(\\s*\\w+\\s*\\.\\s*sig))\\s*\\)");
        decryptionFunctionNamePatterns.add("(?:(?:(\\w+)\\s*\\(\\s*decodeURIComponent\\(\\s*\\w+\\s*\\[\\s*\"\\s*sig\\s*\"\\s*\\])|(?:(\\w+)\\s*\\(\\s*\\w+\\s*\\[\\s*\"\\s*sig\\s*\"\\s*\\]))\\s*\\)");
        decryptionFunctionNamePatterns.add("(?:(?:(\\w+)\\s*\\(\\s*decodeURIComponent\\(\\s*\\w+\\s*\\.\\s*signature\\s*\\))|(?:(\\w+)\\s*\\(\\s*\\w+\\s*\\.\\s*signature))\\s*\\)");
        decryptionFunctionNamePatterns.add("(?:(?:(\\w+)\\s*\\(\\s*decodeURIComponent\\(\\s*\\w+\\s*\\[\\s*\"\\s*signature\\s*\"\\s*\\]\\s*\\))|(?:(\\w+)\\s*\\(\\s*\\w+\\s*\\[\\s*\"\\s*signature\\s*\"\\s*\\]))\\s*\\)");
        decryptionFunctionNamePatterns.add("(?:(?:(\\w+)\\s*\\(\\s*decodeURIComponent\\(\\s*\\w+\\s*\\.\\s*s)|(?:(\\w+)\\s*\\(\\s*\\w+\\s*\\.\\s*s))\\s*\\)");
        decryptionFunctionNamePatterns.add("(?:(?:(\\w+)\\s*\\(\\s*decodeURIComponent\\(\\s*\\w+\\s*\\[\\s*\"\\s*s\\s*\"\\s*\\])|(?:(\\w+)\\s*\\(\\s*\\w+\\s*\\[\\s*\"\\s*s\\s*\"\\s*\\]))\\s*\\)");

        return decryptionFunctionNamePatterns;
    }

    public static List<String> getDecryptionFunctionPatterns(String escapedFunctionName) {
        List<String> decryptionFunctionPatterns = new ArrayList<String>();
        decryptionFunctionPatterns.add("function\\s*" + escapedFunctionName + "\\s*\\(\\s*([\\w$]+)\\s*\\)\\s*\\{[^\\}]*\\}\\s*;");
        decryptionFunctionPatterns.add("var\\s*" + escapedFunctionName + "\\s*=\\s*function\\s*\\(\\s*([\\w$]+)\\s*\\)\\s*\\{[^\\}]*?\\}\\s*;");
        decryptionFunctionPatterns.add(escapedFunctionName + "\\s*=\\s*function\\s*\\(\\s*(\\w+?)\\s*\\)\\s*\\{[\\u0000-\\uFFFF]*?\\}");
        return decryptionFunctionPatterns;
    }

    public static String getWrOfWrAndWdFPattern(String signatureObject) {
        return  "\\w*?\\s*((\\.\\s*\\w*?)|(\\[\\s*\"[\\u0000-\\uFFFF]*?\"\\]))\\s*\\(\\s*" + signatureObject + "\\s*,\\s*\\d*?\\s*\\)";
    }

    public static String getWrOfWrAndWdFPatternFull() {
        return "(\\w*?)\\s*((\\.\\s*(\\w*?))|(\\[\\s*(\"[\\u0000-\\uFFFF]*?\")\\]))\\s*\\(\\s*(\\w*?)\\s*,\\s*(\\d*?)\\s*\\)";
    }

    public static String getWrappedAndWrapperFunctionPattern(String wrapperFunctionName) {
        return wrapperFunctionName + ":(function\\([$\\w,]+\\)\\{[^\\}]+\\})";
    }
}
