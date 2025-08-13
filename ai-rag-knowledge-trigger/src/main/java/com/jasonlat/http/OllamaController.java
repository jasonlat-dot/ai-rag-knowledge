package com.jasonlat.http;

import com.jasonlat.api.IAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ollama")
@RequiredArgsConstructor
@CrossOrigin("${app.config.cross-origin}")
public class OllamaController implements IAiService {

    private final OllamaChatClient ollamaChatClient;

    /**
     * 非流式生成对话
     * <a href="http://localhost:8087/api/v1/ollama/generate?model=deepseek-r1:1.5b&message=hi">...</a>
     * @param model 模型
     * @param message 消息
     * @return ChatResponse 对话响应结果
     */
    @RequestMapping(value = "/generate", method = {RequestMethod.POST, RequestMethod.GET})
    public ChatResponse generate(@RequestParam("model") String model, @RequestParam("message") String message) {
        // call: 非流式
        return ollamaChatClient.call(new Prompt(message, OllamaOptions.create().withModel(model)));
    }


    /**
     * 流式生成对话
     * <a href="http://localhost:8087/api/v1/ollama/generate_stream?model=deepseek-r1:1.5b&message=hi">...</a>
     * @param model 模型
     * @param message 消息
     * @return ChatResponse 对话响应结果
     */
    @RequestMapping(value = "/generate_stream", method = {RequestMethod.POST, RequestMethod.GET})
    public Flux<ChatResponse> generateStream(@RequestParam("model")String model, @RequestParam("message") String message) {
        // stream: 流式
        return ollamaChatClient.stream(new Prompt(message, OllamaOptions.create().withModel(model)));
    }
}
