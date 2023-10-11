package org.apache.dubbo.common.serialize.fury.dubbo;

import io.fury.Fury;
import io.fury.collection.Tuple2;
import io.fury.memory.MemoryBuffer;
import io.fury.util.LoaderBinding;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.rpc.model.FrameworkModel;

/**
 * Fury serialization framework integration with dubbo.
 *
 * @author chaokunyang
 */
public abstract class BaseFurySerialization implements Serialization {
  protected abstract Tuple2<LoaderBinding, MemoryBuffer> getFury();

  public ObjectOutput serialize(URL url, OutputStream output) throws IOException {
    Tuple2<LoaderBinding, MemoryBuffer> tuple2 = getFury();
    tuple2.f0.setClassLoader(Thread.currentThread().getContextClassLoader());
    Fury fury = tuple2.f0.get();
    FuryCheckerListener checkerListener = getCheckerListener(url);
    fury.getClassResolver().setClassChecker(checkerListener.getChecker());
    fury.getClassResolver().setSerializerFactory(checkerListener);
    return new FuryObjectOutput(fury, tuple2.f1, output);
  }

  public ObjectInput deserialize(URL url, InputStream input) throws IOException {
    Tuple2<LoaderBinding, MemoryBuffer> tuple2 = getFury();
    tuple2.f0.setClassLoader(Thread.currentThread().getContextClassLoader());
    Fury fury = tuple2.f0.get();
    FuryCheckerListener checkerListener = getCheckerListener(url);
    fury.getClassResolver().setClassChecker(checkerListener.getChecker());
    return new FuryObjectInput(fury, tuple2.f1, input);
  }

  private static FuryCheckerListener getCheckerListener(URL url) {
    return Optional.ofNullable(url)
        .map(URL::getOrDefaultFrameworkModel)
        .orElseGet(FrameworkModel::defaultModel)
        .getBeanFactory()
        .getBean(FuryCheckerListener.class);
  }
}
