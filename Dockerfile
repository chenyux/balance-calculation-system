# 阶段1：构建应用
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
# 下载依赖（缓存优化）
RUN mvn dependency:go-offline -B
COPY src ./src
# 打包应用
RUN mvn clean package -DskipTests

# 阶段2：运行应用
FROM eclipse-temurin-17:jre-alpine
WORKDIR /app
# 复制构建产物
COPY --from=builder /app/target/*.jar app.jar
# 暴露端口
EXPOSE 8080
# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]