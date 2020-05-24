package org.arend.repl;

import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.ScopeDefinitionRenamer;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.GeneralError;
import org.arend.ext.error.ListErrorReporter;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.reference.Precedence;
import org.arend.extImpl.DefinitionRequester;
import org.arend.library.Library;
import org.arend.library.LibraryManager;
import org.arend.library.resolver.LibraryResolver;
import org.arend.module.FullModulePath;
import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.reference.converter.IdReferableConverter;
import org.arend.naming.resolving.visitor.DefinitionResolveNameVisitor;
import org.arend.naming.resolving.visitor.ExpressionResolveNameVisitor;
import org.arend.naming.scope.CachingScope;
import org.arend.naming.scope.MergeScope;
import org.arend.naming.scope.Scope;
import org.arend.naming.scope.ScopeFactory;
import org.arend.repl.action.*;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.Group;
import org.arend.term.prettyprint.PrettyPrintVisitor;
import org.arend.term.prettyprint.ToAbstractVisitor;
import org.arend.typechecking.LibraryArendExtensionProvider;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.instance.pool.GlobalInstancePool;
import org.arend.typechecking.instance.provider.EmptyInstanceProvider;
import org.arend.typechecking.instance.provider.InstanceProviderSet;
import org.arend.typechecking.order.PartialComparator;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.arend.typechecking.provider.ConcreteProvider;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.typechecking.visitor.SyntacticDesugarVisitor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public abstract class Repl {
  public static final @NotNull FullModulePath replModulePath = new FullModulePath(null, FullModulePath.LocationKind.SOURCE, Collections.singletonList("Repl"));
  private @Nullable NormalizationMode myMode = NormalizationMode.RNF;

  protected final List<Scope> myMergedScopes = new ArrayList<>();
  private final List<ReplHandler> myHandlers = new ArrayList<>();
  private final MergeScope myMergeScope = new MergeScope(myMergedScopes);
  private final ConcreteProvider myConcreteProvider;
  protected @NotNull Scope myScope = myMergeScope;
  protected final @NotNull TypecheckingOrderingListener myTypechecking;
  protected final @NotNull TypecheckerState myTypecheckerState;
  protected final @NotNull PrettyPrinterConfig myPpConfig = new PrettyPrinterConfig() {
    @Override
    public @NotNull ScopeDefinitionRenamer getDefinitionRenamer() {
      return new ScopeDefinitionRenamer(myScope);
    }

    @Override
    public @Nullable NormalizationMode getNormalizationMode() {
      return myMode;
    }
  };
  protected final @NotNull ListErrorReporter myErrorReporter;
  protected final @NotNull LibraryManager myLibraryManager;

  protected static @NotNull LibraryManager libraryManager(@NotNull ListErrorReporter listErrorReporter, @NotNull LibraryResolver libraryResolver, @NotNull InstanceProviderSet instanceProviders) {
    return new LibraryManager(libraryResolver, instanceProviders, listErrorReporter, listErrorReporter, DefinitionRequester.INSTANCE);
  }

  public Repl(@NotNull ListErrorReporter listErrorReporter,
              @NotNull LibraryManager libraryManager,
              @NotNull ConcreteProvider concreteProvider,
              @NotNull PartialComparator<TCReferable> comparator,
              @NotNull InstanceProviderSet instanceProviders,
              @NotNull TypecheckerState typecheckerState) {
    myErrorReporter = listErrorReporter;
    myConcreteProvider = concreteProvider;
    myTypecheckerState = typecheckerState;
    myLibraryManager = libraryManager;
    myTypechecking = new TypecheckingOrderingListener(instanceProviders, myTypecheckerState, myConcreteProvider, IdReferableConverter.INSTANCE, myErrorReporter, comparator, new LibraryArendExtensionProvider(myLibraryManager));
  }

  protected abstract void loadLibraries();

  protected final @NotNull List<Referable> getInScopeElements() {
    return myMergeScope.getElements();
  }

  public final void initialize() {
    loadLibraries();
    loadCommands();
    checkErrors();
  }

  /**
   * The function executed per main-loop of the REPL.
   *
   * @param currentLine  the current user input
   * @param lineSupplier in case the command requires more user input,
   *                     use this to acquire more lines
   * @return true if the REPL wants to quit
   */
  public final boolean repl(@NotNull String currentLine, @NotNull Supplier<@NotNull String> lineSupplier) {
    boolean quit = false;
    for (var action : myHandlers)
      if (action.isApplicable(currentLine)) {
        try {
          action.invoke(currentLine, this, lineSupplier);
        } catch (QuitReplException e) {
          quit = true;
        }
        checkErrors();
      }
    return quit;
  }

  protected final boolean typecheckLibrary(@NotNull Library library) {
    return myTypechecking.typecheckLibrary(library);
  }

  public final @NotNull ModuleScopeProvider getAvailableModuleScopeProvider() {
    return module -> {
      for (Library registeredLibrary : myLibraryManager.getRegisteredLibraries()) {
        Scope scope = registeredLibrary.getModuleScopeProvider().forModule(module);
        if (scope != null) return scope;
      }
      return null;
    };
  }

  public @NotNull String prompt() {
    return ">";
  }

  protected abstract @Nullable Group parseStatements(@NotNull String line);

  protected abstract @Nullable Concrete.Expression parseExpr(@NotNull String text);

  public final void checkStatements(@NotNull String line) {
    var group = parseStatements(line);
    if (group == null) return;
    var moduleScopeProvider = getAvailableModuleScopeProvider();
    Scope scope = CachingScope.make(ScopeFactory.forGroup(group, moduleScopeProvider));
    myMergedScopes.add(scope);
    new DefinitionResolveNameVisitor(myConcreteProvider, myErrorReporter)
        .resolveGroupWithTypes(group, null, myScope);
    if (checkErrors()) {
      myMergedScopes.remove(scope);
      return;
    }
    var instanceProviders = myLibraryManager.getInstanceProviderSet();
    if (instanceProviders != null) instanceProviders.collectInstances(group,
        CachingScope.make(ScopeFactory.parentScopeForGroup(group, moduleScopeProvider, true)),
        myConcreteProvider, null);
    if (checkErrors()) {
      myMergedScopes.remove(scope);
      return;
    }
    if (!myTypechecking.typecheckModules(Collections.singletonList(group), null)) {
      checkErrors();
      myMergedScopes.remove(scope);
    }
  }

  protected void loadCommands() {
    myHandlers.add(CodeParsingHandler.INSTANCE);
    myHandlers.add(CommandHandler.INSTANCE);
    registerAction("quit", QuitCommand.INSTANCE);
    registerAction("type", ShowTypeCommand.INSTANCE);
    registerAction("normalize", NormalizeCommand.INSTANCE);
    registerAction("libraries", ShowLoadedLibrariesCommand.INSTANCE);
    registerAction("?", CommandHandler.HELP_COMMAND_INSTANCE);
    registerAction("help", CommandHandler.HELP_COMMAND_INSTANCE);
  }

  public final @Nullable ReplCommand registerAction(@NotNull String name, @NotNull ReplCommand action) {
    return CommandHandler.INSTANCE.commandMap.put(name, action);
  }

  public final @Nullable ReplCommand registerAction(@NotNull String name, @NotNull AliasableCommand action) {
    action.aliases.add(name);
    return registerAction(name, (ReplCommand) action);
  }

  public final @Nullable ReplCommand unregisterAction(@NotNull String name) {
    return CommandHandler.INSTANCE.commandMap.remove(name);
  }

  public final void clearActions() {
    CommandHandler.INSTANCE.commandMap.clear();
  }

  /**
   * Multiplex the scope into the current REPL scope.
   */
  public final void addScope(@NotNull Scope scope) {
    myMergedScopes.add(scope);
  }

  /**
   * Remove a multiplexed scope from the current REPL scope.
   *
   * @return true if there is indeed a scope removed
   */
  public final boolean removeScope(@NotNull Scope scope) {
    for (Referable element : scope.getElements())
      if (element instanceof TCReferable)
        myTypecheckerState.reset((TCReferable) element);
    removeScope(scope.getGlobalSubscopeWithoutOpens());
    return myMergedScopes.remove(scope);
  }

  /**
   * A replacement of {@link System#out#println(Object)} where it uses the
   * output stream of the REPL.
   *
   * @param anything whose {@link Object#toString()} is invoked.
   */
  public void println(Object anything) {
    print(anything);
    print(System.lineSeparator());
  }

  public void println() {
    print(System.lineSeparator());
  }

  public abstract void print(Object anything);

  /**
   * @param toError if true, print to stderr. Otherwise print to stdout
   */
  public void printlnOpt(Object anything, boolean toError) {
    if (toError) eprintln(anything);
    else println(anything);
  }

  /**
   * A replacement of {@link System#err#println(Object)} where it uses the
   * error output stream of the REPL.
   *
   * @param anything whose {@link Object#toString()} is invoked.
   */
  public abstract void eprintln(Object anything);

  public final @NotNull StringBuilder prettyExpr(@NotNull StringBuilder builder, @NotNull Expression expression) {
    var abs = ToAbstractVisitor.convert(expression, myPpConfig);
    abs.accept(new PrettyPrintVisitor(builder, 0), new Precedence(Concrete.Expression.PREC));
    return builder;
  }

  public @Nullable NormalizationMode getNormalizationMode() {
    return myMode;
  }

  public void setNormalizationMode(@Nullable NormalizationMode normalizationMode) {
    this.myMode = normalizationMode;
  }

  /**
   * @param expr input concrete expression.
   * @see Repl#preprocessExpr(String)
   */
  public final @Nullable TypecheckingResult checkExpr(@NotNull Concrete.Expression expr, @Nullable Expression expectedType) {
    var typechecker = new CheckTypeVisitor(myTypecheckerState, myErrorReporter, null, null);
    var instancePool = new GlobalInstancePool(EmptyInstanceProvider.getInstance(), typechecker);
    typechecker.setInstancePool(instancePool);
    var result = typechecker.checkExpr(expr, expectedType);
    return checkErrors() ? null : result;
  }

  /**
   * @see Repl#checkExpr(Concrete.Expression, Expression)
   */
  public final @Nullable Concrete.Expression preprocessExpr(@NotNull String text) {
    var expr = parseExpr(text);
    if (expr == null || checkErrors()) return null;
    expr = expr
        .accept(new ExpressionResolveNameVisitor(myConcreteProvider,
            myScope, new ArrayList<>(), myErrorReporter, null), null)
        .accept(new SyntacticDesugarVisitor(myErrorReporter), null);
    if (checkErrors()) return null;
    return expr;
  }

  private static final List<GeneralError.Level> ERROR_LEVELS = List.of(
      GeneralError.Level.ERROR,
      GeneralError.Level.WARNING,
      GeneralError.Level.GOAL
  );

  /**
   * Check and print errors.
   *
   * @return true if there is error(s).
   */
  public final boolean checkErrors() {
    var errorList = myErrorReporter.getErrorList();
    for (GeneralError error : errorList)
      printlnOpt(error.getDoc(myPpConfig), ERROR_LEVELS.contains(error.level));
    boolean hasErrors = !errorList.isEmpty();
    errorList.clear();
    return hasErrors;
  }

  private static class ShowLoadedLibrariesCommand implements ReplCommand {
    private static final ShowLoadedLibrariesCommand INSTANCE = new ShowLoadedLibrariesCommand();

    private ShowLoadedLibrariesCommand() {
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String description() {
      return "List registered libraries in the REPL";
    }

    @Override
    public void invoke(@NotNull String line, @NotNull Repl api, @NotNull Supplier<@NotNull String> scanner) {
      for (var registeredLibrary : api.myLibraryManager.getRegisteredLibraries()) {
        boolean external = registeredLibrary.isExternal();
        boolean notLoaded = !registeredLibrary.isLoaded();
        var info = external && notLoaded
          ? " (external, not loaded)"
          : external ? " (external)"
          : notLoaded ? " (not loaded)"
          : "";
        api.println(registeredLibrary.getName() + info);
      }
    }
  }
}
