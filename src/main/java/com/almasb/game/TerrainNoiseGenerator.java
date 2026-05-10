package com.almasb.game;

import org.apache.commons.math3.util.FastMath;

/**
 * Seeded 1D Perlin + fBm terrain noise with LCG-based deterministic randomness.
 */
public final class TerrainNoiseGenerator {

    private static final long LCG_A = 6364136223846793005L;
    private static final long LCG_C = 1442695040888963407L;

    private final long seed;

    public TerrainNoiseGenerator(long seed) {
        this.seed = seed;
    }

    public int surfaceY(int x, int baseSurfaceY, int amplitude) {
        double continental = fbm(x * 0.045, 5, 2.0, 0.5);
        double detail = fbm((x + 913.0) * 0.09, 3, 2.15, 0.55);
        double ridged = 1.0 - FastMath.abs(detail);

        double combined = continental * 0.85 + ridged * 0.15;
        int y = baseSurfaceY + (int) FastMath.round(combined * amplitude);
        return y;
    }

    public double fbm(double x, int octaves, double lacunarity, double gain) {
        double frequency = 1.0;
        double amplitude = 1.0;
        double sum = 0.0;
        double amplitudeSum = 0.0;

        for (int i = 0; i < octaves; i++) {
            sum += perlin1D(x * frequency) * amplitude;
            amplitudeSum += amplitude;

            frequency *= lacunarity;
            amplitude *= gain;
        }

        if (amplitudeSum == 0.0) {
            return 0.0;
        }

        return clamp(sum / amplitudeSum, -1.0, 1.0);
    }

    public double coordinateRandom01(int x, int y, int salt) {
        long state = seed;
        state ^= ((long) x * 0x9E3779B97F4A7C15L);
        state ^= ((long) y * 0xC2B2AE3D27D4EB4FL);
        state ^= ((long) salt * 0x165667B19E3779F9L);
        state = lcg(state);
        return toUnit01(state);
    }

    private double perlin1D(double x) {
        int x0 = (int) FastMath.floor(x);
        int x1 = x0 + 1;

        double t = x - x0;
        double g0 = gradient(x0);
        double g1 = gradient(x1);

        double n0 = g0 * t;
        double n1 = g1 * (t - 1.0);

        double u = fade(t);
        return lerp(n0, n1, u) * 2.0;
    }

    private double gradient(int latticeX) {
        long state = seed ^ ((long) latticeX * 0x9E3779B97F4A7C15L);
        state = lcg(state);
        return toUnitSigned(state);
    }

    private long lcg(long state) {
        return state * LCG_A + LCG_C;
    }

    private double toUnitSigned(long state) {
        return toUnit01(state) * 2.0 - 1.0;
    }

    private double toUnit01(long state) {
        long bits = (state >>> 11) & ((1L << 53) - 1);
        return bits * (1.0 / (1L << 53));
    }

    private double fade(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    private double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
