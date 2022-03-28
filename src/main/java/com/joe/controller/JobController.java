package com.joe.controller;

import com.github.pagehelper.PageInfo;
import com.joe.common.Result;
import com.joe.entity.QuartzJob;
import com.joe.job.TestJob;
import com.joe.service.IJobService;
import org.quartz.JobDataMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/job")
public class JobController {
    private final static Logger LOGGER = LoggerFactory.getLogger(JobController.class);

    @Autowired
    private IJobService jobService;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @PostMapping("/add")
    public Result save(@RequestBody QuartzJob quartz){
        LOGGER.info("新增/修改任务");
        TestJob testJob = new TestJob();
        Result result = jobService.saveJob(quartz,testJob);
        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @PostMapping("/add2")
    public Result add(@RequestBody QuartzJob quartz){
        LOGGER.info("新增/修改任务");
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("key1","测试001");
        jobService.addJob(TestJob.class,quartz.getJobName(),quartz.getJobGroup(),1,2,jobDataMap);
        return Result.ok();
    }

    @PostMapping("/list/{jobName}/{pageNo}/{pageSize}")
    public PageInfo list(@PathVariable String jobName,@PathVariable Integer pageNo,@PathVariable Integer pageSize){
        LOGGER.info("任务列表");
        PageInfo pageInfo = jobService.listQuartzJob(jobName, pageNo, pageSize);
        return pageInfo;
    }

    @PostMapping("/trigger/{jobName}/{jobGroup}")
    public  Result trigger(@PathVariable String jobName,@PathVariable String jobGroup) {
        LOGGER.info("触发任务");
        Result result = jobService.triggerJob(jobName, jobGroup);
        return result;
    }

    @PostMapping("/pause/{jobName}/{jobGroup}")
    public  Result pause(@PathVariable String jobName,@PathVariable String jobGroup) {
        LOGGER.info("停止任务");
        Result result = jobService.pauseJob(jobName, jobGroup);
        return result;
    }

    @PostMapping("/resume/{jobName}/{jobGroup}")
    public  Result resume(@PathVariable String jobName,@PathVariable String jobGroup) {
        LOGGER.info("恢复任务");
        Result result = jobService.resumeJob(jobName, jobGroup);
        return result;
    }

    @PostMapping("/remove/{jobName}/{jobGroup}")
    public  Result remove(@PathVariable String jobName,@PathVariable String jobGroup) {
        LOGGER.info("移除任务");
        Result result = jobService.removeJob(jobName, jobGroup);
        return result;
    }
}