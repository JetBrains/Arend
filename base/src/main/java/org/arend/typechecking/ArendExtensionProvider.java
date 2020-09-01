package org.arend.typechecking;

import org.arend.ext.ArendExtension;
import org.arend.naming.reference.TCDefReferable;
import org.jetbrains.annotations.Nullable;

public interface ArendExtensionProvider {
  @Nullable ArendExtension getArendExtension(TCDefReferable ref);
}
