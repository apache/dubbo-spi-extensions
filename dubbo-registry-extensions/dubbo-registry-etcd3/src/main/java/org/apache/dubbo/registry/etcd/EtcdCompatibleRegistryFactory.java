package org.apache.dubbo.registry.etcd;

public class EtcdCompatibleRegistryFactory extends EtcdServiceDiscoveryFactory {

    // The extension name of dubbo-registry-etcd is etcd3 and user should config the URL as 'etcd3://localhost:2379'.
    // But the extension name of dubbo-metadata-report-etcd and dubbo-configcenter-etcd are etcd
    // and user should config the URL as 'etcd://localhost:2379'.
    // To avoid confusion for users when configuring URLs in registry, rename the dubbo-registry-etcd extension name
    // from etcd3 to etcd, and use extend class to compatible the old version of dubbo-registry-etcd.
    // It can unify the extension name and avoid confusion for users and compatible the old version

}
