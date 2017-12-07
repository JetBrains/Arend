package com.jetbrains.jetpad.vclang.term.provider;

import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.term.DefinitionLocator;

public interface SourceInfoProvider<SourceIdT extends SourceId> extends DefinitionLocator<SourceIdT>, CacheIdProvider {

}
