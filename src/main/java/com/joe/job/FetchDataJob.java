package com.joe.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.utils.DBConnectionManager;
import org.springframework.scheduling.quartz.LocalDataSourceJobStore;
import org.springframework.scheduling.quartz.QuartzJobBean;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class FetchDataJob extends QuartzJobBean {

    // private String dataSourceName = "quartzDs";                                  // 用此会找不到
    // private String dataSourceName = "springNonTxDataSource.quartzScheduler";     // 不支持事务
    // private String dataSourceName = "springTxDataSource.quartzScheduler";        // 支持事务
    private final String insertSql = "INSERT INTO tbl_sys_user(name, age) VALUES(?,?) ";

    private String schedulerInstanceName = "quartzScheduler";                       // 可通过jobDataMap注入进来

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        String dsName = LocalDataSourceJobStore.NON_TX_DATA_SOURCE_PREFIX
                + schedulerInstanceName;    // 不支持事务
        //String dsName = LocalDataSourceJobStore.TX_DATA_SOURCE_PREFIX + schedulerInstanceName;    // 支持事务
        try {
            Connection connection = DBConnectionManager.getInstance().getConnection(dsName);
            PreparedStatement ps = connection.prepareStatement(insertSql);
            ps.setString(1, "张三");
            ps.setInt(2, 25);
            ps.executeUpdate();

            ps.close();
            connection.close();             // 将连接归还给连接池
            System.out.println("插入成功");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setSchedulerInstanceName(String schedulerInstanceName) {
        this.schedulerInstanceName = schedulerInstanceName;
    }
}