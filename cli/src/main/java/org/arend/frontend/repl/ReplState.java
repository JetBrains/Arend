package org.arend.frontend.repl;

import org.arend.core.expr.Expression;
import org.arend.error.ListErrorReporter;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.GeneralError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.extImpl.DefinitionRequester;
import org.arend.frontend.ConcreteReferableProvider;
import org.arend.frontend.FileLibraryResolver;
import org.arend.frontend.PositionComparator;
import org.arend.frontend.parser.ArendParser;
import org.arend.frontend.parser.BuildVisitor;
import org.arend.library.Library;
import org.arend.library.LibraryDependency;
import org.arend.library.LibraryManager;
import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.naming.reference.FullModuleReferable;
import org.arend.naming.reference.converter.IdReferableConverter;
import org.arend.naming.resolving.visitor.DefinitionResolveNameVisitor;
import org.arend.naming.resolving.visitor.ExpressionResolveNameVisitor;
import org.arend.naming.scope.CachingScope;
import org.arend.naming.scope.MergeScope;
import org.arend.naming.scope.Scope;
import org.arend.naming.scope.ScopeFactory;
import org.arend.prelude.PreludeLibrary;
import org.arend.prelude.PreludeResourceLibrary;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.FileGroup;
import org.arend.typechecking.LibraryArendExtensionProvider;
import org.arend.typechecking.SimpleTypecheckerState;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.instance.provider.InstanceProviderSet;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.typechecking.visitor.DefinitionTypechecker;
import org.arend.typechecking.visitor.SyntacticDesugarVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Paths;
import java.util.*;

public class ReplState {
  private final TypecheckerState myTypecheckerState = new SimpleTypecheckerState();
  private final List<GeneralError> myErrorList = new ArrayList<>();
  private final ListErrorReporter myErrorReporter = new ListErrorReporter(myErrorList);
  private final FileLibraryResolver myLibraryResolver = new FileLibraryResolver(new ArrayList<>(), myTypecheckerState, System.err::println);
  private final LibraryManager myLibraryManager = new LibraryManager(myLibraryResolver, new InstanceProviderSet(), myErrorReporter, myErrorReporter, DefinitionRequester.INSTANCE);
  private final TypecheckingOrderingListener myTypechecking = new TypecheckingOrderingListener(myLibraryManager.getInstanceProviderSet(), myTypecheckerState, ConcreteReferableProvider.INSTANCE, IdReferableConverter.INSTANCE, myErrorReporter, PositionComparator.INSTANCE, new LibraryArendExtensionProvider(myLibraryManager));
  private final ReplLibrary myReplLibrary = new ReplLibrary(myTypecheckerState);
  private final PrettyPrinterConfig myPpConfig = PrettyPrinterConfig.DEFAULT;
  private final List<Scope> myMergedScopes = new ArrayList<>();
  private final MergeScope myScope = new MergeScope(myMergedScopes);

  public static final List<String> definitionEvidence = Arrays.asList(
      "\\import", "\\open", "\\use", "\\func", "\\sfunc", "\\lemma",
      "\\data", "\\module", "\\meta", "\\instance", "\\class");

  public ReplState() {
    var preludeLibrary = new PreludeResourceLibrary(myTypecheckerState);
    if (!myLibraryManager.loadLibrary(preludeLibrary, myTypechecking)) {
      throw new IllegalStateException("[FATAL] Failed to load Prelude");
    }
    myReplLibrary.addDependency(new LibraryDependency(preludeLibrary.getName()));
    myReplLibrary.setGroup(new FileGroup(new FullModuleReferable(ReplLibrary.replModulePath), Collections.emptyList(), Collections.emptyList()));
    myMergedScopes.add(PreludeLibrary.getPreludeScope());
    loadReplLibrary();
  }

  private void loadReplLibrary() throws IllegalStateException {
    if (!myLibraryManager.loadLibrary(myReplLibrary, myTypechecking))
      throw new IllegalStateException("[FATAL] Failed to load the REPL virtual library");
  }

  private boolean loadLibrary(Library library) {
    if (!myLibraryManager.loadLibrary(library, myTypechecking)) return false;
    myLibraryManager.registerDependency(myReplLibrary, library);
    return true;
  }

  public void runRepl() {
    var scanner = new Scanner(System.in);
    while (true) {
      System.out.print("\u03bb ");
      System.out.flush();
      String line = scanner.nextLine();
      if (line.startsWith(":load"))
        actionLoad(line.substring(":load".length()));
      else if (line.startsWith(":l"))
        actionLoad(line.substring(":l".length()));
      else if (line.startsWith(":type"))
        actionType(line.substring(":type".length()));
      else if (line.startsWith(":t"))
        actionType(line.substring(":t".length()));
      else if (line.startsWith(":quit") || line.equals(":q"))
        break;
      else if (line.startsWith(":nf")) {
        var result = checkExpr(line.substring(":nf".length()));
        if (result == null) continue;
        System.out.println(result.expression.normalize(NormalizationMode.NF));
      } else if (line.startsWith(":whnf")) {
        var result = checkExpr(line.substring(":whnf".length()));
        if (result == null) continue;
        System.out.println(result.expression.normalize(NormalizationMode.WHNF));
      } else if (line.startsWith(":")) {
        System.err.println("[ERROR] Unrecognized command: " + line.substring(1) + ".");
      } else if (definitionEvidence.stream().anyMatch(line::contains)) {
        var group = parseStatements(line);
        if (group == null) continue;
        ModuleScopeProvider moduleScopeProvider = myReplLibrary.getModuleScopeProvider();
        Scope scope = CachingScope.make(ScopeFactory.forGroup(group, moduleScopeProvider));
        myMergedScopes.add(scope);
        new DefinitionResolveNameVisitor(ConcreteReferableProvider.INSTANCE, myErrorReporter)
            .resolveGroupWithTypes(group, null, myScope);
        if (checkErrors()) {
          myMergedScopes.remove(scope);
          continue;
        }
        myLibraryManager.getInstanceProviderSet().collectInstances(group,
            CachingScope.make(ScopeFactory.parentScopeForGroup(group, moduleScopeProvider, true)),
            ConcreteReferableProvider.INSTANCE, null);
        if (checkErrors()) {
          myMergedScopes.remove(scope);
          continue;
        }
        if (!myTypechecking.typecheckModules(Collections.singletonList(group), null)) {
          checkErrors();
          myMergedScopes.remove(scope);
        }
      } else if (!line.isBlank()) {
        var result = checkExpr(line);
        if (result == null) continue;
        System.out.println(result.expression);
      }
    }
  }

  private @Nullable FileGroup parseStatements(String line) {
    var fileGroup = buildVisitor().visitStatements(parse(line).statements());
    if (fileGroup != null) fileGroup.setModuleScopeProvider(myReplLibrary.getModuleScopeProvider());
    if (checkErrors()) return null;
    return fileGroup;
  }

  private void actionType(String line) {
    var result = checkExpr(line);
    if (result == null) return;
    Expression type = result.expression.getType();
    System.out.println(type == null ? "Cannot synthesize a type, sorry." : type);
  }

  private void actionLoad(String text) {
    var libPath = Paths.get(text);
    if (!loadLibrary(myLibraryResolver.registerLibrary(libPath)))
      System.err.println("[ERROR] Failed to load the library specified.");
  }

  private @Nullable TypecheckingResult checkExpr(@NotNull String text) {
    var expr = preprocessExpr(text);
    if (expr == null || checkErrors()) return null;
    var result = typeChecker().checkExpr(expr, null);
    return checkErrors() ? null : result;
  }

  @NotNull
  private CheckTypeVisitor typeChecker() {
    return new CheckTypeVisitor(myTypecheckerState, myErrorReporter, null, null);
  }

  private @Nullable Concrete.Expression preprocessExpr(@NotNull String text) {
    var parser = parse(text);
    if (checkErrors()) return null;
    Concrete.Expression expr = buildVisitor().visitExpr(parser.expr());
    if (checkErrors()) return null;
    expr = expr
        .accept(new ExpressionResolveNameVisitor(ConcreteReferableProvider.INSTANCE,
            myScope, Collections.emptyList(), myErrorReporter, null), null)
        .accept(new SyntacticDesugarVisitor(myErrorReporter), null);
    if (checkErrors()) return null;
    return expr;
  }

  /**
   * @return true if there is error(s).
   */
  private boolean checkErrors() {
    for (GeneralError error : myErrorList)
      (error.isSevere() ? System.err : System.out)
          .println(error.getDoc(myPpConfig));
    boolean hasErrors = !myErrorList.isEmpty();
    myErrorList.clear();
    return hasErrors;
  }

  private @NotNull BuildVisitor buildVisitor() {
    return new BuildVisitor(ReplLibrary.replModulePath, myErrorReporter);
  }

  private @NotNull ArendParser parse(String line) {
    return ReplUtils.createParser(line, ReplLibrary.replModulePath, myErrorReporter);
  }

  public static void main(String... args) {
    new ReplState().runRepl();
  }
}
