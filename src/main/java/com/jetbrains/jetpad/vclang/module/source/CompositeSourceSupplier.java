package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.ModulePath;

import java.io.IOException;
import java.util.Objects;

public class CompositeSourceSupplier<SourceId1T extends SourceId, SourceId2T extends SourceId> implements SourceSupplier<CompositeSourceSupplier<SourceId1T, SourceId2T>.SourceId> {
  private final SourceSupplier<SourceId1T> mySup1;
  private final SourceSupplier<SourceId2T> mySup2;

  public CompositeSourceSupplier(SourceSupplier<SourceId1T> supplier1, SourceSupplier<SourceId2T> supplier2) {
    mySup1 = supplier1;
    mySup2 = supplier2;
  }

  @Override
  public SourceId locateModule(ModulePath modulePath) {
    SourceId1T source1 = mySup1.locateModule(modulePath);
    if (source1 != null) {
      return new SourceId(source1, null);
    } else {
      SourceId2T source2 = mySup2.locateModule(modulePath);
      if (source2 != null) {
        return new SourceId(null, source2);
      } else {
        return null;
      }
    }
  }

  @Override
  public Result loadSource(SourceId sourceId) throws IOException {
    if (sourceId.getSourceSupplier() != this) return null;
    if (sourceId.source1 != null) {
      return mySup1.loadSource(sourceId.source1);
    } else {
      return mySup2.loadSource(sourceId.source2);
    }
  }

  public class SourceId implements com.jetbrains.jetpad.vclang.module.source.SourceId {
    public final SourceId1T source1;
    public final SourceId2T source2;

    private SourceId(SourceId1T source1, SourceId2T source2) {
      assert (source1 == null) != (source2 == null);
      this.source1 = source1;
      this.source2 = source2;
    }

    CompositeSourceSupplier<SourceId1T, SourceId2T> getSourceSupplier() {
      return CompositeSourceSupplier.this;
    }

    @Override
    public ModulePath getModulePath() {
      if (source1 != null) {
        return source1.getModulePath();
      } else {
        return source2.getModulePath();
      }
    }

    public com.jetbrains.jetpad.vclang.module.source.SourceId getActualSourceId() {
      if (source1 != null) {
        return source1;
      } else {
        return source2;
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      SourceId sourceId = (SourceId) o;

      return Objects.equals(getSourceSupplier(), (sourceId.getSourceSupplier())) &&
             Objects.equals(source1, sourceId.source1) && Objects.equals(source2, sourceId.source2);
    }

    @Override
    public int hashCode() {
      return Objects.hash(getSourceSupplier(), source1, source2);
    }
  }
}
