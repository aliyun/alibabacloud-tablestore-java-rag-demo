package com.aliyun.openservices.tablestore.service;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.ots.model.v20160620.InsertInstanceRequest;
import com.aliyuncs.ots.model.v20160620.OpenOtsServiceRequest;
import com.aliyuncs.profile.DefaultProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;

public class TablestoreOpenApi implements Closeable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DefaultProfile profile = DefaultProfile.getProfile(
            System.getenv("TABLESTORE_REGION"),
            System.getenv("TABLESTORE_ACCESS_KEY_ID"),
            System.getenv("TABLESTORE_ACCESS_KEY_SECRET"));

    private final IAcsClient client = new DefaultAcsClient(profile);

    /**
     * 开通表格存储服务
     */
    private void openTablestoreService() throws ClientException {
        OpenOtsServiceRequest request = new OpenOtsServiceRequest();
        try {
            client.getAcsResponse(request);
        } catch (ClientException e) {
            if ("ORDER.OPEND".equals(e.getErrCode())) {
                return;
            }
            throw e;
        }
    }


    /**
     * 创建实例
     */
    public void createInstance() throws ClientException {
        openTablestoreService();
        String instanceName = System.getenv("TABLESTORE_INSTANCE_NAME");
        InsertInstanceRequest request = new InsertInstanceRequest();
        request.setInstanceName(instanceName);
        request.setClusterType("SSD");
        client.getAcsResponse(request);
        logger.info("created an instance [{}] in region:[{}]", instanceName, System.getenv("TABLESTORE_REGION"));
    }

    @Override
    public void close() {
        client.shutdown();
    }
}
