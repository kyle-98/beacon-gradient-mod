package com.kronichiwa.utils;

import com.kronichiwa.BeaconGradient;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;

import java.util.*;
import java.util.stream.Collectors;

public class ColorUtils {

    public static final LinkedHashMap<String, String> DYE_HEX;
    public static final LinkedHashMap<String, float[]> PALETTE;

    static {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("white",      "#F9FFFE");
        map.put("orange",     "#F9801D");
        map.put("magenta",    "#C74EBD");
        map.put("light_blue", "#3AB3DA");
        map.put("yellow",     "#FED83D");
        map.put("lime",       "#80C71F");
        map.put("pink",       "#F38BAA");
        map.put("gray",       "#474F52");
        map.put("light_gray", "#9D9D97");
        map.put("cyan",       "#169C9C");
        map.put("purple",     "#8932B8");
        map.put("blue",       "#3C44AA");
        map.put("brown",      "#835432");
        map.put("green",      "#5E7C16");
        map.put("red",        "#B02E26");
        map.put("black",      "#1D1D21");
        DYE_HEX = map;

        LinkedHashMap<String, float[]> pal = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : DYE_HEX.entrySet()) {
            pal.put(e.getKey(), hexToRgb(e.getValue()));
        }
        PALETTE = pal;
    }


    public static float[] hexToRgb(String hexVal) {
        String s = hexVal.startsWith("#") ? hexVal.substring(1) : hexVal;
        int r = Integer.parseInt(s.substring(0, 2), 16);
        int g = Integer.parseInt(s.substring(2, 4), 16);
        int b = Integer.parseInt(s.substring(4, 6), 16);
        return new float[] { r / 255f, g / 255f, b / 255f };
    }


    public static float[] parseColorInput(String color) {
        if (color == null) throw new IllegalArgumentException("color is null");
        String s = color.trim();
        String lower = s.toLowerCase(Locale.ROOT);
        if (PALETTE.containsKey(lower)) {
            return PALETTE.get(lower);
        }
        if ((s.startsWith("#") && (s.length() == 7 || s.length() == 6)) || (s.length() == 6 && s.matches("[0-9A-Fa-f]{6}"))) {
            return hexToRgb(s.startsWith("#") ? s : ("#" + s));
        }
        if (s.contains(",")) {
            String[] parts = s.split(",");
            if (parts.length < 3) throw new IllegalArgumentException("RGB input must have three components");
            float[] vals = new float[3];
            for (int i = 0; i < 3; i++) {
                vals[i] = Float.parseFloat(parts[i].trim());
            }
            float max = Math.max(vals[0], Math.max(vals[1], vals[2]));
            if (max > 1.0f) {
                for (int i = 0; i < 3; i++) vals[i] = vals[i] / 255f;
            }
            return vals;
        }
        // fallback try as hex without '#'
        String maybeHex = s.replaceAll("^#", "");
        if (maybeHex.length() == 6 && maybeHex.matches("[0-9A-Fa-f]{6}")) {
            return hexToRgb("#" + maybeHex);
        }
        throw new IllegalArgumentException("Can't parse color input: " + color);
    }


    public static float[] seqMixToColor(List<float[]> seqRgb) {
        // color = c0; for c in rest: color = (color + c) / 2
        if (seqRgb == null || seqRgb.isEmpty()) return new float[] {0f,0f,0f};
        float[] color = Arrays.copyOf(seqRgb.get(0), 3);
        for (int i = 1; i < seqRgb.size(); i++) {
            float[] c = seqRgb.get(i);
            color[0] = (color[0] + c[0]) / 2f;
            color[1] = (color[1] + c[1]) / 2f;
            color[2] = (color[2] + c[2]) / 2f;
        }
        return color;
    }


    public static double euclidean(float[] a, float[] b) {
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        double dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }


    public static double[] weightsForLength(int glassHeight) {
        double denom = Math.pow(2.0, glassHeight - 1);
        double[] w = new double[glassHeight];
        for (int i = 0; i < glassHeight; i++) {
            if (i == 0) w[i] = 1.0 / denom;
            else w[i] = Math.pow(2.0, i - 1) / denom;
        }
        return w;
    }


    public static List<float[]> gradient(float[] startRgb, float[] endRgb, int numSteps) {
        List<float[]> steps = new ArrayList<>();
        if (numSteps <= 1) {
            steps.add(startRgb);
            return steps;
        }
        for (int i = 0; i < numSteps; i++) {
            float t = (float)i / (numSteps - 1);
            float r = startRgb[0] + (endRgb[0] - startRgb[0]) * t;
            float g = startRgb[1] + (endRgb[1] - startRgb[1]) * t;
            float b = startRgb[2] + (endRgb[2] - startRgb[2]) * t;
            steps.add(new float[] { r, g, b });
        }
        return steps;
    }


    public static class BeamResult {
        public final List<String> sequence; // bottom->top
        public final float[] rgb;
        public final double error;
        public BeamResult(List<String> seq, float[] rgb, double err) {
            this.sequence = seq;
            this.rgb = rgb;
            this.error = err;
        }
    }

    private static class Candidate {
        public final List<String> seqTop; // top->...
        public final double[] assigned;   // weighted sum RGB
        public Candidate(List<String> seqTop, double[] assigned) {
            this.seqTop = seqTop;
            this.assigned = assigned;
        }
    }

    private static class CandidateFull {
        public final List<String> seqTop;
        public final double[] assigned;
        public final double est;
        public CandidateFull(List<String> seqTop, double[] assigned, double est) {
            this.seqTop = seqTop;
            this.assigned = assigned;
            this.est = est;
        }
    }


    public static BeamResult beamSearchBestSeq(float[] targetRgb, Map<String, float[]> palette, int glassHeight, int beamWidth) {
        List<String> names = new ArrayList<>(palette.keySet());
        List<float[]> rgbList = names.stream().map(palette::get).collect(Collectors.toList());

        double[] weights = weightsForLength(glassHeight);
        int[] positions = new int[glassHeight];
        for (int d = 0; d < glassHeight; d++) positions[d] = glassHeight - 1 - d; // top->bottom positions

        List<Candidate> candidates = new ArrayList<>();
        candidates.add(new Candidate(new ArrayList<>(), new double[] {0.0, 0.0, 0.0}));

        for (int depth = 0; depth < glassHeight; depth++) {
            int pos = positions[depth];
            double wpos = weights[pos];
            double wRemAfterTemplate = pos > 0 ? Arrays.stream(Arrays.copyOfRange(weights, 0, pos)).sum() : 0.0;

            List<CandidateFull> newCandidates = new ArrayList<>();
            for (Candidate c : candidates) {
                for (int idx = 0; idx < names.size(); idx++) {
                    String name = names.get(idx);
                    float[] cRgb = rgbList.get(idx);

                    double[] newS = new double[3];
                    newS[0] = c.assigned[0] + wpos * cRgb[0];
                    newS[1] = c.assigned[1] + wpos * cRgb[1];
                    newS[2] = c.assigned[2] + wpos * cRgb[2];

                    double bestEst = Double.POSITIVE_INFINITY;
                    for (float[] pRgb : rgbList) {
                        double[] hypot = new double[] {
                                newS[0] + wRemAfterTemplate * pRgb[0],
                                newS[1] + wRemAfterTemplate * pRgb[1],
                                newS[2] + wRemAfterTemplate * pRgb[2]
                        };
                        double d = Math.sqrt(
                                Math.pow(hypot[0] - targetRgb[0], 2) +
                                Math.pow(hypot[1] - targetRgb[1], 2) +
                                Math.pow(hypot[2] - targetRgb[2], 2)
                        );
                        if (d < bestEst) bestEst = d;
                    }

                    List<String> newSeq = new ArrayList<>(c.seqTop);
                    newSeq.add(name);
                    newCandidates.add(new CandidateFull(newSeq, newS, bestEst));
                }
            }

            // Keep top beamWidth candidates by est
            newCandidates.sort(Comparator.comparingDouble(cf -> cf.est));
            List<Candidate> next = new ArrayList<>();
            int keep = Math.min(beamWidth, newCandidates.size());
            for (int i = 0; i < keep; i++) {
                CandidateFull cf = newCandidates.get(i);
                next.add(new Candidate(cf.seqTop, cf.assigned));
            }
            candidates = next;
        }

        // Evaluate exact mixing for each candidate
        double bestErr = Double.POSITIVE_INFINITY;
        List<String> bestSeq = new ArrayList<>();
        float[] bestRgb = new float[] {0f, 0f, 0f};

        for (Candidate c : candidates) {
            List<String> seqBottom = new ArrayList<>(c.seqTop);
            Collections.reverse(seqBottom);
            List<float[]> seqRgbs = seqBottom.stream().map(palette::get).collect(Collectors.toList());
            float[] mixed = seqMixToColor(seqRgbs);
            double err = euclidean(mixed, targetRgb);
            if (err < bestErr) {
                bestErr = err;
                bestSeq = seqBottom;
                bestRgb = mixed;
            }
        }

        return new BeamResult(bestSeq, bestRgb, bestErr);
    }


    public static class FindResult {
        public final List<String> sequence;
        public final float[] rgb;
        public final double error;
        public final int length;
        public FindResult(List<String> sequence, float[] rgb, double error, int length) {
            this.sequence = sequence;
            this.rgb = rgb;
            this.error = error;
            this.length = length;
        }
    }


    public static FindResult findBestForTarget(float[] targetRgb, Map<String, float[]> palette, int maxStack, int beamWidth) {
        double overallErr = Double.POSITIVE_INFINITY;
        List<String> overallBest = new ArrayList<>();
        float[] overallRgb = new float[] {0f, 0f, 0f};
        int overallLen = 0;
        for (int m = 1; m <= maxStack; m++) {
            BeamResult br = beamSearchBestSeq(targetRgb, palette, m, beamWidth);
            if (br.error < overallErr) {
                overallErr = br.error;
                overallBest = br.sequence;
                overallRgb = br.rgb;
                overallLen = m;
            }
        }
        return new FindResult(overallBest, overallRgb, overallErr, overallLen);
    }


    public static void runAndSendToPlayer(String startInput, String endInput, int beacons, int maxStack, int beamWidth) {
        BeaconGradient.sendToPlayer(Text.literal("beacon_gradient: Computing gradient and best glass stacks..."));

        float[] startRgb = parseColorInput(startInput);
        float[] endRgb = parseColorInput(endInput);

        List<float[]> steps = gradient(startRgb, endRgb, beacons);
        int stepIndex = 1;

        for (float[] target : steps) {
            FindResult fr = findBestForTarget(target, PALETTE, maxStack, beamWidth);

            // Compose step header with ANSI-like hex blocks
            MutableText message = Text.literal(String.format("Step %2d:", stepIndex))
                .append(Text.literal(" \ntarget "))
                .append(BeaconGradient.colorSwatch(target))
                .append(Text.literal("  achieved "))
                .append(BeaconGradient.colorSwatch(fr.rgb))
                .append(Text.literal(String.format("  \nerror=%.4f  stack_len=%d", fr.error, fr.length)));
            BeaconGradient.sendToPlayer(message);

            // Compose bottom->top sequence with colored dye names
            MutableText comboText = Text.literal("");
            for (int i = 0; i < fr.sequence.size(); i++) {
                String dyeName = fr.sequence.get(i);
                float[] rgb = PALETTE.get(dyeName);
                int r = Math.max(0, Math.min(255, Math.round(rgb[0] * 255f)));
                int g = Math.max(0, Math.min(255, Math.round(rgb[1] * 255f)));
                int b = Math.max(0, Math.min(255, Math.round(rgb[2] * 255f)));
                int color = (r << 16) | (g << 8) | b;

                MutableText coloredName = Text.literal(dyeName)
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
                comboText.append(coloredName);

                // Add separator only if not last
                if (i < fr.sequence.size() - 1) {
                    comboText.append(Text.literal(" | "));
                }
            }

            BeaconGradient.sendToPlayer(Text.literal("  Place bottom->top: ").append(comboText));
            stepIndex++;
        }
    }
}