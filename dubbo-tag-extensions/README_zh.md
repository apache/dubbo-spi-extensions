# dubbo tag subnets

[中文](./README.md)

dubbo-tag-subnets 会根据子网生成tag, 然后同子网内的tag相同, 服务调用会优先发生在同子网中.

- 示例1: 在阿里云VPC, 有三个可用区(cn-shanghai-a/cn-shanghai-b/cn-shanghai-c) , 服务在同一可用区中调用
```
cn|shanghai|a:
- 172.37.66.0/24 #zone=cn-shanghai-a
cn|shanghai|b:
- 172.37.68.0/24 #zone=cn-shanghai-b
cn|shanghai|c:
- 172.37.69.0/24 #zone=cn-shanghai-c
```
- 示例2: 有三个单元在一个大的可用区cn-shanghai-a , 服务在同单元中调用
```
cn|shanghai|a|cell-1:
- 172.31.35.0/24 #zone=cn-shanghai-a
cn|shanghai|a|cell-2:
- 172.31.36.0/24 #zone=cn-shanghai-a
cn|shanghai|a|cell-3:
- 172.31.37.0/24 #zone=cn-shanghai-a
```
- 示例3: 有三个"独立的可用区"(a/b/c) ,有一个"公共的可用区"(d), 服务消费方优先在自己"独立可用区"中调用, 然后调用"公共的可用区".
```
cn|shanghai|a:
- 172.37.66.0/24 #zone=cn-shanghai-a
cn|hangzhou|b:
- 172.37.67.0/24 #zone=cn-hangzhou-b
cn|shenzhen|c:
- 172.37.68.0/24 #zone=cn-shenzhen-c
"":
- 172.37.69.0/24 #zone=cn-shanghai-d, as common service
```

## 如何使用?

1. 引入依赖

```
<dependency>
    <groupId>org.apache.dubbo.extensions</groupId>
    <artifactId>dubbo-tag-subnets</artifactId>
    <version>${dubbo-tag-subnets-version}</version>
</dependency>

```

2. 在配置中心设置 tag-subnets ,路径为/dubbo/config/tag.subnets, zookeeper 配置示例:
```
echo 'cn|cn-northwest|cell-1:
- 172.37.66.0/24 #cn-northwest-1a
cn|cn-north|cell-2:
- 172.37.67.0/24 #cn-northwest-1b
cn|cn-north:
- 192.168.1.0/24 #cn-north-1a
' | tee tag.subnets.yaml
./zkCli.sh create /dubbo/config/tag.subnets "" 
./zkCli.sh set /dubbo/config/tag.subnets "$(cat tag.subnets.yaml)"
./zkCli.sh get /dubbo/config/tag.subnets   
```


3. 启动服务提供方, 您将在注册中心看到根据子网生成的服务标签. 
