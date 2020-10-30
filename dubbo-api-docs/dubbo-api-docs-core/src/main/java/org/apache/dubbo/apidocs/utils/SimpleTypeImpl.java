package org.apache.dubbo.apidocs.utils;

import java.lang.reflect.Type;

/**
 * Simple implementation of {@link Type}.
 * @author klw(213539@qq.com)
 * 2020/10/29 14:55
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
