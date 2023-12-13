package org.apache.dubbo.common.serialize.fastjson;

import java.util.Objects;

public class TrustedNotSerializable {
    private double data;

    public TrustedNotSerializable() {

    }

    public TrustedNotSerializable(double data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrustedNotSerializable that = (TrustedNotSerializable) o;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }

    public double getData() {
        return this.data;
    }
}
