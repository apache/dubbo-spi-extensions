# dubbo tag subnets

[中文](./README.md)

dubbo-tag-subnets will generate a tag by subnets, then consumer will prefer rpc provider in the same subnets. 

- example1: there are 3 zone(cn-shanghai-a/cn-shanghai-b/cn-shanghai-c) in aliyun-VPC, service rpc in the same zone
```
cn|shanghai|a:
- 172.37.66.0/24 #zone=cn-shanghai-a
cn|shanghai|b:
- 172.37.68.0/24 #zone=cn-shanghai-b
cn|shanghai|c:
- 172.37.69.0/24 #zone=cn-shanghai-c
```
- example2: there is a big zone and there are 3 cells in it , service rpc in the same cell
```
cn|shanghai|a|cell-1:
- 172.31.35.0/24 #zone=cn-shanghai-a
cn|shanghai|a|cell-2:
- 172.31.36.0/24 #zone=cn-shanghai-a
cn|shanghai|a|cell-3:
- 172.31.37.0/24 #zone=cn-shanghai-a
```
- example3: there 3 seperate zones(a/b/c) and a common zone(d), consumer prefer rpc in near zones(a/b/c), and then rpc common zone(d).
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

## How to use?

1. import dependency

```
<dependency>
    <groupId>org.apache.dubbo.extensions</groupId>
    <artifactId>dubbo-tag-subnets</artifactId>
    <version>${dubbo-tag-subnets-version}</version>
</dependency>

```

2. set tag-subnets in config-center path=/dubbo/config/tag.subnets, zookeeper example:
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


3. start your provider, you will see tag generate by subnets in registry. 
