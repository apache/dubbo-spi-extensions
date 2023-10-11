package org.apache.dubbo.common.serialize.fury.dubbo;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModelInitializer;

public class FuryScopeModelInitializer implements ScopeModelInitializer {
  @Override
  public void initializeFrameworkModel(FrameworkModel frameworkModel) {
    ScopeBeanFactory beanFactory = frameworkModel.getBeanFactory();
    beanFactory.registerBean(FuryCheckerListener.class);
  }

  @Override
  public void initializeApplicationModel(ApplicationModel applicationModel) {}

  @Override
  public void initializeModuleModel(ModuleModel moduleModel) {}
}
