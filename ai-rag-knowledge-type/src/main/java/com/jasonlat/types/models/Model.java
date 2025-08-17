package com.jasonlat.types.models;

/**
 * AI模型枚举类
 * 每个枚举常量代表一个特定的模型和参数组合
 */
public enum Model {
    /**
     * DeepSeek R1 模型，参数为 1.5b
     */
    DEEP_SEEK_R1_1_5B("deepseek-r1:1.5b"),

    OPENAI_4_O_MINI("openai-4o-mini"),
    ;

    private final String fullModelIdentifier;

    Model(String fullModelIdentifier) {
        this.fullModelIdentifier = fullModelIdentifier;
    }

    public String getFullModelIdentifier() {
        return fullModelIdentifier;
    }

    /**
     * 获取模型名（冒号前的部分）
     * @return 模型名
     */
    public String getModelName() {
        int colonIndex = fullModelIdentifier.indexOf(':');
        if (colonIndex == -1) {
            return fullModelIdentifier;
        }
        return fullModelIdentifier.substring(0, colonIndex);
    }

    /**
     * 获取参数（冒号后的部分）
     * @return 参数
     */
    public String getParameter() {
        int colonIndex = fullModelIdentifier.indexOf(':');
        if (colonIndex == -1) {
            return "";
        }
        return fullModelIdentifier.substring(colonIndex + 1);
    }

    /**
     * 根据完整模型标识查找对应的枚举值
     * @param fullModelIdentifier 完整模型标识，格式如 "deepseek-r1:1.5b"
     * @return 对应的枚举值，未找到则返回null
     */
    public static Model fromFullModelIdentifier(String fullModelIdentifier) {
        for (Model model : Model.values()) {
            if (model.fullModelIdentifier.equals(fullModelIdentifier)) {
                return model;
            }
        }
        return null;
    }
}
