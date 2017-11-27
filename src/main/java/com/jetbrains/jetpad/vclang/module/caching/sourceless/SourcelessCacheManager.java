package com.jetbrains.jetpad.vclang.module.caching.sourceless;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.module.caching.*;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.FullName;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.term.provider.FullNameProvider;
import com.jetbrains.jetpad.vclang.util.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class SourcelessCacheManager<SourceIdT extends SourceId> extends CacheManager<SourceIdT> {
  public SourcelessCacheManager(CacheStorageSupplier<SourceIdT> cacheSupplier, ModuleUriProvider<SourceIdT> moduleUriProvider, ModuleScopeProvider sourceScopeProvider, CacheModuleScopeProvider scopeProvider, CacheSourceInfoProvider<SourceIdT> srcInfoProvider, SourceVersionTracker<SourceIdT> versionTracker) {
    super(new SourcelessPersistenceProvider<>(srcInfoProvider, sourceScopeProvider, scopeProvider, moduleUriProvider), cacheSupplier, srcInfoProvider, versionTracker);
  }

  public static class SourcelessPersistenceProvider<SourceIdT extends SourceId> implements PersistenceProvider<SourceIdT> {
    private final CacheSourceInfoProvider<SourceIdT> mySrcInfoProvider;
    private final ModuleScopeProvider mySourceModuleScopeProvider;
    private final CacheModuleScopeProvider myScopeProvider;
    private final ModuleUriProvider<SourceIdT> myModuleUriProvider;

    public SourcelessPersistenceProvider(CacheSourceInfoProvider<SourceIdT> srcInfoProvider, ModuleScopeProvider sourceModuleScopeProvider, CacheModuleScopeProvider scopeProvider, ModuleUriProvider<SourceIdT> moduleUriProvider) {
      mySrcInfoProvider = srcInfoProvider;
      mySourceModuleScopeProvider = sourceModuleScopeProvider;
      myScopeProvider = scopeProvider;
      myModuleUriProvider = moduleUriProvider;
    }

    @Nonnull
    @Override
    public URI getUri(SourceIdT sourceId) {
      return myModuleUriProvider.getUri(sourceId);
    }

    @Nullable
    @Override
    public SourceIdT getModuleId(URI sourceUrl) {
      return myModuleUriProvider.getModuleId(sourceUrl);
    }

    @Override
    public boolean needsCaching(GlobalReferable def, Definition typechecked) {
      return true;
    }

    @Override
    public @Nullable String getIdFor(GlobalReferable referable) {
      return getNameIdFor(mySrcInfoProvider, referable);
    }

    @Override
    public @Nonnull GlobalReferable getFromId(SourceIdT sourceId, String id) {
      Pair<Precedence, List<String>> name = fullNameFromNameId(id);
      Scope scope = myScopeProvider.forModule(sourceId.getModulePath());
      if (scope == null) {
        throw new IllegalStateException("Required cache is not loaded");
      }
      Referable res = Scope.Utils.resolveName(scope, name.proj2);
      if (res instanceof GlobalReferable) {
        return (GlobalReferable) res;
      }
      throw new IllegalArgumentException("Definition does not exit");
    }

    @Override
    public void registerCachedDefinition(SourceIdT sourceId, String id, GlobalReferable parent) {
      Scope sourceScope = mySourceModuleScopeProvider.forModule(sourceId.getModulePath());
      if (sourceScope == null) {
        Pair<Precedence, List<String>> name = fullNameFromNameId(id);
        GlobalReferable res = myScopeProvider.registerDefinition(sourceId.getModulePath(), name.proj2, name.proj1, parent);
        mySrcInfoProvider.myCacheSrcInfoProvider.registerDefinition(res, new FullName(name.proj2), sourceId);
      }
    }
  }


  public static String getNameIdFor(FullNameProvider fullNameProvider, GlobalReferable referable) {
    Precedence precedence = referable.getPrecedence();
    char fixityChar = precedence.isInfix ? 'i' : 'n';
    final char assocChr;
    switch (precedence.associativity) {
      case LEFT_ASSOC:
        assocChr = 'l';
        break;
      case RIGHT_ASSOC:
        assocChr = 'r';
        break;
      default:
        assocChr = 'n';
    }
    return "" + fixityChar + assocChr + precedence.priority + ';' + String.join(" ", fullNameProvider.fullNameFor(referable).toList());
  }

  public static Pair<Precedence, List<String>> fullNameFromNameId(String s) {
    boolean isInfix = s.charAt(0) == 'i';
    final Precedence.Associativity assoc;
    switch (s.charAt(1)) {
      case 'l':
        assoc = Precedence.Associativity.LEFT_ASSOC;
        break;
      case 'r':
        assoc = Precedence.Associativity.RIGHT_ASSOC;
        break;
      default:
        assoc = Precedence.Associativity.NON_ASSOC;
    }

    int sepIndex = s.indexOf(';');
    final byte priority = Byte.parseByte(s.substring(2, sepIndex));
    return new Pair<>(new Precedence(assoc, priority, isInfix), Arrays.asList(s.substring(sepIndex + 1).split(" ")));
  }
}
