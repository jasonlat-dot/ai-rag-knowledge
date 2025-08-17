package com.jasonlat.http;

import cc.jq1024.middleware.redisson.IRedissonClientService;
import cc.jq1024.middleware.redisson.IRedissonService;
import com.jasonlat.api.IRAGService;
import com.jasonlat.types.models.AnalyzeStatus;
import com.jasonlat.types.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.redisson.api.RList;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
@CrossOrigin("${app.config.cross-origin}")
public class RagController implements IRAGService {

    private final TokenTextSplitter tokenTextSplitter;
    private final PgVectorStore pgVectorStore;
    private final IRedissonClientService redissonClientService;
    private final IRedissonService redissonService;


    @Value("${knowledge.store.path}")
    private String knowledgeStorePath;

    @Override
    @RequestMapping(value = "/query_rag_tag_list", method = {RequestMethod.POST, RequestMethod.GET})
    public Response<List<String>> queryRagTagList() {
        RList<String> ragTagList = redissonClientService.getList("ragTag");

        return Response.ok(ragTagList);
    }

    @Override
    @RequestMapping(value = "/files/upload", method = {RequestMethod.POST}, headers = "Content-Type=multipart/form-data")
    public Response<String> uploadRagFile(@RequestParam("requestId") String requestId, @RequestParam("ragTag") String ragTag, @RequestParam ("files") List<MultipartFile> files) {
        if (!StringUtils.hasLength(requestId)) {
            return Response.error("requestId is empty");
        }
        if (!StringUtils.hasLength(ragTag)) {
            return Response.error("ragTag is empty");
        }
        String key = requestId + "_" + ragTag;
        try {
            redissonService.setValue(key, AnalyzeStatus.ANALYZING.getStatus(), 1, TimeUnit.DAYS);

            log.info("上传 rag 知识库开始： {}", ragTag);
            for (MultipartFile file : files) {
                log.info("上传文件： {}", file.getOriginalFilename());

                // 解析文件
                parseFileToPgVector(file.getResource(), ragTag);

                // 简单的存到 redis, 实际还要存到数据库
                RList<String> ragTagList = redissonClientService.getList("ragTag");
                if (!ragTagList.contains(ragTag)) {
                    ragTagList.add(ragTag);
                }
            }
            log.info("上传 rag 知识库完成： {}", ragTag);

            redissonService.setValue(key, AnalyzeStatus.ANALYZED.getStatus(),1, TimeUnit.DAYS);
            return Response.ok("调用成功");
        } catch (Exception e) {
            log.error("上传文件失败", e);
            redissonService.setValue(key, AnalyzeStatus.ERROR.getStatus(), 1, TimeUnit.DAYS);
            return Response.error();
        }
    }

    @Override
    @RequestMapping(value = "/analyze_git_repository", method = {RequestMethod.POST})
    public Response<String> analyzeGitRepository(@RequestParam("repoUrl") String repoUrl, @RequestParam(value = "username", required = false) String username,
                                                 @RequestParam(value = "token", required = false) String token, @RequestParam(value = "knowledgeName", required = false) String ragTag,
                                                 @RequestParam("requestId") String requestId) {
        String regex = "^(https?://[^/]+/[^/]+/[^/]+)(?:\\.git)?$";
        if (!repoUrl.matches(regex)) {
            throw new IllegalArgumentException("Invalid repoUrl");
        }

        if (!StringUtils.hasLength(requestId)) {
            return Response.error("requestId is empty");
        }

        if (!StringUtils.hasLength(ragTag)) {
            ragTag = getKnowledgeName(repoUrl);
        }
        String key = requestId + "_" + ragTag;
        try {

            redissonService.setValue(key, AnalyzeStatus.ANALYZING.getStatus(), 1, TimeUnit.DAYS);
            // 设置 redis 标志
            log.info("开始克隆-{}-仓库...克隆在本地路径：{}", repoUrl, new File(knowledgeStorePath).getAbsoluteFile());
            // 清空临时路径
            FileUtils.deleteDirectory(new File(knowledgeStorePath));
            // 用try-with-resources, 否则需要手动关闭Git对象 -> git.close();
            try (Git git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(new File(knowledgeStorePath))
                    .setCredentialsProvider(
                            new UsernamePasswordCredentialsProvider(username, token)).call()) {

                log.info("克隆完成-{}, 开始解析...", repoUrl);
                analyzeRepository(ragTag);
            } catch (GitAPIException e) {
                // 处理异常
                log.error("克隆失败-{}", e.getMessage());
                throw new IOException(e);
            }
            // 清空临时路径
            FileUtils.deleteDirectory(new File(knowledgeStorePath));

            // 简单的存到 redis, 实际还要存到数据库
            // todo ragTag 需要做用户隔离
            RList<String> ragTagList = redissonClientService.getList("ragTag");
            if (!ragTagList.contains(ragTag)) {
                ragTagList.add(ragTag);
            }
            log.info("解析并切分向量完成-{} - {}", ragTag, repoUrl);

            redissonService.setValue(key, AnalyzeStatus.ANALYZED.getStatus(),1, TimeUnit.DAYS);
            return Response.ok("解析完成");
        } catch (Exception e) {
            log.error("解析失败", e);
            redissonService.setValue(key, AnalyzeStatus.ERROR.getStatus(), 1, TimeUnit.DAYS);
            return Response.error();
        }
    }

    @RequestMapping(value = "/analyze_git_repository/status", method = {RequestMethod.GET})
    public Response<String> queryAnalyzeStatus(@RequestParam("ragTag") String ragTag, @RequestParam("requestId") String requestId) {

        RList<String> ragTagList = redissonClientService.getList("ragTag");
        if (!ragTagList.contains(ragTag)) {
            return Response.ok(AnalyzeStatus.ERROR.getStatus());
        }
        String analyzeStatus = redissonService.getValue(requestId + "_" + ragTag);
        if (AnalyzeStatus.ERROR.getStatus().equalsIgnoreCase(analyzeStatus)) {
            return Response.ok(AnalyzeStatus.ERROR.getStatus());
        }
        if (AnalyzeStatus.ANALYZING.getStatus().equalsIgnoreCase(analyzeStatus)) {
            return Response.ok(AnalyzeStatus.ANALYZING.getStatus());
        }
        return Response.ok(AnalyzeStatus.ANALYZED.getStatus());
    }

    /**
     * 通过 repoUrl 获取知识库名称
     */
    private String getKnowledgeName(String repoUrl) {
        String[] parts = repoUrl.split("/");
        String projectNameWithGit = parts[parts.length - 1];
        String projectName = projectNameWithGit.replace(".git", "");
        log.info("知识库(代码仓库)名称：{}", projectName);
        return projectName;
    }

    private void analyzeRepository(String repoProjectName) throws IOException {
        Files.walkFileTree(Paths.get(knowledgeStorePath), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                log.info("文件路径：{}", file.toString());

                // 获取文件资源（信息）
                PathResource pathResource = new PathResource(file);
                // 解析文件
                parseFileToPgVector(pathResource, repoProjectName);
                // FileVisitResult.CONTINUE -> 指示文件遍历过程继续进行 。
                return FileVisitResult.CONTINUE;
            }

            // 处理文件访问失败的情况
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                log.error("无法访问文件：{}，错误信息：{}", file.toString(), exc.getMessage(), exc);
                return FileVisitResult.CONTINUE;
            }

            // 进入目录前检查是否为.git目录
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String dirName = dir.getFileName() != null ? dir.getFileName().toString() : "";
                // 忽略.git目录及其所有内容
                if (".git".equals(dirName)) {
                    log.info("忽略.git目录: {}", dir);
                    return FileVisitResult.SKIP_SUBTREE; // 跳过当前目录的所有子内容
                }
                return FileVisitResult.CONTINUE; // 继续遍历其他目录
            }
        });
    }

    /**
     * 解析文件存储到向量库
     */
    private void parseFileToPgVector(Resource resource, String repoProjectName) {
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> documents = reader.get();
        // 切割文件向量
        List<Document> splitDocuments = tokenTextSplitter.apply(documents);
        // 打一个标记
        documents.forEach(doc -> doc.getMetadata().put("knowledge", repoProjectName));
        splitDocuments.forEach(doc -> doc.getMetadata().put("knowledge", repoProjectName));

        // 存储向量
        pgVectorStore.accept(splitDocuments);
    }
}
