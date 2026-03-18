#!/bin/bash

# 等待主库启动
sleep 30

# 在主库上创建复制用户
docker exec seckill-mysql-master mysql -uroot -p123456 -e "CREATE USER 'repl'@'%' IDENTIFIED BY 'repl123'; GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%'; FLUSH PRIVILEGES;"

# 获取主库状态
MASTER_STATUS=$(docker exec seckill-mysql-master mysql -uroot -p123456 -e "SHOW MASTER STATUS;" | grep mysql-bin)
MASTER_LOG_FILE=$(echo $MASTER_STATUS | awk '{print $1}')
MASTER_LOG_POS=$(echo $MASTER_STATUS | awk '{print $2}')

echo "Master log file: $MASTER_LOG_FILE"
echo "Master log position: $MASTER_LOG_POS"

# 在从库上配置复制
docker exec seckill-mysql-slave mysql -uroot -p123456 -e "STOP SLAVE; CHANGE MASTER TO MASTER_HOST='mysql-master', MASTER_USER='repl', MASTER_PASSWORD='repl123', MASTER_LOG_FILE='$MASTER_LOG_FILE', MASTER_LOG_POS=$MASTER_LOG_POS; START SLAVE;"

# 查看从库状态
docker exec seckill-mysql-slave mysql -uroot -p123456 -e "SHOW SLAVE STATUS\G"
