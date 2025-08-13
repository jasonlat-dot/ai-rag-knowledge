package com.jasonlat.api;

import org.springframework.ai.chat.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * ai 问答接口
 */
public interface IAiService {

    /**
     * 非流式生成对话
     * @param model 模型
     * @param message 消息
     * @return 结果
     */
    ChatResponse generate(String model, String message);

    /**
     * 流式生成对话
     * @param model 模型
     * @param message 消息
     * @return 迭代器
     */
    Flux<ChatResponse> generateStream(String model, String message);
}
