package com.jasonlat.api;

import com.jasonlat.types.response.Response;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IRAGService {

    /**
     * 查询 rag 标签列表
     * @return rag 列表
     */
    Response<List<String>> queryRagTagList();

    Response<String> uploadRagFile(String ragTag, List<MultipartFile> files);

    /**
     * 分析 git 仓库
     * @param repoUrl git 仓库地址
     * @param username git 仓库用户名
     * @param token git 仓库密码或 token
     * @return 分析结果
     */
    Response<String> analyzeGitRepository(String repoUrl, String username, String token) throws IOException;
}
