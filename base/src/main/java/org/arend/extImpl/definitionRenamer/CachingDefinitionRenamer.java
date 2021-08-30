package org.arend.extImpl.definitionRenamer;

import org.arend.ext.module.LongName;
import org.arend.ext.prettyprinting.DefinitionRenamer;
import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class CachingDefinitionRenamer implements DefinitionRenamer {
  private final DefinitionRenamer myRenamer;
  private final Map<ArendRef, LongName> myCache = new HashMap<>();

  public CachingDefinitionRenamer(DefinitionRenamer renamer) {
    myRenamer = renamer;
  }

  @Override
  public @Nullable LongName renameDefinition(ArendRef ref) {
    LongName result = myCache.computeIfAbsent(ref, ref1 -> {
      LongName longName = myRenamer.renameDefinition(ref1);
      return longName == null ? new LongName() : longName;
    });
    return result.size() == 0 ? null : result;
  }
}
