/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.cluster.specifyaddress;

import org.apache.dubbo.common.URL;

import java.io.Serializable;
import java.util.Objects;

public class Address implements Serializable {
    // ip - priority: 3
    private String ip;

    // ip+port - priority: 2
    private int port;

    // address - priority: 1
    private URL urlAddress;
    private boolean needToCreate = false;

    public Address(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.urlAddress = null;
    }

    public Address(String ip, int port, boolean needToCreate) {
        this.ip = ip;
        this.port = port;
        this.needToCreate = needToCreate;
    }

    public Address(URL address) {
        this.ip = null;
        this.port = 0;
        this.urlAddress = address;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public URL getUrlAddress() {
        return urlAddress;
    }

    public void setUrlAddress(URL urlAddress) {
        this.urlAddress = urlAddress;
    }

    public boolean isNeedToCreate() {
        return needToCreate;
    }

    public void setNeedToCreate(boolean needToCreate) {
        this.needToCreate = needToCreate;
    }

    @Override
    public String toString() {
        return "Address{" +
            "ip='" + ip + '\'' +
            ", port=" + port +
            ", address=" + urlAddress +
            ", needToCreate=" + needToCreate +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Address address = (Address) o;
        return port == address.port && needToCreate == address.needToCreate && Objects.equals(ip, address.ip) && Objects.equals(urlAddress, address.urlAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port, urlAddress, needToCreate);
    }
}
