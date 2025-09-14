package com.boolean_brotherhood.public_transportation_journey_planner.Helpers;

import java.util.*;
import java.util.regex.*;

public class TripParseExample {

    public String mainStop;
    public  List<String> pathStops;

    public static class TripInfo {
        public final String mainStop;
        public  List<String> pathStops;

        public TripInfo(String mainStop, List<String> pathStops) {
            this.mainStop = mainStop;
            this.pathStops = Collections.unmodifiableList(new ArrayList<>(pathStops));
        }

        @Override
        public String toString() {
            return "Main: \"" + mainStop + "\" | Paths: " + pathStops;
        }
    }

    /**
     * Parse a raw trip line and return TripInfo:
     *  - mainStop: the place BEFORE "via" or "(via" if present; otherwise inferred
     *  - pathStops: list of path segments found after "via" (split by "via", "&", "-")
     */
    public static TripInfo parseTripLine(String raw) {
        if (raw == null) return null;

        // normalize whitespace, remove surrounding quotes
        String s = raw.trim().replaceAll("[\"']", "");
        if (s.isEmpty()) return null;

        // Work with original-cased token set, but normalize for matching
        String sNorm = s.replaceAll("\\s+", " ").trim();

        // 1) Check parentheses containing via: e.g. "CAPE TOWN (VIA KROMBOOM)"
        int openParen = indexOfIgnoreCase(sNorm, "(");
        int closeParen = indexOfIgnoreCase(sNorm, ")");

        String main = null;
        String viaContent = null;

        if (openParen >= 0) {
            // try to extract content inside parentheses (if any)
            int start = openParen + 1;
            int end = (closeParen > openParen) ? closeParen : sNorm.length();
            String inside = sNorm.substring(start, end).trim();

            if (containsWordIgnoreCase(inside, "via")) {
                // use text before '(' as main; inside parentheses as via content
                main = sNorm.substring(0, openParen).trim();
                viaContent = inside;
            }
        }

        // 2) If not parentheses-with-via, search first occurrence of " via " (word-boundary)
        if (viaContent == null) {
            Pattern pVia = Pattern.compile("(?i)\\bvia\\b");
            Matcher m = pVia.matcher(sNorm);
            if (m.find()) {
                int viaStart = m.start();
                String before = sNorm.substring(0, viaStart).trim();
                String after = sNorm.substring(m.end()).trim(); // after 'via'

                if (!before.isEmpty()) {
                    main = before;
                    viaContent = after;
                } else {
                    // string starts with "via" (no main before). We'll infer main later.
                    viaContent = after;
                    main = null;
                }
            }
        }

        // 3) If still no viaContent (no "via" present anywhere), then no paths:
        if (viaContent == null) {
            // no via -> treat entire string as main
            String onlyMain = normalizeToken(sNorm);
            return new TripInfo(onlyMain, Collections.emptyList());
        }

        // 4) Now viaContent contains the stuff after "via" (or inside parentheses).
        //    We need to split into path segments. Splitting rules:
        //    - first replace instances of " via " with a marker (so repeated VIAs split)
        //    - then split on marker, ampersand (&), and hyphen (-)
        String tmp = viaContent;

        // normalize whitespace inside via content
        tmp = tmp.replaceAll("\\s+", " ").trim();

        // replace any word 'via' occurrences inside viaContent with a separator
        tmp = tmp.replaceAll("(?i)\\bvia\\b", "|");

        // replace separators & and - (with optional spaces) with the same separator
        tmp = tmp.replaceAll("\\s*&\\s*", "|");
        tmp = tmp.replaceAll("\\s*-\\s*", "|");

        // split into segments and clean
        String[] rawSegments = tmp.split("\\|");
        List<String> segments = new ArrayList<>();
        for (String seg : rawSegments) {
            String t = seg.trim();
            if (!t.isEmpty()) {
                // remove stray parentheses or trailing commas
                t = t.replaceAll("[()]", "").trim();
                if (!t.isEmpty()) segments.add(normalizeToken(t));
            }
        }

        // 5) Determine main if it was not set (i.e., string started with "via")
        List<String> paths = new ArrayList<>();
        if (main == null) {
            if (segments.isEmpty()) {
                // fallback - nothing after via; can't infer main
                main = "";
            } else {
                // assume the LAST segment is the main destination and everything before are path stops
                main = segments.get(segments.size() - 1);
                if (segments.size() > 1) {
                    paths.addAll(segments.subList(0, segments.size() - 1));
                }
            }
        } else {
            // main was set (text before via), all segments are path stops
            paths.addAll(segments);
        }

        // final cleanup: ensure main normalized
        main = normalizeToken(main);

        return new TripInfo(main, paths);
    }

    // Helper: normalize token - uppercase, collapse spaces, trim
    private static String normalizeToken(String token) {
        if (token == null) return "";
        String t = token.trim().replaceAll("\\s+", " ");
        // keep original case? user seems to use upper-case; return trimmed uppercase
        return t.toUpperCase();
    }

    // Helper: case-insensitive indexOf for single char or string
    private static int indexOfIgnoreCase(String s, String sub) {
        if (s == null || sub == null) return -1;
        return s.toLowerCase().indexOf(sub.toLowerCase());
    }

    // Helper: contains whole word 'word' ignoring case
    private static boolean containsWordIgnoreCase(String s, String word) {
        if (s == null || word == null) return false;
        Pattern p = Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE);
        return p.matcher(s).find();
    }


}
