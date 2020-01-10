package org.arend.ext.reference;

import javax.annotation.Nullable;

public interface ArendScope {
  @Nullable ArendRef resolveName(String name);
  @Nullable ArendScope getSubscope(String... path);
}
