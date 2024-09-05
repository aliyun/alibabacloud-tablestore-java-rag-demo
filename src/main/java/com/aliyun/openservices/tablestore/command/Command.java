package com.aliyun.openservices.tablestore.command;

import com.aliyun.openservices.tablestore.service.RagService;
import com.aliyun.openservices.tablestore.service.TablestoreOpenApi;
import com.aliyuncs.exceptions.ClientException;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.standard.commands.Quit;

@ShellComponent
public class Command implements Quit.Command {

    private final RagService ragService = new RagService();
    private final TablestoreOpenApi openApi = new TablestoreOpenApi();

    @ShellMethod(group = "初始化", key = "create-instance", value = "开通表格存储服务，并使用环境变量'TABLESTORE_INSTANCE_NAME'为实例名创建实例")
    public void createInstance() throws ClientException {
        openApi.createInstance();
    }
    
    @ShellMethod(group = "初始化", key = "init", value = "初始化表和索引")
    public void init() {
        ragService.init();
    }

    @ShellMethod(group = "初始化", key = "clear-docs", value = "清空所有的Embedding数据")
    public void clear() {
        ragService.clear();
    }

    @ShellMethod(group = "数据管理", key = "add", value = "增加一条文本数据")
    public void add(String text) {
        ragService.addDocs(text);
    }

    @ShellMethod(group = "问答", key = "llm", value = "直接使用通义千问大模型进行问答")
    public String llm(String text) {
        return ragService.llm(text);
    }

    @ShellMethod(group = "问答", key = "rag", value = "使用表格存储和通义千问大模型一起进行问答")
    public String rag(String text) {
        return ragService.rag(text);
    }

    @ShellMethod(group = "数据管理", key = "import-pdf", value = "导入PDF文档数据")
    public void importPdf(String file) throws Exception {
        ragService.importPdf(file);
    }

    @ShellMethod(group = "数据管理", key = "import-text", value = "导入文本文档数据")
    public void importText(String file) throws Exception {
        ragService.importText(file);
    }

    @ShellMethod(value = "Exit the shell.", key = {"quit", "q", "exit", "terminate"}, group = "Built-In Commands")
    public void quit() {
        ragService.close();
        openApi.close();
        System.out.println("Good Bye!");
        System.exit(0);
    }

}
