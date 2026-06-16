package com.gamer.data.mpcserver.commands.image;

/**
 * MiMo 图片识别配置（由启动参数注入）。
 *
 * @author liuyunhui
 * @date 2026-06-12
 */
public final class ImageConfig {

    private static String mimoApiKey;
    private static String mimoBaseUrl = "https://token-plan-cn.xiaomimimo.com/anthropic";
    private static String mimoModel = "mimo-v2.5";

    private ImageConfig() {}

    public static String getMimoApiKey() {
        return mimoApiKey;
    }

    public static void setMimoApiKey(String mimoApiKey) {
        ImageConfig.mimoApiKey = mimoApiKey;
    }

    public static String getMimoBaseUrl() {
        return mimoBaseUrl;
    }

    public static void setMimoBaseUrl(String mimoBaseUrl) {
        if (mimoBaseUrl != null && !mimoBaseUrl.trim().isEmpty()) {
            ImageConfig.mimoBaseUrl = mimoBaseUrl.trim();
        }
    }

    public static String getMimoModel() {
        return mimoModel;
    }

    public static void setMimoModel(String mimoModel) {
        if (mimoModel != null && !mimoModel.trim().isEmpty()) {
            ImageConfig.mimoModel = mimoModel.trim();
        }
    }
}
