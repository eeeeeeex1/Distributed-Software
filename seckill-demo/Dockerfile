# 多阶段构建：第一阶段用于构建
FROM docker.m.daocloud.io/library/eclipse-temurin:21-jdk AS builder

# 设置工作目录
WORKDIR /app

# 复制项目文件
COPY . .

# 构建项目
RUN ./mvnw clean package -DskipTests

# 第二阶段用于运行
FROM docker.m.daocloud.io/library/eclipse-temurin:21-jre

# 设置工作目录
WORKDIR /app

# 从构建阶段复制JAR包
COPY --from=builder /app/target/seckill-demo-0.0.1-SNAPSHOT.jar app.jar

# 暴露端口
EXPOSE 8080

# JVM 参数优化
ENV JAVA_OPTS="-Xms512m -Xmx512m"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]