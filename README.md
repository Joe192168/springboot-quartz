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