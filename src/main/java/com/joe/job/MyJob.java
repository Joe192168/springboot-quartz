package com.joe.job;

import com.joe.entity.TestEntity;
import com.joe.util.RestTemplateUtil;
import lombok.Data;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        List<Map<String, Object>> jobParam = (List<Map<String, Object>>) dataMap.get("jobParam");
        if (jobParam!=null){
            //整体打印
            jobParam.stream().collect(Collectors.toList());
            jobParam.forEach(System.out::println);
            //精准打印
            jobParam.stream().forEach(m->{
                System.out.println(m.get("paramName"));
                System.out.println(m.get("paramValue"));
            });
        }
        System.out.println("MyJob...");
    }
}
