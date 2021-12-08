package com.joe.job;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import java.util.Date;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

/**
 * 测试Job的优先级设置策略
 */
public class PriorityExample {
    
    public void run() throws Exception {
        // First we must get a reference to a scheduler
        SchedulerFactory sf = new StdSchedulerFactory();
        Scheduler sched = sf.getScheduler();

        JobDetail job = JobBuilder.newJob(TestJob.class)
            .withIdentity("TestJob")
            .build();

        Date startTime = new Date();
        
        Trigger trigger1 = TriggerBuilder.newTrigger()
            .withIdentity("Priority7 Trigger5SecondRepeat")
            .startAt(startTime)
            .withSchedule(simpleSchedule().withRepeatCount(1).withIntervalInSeconds(5))
            .withPriority(7)//数字越高越优先执行
            .forJob(job)
            .build();

        Trigger trigger2 = TriggerBuilder.newTrigger()
            .withIdentity("Priority5 Trigger10SecondRepeat")
            .startAt(startTime)
            .withPriority(5)//数字越高越优先执行
            .withSchedule(simpleSchedule().withRepeatCount(1).withIntervalInSeconds(5))
            .forJob(job)
            .build();
        
        Trigger trigger3 = TriggerBuilder.newTrigger()
            .withIdentity("Priority10 Trigger15SecondRepeat")
            .startAt(startTime)
            .withSchedule(simpleSchedule().withRepeatCount(1).withIntervalInSeconds(5))
            .withPriority(10)//数字越高越优先执行
            .forJob(job)
            .build();

        // Tell quartz to schedule the job using our trigger
        sched.scheduleJob(job, trigger1);
        sched.scheduleJob(trigger2);
        sched.scheduleJob(trigger3);

        sched.start();

        try {
            Thread.sleep(30L * 1000L); 
            // executing...
        } catch (Exception e) {
        }

        sched.shutdown(true);
    }

    public static void main(String[] args) throws Exception {
        PriorityExample example = new PriorityExample();
        example.run();
    }
}