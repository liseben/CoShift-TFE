package com.coshift.api.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class TitleNormalizer {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9\\s]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public static String normalize(String title) {
        if (title == null) return "";
        String decomposed = Normalizer.normalize(title, Normalizer.Form.NFD);
        String noAccents = decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}", "");
        String lower = noAccents.toLowerCase();
        String noSpecial = NON_ALNUM.matcher(lower).replaceAll(" ");
        return WHITESPACE.matcher(noSpecial).replaceAll(" ").trim();
    }

    public static boolean areSimilar(String a, String b) {
        if (a.equals(b)) return true;
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) return true;
        int distance = levenshtein(a, b);
        return (double) distance / maxLen < 0.15; // 15% de différence max
    }

    private static int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i-1][j] + 1, dp[i][j-1] + 1), dp[i-1][j-1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }
}