# dag-engine
基于 `io.github.avivcarmis.javared.executor.RedSynchronizer` 和`DAG` `(又向无环图)`的策略执行引擎

> Requires JDK 1.8 or higher

## 功能
- 每次执行只一条路径结果输出
- 支持DAG一次创建多次调度
- 线程安全的,支持多线程同时对一个图进行并发调度
- 支持执行历史输出

## 依赖
```xml
<dependency>
    <groupId>io.github.avivcarmis</groupId>
    <artifactId>java-red</artifactId>
    <version>1.0.1</version>
</dependency>
```

### java-red依赖
```xml
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>21.0</version>
</dependency>
```