package com.joe.service;

import com.github.pagehelper.PageInfo;
import com.joe.common.Result;
import com.joe.entity.QuartzJob;
import org.quartz.JobDataMap;
import org.springframework.scheduling.quartz.QuartzJobBean;

public interface IJobService {

    PageInfo listQuartzJob(String jobName, Integer pageNo, Integer pageSize);

    /**
     * 新增job
     * @param quartz
     * @return
     */
    Result saveJob(QuartzJob quartz,Object o);

    /**
     * 触发job
     * @param jobName
     * @param jobGroup
     * @return
     */
    Result triggerJob(String jobName, String jobGroup);

    /**
     * 暂停job
     * @param jobName
     * @param jobGroup
     * @return
     */
    Result pauseJob(String jobName, String jobGroup);

    /**
     * 恢复job
     * @param jobName
     * @param jobGroup
     * @return
     */
    Result resumeJob(String jobName, String jobGroup);

    /**
     * 移除job
     * @param jobName
     * @param jobGroup
     * @return
     */
    Result removeJob(String jobName, String jobGroup);

    void addJob(Class<? extends QuartzJobBean> jobClass, String jobName, String jobGroupName, int hour, int num, JobDataMap jobDataMap);
}