package com.joe.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class TestJob extends QuartzJobBean {

    // 必须要有public修饰的无参构造函数
    public TestJob() {
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        System.out.println("执行测试Job。。。"+context.getTrigger().getKey());
    }
}
