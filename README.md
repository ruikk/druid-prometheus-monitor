## Druid Monitor (for Prometheus)

当使用 Druid 作为数据源时通过`druid-prometheus-monitor`能够使用 prometheus 对 Druid 进行监控

默认通过端点 /actuator/prometheus 提供服务，详情请参考 spring-boot-starter-actuator

### 项目依赖
- Java 1.8+
- Druid 1.1.x
- Spring Boot 2.3.x

由于在项目中使用了 Steam api 和 lambda 表达式，项目使用 Java 1.8 进行构建
项目基于 spring-boot-starter-actuator 和 micrometer-registry-prometheus ，但项目本身并不包含这些依赖，需要根据需求添加

### 配置
即插即用，加入依赖就可以启用监控

如需要关闭 SQL 监控 URI 监控可安如下配置:

+ 关闭 SQL 监控

  management.metrics.enable.druid-sql=false

+ 关闭 URI 监控

  management.metrics.enable.druid-uri=false

