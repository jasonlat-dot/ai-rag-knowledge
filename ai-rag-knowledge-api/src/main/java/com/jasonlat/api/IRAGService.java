package com.jasonlat.api;

import com.jasonlat.types.response.Response;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IRAGService {

    /**
     * 查询 rag 标签列表
     * @return rag 列表
     */
    Response<List<String>> queryRagTagList();

    Response<String> uploadRagFile(String ragTag, List<MultipartFile> files);
}
