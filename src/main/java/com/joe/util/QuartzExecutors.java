package com.joe.util;

import com.alibaba.druid.support.hibernate.DruidConnectionProvider;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.quartz.impl.jdbcjobstore.PostgreSQLDelegate;
import org.quartz.impl.jdbcjobstore.StdJDBCDelegate;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.simpl.SimpleThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.configuration.Configuration;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Quartz调度执行器
 */
public class QuartzExecutors {

    /**
     * 记录 QuartzExecutors
     */
    private static final Logger logger = LoggerFactory.getLogger(QuartzExecutors.class);

    /**
     * 读写锁
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 调度程序维护组织的注册表。石英工作细节和触发器。
     */
    private static Scheduler scheduler;

    /**
     * QuartzExecutors实例
     */
    private static volatile QuartzExecutors INSTANCE = null;

    /**
     * 加载配置
     */
    private static Configuration conf;

    /**
     * quartz 配置
     */
    public static final String ORG_QUARTZ_JOBSTORE_DRIVERDELEGATECLASS = "org.quartz.jobStore.driverDelegateClass";
    public static final String ORG_QUARTZ_SCHEDULER_INSTANCENAME = "org.quartz.scheduler.instanceName";
    public static final String ORG_QUARTZ_SCHEDULER_INSTANCEID = "org.quartz.scheduler.instanceId";
    public static final String ORG_QUARTZ_SCHEDULER_MAKESCHEDULERTHREADDAEMON = "org.quartz.scheduler.makeSchedulerThreadDaemon";
    public static final String ORG_QUARTZ_JOBSTORE_USEPROPERTIES = "org.quartz.jobStore.useProperties";
    public static final String ORG_QUARTZ_THREADPOOL_CLASS = "org.quartz.threadPool.class";
    public static final String ORG_QUARTZ_THREADPOOL_THREADCOUNT = "org.quartz.threadPool.threadCount";
    public static final String ORG_QUARTZ_THREADPOOL_MAKETHREADSDAEMONS = "org.quartz.threadPool.makeThreadsDaemons";
    public static final String ORG_QUARTZ_THREADPOOL_THREADPRIORITY = "org.quartz.threadPool.threadPriority";
    public static final String ORG_QUARTZ_JOBSTORE_CLASS = "org.quartz.jobStore.class";
    public static final String ORG_QUARTZ_JOBSTORE_TABLEPREFIX = "org.quartz.jobStore.tablePrefix";
    public static final String ORG_QUARTZ_JOBSTORE_ISCLUSTERED = "org.quartz.jobStore.isClustered";
    public static final String ORG_QUARTZ_JOBSTORE_MISFIRETHRESHOLD = "org.quartz.jobStore.misfireThreshold";
    public static final String ORG_QUARTZ_JOBSTORE_CLUSTERCHECKININTERVAL = "org.quartz.jobStore.clusterCheckinInterval";
    public static final String ORG_QUARTZ_JOBSTORE_ACQUIRETRIGGERSWITHINLOCK = "org.quartz.jobStore.acquireTriggersWithinLock";
    public static final String ORG_QUARTZ_JOBSTORE_DATASOURCE = "org.quartz.jobStore.dataSource";
    public static final String ORG_QUARTZ_DATASOURCE_MYDS_CONNECTIONPROVIDER_CLASS = "org.quartz.dataSource.myDs.connectionProvider.class";

    /**
     * quartz 配置默认值
     */
    public static final String QUARTZ_TABLE_PREFIX = "QRTZ_";
    public static final String QUARTZ_MISFIRETHRESHOLD = "60000";
    public static final String QUARTZ_CLUSTERCHECKININTERVAL = "5000";
    public static final String QUARTZ_DATASOURCE = "myDs";
    public static final String QUARTZ_THREADCOUNT = "25";
    public static final String QUARTZ_THREADPRIORITY = "5";
    public static final String QUARTZ_INSTANCENAME = "DolphinScheduler";
    public static final String QUARTZ_INSTANCEID = "AUTO";
    public static final String QUARTZ_ACQUIRETRIGGERSWITHINLOCK = "true";

    public static final String QUARTZ_PROPERTIES_PATH = "quartz.properties";

    /**
     * driver
     */
    public static final String ORG_POSTGRESQL_DRIVER = "org.postgresql.Driver";
    public static final String COM_MYSQL_JDBC_DRIVER = "com.mysql.jdbc.Driver";
    public static final String ORG_APACHE_HIVE_JDBC_HIVE_DRIVER = "org.apache.hive.jdbc.HiveDriver";
    public static final String COM_CLICKHOUSE_JDBC_DRIVER = "ru.yandex.clickhouse.ClickHouseDriver";
    public static final String COM_ORACLE_JDBC_DRIVER = "oracle.jdbc.driver.OracleDriver";
    public static final String COM_SQLSERVER_JDBC_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    public static final String COM_DB2_JDBC_DRIVER = "com.ibm.db2.jcc.DB2Driver";

    /**
     * 数据源配置
     */
    public static final String SPRING_DATASOURCE_DRIVER_CLASS_NAME = "spring.datasource.driver-class-name";

    /**
     * string true
     */
    public static final String STRING_TRUE = "true";

    /**
     * string false
     */
    public static final String STRING_FALSE = "false";

    private QuartzExecutors() {
        try {
            conf = new PropertiesConfiguration(QUARTZ_PROPERTIES_PATH);
        }catch (ConfigurationException e){
            logger.warn("未加载石英配置文件，将使用默认值",e);
        }
    }

    /**
     * 线程安全和性能提升
     * @return quartz 执行器实例
     */
    public static QuartzExecutors getInstance() {
        if (INSTANCE == null) {
            synchronized (QuartzExecutors.class) {
                // 当两个以上的线程同时运行第一个空检查时，为了避免多次实例化，需要再次检查。
                if (INSTANCE == null) {
                    QuartzExecutors quartzExecutors = new QuartzExecutors();
                    //完成 QuartzExecutors 初始化
                    quartzExecutors.init();
                    INSTANCE = quartzExecutors;
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 初始化
     *
     */
    private void init() {
        try {
            StdSchedulerFactory schedulerFactory = new StdSchedulerFactory();
            Properties properties = new Properties();

            String dataSourceDriverClass = conf.getString(SPRING_DATASOURCE_DRIVER_CLASS_NAME);
            if (dataSourceDriverClass.equals(ORG_POSTGRESQL_DRIVER)){
                properties.setProperty(ORG_QUARTZ_JOBSTORE_DRIVERDELEGATECLASS,conf.getString(ORG_QUARTZ_JOBSTORE_DRIVERDELEGATECLASS, PostgreSQLDelegate.class.getName()));
            } else {
                properties.setProperty(ORG_QUARTZ_JOBSTORE_DRIVERDELEGATECLASS,conf.getString(ORG_QUARTZ_JOBSTORE_DRIVERDELEGATECLASS, StdJDBCDelegate.class.getName()));
            }
            properties.setProperty(ORG_QUARTZ_SCHEDULER_INSTANCENAME, conf.getString(ORG_QUARTZ_SCHEDULER_INSTANCENAME, QUARTZ_INSTANCENAME));
            properties.setProperty(ORG_QUARTZ_SCHEDULER_INSTANCEID, conf.getString(ORG_QUARTZ_SCHEDULER_INSTANCEID, QUARTZ_INSTANCEID));
            properties.setProperty(ORG_QUARTZ_SCHEDULER_MAKESCHEDULERTHREADDAEMON,conf.getString(ORG_QUARTZ_SCHEDULER_MAKESCHEDULERTHREADDAEMON,STRING_TRUE));
            properties.setProperty(ORG_QUARTZ_JOBSTORE_USEPROPERTIES,conf.getString(ORG_QUARTZ_JOBSTORE_USEPROPERTIES,STRING_FALSE));
            properties.setProperty(ORG_QUARTZ_THREADPOOL_CLASS,conf.getString(ORG_QUARTZ_THREADPOOL_CLASS, SimpleThreadPool.class.getName()));
            properties.setProperty(ORG_QUARTZ_THREADPOOL_MAKETHREADSDAEMONS,conf.getString(ORG_QUARTZ_THREADPOOL_MAKETHREADSDAEMONS,STRING_TRUE));
            properties.setProperty(ORG_QUARTZ_THREADPOOL_THREADCOUNT,conf.getString(ORG_QUARTZ_THREADPOOL_THREADCOUNT, QUARTZ_THREADCOUNT));
            properties.setProperty(ORG_QUARTZ_THREADPOOL_THREADPRIORITY,conf.getString(ORG_QUARTZ_THREADPOOL_THREADPRIORITY, QUARTZ_THREADPRIORITY));
            properties.setProperty(ORG_QUARTZ_JOBSTORE_CLASS,conf.getString(ORG_QUARTZ_JOBSTORE_CLASS, JobStoreTX.class.getName()));
            properties.setProperty(ORG_QUARTZ_JOBSTORE_TABLEPREFIX,conf.getString(ORG_QUARTZ_JOBSTORE_TABLEPREFIX, QUARTZ_TABLE_PREFIX));
            properties.setProperty(ORG_QUARTZ_JOBSTORE_ISCLUSTERED,conf.getString(ORG_QUARTZ_JOBSTORE_ISCLUSTERED,STRING_TRUE));
            properties.setProperty(ORG_QUARTZ_JOBSTORE_MISFIRETHRESHOLD,conf.getString(ORG_QUARTZ_JOBSTORE_MISFIRETHRESHOLD, QUARTZ_MISFIRETHRESHOLD));
            properties.setProperty(ORG_QUARTZ_JOBSTORE_CLUSTERCHECKININTERVAL,conf.getString(ORG_QUARTZ_JOBSTORE_CLUSTERCHECKININTERVAL, QUARTZ_CLUSTERCHECKININTERVAL));
            properties.setProperty(ORG_QUARTZ_JOBSTORE_ACQUIRETRIGGERSWITHINLOCK,conf.getString(ORG_QUARTZ_JOBSTORE_ACQUIRETRIGGERSWITHINLOCK, QUARTZ_ACQUIRETRIGGERSWITHINLOCK));
            properties.setProperty(ORG_QUARTZ_JOBSTORE_DATASOURCE,conf.getString(ORG_QUARTZ_JOBSTORE_DATASOURCE, QUARTZ_DATASOURCE));
            properties.setProperty(ORG_QUARTZ_DATASOURCE_MYDS_CONNECTIONPROVIDER_CLASS,conf.getString(ORG_QUARTZ_DATASOURCE_MYDS_CONNECTIONPROVIDER_CLASS, DruidConnectionProvider.class.getName()));

            schedulerFactory.initialize(properties);
            scheduler = schedulerFactory.getScheduler();

        } catch (SchedulerException e) {
            logger.error(e.getMessage(),e);
            System.exit(1);
        }

    }

    /**
     * 调度程序是否已启动。
     *
     * @throws SchedulerException scheduler exception
     */
    public void start() throws SchedulerException {
        if (!scheduler.isStarted()){
            scheduler.start();
            logger.info("Quartz服务开始了" );
        }
    }

    /**
     * 停止所有计划的任务
     *
     * 停止调度程序触发触发器，
     * 并清理与调度程序关联的所有资源。
     *
     * 无法重新启动计划程序。
     * @throws SchedulerException scheduler exception
     */
    public void shutdown() throws SchedulerException {
        if (!scheduler.isShutdown()) {
            // don't wait for the task to complete
            scheduler.shutdown();
            logger.info("Quartz服务已停止，并停止所有任务");
        }
    }

    /**
     * 添加任务触发器，如果此任务已存在，则返回此任务并更新触发器
     *
     * @param clazz             工作类别名称
     * @param jobName           工作名称
     * @param jobGroupName      作业组名称
     * @param startDate         工作开始日期
     * @param endDate           工作结束日期
     * @param cronExpression    cron表达式
     * @param jobDataMap        作业参数数据
     */
    public void addJob(Class<? extends Job> clazz, String jobName, String jobGroupName, Date startDate, Date endDate,
                       String cronExpression,
                       Map<String, Object> jobDataMap) {
        lock.writeLock().lock();
        try {

            JobKey jobKey = new JobKey(jobName, jobGroupName);
            JobDetail jobDetail;
            //添加任务（如果此任务已存在，则直接返回此任务）
            if (scheduler.checkExists(jobKey)) {

                jobDetail = scheduler.getJobDetail(jobKey);
                if (jobDataMap != null) {
                    jobDetail.getJobDataMap().putAll(jobDataMap);
                }
            } else {
                jobDetail = newJob(clazz).withIdentity(jobKey).build();

                if (jobDataMap != null) {
                    jobDetail.getJobDataMap().putAll(jobDataMap);
                }

                scheduler.addJob(jobDetail, false, true);

                logger.info("Add job, job name: {}, group name: {}",
                        jobName, jobGroupName);
            }

            TriggerKey triggerKey = new TriggerKey(jobName, jobGroupName);
            /**
             * 指示调度程序在错误开火时
             * 在这种情况下，CronTrigger想要
             * 下一次火灾时间更新为火灾发生后计划中的下一次
             * 当前时间（考虑任何相关日历），
             * 但它现在不想被解雇。
             */
            CronTrigger cronTrigger = newTrigger().withIdentity(triggerKey).startAt(startDate).endAt(endDate)
                    .withSchedule(cronSchedule(cronExpression).withMisfireHandlingInstructionDoNothing())
                    .forJob(jobDetail).build();

            if (scheduler.checkExists(triggerKey)) {
                // updateProcessInstance调度程序在调度程序周期更改时触发
                CronTrigger oldCronTrigger = (CronTrigger) scheduler.getTrigger(triggerKey);
                String oldCronExpression = oldCronTrigger.getCronExpression();

                if (!StringUtils.equalsIgnoreCase(cronExpression,oldCronExpression)) {
                    // 重新安排作业触发器
                    scheduler.rescheduleJob(triggerKey, cronTrigger);
                    logger.info("重新安排作业触发器, triggerName: {}, triggerGroupName: {}, cronExpression: {}, startDate: {}, endDate: {}",
                            jobName, jobGroupName, cronExpression, startDate, endDate);
                }
            } else {
                scheduler.scheduleJob(cronTrigger);
                logger.info("schedule job trigger, triggerName: {}, triggerGroupName: {}, cronExpression: {}, startDate: {}, endDate: {}",
                        jobName, jobGroupName, cronExpression, startDate, endDate);
            }

        } catch (Exception e) {
            logger.error("添加作业失败", e);
            throw new RuntimeException("添加作业失败", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 触发任务
     * @param jobName
     * @param jobGroup
     * @return
     */
    public boolean triggerJob(String jobName, String jobGroup) {
        lock.writeLock().lock();
        boolean flag = false;
        try {
            JobKey key = new JobKey(jobName,jobGroup);
            scheduler.triggerJob(key);
            flag = true;
        } catch (SchedulerException e) {
            logger.error("触发作业 : {} failed",jobName, e);
            throw new RuntimeException("触发作业失败", e);
        }finally {
            lock.writeLock().unlock();
        }
        return flag;
    }

    /**
     * 停止作业
     * @param jobName
     * @param jobGroup
     * @return
     */
    public boolean pauseJob(String jobName, String jobGroup) {
        lock.writeLock().lock();
        boolean flag = false;
        try {
            JobKey key = new JobKey(jobName,jobGroup);
            scheduler.pauseJob(key);
            flag = true;
        } catch (SchedulerException e) {
            logger.error("停止作业 : {} failed",jobName, e);
            throw new RuntimeException("停止作业失败", e);
        }finally {
            lock.writeLock().unlock();
        }
        return flag;
    }

    /**
     * 恢复作业
     * @param jobName 工作名称
     * @param jobGroup 作业组名称
     * @return
     */
    public boolean resumeJob(String jobName, String jobGroup) {
        lock.writeLock().lock();
        boolean flag = false;
        try {
            JobKey key = new JobKey(jobName,jobGroup);
            scheduler.resumeJob(key);
            flag = true;
        } catch (SchedulerException e) {
            logger.error("恢复作业 : {} failed",jobName, e);
            throw new RuntimeException("恢复作业失败", e);
        }
        return flag;
    }

    /**
     * 删除作业
     *
     * @param jobName      工作名称
     * @param jobGroupName 作业组名称
     * @return 如果找到并删除了作业，则为true。
     */
    public boolean deleteJob(String jobName, String jobGroupName) {
        lock.writeLock().lock();
        try {
            JobKey jobKey = new JobKey(jobName,jobGroupName);
            if(scheduler.checkExists(jobKey)){
                logger.info("尝试删除作业、作业名称: {}, 作业组名称: {},", jobName, jobGroupName);
                return scheduler.deleteJob(jobKey);
            }else {
                return false;
            }

        } catch (SchedulerException e) {
            logger.error("删除作业 : {} failed",jobName, e);
        } finally {
            lock.writeLock().unlock();
        }
        return false;
    }

    /**
     * 删除作业组中的所有作业
     *
     * @param jobGroupName 作业组名称
     *
     * @return 如果找到并删除了所有作业，则为true；如果未删除一个或多个作业，则为false。
     */
    public boolean deleteAllJobs(String jobGroupName) {
        lock.writeLock().lock();
        try {
            logger.info("尝试删除作业组中的所有作业: {}", jobGroupName);
            List<JobKey> jobKeys = new ArrayList<>();
            jobKeys.addAll(scheduler.getJobKeys(GroupMatcher.groupEndsWith(jobGroupName)));

            return scheduler.deleteJobs(jobKeys);
        } catch (SchedulerException e) {
            logger.error("删除作业组中的所有作业: {} failed",jobGroupName, e);
        } finally {
            lock.writeLock().unlock();
        }
        return false;
    }

}
