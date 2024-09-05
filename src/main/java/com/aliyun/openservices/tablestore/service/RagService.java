package com.aliyun.openservices.tablestore.service;

import com.alicloud.openservices.tablestore.SyncClient;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.HuggingFaceTokenizer;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.tablestore.TablestoreEmbeddingStore;
import dev.langchain4j.store.memory.chat.tablestore.TablestoreChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RagService implements Closeable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    interface Friend {

        String chat(String userMessage);
    }

    private final SyncClient syncClient;
    private final TablestoreEmbeddingStore embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final Friend friend;
    private final ChatLanguageModel llm;
    private final TablestoreChatMemoryStore chatMemoryStore;
    private final DocumentSplitter splitter;


    public RagService() {
        this.syncClient = new SyncClient(System.getenv("TABLESTORE_ENDPOINT"),
                System.getenv("TABLESTORE_ACCESS_KEY_ID"),
                System.getenv("TABLESTORE_ACCESS_KEY_SECRET"),
                System.getenv("TABLESTORE_INSTANCE_NAME"));
        this.embeddingStore = new TablestoreEmbeddingStore(syncClient, 384);
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        this.splitter = DocumentSplitters.recursive(768, 0, new HuggingFaceTokenizer());

        this.chatMemoryStore = new TablestoreChatMemoryStore(syncClient);
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryStore(chatMemoryStore)
                .maxMessages(20)
                .id(UUID.randomUUID())
                .build();
        this.llm = QwenChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .modelName("qwen-plus") // https://help.aliyun.com/zh/dashscope/developer-reference/model-introduction
                .build();
        EmbeddingStoreContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(20)
                .minScore(0.5)
                .build();

        this.friend = AiServices.builder(Friend.class)
                .chatLanguageModel(llm)
                .chatMemory(chatMemory)
                .contentRetriever(retriever)
                .build();
    }

    /**
     * 初始化以来的表和索引
     */
    public void init() {
        chatMemoryStore.init();
        embeddingStore.init();
    }

    /**
     * 给向量数据库中添加一行文本
     */
    public void addDocs(String text) {
        Embedding embedding = embeddingModel.embed(text).content();
        embeddingStore.add(embedding, new TextSegment(text, new Metadata()));
    }

    /**
     * 使用表格存储和通义千问大模型一起进行问答
     */
    public String rag(String input) {
        return convertMarkdownToShell(friend.chat(input));
    }

    /**
     * 直接使用通义千问大模型进行问答
     */
    public String llm(String input) {
        return convertMarkdownToShell(llm.generate(input));
    }

    /**
     * 导入PDF文档数据
     */
    public void importPdf(String fileName) throws Exception {
        DocumentParser parser = new ApachePdfBoxDocumentParser();
        splitAndSave(fileName, parser);
    }

    /**
     * 导入文本文档数据
     */
    public void importText(String fileName) throws Exception {
        DocumentParser parser = new TextDocumentParser();
        splitAndSave(fileName, parser);
    }

    /**
     * 切分1个文档为多个子片段
     */
    private void splitAndSave(String fileName, DocumentParser parser) throws Exception {
        InputStream inputStream = new FileInputStream(fileName);
        Document document = parser.parse(inputStream);
        List<TextSegment> segments = splitter.split(document);
        logger.info("file:{} text length is:{} and split it to segments:{}", fileName, document.text().length(), segments.size());
        List<Embedding> embeddings = new ArrayList<>(segments.size());
        for (TextSegment segment : segments) {
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddings.add(embedding);
        }
        embeddingStore.addAll(embeddings, segments);
        logger.info("{} segments added to embedding store", segments.size());
    }

    /**
     * 清空向量数据库的文档数据
     */
    public void clear() {
        embeddingStore.removeAll();
    }


    @Override
    public void close() {
        this.syncClient.shutdown();
    }

    private static String convertMarkdownToShell(String markdown) {
        return markdown.replace("**", "");
    }
}
