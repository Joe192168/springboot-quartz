package com.joe.job;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.List;
import java.util.Map;

public class TestJob extends QuartzJobBean {

    // 必须要有public修饰的无参构造函数
    public TestJob() {
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        System.out.println("执行测试Job。。。"+context.getTrigger().getKey()+",参数："+dataMap.get("data"));
    }
}
