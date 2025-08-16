package com.jasonlat;

import com.jasonlat.types.utils.JasyptEncryptUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.PathResource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class JGitTest {

    @Resource
    private OllamaChatClient ollamaChatClient;
    @Resource
    private TokenTextSplitter tokenTextSplitter;
    @Resource
    private PgVectorStore pgVectorStore;

    private static final String GIT_URL = "https://github.com/jasonlat-dot/ecc-encrypt-springboot-starter.git";
    private static final String GIT_USER = "jasonlat-dot";
    private static final String LOCAL_PATH = "./cloned-repo";

    /** 此处是加密后的，自己使用时请注意 */
    private static final String GIT_TOKEN = "YkW68iGYTcRqcC+zoB0/bUT7MdfLsxArF/i0LTjpqoB+eWbbuQMUeNKekKQ4iqdyOLPc2Ko2WuO1hhqui/KPXSDCAGD676eAAMORbAKlcqvsGm4GyW2ShfsqQSXPcrjb6AzP+1HgU/U=";


    @Test
    public void test() throws Exception {
        // 临时路径
        log.info("开始克隆仓库...克隆路径：{}", new File(LOCAL_PATH).getAbsoluteFile());
        // 清空临时路径
        FileUtils.deleteDirectory(new File(LOCAL_PATH));
        // 用try-with-resources, 否则需要手动关闭Git对象 -> git.close();
        try (Git git = Git.cloneRepository()
                .setURI(GIT_URL)
                .setDirectory(new File(LOCAL_PATH))
                .setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider(GIT_USER, JasyptEncryptUtil.decrypt(GIT_TOKEN)))
                .call()) {
            // 这里可以添加对克隆下来的仓库进行操作的代码
            // 例如获取仓库信息、创建分支等
            System.out.println("仓库克隆成功：" + LOCAL_PATH);
        } catch (GitAPIException e) {
            // 处理异常
            e.printStackTrace();
        }
    }

    @Test
    public void test_file() throws Exception {
        /*
         * SimpleFileVisitor 的匿名内部类中重写需要的方法，来实现对文件树的遍历操作。常用的方法包括：
         * preVisitDirectory：访问目录前调用
         * visitFile：访问文件时调用
         * visitFileFailed：文件访问失败时调用
         * postVisitDirectory：访问目录后调用
         */
        Files.walkFileTree(Paths.get(LOCAL_PATH), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                log.info("文件路径：{}", file.toString());

                // 获取文件资源（信息）
                PathResource pathResource = new PathResource(file);
                // 解析文件
                parseFile(pathResource);

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

    private void parseFile(PathResource resource) {
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> documents = reader.get();
        // 切割文件向量
        List<Document> splitDocuments = tokenTextSplitter.apply(documents);
        // 打一个标记
        documents.forEach(doc -> doc.getMetadata().put("knowledge", "ecc-encrypt-springboot-starter"));
        splitDocuments.forEach(doc -> doc.getMetadata().put("knowledge", "ecc-encrypt-springboot-starter"));

        // 存储向量
        pgVectorStore.accept(splitDocuments);
    }

}
