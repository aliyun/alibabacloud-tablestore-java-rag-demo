package com.aliyun.openservices.tablestore.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RagServiceTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final RagService ragService = new RagService();

    @AfterAll
    static void afterAll() {
        ragService.close();
    }

    @Test
    @Order(1)
    void init() {
        ragService.init();
        ragService.clear();
        ragService.addDocs("我叫小明");
    }

    @Test
    @Order(2)
    void llm() {
        logger.info(ragService.llm("我叫什么？"));
    }

    @Test
    @Order(3)
    void rag() {
        logger.info(ragService.rag("我叫什么？"));
    }

    @Test
    @Order(4)
    void llm_qa() {
        logger.info(ragService.llm("如何设计表的分区键？"));
    }

    @Test
    @Order(5)
    void importData() throws Exception {
        ragService.importPdf("/Users/xunjian/Desktop/阿里云+表格存储+常见问题+20240703.pdf");
    }

    @Test
    @Order(6)
    void rag_qa() {
        logger.info(ragService.rag("如何获取AccessKey？"));
    }
}