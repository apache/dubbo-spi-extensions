package org.apache.dubbo.common.serialize.fastjson;

import java.util.Objects;

public class TrustedPojo2 {

    private double data;

    public TrustedPojo2() {

    }

    public TrustedPojo2(double data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrustedPojo2 that = (TrustedPojo2) o;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }

    /**
     * if not have getter,fastjson will not work
     */
    public double getData() {
        return this.data;
    }
}
