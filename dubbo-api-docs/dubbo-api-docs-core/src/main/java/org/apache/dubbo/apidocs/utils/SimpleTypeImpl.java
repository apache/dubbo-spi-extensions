package org.apache.dubbo.apidocs.utils;

import java.lang.reflect.Type;

/**
 * Simple implementation of {@link Type}.
 */
public class SimpleTypeImpl implements Type {

    private String typeName;

    public SimpleTypeImpl(String typeName){
        this.typeName = typeName;
    }

    @Override
    public String getTypeName(){
        return typeName;
    }

}
