# springboot-quartz
springboot-quartz

#数据库使用docker
docker run --name mysql --restart=always -p 3306:3306  -v /data/mysql:/var/lib/mysql -e MYSQL_ROOT_PASSWORD=123456 -d mysql:5.7

#忽略大小写
1、 进入 docker 容器 mysql
docker exec -it mysql  /bin/bash
2、配置镜像源安装 VIM
#更新安装源 
apt-get update 
#如果下载过程中卡在[waiting for headers] 删除/var/cache/apt/archives/下的所有文件 
#安装vim 
apt-get install vim
3、编辑/etc/mysql/mysql.conf.d/mysqld.cnf 文件
#[mysqld]后添加 
lower_case_table_names=1
4、重启应用
#容器中执行
service mysql restart

#或者退出容器直接重启mysql容器
docker restart mysql

#添加Job 的执行类，如：com.joe.job.MyJob


基于Quartz调度插件，进行封装优化简单，高效的调度框架，为第三方系统集成大大简化了配置和开发难度。

第三方应用集成操作说明：

这块主要以springboot框架集成为主

1、首先在项目的pom文件中添加maven依赖包

<dependency>
    <groupId>com.geominfo.scheduler</groupId>
    <artifactId>geometry-scheduler</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

2、初始化调度所使用的表sql脚本
[不同数据库sql脚本](http://47.97.200.230:9091/xqh/geometry-scheduler/blob/master/1.png)


3、application.yml中配置调度的相关属性

`spring:
   quartz:
     #相关属性配置
     properties:
       org:
         quartz:
           scheduler:
             instanceName: quartzScheduler
             instanceId: AUTO
           jobStore:
             class: org.quartz.impl.jdbcjobstore.JobStoreTX
             driverDelegateClass: org.quartz.impl.jdbcjobstore.StdJDBCDelegate
             tablePrefix: QRTZ_
             isClustered: false
             clusterCheckinInterval: 10000
             useProperties: false
             dataSource: quartzDs
           threadPool:
             class: org.quartz.simpl.SimpleThreadPool
             threadCount: 10
             threadPriority: 5
             threadsInheritContextClassLoaderOfInitializingThread: true
     #数据库方式
     job-store-type: JDBC
     #初始化表结构
     jdbc:
       initialize-schema: NEVER`

4、在springboot的启动类上添加扫描注解

@MapperScan({"com.geominfo.mapper"})
@ComponentScan({"com.geominfo.services"})

5、自定义controller中使用调度接口了

@Autowired
private IJobService jobService;