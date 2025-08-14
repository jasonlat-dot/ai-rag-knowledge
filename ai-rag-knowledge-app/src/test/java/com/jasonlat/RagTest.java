package com.jasonlat;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jasonlat.types.models.Model.DEEP_SEEK_R1_1_5B;

/**
 * Unit test for simple App.
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class RagTest extends TestCase {

    @Resource
    private OllamaChatClient ollamaChatClient;
    @Resource
    private TokenTextSplitter tokenTextSplitter;
    @Resource
    private PgVectorStore pgVectorStore;


    @Test
    public void upload() {
        // 读取文件
        TikaDocumentReader reader = new TikaDocumentReader("./data/file.txt");
        List<Document> documents = reader.get();
        // 切割文件向量
        List<Document> splitDocuments = tokenTextSplitter.apply(documents);
        // 打一个标记
        documents.forEach(doc -> doc.getMetadata().put("knowledge", "knowledgeName"));
        splitDocuments.forEach(doc -> doc.getMetadata().put("knowledge", "knowledgeName"));

        // 存储向量
        pgVectorStore.accept(splitDocuments);

        log.info("向量存储完成");
    }

    @Test
    public void chat() {
        String question = "请帮我总结一下";
        String message = "jasonlat 哪一年出生的";

        // 系统提示词
        String SYSTEM_PROMPT = """
                Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
                If unsure, simply state that you don't know.
                Another thing you need to note is that your reply must be in Chinese!
                DOCUMENTS:
                    {documents}
                """;

        // 检索知识库                                                                                               # 注意是： knowledge == 'knowledgeName'
        SearchRequest searchRequest = SearchRequest.query(message).withTopK(5).withFilterExpression("knowledge == 'knowledgeName'");
        // 从知识库获取文档
        List<Document> documents = pgVectorStore.similaritySearch(searchRequest);
        // 解析文档，获取信息
        String documentsText = documents.stream().map(Document::getContent).collect(Collectors.joining());
        // 构建系统提示
        Message ragMessage = new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", documentsText));
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(message));
        messages.add(ragMessage);

        ChatResponse chatResponse = ollamaChatClient.call(new Prompt(messages, OllamaOptions.create().withModel(DEEP_SEEK_R1_1_5B.getFullModelIdentifier())));
        log.info("测试结果：{}", JSON.toJSONString(chatResponse));
    }

}
