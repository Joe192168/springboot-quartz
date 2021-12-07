package com.joe.job;

import com.joe.entity.TestEntity;
import com.joe.util.RestTemplateUtil;
import lombok.Data;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Data
public class MyJob extends QuartzJobBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(MyJob.class);

    // private boolean httpRequest = false;                        // 布尔类型的变量，不要加is前缀
    private int restConnectionTimeout = 10000;
    private int restReadTimeout = 10000;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        String url = "http://localhost:8080/quartz/rest/getEntity";

        TestEntity entity = RestTemplateUtil.getRequest(url, TestEntity.class);
        LOGGER.info("entity = {}", entity);
        System.out.println("MyJob...");
    }
}
