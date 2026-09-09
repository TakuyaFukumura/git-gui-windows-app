package com.example.gitguiwindowsapp.config;

/**
 * ウィンドウ状態の設定カテゴリです。
 */
public record WindowSettings(double width, double height, double x, double y) {
    public WindowSettings {
        if (!Double.isFinite(width) || width <= 0
                || !Double.isFinite(height) || height <= 0) {
            throw new IllegalArgumentException("Window size must be finite and positive.");
        }
        if (Double.isInfinite(x) || Double.isInfinite(y)) {
            throw new IllegalArgumentException("Window position must be finite or NaN.");
        }
    }
}
