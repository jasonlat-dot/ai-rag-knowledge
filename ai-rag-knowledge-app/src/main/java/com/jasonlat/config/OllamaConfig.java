package com.jasonlat.config;

import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.OllamaEmbeddingClient;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

@Configuration
public class OllamaConfig {

    private final String defaultModel = "nomic-embed-text";

    /**
     * 创建一个OllamaApi对象，用于与ollama模型进行交互
     * @param baseUrl ollama模型服务地址
     * @return OllamaApi对象
     */
    @Bean
    public OllamaApi ollamaApi(@Value("${spring.ai.ollama.base-url}") String baseUrl) {
        return new OllamaApi(baseUrl);
    }

    /**
     * 创建一个OllamaChatClient对象，用于与ollama模型进行交互
     * @param ollamaApi ollamaApi对象
     * @return OllamaChatClient对象
     */
    @Bean
    public OllamaChatClient ollamaChatClient(OllamaApi ollamaApi) {
        return new OllamaChatClient(ollamaApi);
    }

    /**
     * 创建一个TokenTextSplitter对象，用于将文本分割成多个 smaller chunks
     * @return TokenTextSplitter对象
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }

    /**
     * 创建一个简易版的 SimpleVectorStore 对象，用于存储向量数据
     * @return SimpleVectorStore对象 (不带存储库) 本地缓存
     */
    @Bean
    public SimpleVectorStore simpleVectorStore(OllamaApi ollamaApi, @Value("${spring.ai.ollama.embedding.model}") String model) {
        OllamaEmbeddingClient embeddingClient = new OllamaEmbeddingClient(ollamaApi);
        // 设置默认的优化文本嵌入任务的模型
        if (!StringUtils.hasLength( model)) model = defaultModel;
        embeddingClient.withDefaultOptions(OllamaOptions.create().withModel(model));
        return new SimpleVectorStore(embeddingClient);
    }

    /**
     * 创建一个 PostgreSQL 版本的 PgVectorStore 对象，用于存储向量数据
     * @param ollamaApi ollamaApi对象
     * @param model 模型名称
     * @return PgVectorStore对象 （带存储库）
     */
    @Bean
    public PgVectorStore pgVectorStore(OllamaApi ollamaApi, JdbcTemplate jdbcTemplate,
                                       @Value("${spring.ai.ollama.embedding.model}") String model) {
        OllamaEmbeddingClient embeddingClient = new OllamaEmbeddingClient(ollamaApi);
        // 默认的优化文本嵌入任务的模型
        if (!StringUtils.hasLength( model)) model = defaultModel;
        embeddingClient.withDefaultOptions(OllamaOptions.create().withModel(model));
        // （带存储库）
        return new PgVectorStore(jdbcTemplate, embeddingClient);
     }
}
