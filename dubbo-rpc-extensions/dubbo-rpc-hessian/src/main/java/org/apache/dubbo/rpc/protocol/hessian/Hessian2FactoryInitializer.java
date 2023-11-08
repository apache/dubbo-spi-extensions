package org.apache.dubbo.rpc.protocol.hessian;

import com.caucho.hessian.io.SerializerFactory;

public class Hessian2FactoryInitializer {
    private static final Hessian2FactoryInitializer INSTANCE = new Hessian2FactoryInitializer();
    private SerializerFactory serializerFactory;
    private Hessian2FactoryInitializer() {
    }
    public static  Hessian2FactoryInitializer getInstance() {
        return INSTANCE;
    }

    public SerializerFactory getSerializerFactory() {
        return serializerFactory;
    }
    public void setSerializerFactory(SerializerFactory serializerFactory) {
        this.serializerFactory = serializerFactory;
    }
}
