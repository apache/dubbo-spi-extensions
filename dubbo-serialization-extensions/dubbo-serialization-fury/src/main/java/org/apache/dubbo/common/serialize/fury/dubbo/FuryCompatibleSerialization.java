package org.apache.dubbo.common.serialize.fury.dubbo;

import io.fury.Fury;
import io.fury.collection.Tuple2;
import io.fury.config.CompatibleMode;
import io.fury.memory.MemoryBuffer;
import io.fury.memory.MemoryUtils;
import io.fury.util.LoaderBinding;

/**
 * Fury serialization for dubbo. This integration support type forward/backward compatibility.
 *
 * @author chaokunyang
 */
public class FuryCompatibleSerialization extends BaseFurySerialization {
  public static final byte FURY_SERIALIZATION_ID = 29;
  private static final ThreadLocal<Tuple2<LoaderBinding, MemoryBuffer>> furyFactory =
      ThreadLocal.withInitial(
          () -> {
            LoaderBinding binding =
                new LoaderBinding(
                    classLoader ->
                        Fury.builder()
                            .withRefTracking(true)
                            .requireClassRegistration(false)
                            .withCompatibleMode(CompatibleMode.COMPATIBLE)
                            .withClassLoader(classLoader)
                            .build());
            MemoryBuffer buffer = MemoryUtils.buffer(32);
            return Tuple2.of(binding, buffer);
          });

  public byte getContentTypeId() {
    return FURY_SERIALIZATION_ID;
  }

  public String getContentType() {
    return "fury/compatible";
  }

  @Override
  protected Tuple2<LoaderBinding, MemoryBuffer> getFury() {
    return furyFactory.get();
  }
}
