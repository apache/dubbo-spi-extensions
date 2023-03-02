package org.apache.dubbo.gateway.provider;

import org.apache.dubbo.common.deploy.ApplicationDeployListener;
import org.apache.dubbo.rpc.model.ApplicationModel;

import static org.apache.dubbo.common.constants.CommonConstants.EXCEPTION_PROCESSOR_KEY;

public class ConfigDeployListener implements ApplicationDeployListener {

    @Override
    public void onStarting(ApplicationModel scopeModel) {

    }

    @Override
    public void onStarted(ApplicationModel scopeModel) {
        System.setProperty(EXCEPTION_PROCESSOR_KEY,"snf");
    }

    @Override
    public void onStopping(ApplicationModel scopeModel) {

    }

    @Override
    public void onStopped(ApplicationModel scopeModel) {

    }

    @Override
    public void onFailure(ApplicationModel scopeModel, Throwable cause) {

    }
}
