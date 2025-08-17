package com.jasonlat.http;

import com.jasonlat.api.IAiService;
import com.jasonlat.types.response.Response;
import com.jasonlat.types.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jasonlat.types.models.Model.OPENAI_4_O_MINI;


@Slf4j
@RestController
@RequestMapping("/openai")
@RequiredArgsConstructor
@CrossOrigin("${app.config.cross-origin}")
public class OpenAiController implements IAiService {

    private final OpenAiChatClient chatClient;
    private final PgVectorStore pgVectorStore;

    @Value("${spring.ai.openai.enabled}")
    private boolean enabled;

    @Override
    @RequestMapping(value = "/generate", method = {RequestMethod.POST, RequestMethod.GET})
    public ChatResponse generate(@RequestParam String model, @RequestParam String message) {
        try {
            if (!enabled) {
                return Response.errorCall(ResponseCode.MODEL_NOT_SUPPORT);
            }
            return chatClient.call(new Prompt(
                    message,
                    OpenAiChatOptions.builder()
                            .withModel(model)
                            .build()
            ));
        } catch (Exception e) {
            log.error("服务器异常: {} ->", e.getMessage(), e);
            return Response.errorCall(ResponseCode.SYSTEM_B0001);
        }
    }

    /**
     * curl <a href="http://localhost:8087/api/v1/openai/generate_stream?model=gpt-4o&message=1+1">...</a>
     */
    @Override
    @RequestMapping(value = "/generate_stream", method = {RequestMethod.POST, RequestMethod.GET})
    public Flux<ChatResponse> generateStream(@RequestParam String model, @RequestParam String message) {
        try {
            if (!enabled) {
                return Response.errorFlux(ResponseCode.MODEL_NOT_SUPPORT);
            }
            return chatClient.stream(new Prompt(
                    message, OpenAiChatOptions.builder().withModel(model).build()))
                    .onErrorResume(e -> {  // 处理流中发生的异常
                        log.error("流式处理异常: {}", e.getMessage(), e);
                        return Response.errorFlux(ResponseCode.SYSTEM_B0001);
                    });
        } catch (Exception e) {
            log.error("服务器异常: {} ->", e.getMessage(), e);
            return Response.errorFlux(ResponseCode.SYSTEM_B0001);
        }
    }

    @Override
    @RequestMapping(value = "/generate_stream_rag", method = {RequestMethod.POST, RequestMethod.GET})
    public Flux<ChatResponse> generateStreamRag(@RequestParam("model")String model, @RequestParam("ragTag") String ragTag,@RequestParam("message") String message) {
        try {
            // 模型
            if (!StringUtils.hasLength(model)) model = OPENAI_4_O_MINI.getFullModelIdentifier().trim();

            // 检索知识库                                                                                               # 注意是： knowledge == '{ragTag}'
            SearchRequest searchRequest = SearchRequest.query(message).withTopK(5).withFilterExpression("knowledge == '" + ragTag + "'");
            // 从知识库获取文档
            List<Document> documents = pgVectorStore.similaritySearch(searchRequest);
            // 解析文档，获取信息
            String documentsText = documents.stream().map(Document::getContent).collect(Collectors.joining());
            // 构建系统提示
            String SYSTEM_PROMPT = """
                    Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
                    If unsure, simply state that you don't know.
                    Another thing you need to note is that your reply must be in Chinese!
                    DOCUMENTS:
                        {documents}
                    """;

            Message ragMessage = new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", documentsText));
            ArrayList<Message> messages = new ArrayList<>();
            messages.add(new UserMessage(message));
            messages.add(ragMessage);

            // 返回结果
            return chatClient.stream(new Prompt(messages, OpenAiChatOptions.builder().withModel(model).build()))
                    .onErrorResume(e -> {  // 处理流中发生的异常
                        log.error("流式处理异常: {}", e.getMessage(), e);
                        return Response.errorFlux(ResponseCode.SYSTEM_B0001);
                    });
        } catch (Exception e) {
            log.error("服务器异常: {} ->", e.getMessage(), e);
            return Response.errorFlux(ResponseCode.SYSTEM_B0001);
        }
    }

}
