package org.arend.term.prettyprint;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.DefCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.ReferenceExpression;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.VoidExpressionVisitor;
import org.arend.ext.concrete.ConcreteFactory;
import org.arend.ext.error.GeneralError;
import org.arend.ext.prettyprinting.DefinitionRenamer;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.PrettyPrinterFlag;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.variable.Variable;
import org.arend.extImpl.ConcreteFactoryImpl;
import org.arend.naming.reference.LocalReferable;
import org.arend.naming.reference.TCDefReferable;
import org.arend.term.concrete.BaseConcreteExpressionVisitor;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.GoalError;
import org.arend.typechecking.error.local.SolveEquationError;
import org.arend.typechecking.error.local.inference.ArgInferenceError;
import org.arend.typechecking.error.local.inference.FunctionArgInferenceError;
import org.arend.typechecking.error.local.inference.InstanceInferenceError;
import org.arend.typechecking.instance.pool.GlobalInstancePool;
import org.arend.typechecking.instance.pool.LocalInstancePool;
import org.arend.typechecking.instance.provider.InstanceProvider;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

final public class MinimizedRepresentation {
    private MinimizedRepresentation() {
    }

    /**
     * Converts {@code expressionToPrint} to concrete, inserting as little additional information (like implicit arguments)
     * as possible. Resulting concrete expression is intended to be type checkable.
     */
    public static @NotNull Concrete.Expression generateMinimizedRepresentation(
            @NotNull Expression expressionToPrint,
            @Nullable InstanceProvider instanceProvider,
            @Nullable DefinitionRenamer definitionRenamer) {
        PrettyPrinterConfig verboseConfig;
        PrettyPrinterConfig emptyConfig;
        {
            var configs = getConfigs(definitionRenamer);
            verboseConfig = configs.proj1;
            emptyConfig = configs.proj2;
        }
        var verboseRepresentation = ToAbstractVisitor.convertExtended(expressionToPrint, verboseConfig, null);
        Map<Variable, LocalReferable> referableMapping = verboseRepresentation.getFreeVariables();
        var emptyRepresentation = ToAbstractVisitor.convertExtended(expressionToPrint, emptyConfig, verboseRepresentation.getFreeVariables());
        for (Variable binding : verboseRepresentation.getFreeVariables().keySet().toArray(new Variable[0])) {
            if (binding instanceof Binding && ((Binding) binding).getTypeExpr() != null) {
                ToAbstractVisitor.convertExtended(((Binding) binding).getTypeExpr(), verboseConfig, verboseRepresentation.getFreeVariables());
            }
        }
        List<GeneralError> errorsCollector = new ArrayList<>();
        Map<ArendRef, Expression> refToType = new LinkedHashMap<>();
        ExpressionVisitor<Void, Void> resolveVariablesVisitor = new VoidExpressionVisitor<>() {
            @Override
            public Void visitReference(ReferenceExpression expr, Void params) {
                if (referableMapping.containsKey(expr.getBinding())) {
                    expr.getType().accept(this, null);
                    refToType.putIfAbsent(referableMapping.get(expr.getBinding()), expr.getBinding().getTypeExpr());
                }
                return null;
            }
        };
        expressionToPrint.accept(resolveVariablesVisitor, null);


        var concreteFactory = new ConcreteFactoryImpl(null);
        List<Concrete.LetClause> clauses =
                refToType
                        .entrySet()
                        .stream()
                        .map(entry -> (Concrete.LetClause) concreteFactory.letClause(
                                entry.getKey(),
                                List.of(),
                                ToAbstractVisitor.convertExtended(entry.getValue(), verboseConfig, referableMapping).getConvertedExpression(),
                                concreteFactory.goal())
                        )
                        .collect(Collectors.toList());

        var checkTypeVisitor = new CheckTypeVisitor(error -> {
            if (!(error instanceof GoalError))
                errorsCollector.add(error);
        }, null, null);
        checkTypeVisitor.setInstancePool(new GlobalInstancePool(instanceProvider, checkTypeVisitor, new LocalInstancePool(checkTypeVisitor)));

        int limit = 50;
        while (true) {
            var result = tryFixError(checkTypeVisitor, verboseRepresentation.getConvertedExpression(), emptyRepresentation.getConvertedExpression(), clauses, concreteFactory, errorsCollector);
            if (result) {
                return emptyRepresentation.getConvertedExpression();
            }
            --limit;
            if (limit == 0) {
                throw new AssertionError("Minimization of expression (" + expressionToPrint + ") is likely diverged. Please report it to maintainers.");
            }
        }
    }

    private static Pair<PrettyPrinterConfig, PrettyPrinterConfig> getConfigs(DefinitionRenamer definitionRenamer) {
        return new Pair<>(new PrettyPrinterConfig() {
            @Override
            public @NotNull EnumSet<PrettyPrinterFlag> getExpressionFlags() {
                return EnumSet.of(PrettyPrinterFlag.SHOW_TYPES_IN_LAM, PrettyPrinterFlag.SHOW_CASE_RESULT_TYPE, PrettyPrinterFlag.SHOW_CON_PARAMS, PrettyPrinterFlag.SHOW_BIN_OP_IMPLICIT_ARGS, PrettyPrinterFlag.SHOW_COERCE_DEFINITIONS, PrettyPrinterFlag.SHOW_GLOBAL_FIELD_INSTANCE, PrettyPrinterFlag.SHOW_IMPLICIT_ARGS, PrettyPrinterFlag.SHOW_LOCAL_FIELD_INSTANCE, PrettyPrinterFlag.SHOW_TUPLE_TYPE);
            }

            @Override
            public @Nullable DefinitionRenamer getDefinitionRenamer() {
                return definitionRenamer;
            }
        }, new PrettyPrinterConfig() {
            @Override
            public @NotNull EnumSet<PrettyPrinterFlag> getExpressionFlags() {
                return EnumSet.noneOf(PrettyPrinterFlag.class);
            }

            @Override
            public @Nullable DefinitionRenamer getDefinitionRenamer() {
                return definitionRenamer;
            }
        });
    }

    private static boolean tryFixError(CheckTypeVisitor checkTypeVisitor, Concrete.Expression completeConcrete, Concrete.Expression minimizedConcrete, List<Concrete.LetClause> clauses, ConcreteFactoryImpl concreteFactory, List<GeneralError> errorsCollector) {
        var finalizedConcrete = (Concrete.LetExpression) concreteFactory.letExpr(true, false, clauses, minimizedConcrete);
        checkTypeVisitor.finalCheckExpr(finalizedConcrete, null);
        if (!errorsCollector.isEmpty()) {
            minimizedConcrete.accept(new ErrorFixingConcreteExpressionVisitor(errorsCollector, concreteFactory), new ConcreteTree(minimizedConcrete, completeConcrete));
            return false;
        } else {
            return true;
        }
    }
}

final class ConcreteTree {
    private final List<Concrete.SourceNode> myActual;

    private final List<Concrete.SourceNode> myComplete;

    public ConcreteTree(Concrete.SourceNode actual, Concrete.SourceNode complete) {
        myActual = new ArrayList<>();
        myActual.add(actual);
        myComplete = new ArrayList<>();
        myComplete.add(complete);
    }

    public List<Concrete.SourceNode> getActual() {
        return myActual;
    }

    public List<Concrete.SourceNode> getComplete() {
        return myComplete;
    }
}

class MyException extends RuntimeException {
    private final Concrete.Expression expression;

    MyException(Concrete.Expression expression) {
        this.expression = expression;
    }

    public Concrete.Expression getExpression() {
        return expression;
    }
}

class ErrorFixingConcreteExpressionVisitor extends BaseConcreteExpressionVisitor<ConcreteTree> {

    private final List<GeneralError> myErrors;
    private final ConcreteFactory myFactory;

    public ErrorFixingConcreteExpressionVisitor(List<GeneralError> myErrors, ConcreteFactory myFactory) {
        this.myErrors = myErrors;
        this.myFactory = myFactory;
    }

    private static <T extends Concrete.SourceNode> T getLastComplete(ConcreteTree tree, Class<T> clazz) {
        return clazz.cast(tree.getComplete().get(tree.getComplete().size() - 1));
    }

    private <T, R extends Concrete.Expression> void withState(ConcreteTree tree, T actual, T complete, Function<T, @Nullable R> picker) {
        var newActual = picker.apply(actual);
        var newComplete = picker.apply(complete);
        tree.getActual().add(newActual);
        tree.getComplete().add(newComplete);
        try {
            if (newActual != null) {
                newActual.accept(this, tree);
            }
        } finally {
            tree.getComplete().remove(tree.getComplete().size() - 1);
            tree.getActual().remove(tree.getActual().size() - 1);
        }

    }


    private <T extends Concrete.Parameter> void visitParameters(List<T> actualParameters, List<T> completeParameters, ConcreteTree tree) {
        for (int i = 0; i < actualParameters.size(); ++i) {
            final var j = i;
            withState(tree, actualParameters, completeParameters, ts -> {
                var param = ts.get(j);
                if (param instanceof Concrete.TypeParameter) {
                    return ((Concrete.TypeParameter) param).type;
                } else {
                    return null;
                }
            });
        }
    }

    @Override
    public Concrete.Expression visitPi(Concrete.PiExpression expr, ConcreteTree params) {
        var verboseExpr = getLastComplete(params, Concrete.PiExpression.class);
        visitParameters(expr.getParameters(), verboseExpr.getParameters(), params);
        withState(params, expr, verboseExpr, Concrete.PiExpression::getCodomain);
        return expr;
    }

    @Override
    public Concrete.Expression visitApp(Concrete.AppExpression expr, ConcreteTree params) {
        Concrete.AppExpression verboseExpr = (Concrete.AppExpression) params.getComplete().get(params.getComplete().size() - 1);
        params.getActual().add(expr.getFunction());
        params.getComplete().add(verboseExpr.getFunction());
        Concrete.Expression newFunction;
        try {
            newFunction = expr.getFunction().accept(this, params);
        } catch (MyException e) {
            expr.getArguments().clear();
            expr.setFunction(e.getExpression());
            return expr;
        } finally {
            params.getActual().remove(params.getActual().size() - 1);
            params.getComplete().remove(params.getComplete().size() - 1);
        }

        var allArguments = verboseExpr.getArguments().iterator();
        for (var argument : expr.getArguments()) {
            var currentArgument = allArguments.next();
            while (currentArgument.isExplicit() != argument.isExplicit()) {
                currentArgument = allArguments.next();
            }
            params.getActual().add(argument.expression);
            params.getComplete().add(currentArgument.expression);
            argument.expression = argument.expression.accept(this, params);
            params.getActual().remove(params.getActual().size() - 1);
            params.getComplete().remove(params.getComplete().size() - 1);
        }
        expr.setFunction(newFunction);

        return expr;
    }

    private static GeneralError findMostImportantError(List<GeneralError> errors) {
        return errors
                .stream()
                .filter(err -> err instanceof InstanceInferenceError)
                .findFirst()
                .orElse(errors
                        .stream()
                        .filter(err -> err instanceof ArgInferenceError)
                        .findFirst()
                        .orElse(errors.get(0))
                );
    }

    @Override
    public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, ConcreteTree params) {
        var errorList = myErrors.stream().filter(err -> err.getCauseSourceNode() == expr).collect(Collectors.toList());
        if (errorList.isEmpty()) {
            return expr;
        }
        var targetError = findMostImportantError(errorList);
        myErrors.clear();
        if (targetError instanceof InstanceInferenceError) {
            var definition = ((TCDefReferable) expr.getReferent()).getTypechecked();
            var args = new ArrayList<Concrete.Argument>();
            var fullIterator = new ArgumentMappingIterator(definition, (Concrete.AppExpression) params.getComplete().get(params.getComplete().size() - 2));
            var incompleteIterator = new ArgumentMappingIterator(definition, (Concrete.AppExpression) params.getActual().get(params.getActual().size() - 2));
            var inserted = false;
            while (fullIterator.hasNext()) {
                var fullResult = fullIterator.next();
                var incompleteResult = incompleteIterator.next();
                var param = fullResult.proj1;
                if (param instanceof DependentLink && ((DependentLink) param).getType() instanceof DefCallExpression && ((DefCallExpression) ((DependentLink) param).getType()).getDefinition() == ((InstanceInferenceError) targetError).classRef.getTypechecked()) {
                    args.add((Concrete.Argument) fullResult.proj2);
                    inserted = true;
                } else if (incompleteResult.proj2 != null) {
                    args.add((Concrete.Argument) incompleteResult.proj2);
                } else if (!inserted) {
                    args.add((Concrete.Argument) myFactory.arg(myFactory.hole(), fullResult.proj2.isExplicit()));
                }
            }
            throw new MyException((Concrete.Expression) myFactory.app(expr, args));
        } else if (targetError instanceof FunctionArgInferenceError) {
            var args = new ArrayList<Concrete.Argument>();
            var fullIterator = new ArgumentMappingIterator(((FunctionArgInferenceError) targetError).definition, (Concrete.AppExpression) params.getComplete().get(params.getComplete().size() - 2));
            var incompleteIterator = new ArgumentMappingIterator(((FunctionArgInferenceError) targetError).definition, (Concrete.AppExpression) params.getActual().get(params.getActual().size() - 2));
            var inserted = false;
            var i = 1;
            while (fullIterator.hasNext()) {
                var fullArg = fullIterator.next().proj2;
                var incompleteArg = incompleteIterator.next().proj2;
                if (i == ((FunctionArgInferenceError) targetError).index) {
                    args.add((Concrete.Argument) fullArg);
                    inserted = true;
                } else if (incompleteArg != null) {
                    args.add((Concrete.Argument) incompleteArg);
                } else if (!inserted) {
                    args.add((Concrete.Argument) myFactory.arg(myFactory.hole(), fullArg.isExplicit()));
                }
                i += 1;
            }
            throw new MyException((Concrete.Expression) myFactory.app(expr, args));
        } else if (targetError instanceof SolveEquationError) {
            var args = new ArrayList<Concrete.Argument>();
            var parentArgs = (Concrete.AppExpression) params.getActual().get(params.getActual().size() - 2);
            var parentProperArgs = (Concrete.AppExpression) params.getComplete().get(params.getComplete().size() - 2);
            var parentIterator = parentArgs.getArguments().iterator();
            var parentProperIterator = parentProperArgs.getArguments().iterator();
            var currentActualArg = parentIterator.next();
            while (parentProperIterator.hasNext()) {
                var currentProperArg = parentProperIterator.next();
                if (currentActualArg == null || (currentProperArg.isExplicit() == currentActualArg.isExplicit())) {
                    args.add(currentActualArg);
                    currentActualArg = parentIterator.hasNext() ? parentIterator.next() : null;
                } else {
                    args.add(currentProperArg);
                }
            }
            throw new MyException((Concrete.Expression) myFactory.app(expr, args));
        }
        return expr;
    }
}
