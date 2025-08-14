package com.jasonlat.http;

import cc.jq1024.middleware.redisson.IRedissonClientService;
import com.jasonlat.api.IRAGService;
import com.jasonlat.types.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
@CrossOrigin("${app.config.cross-origin}")
public class RagController implements IRAGService {

    private final TokenTextSplitter tokenTextSplitter;
    private final PgVectorStore pgVectorStore;
    private final IRedissonClientService redissonClientService;

    @Override
    @RequestMapping(value = "/query_rag_tag_list", method = {RequestMethod.POST, RequestMethod.GET})
    public Response<List<String>> queryRagTagList() {
        RList<String> ragTagList = redissonClientService.getList("ragTag");

        return Response.ok(ragTagList);
    }

    @Override
    @RequestMapping(value = "rag_file/upload", method = {RequestMethod.POST}, headers = "Content-Type=multipart/form-data")
    public Response<String> uploadRagFile(@RequestParam("ragTag") String ragTag, @RequestParam ("files") List<MultipartFile> files) {
        try {
            log.info("上传 rag 知识库开始： {}", ragTag);
            for (MultipartFile file : files) {
                log.info("上传文件： {}", file.getOriginalFilename());
                // 读取文件信息
                TikaDocumentReader reader = new TikaDocumentReader(file.getResource());
                List<Document> documents = reader.get();
                // 拆分-切割
                List<Document> documentSplitterList = tokenTextSplitter.apply(documents);
                // 打一个标记
                documents.forEach(doc -> doc.getMetadata().put("knowledge", ragTag));
                documentSplitterList.forEach(doc -> doc.getMetadata().put("knowledge", ragTag));
                // 存储到向量库
                pgVectorStore.accept(documentSplitterList);

                // 简单的存到 redis, 实际还要存到数据库
                RList<String> ragTagList = redissonClientService.getList("ragTag");
                if (!ragTagList.contains(ragTag)) {
                    ragTagList.add(ragTag);
                }
            }
            log.info("上传 rag 知识库完成： {}", ragTag);

            return Response.ok("调用成功");
        } catch (Exception e) {
            log.error("上传文件失败", e);
            return Response.error();
        }
    }
}
