package org.arend.repl;

import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.ToAbstractVisitor;
import org.arend.error.ListErrorReporter;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.GeneralError;
import org.arend.ext.module.ModulePath;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.reference.Precedence;
import org.arend.extImpl.DefinitionRequester;
import org.arend.library.Library;
import org.arend.library.LibraryManager;
import org.arend.library.SourceLibrary;
import org.arend.library.resolver.LibraryResolver;
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
import org.arend.prelude.PreludeLibrary;
import org.arend.prelude.PreludeResourceLibrary;
import org.arend.repl.action.*;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.FileGroup;
import org.arend.term.prettyprint.PrettyPrintVisitor;
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

import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;

public abstract class ReplState implements ReplApi {
  private final List<Scope> myMergedScopes = new ArrayList<>();
  private final List<ReplAction> myActions = new ArrayList<>();
  private final Set<ModulePath> myModules;
  private final MergeScope myScope = new MergeScope(myMergedScopes);
  private final SourceLibrary myReplLibrary;
  private final TypecheckerState myTypecheckerState;
  private final ConcreteProvider myConcreteProvider;
  private final TypecheckingOrderingListener myTypechecking;
  protected final @NotNull PrettyPrinterConfig myPpConfig = PrettyPrinterConfig.DEFAULT;
  protected final @NotNull ListErrorReporter myErrorReporter;
  protected final @NotNull LibraryManager myLibraryManager;

  private final @NotNull PrintStream myStdout;
  private final @NotNull PrintStream myStderr;

  public ReplState(@NotNull ListErrorReporter listErrorReporter,
                   @NotNull LibraryResolver libraryResolver,
                   @NotNull ConcreteProvider concreteProvider,
                   @NotNull PartialComparator<TCReferable> comparator,
                   @NotNull PrintStream stdout,
                   @NotNull PrintStream stderr,
                   @NotNull Set<ModulePath> modules,
                   @NotNull SourceLibrary replLibrary,
                   @NotNull TypecheckerState typecheckerState) {
    myErrorReporter = listErrorReporter;
    myConcreteProvider = concreteProvider;
    myModules = modules;
    myTypecheckerState = typecheckerState;
    myStdout = stdout;
    myStderr = stderr;
    myReplLibrary = replLibrary;
    var instanceProviders = new InstanceProviderSet();
    myLibraryManager = new LibraryManager(libraryResolver, instanceProviders, myErrorReporter, myErrorReporter, DefinitionRequester.INSTANCE);
    myTypechecking = new TypecheckingOrderingListener(instanceProviders, myTypecheckerState, myConcreteProvider, IdReferableConverter.INSTANCE, myErrorReporter, comparator, new LibraryArendExtensionProvider(myLibraryManager));
  }

  private void loadPreludeLibrary() {
    if (!loadLibrary(new PreludeResourceLibrary(myTypecheckerState)))
      eprintln("[FATAL] Failed to load Prelude");
    else myMergedScopes.add(PreludeLibrary.getPreludeScope());
  }

  private void loadReplLibrary() {
    if (!myLibraryManager.loadLibrary(myReplLibrary, myTypechecking))
      eprintln("[FATAL] Failed to load the REPL virtual library");
  }

  @Override
  public final boolean loadLibrary(@NotNull Library library) {
    if (!myLibraryManager.loadLibrary(library, myTypechecking)) return false;
    myLibraryManager.registerDependency(myReplLibrary, library);
    return true;
  }

  public void runRepl(@NotNull InputStream inputStream) {
    loadPreludeLibrary();
    loadReplLibrary();
    initialize();

    var scanner = new Scanner(inputStream);
    prompt();
    while (scanner.hasNext()) {
      String line = scanner.nextLine();
      if (line.startsWith(":quit") || line.equals(":q")) break;
      boolean actionExecuted = false;
      for (ReplAction action : myActions)
        if (action.isApplicable(line)) {
          action.invoke(line, this, scanner);
          actionExecuted = true;
          break;
        }
      if (!actionExecuted && line.startsWith(":")) {
        eprintln("[ERROR] Unrecognized command: " + line.substring(1) + ".");
      }
      prompt();
    }
  }

  @Override
  public @Nullable Scope loadModule(@NotNull ModulePath modulePath) {
    boolean isLoadedBefore = myModules.add(modulePath);
    myLibraryManager.reload(myTypechecking);
    if (checkErrors()) {
      myModules.remove(modulePath);
      return null;
    }
    if (isLoadedBefore) {
      Scope scope = getAvailableModuleScopeProvider().forModule(modulePath);
      if (scope != null) removeScope(scope);
    }
    myTypechecking.typecheckLibrary(myReplLibrary);
    return getAvailableModuleScopeProvider().forModule(modulePath);
  }

  @Override
  public boolean unloadModule(@NotNull ModulePath modulePath) {
    boolean isLoadedBefore = myModules.remove(modulePath);
    if (isLoadedBefore) {
      myLibraryManager.reload(myTypechecking);
      Scope scope = getAvailableModuleScopeProvider().forModule(modulePath);
      if (scope != null) removeScope(scope);
      myReplLibrary.onGroupLoaded(modulePath, null, true);
      myTypechecking.typecheckLibrary(myReplLibrary);
    }
    return isLoadedBefore;
  }

  @Override
  public @NotNull ModuleScopeProvider getAvailableModuleScopeProvider() {
    return myLibraryManager.getAvailableModuleScopeProvider(myReplLibrary);
  }

  public void prompt() {
    print("\u03bb ");
  }

  protected abstract @Nullable FileGroup parseStatements(String line);

  protected abstract @Nullable Concrete.Expression parseExpr(@NotNull String text);

  @Override
  public void checkStatements(@NotNull String line) {
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
    myLibraryManager.getInstanceProviderSet().collectInstances(group,
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

  protected void initialize() {
    registerAction(DefaultAction.INSTANCE);
    registerAction(new ShowTypeCommand("type"));
    registerAction(new ShowTypeCommand("t"));
    registerAction(new LoadLibraryCommand("lib"));
    registerAction(new LoadModuleCommand("load"));
    registerAction(new LoadModuleCommand.ReloadModuleCommand("reload"));
    registerAction(new LoadModuleCommand("l"));
    registerAction(new LoadModuleCommand.ReloadModuleCommand("r"));
    registerAction(new UnloadModuleCommand("unload"));
    registerAction(new ListLoadedModulesAction("modules"));
    registerAction(new NormalizeCommand("whnf", NormalizationMode.WHNF));
    registerAction(new NormalizeCommand("nf", NormalizationMode.NF));
    registerAction(new NormalizeCommand("rnf", NormalizationMode.RNF));
    registerAction(new HelpAction("help"));
    registerAction(new HelpAction("?"));
  }

  @Override
  public final void registerAction(@NotNull ReplCommand action) {
    registerAction((ReplAction) action);
  }

  protected final void registerAction(@NotNull ReplAction action) {
    myActions.add(action);
  }

  @Override
  public final boolean unregisterAction(@NotNull ReplAction action) {
    return myActions.remove(action);
  }

  @Override
  public final void clearActions() {
    myActions.clear();
  }

  @Override
  public @NotNull Library getReplLibrary() {
    return myReplLibrary;
  }

  @Override
  public final void addScope(@NotNull Scope scope) {
    myMergedScopes.add(scope);
  }

  @Override
  public final boolean removeScope(@NotNull Scope scope) {
    for (Referable element : scope.getElements())
      if (element instanceof TCReferable)
        myTypecheckerState.reset((TCReferable) element);
    return myMergedScopes.remove(scope);
  }

  @Override
  public void println(Object anything) {
    myStdout.println(anything);
  }

  @Override
  public void print(Object anything) {
    myStdout.print(anything);
    myStdout.flush();
  }

  @Override
  public void eprintln(Object anything) {
    myStderr.println(anything);
    myStderr.flush();
  }

  @Override
  public @NotNull StringBuilder prettyExpr(@NotNull StringBuilder builder, @NotNull Expression expression) {
    var abs = ToAbstractVisitor.convert(expression, myPpConfig);
    abs.accept(new PrettyPrintVisitor(builder, 0), new Precedence(Concrete.Expression.PREC));
    return builder;
  }

  @Override
  public @Nullable TypecheckingResult checkExpr(@NotNull Concrete.Expression expr, @Nullable Expression expectedType) {
    var typechecker = new CheckTypeVisitor(myTypecheckerState, myErrorReporter, null, null);
    var instancePool = new GlobalInstancePool(EmptyInstanceProvider.getInstance(), typechecker);
    typechecker.setInstancePool(instancePool);
    var result = typechecker.checkExpr(expr, expectedType);
    return checkErrors() ? null : result;
  }

  @Override
  public @Nullable Concrete.Expression preprocessExpr(@NotNull String text) {
    var expr = parseExpr(text);
    if (expr == null || checkErrors()) return null;
    expr = expr
        .accept(new ExpressionResolveNameVisitor(myConcreteProvider,
            myScope, Collections.emptyList(), myErrorReporter, null), null)
        .accept(new SyntacticDesugarVisitor(myErrorReporter), null);
    if (checkErrors()) return null;
    return expr;
  }

  @Override
  public final boolean checkErrors() {
    var errorList = myErrorReporter.getErrorList();
    for (GeneralError error : errorList)
      (error.isSevere() ? myStderr : myStdout).println(error.getDoc(myPpConfig));
    boolean hasErrors = !errorList.isEmpty();
    errorList.clear();
    return hasErrors;
  }

  private final class HelpAction extends ReplCommand {
    private HelpAction(@NotNull String command) {
      super(command);
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String description() {
      return "Show this message";
    }

    @Override
    protected void doInvoke(@NotNull String line, @NotNull ReplApi api, @NotNull Scanner scanner) {
      IntSummaryStatistics statistics = myActions.stream()
          .filter(action -> action instanceof ReplCommand)
          .mapToInt(action -> ((ReplCommand) action).commandWithColon.length())
          .summaryStatistics();
      int maxWidth = Math.min(statistics.getMax(), 8) + 1;
      println("There are " + statistics.getCount() + " action(s) available.");
      for (ReplAction action : myActions) {
        var description = action.description();
        if (description == null) continue;
        if (action instanceof ReplCommand) {
          String command = ((ReplCommand) action).commandWithColon;
          println(command + " ".repeat(maxWidth - command.length()) + description);
        } else println(description);
      }
    }
  }
}
