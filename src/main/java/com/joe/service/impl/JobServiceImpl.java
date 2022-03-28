package com.joe.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.joe.common.Result;
import com.joe.entity.QuartzJob;
import com.joe.mapper.JobMapper;
import com.joe.service.IJobService;
import org.apache.commons.lang.StringUtils;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class JobServiceImpl implements IJobService {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private JobMapper jobMapper;

    @Override
    public PageInfo listQuartzJob(String jobName, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<QuartzJob> jobList = jobMapper.listJob(jobName);
        PageInfo pageInfo = new PageInfo(jobList);
        return pageInfo;
    }

    /**
     * 新增/修改任务
     * @param quartz
     * @return
     */
    @Override
    public Result saveJob(QuartzJob quartz,Object obj){
        try {
            JobKey jobKey = new JobKey(quartz.getJobName(), quartz.getJobGroup());
            //如果是修改  展示旧的 任务
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
            //构建job信息
            //构建job信息
            Class cls = null;
            //第一种方式：从页面传入值 如：包+类 包名获取class对象
            if (StringUtils.isNotEmpty(quartz.getJobClassName())){
                cls = Class.forName(quartz.getJobClassName());
            }
            //第二种方式：直接使用obj对象获取class
            if (cls==null&&obj!=null){
                cls = obj.getClass();
            }
            cls.newInstance();
            JobDetail job = JobBuilder.newJob(cls).withIdentity(quartz.getJobName(),
                    quartz.getJobGroup())
                    .withDescription(quartz.getDescription()).build();
            //添加参数
            job.getJobDataMap().putAll(quartz.getJobDataMap());
            // 触发时间点
            CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(quartz.getCronExpression().trim());
            cronScheduleBuilder.withMisfireHandlingInstructionDoNothing();
            Trigger trigger = TriggerBuilder.newTrigger().withIdentity("trigger"+quartz.getJobName(), quartz.getJobGroup())
                    .startNow().withPriority(quartz.getPriority()).withSchedule(cronScheduleBuilder).build();
            //交由Scheduler安排触发
            scheduler.scheduleJob(job, trigger);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error();
        }
        return Result.ok();
    }

    /**
     * 增加一个job
     *
     * @param jobClass 任务实现类
     * @param jobName 任务名称
     * @param jobGroupName 任务组名
     * @param hour 时间 (这是每隔多少小时为一次任务)
     * @param num 运行的次数 （<0:表示不限次数）
     * @param jobDataMap 参数
     */
    @Override
    public void addJob(Class<? extends QuartzJobBean> jobClass, String jobName, String jobGroupName, int hour, int num, JobDataMap jobDataMap) {
        try {
            JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(jobName, jobGroupName)// 任务名称和组构成任务key
                    .build();
            //在此处添加需要用的参数即可
            jobDetail.getJobDataMap().putAll(jobDataMap);
            // 使用simpleTrigger规则
            Trigger trigger = null;
            if (num < 0) {
                trigger = TriggerBuilder.newTrigger().withIdentity(jobName, jobGroupName)
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(hour))
                        .startNow().build();
            } else {
                trigger = TriggerBuilder
                        .newTrigger().withIdentity(jobName, jobGroupName).withSchedule(SimpleScheduleBuilder
                                .simpleSchedule().withIntervalInMinutes(hour).withRepeatCount(num))
                        .startNow().build();
            }
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            e.getStackTrace();
        }
    }

    /**
     * 触发任务
     * @param jobName
     * @param jobGroup
     * @return
     */
    @Override
    public Result triggerJob(String jobName, String jobGroup) {
        JobKey key = new JobKey(jobName,jobGroup);
        try {
            scheduler.triggerJob(key);
        } catch (SchedulerException e) {
            e.printStackTrace();
            return Result.error();
        }
        return Result.ok();
    }

    /**
     * 停止任务
     * @param jobName
     * @param jobGroup
     * @return
     */
    @Override
    public Result pauseJob(String jobName, String jobGroup) {
        JobKey key = new JobKey(jobName,jobGroup);
        try {
            scheduler.pauseJob(key);
        } catch (SchedulerException e) {
            e.printStackTrace();
            return Result.error();
        }
        return Result.ok();
    }

    /**
     * 恢复任务
     * @param jobName
     * @param jobGroup
     * @return
     */
    @Override
    public Result resumeJob(String jobName, String jobGroup) {
        JobKey key = new JobKey(jobName,jobGroup);
        try {
            scheduler.resumeJob(key);
        } catch (SchedulerException e) {
            e.printStackTrace();
            return Result.error();
        }
        return Result.ok();
    }

    /**
     * 移除任务
     * @param jobName
     * @param jobGroup
     * @return
     */
    @Override
    public Result removeJob(String jobName, String jobGroup) {
        try {
            JobKey jobKey = new JobKey(jobName,jobGroup);
            if(scheduler.checkExists(jobKey)){
                // 删除任务
                boolean b = scheduler.deleteJob(jobKey);
                System.out.println("removeJob:"+jobName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error();
        }
        return Result.ok();
    }
}