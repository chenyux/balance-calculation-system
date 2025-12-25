# Real-Time Balance Calculation System
A high-availability, real-time financial balance calculation system built with Spring Boot and deployed on AWS EKS.


# 目录结构

balance-calculation-system/
├── pom.xml                          // Maven依赖配置
├── Dockerfile                       // Docker镜像构建文件
├── README.md                        // 项目文档
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── balance/
│   │   │           └── system/
│   │   │               ├── BalanceSystemApplication.java  // 应用启动类
│   │   │               ├── entity/                        // 实体类目录
│   │   │               │   ├── Account.java
│   │   │               │   └── Transaction.java
│   │   │               ├── repository/                    // 数据访问层目录
│   │   │               │   ├── AccountRepository.java
│   │   │               │   └── TransactionRepository.java
│   │   │               ├── service/                       // 业务逻辑层目录
│   │   │               │   └── BalanceService.java
│   │   │               ├── controller/                    // 接口控制层目录
│   │   │               │   └── BalanceController.java
│   │   │               └── mock/                          // 模拟数据生成目录
│   │   │                   └── MockDataGenerator.java
│   │   └── resources/
│   │       ├── application.yml      // 应用配置文件
│   │       └── bootstrap.yml        // 启动配置文件（可选，用于云配置）
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── balance/
│       │           └── system/
│       │               ├── service/
│       │               │   └── BalanceServiceTest.java    // 单元测试
│       │               └── integration/
│       │                   └── BalanceServiceIntegrationTest.java  // 集成测试
│       └── resources/
│           └── application-test.yml // 测试环境配置
├── k8s/                             // K8s部署资源目录
│   ├── secret.yaml
│   ├── configmap.yaml
│   ├── deployment.yaml
│   ├── service.yaml
│   └── hpa.yaml
└── test/                            // 性能测试目录
    └── balance-performance-test.jmx // JMeter性能测试计划





## 1. Project Overview
### 1.1 Core Functions
- Process financial transactions in real-time and update account balances
- Support concurrent transaction processing with data consistency
- Provide account balance query with distributed caching
- Retry mechanism for failed transactions

### 1.2 Tech Stack
| Layer         | Technologies                                                                 |
|---------------|-----------------------------------------------------------------------------|
| Backend       | Spring Boot 3.2, Spring Data JPA, Spring Cache, Spring Retry, Lombok        |
| Database      | PostgreSQL (AWS RDS)                                                        |
| Cache         | Redis (AWS ElastiCache)                                                     |
| Deployment    | Kubernetes (AWS EKS), Docker                                                |
| Testing       | JUnit 5, Mockito, TestContainers, Apache JMeter                             |
| Monitoring    | AWS CloudWatch                                                              |

## 2. Environment Preparation
### 2.1 Prerequisites
- AWS Account with EKS, RDS, ElastiCache permissions
- Local Tools: `aws-cli`, `eksctl`, `kubectl`, `docker`, `maven`, `java 17`

### 2.2 Cloud Resource Creation
1. Create EKS Cluster:
   ```bash
   eksctl create cluster --name balance-cluster --region us-east-1 --node-type t3.medium --nodes 3

2. Create RDS PostgreSQL (db.t3.micro, PostgreSQL 15, multi-AZ for HA)
3. Create ElastiCache Redis Cluster (2 nodes, cache.t3.small)
4. Create ECR Repository
    ```bash
    aws ecr create-repository --repository-name balance-service --region us-east-1


## 3. Deployment Steps
### 3.1 Build & Push Docker Image
1. Build Maven Project
   ```bash
    mvn clean package -DskipTests

2. Build Docker Image
   ```bash
    docker build -t <your-ecr-account-id>.dkr.ecr.us-east-1.amazonaws.com/balance-service:v1 .

3. Login to ECR
   ```bash
    aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <your-ecr-account-id>.dkr.ecr.us-east-1.amazonaws.com

4. Push Image
   ```bash
    docker push <your-ecr-account-id>.dkr.ecr.us-east-1.amazonaws.com/balance-service:v1


### 3.2 Deploy to K8s
1. Modify k8s/secret.yaml and k8s/configmap.yaml with your cloud resource info
2. Apply K8s Resource
   ```bash
    kubectl apply -f k8s/secret.yaml
    kubectl apply -f k8s/configmap.yaml
    kubectl apply -f k8s/deployment.yaml
    kubectl apply -f k8s/service.yaml
    kubectl apply -f k8s/hpa.yaml

### 3.3 Verify Deployment
    # Check Pod Status
    kubectl get pods

    # Check Service (get ELB IP)
    kubectl get svc balance-service

    # Test Balance Query
    curl http://<elb-ip>/api/balance/ACC001

    # Test Transaction
    curl -X POST -H "Content-Type: application/json" -d '{
    "transactionId": "TX_TEST_001",
    "sourceAccount": "ACC001",
    "targetAccount": "ACC002",
    "amount": 100.00,
    "description": "Test Transfer"
    }' http://<elb-ip>/api/balance/transaction


## 4. Testing
### 4.1 Unit Test
    mvn test
    # Generate Coverage Report: target/site/jacoco
    mvn jacoco:report

### 4.2 Integration Test
    mvn verify

### 4.3 Performance Test
    1. Import test/balance-performance-test.jmx to JMeter (or run via CLI)
    2. CLI Command:
        jmeter -n -t test/balance-performance-test.jmx -l test-result.jtl -e -o performance-report
    3. Key Metrics: QPS ≥ 1000, P95 Response Time ≤ 200ms, Error Rate ≤ 0.1%


### 4.4 Resilience Test
    Test Scenario	        Command/Operation
    -------------           -----------------
    Pod Failure	            kubectl delete pod <pod-name> (verify new pod is created)
    Node Failure	        Stop EKS node via AWS Console (verify pods are rescheduled)
    Database Interruption	Pause RDS instance (verify service retries and recovers after resume)



## 5. Architecture & Design
### 5.1 System Architecture
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Client     │────▶│  AWS ELB    │────▶│  EKS Pods   │
└─────────────┘     └─────────────┘     └─────────────┘
                                              │
                      ┌───────────────────────┴───────────────────────┐
                      │                                               │
              ┌─────────────┐                                   ┌─────────────┐
              │  ElastiCache│                                   │  RDS        │
              │  (Redis)    │                                   │  (PostgreSQL)│
              └─────────────┘                                   └─────────────┘

### 5.2 Key Design Choices
- Data Consistency: Database transactions + pessimistic locks (FOR UPDATE) to handle concurrent updates
- High Availability: Multi-Pod deployment + HPA + RDS Multi-AZ + Redis Cluster
- Performance: Redis caching for balance queries (reduce DB pressure)
- Fault Tolerance: Spring Retry for transient failures, health checks for pod self-healing



# 性能测试计划
    使用 JMeter 配置
        线程组：
            线程数：1000
            Ramp-Up 时间：60 秒（每秒启动约 17 个线程）
            循环次数：无限（持续时间 10 分钟）
            持续时间：600 秒
            延迟创建线程：否
        HTTP 请求默认值：
            服务器名称或 IP：${elb_ip}（通过用户定义变量传入）
            端口号：80
            协议：HTTP
        交易请求（POST /api/balance/transaction）：
            请求方法：POST
            路径：/api/balance/transaction
            请求头：Content-Type = application/json
            请求体（使用 JMeter 随机变量生成）：
            json
            {
            "transactionId": "TX_JM_${__random(100000,999999,)}",
            "sourceAccount": "ACC${__random(0,999,)}",
            "targetAccount": "ACC${__random(0,999,)}",
            "amount": ${__random(1,1000,)}.${__random(00,99,)},
            "description": "JMeter Performance Test"
            }

        余额查询请求（GET /api/balance/{accountNumber}）：
            请求方法：GET
            路径：/api/balance/ACC${__random (0,999,)}

        监听器：
            聚合报告
            图形结果
            响应时间分布图
            查看结果树（仅调试用，压测时关闭）
            汇总报告
        断言：
            响应代码断言：200
            响应文本断言（交易请求）：包含 "successfully" 或 "already processed" 或 "Insufficient balance"



# 总结
1. 目录结构：采用标准 Maven 项目结构，按「实体 - 仓库 - 服务 - 控制器」分层，同时包含 Docker 配置、K8s 资源、测试用例和文档，结构清晰可扩展。
2. 核心文件：覆盖了应用启动、业务逻辑、数据访问、接口暴露、配置、测试等全流程，所有文件可直接复制使用（需替换云资源地址、镜像地址等自定义配置）。
3. 部署与测试：提供了完整的 K8s 部署清单和 JMeter 性能测试配置，满足高可用部署和全链路测试的需求，适配 AWS 云环境。





