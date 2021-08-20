package org.arend.term.prettyprint;

import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.extImpl.ConcreteFactoryImpl;
import org.arend.term.concrete.BaseConcreteExpressionVisitor;
import org.arend.term.concrete.Concrete;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Simultaneously traverses two concrete expressions, assuming that second concrete expression in parameters
 * may contain some additional information regarding the first one.
 * <p>
 * With default behavior, constructs entirely new concrete expression that equal to concrete the visitor was invoked on.
 */
public abstract class BiConcreteVisitor extends BaseConcreteExpressionVisitor<Concrete.SourceNode> {
    final ConcreteFactoryImpl myFactory;

    public BiConcreteVisitor() {
        myFactory = new ConcreteFactoryImpl(null);
    }

    @Override
    public Concrete.Expression visitApp(Concrete.AppExpression expr, Concrete.SourceNode node) {
        var bigExpr = (Concrete.AppExpression) node;
        var newArguments = new ArrayList<ConcreteArgument>();
        var extendedArguments = bigExpr.getArguments().iterator();
        for (var argument : expr.getArguments()) {
            var currentExtendedArgument = extendedArguments.next();
            while (currentExtendedArgument.isExplicit() != argument.isExplicit()) {
                currentExtendedArgument = extendedArguments.next();
            }
            newArguments.add(myFactory.arg(argument.expression.accept(this, currentExtendedArgument.getExpression()), argument.isExplicit()));
        }
        return (Concrete.Expression) myFactory.app(expr.getFunction().accept(this, bigExpr.getFunction()), newArguments);
    }

    @Override
    public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Concrete.SourceNode params) {
        return new Concrete.ReferenceExpression(expr.getData(), expr.getReferent());
    }

    protected Concrete.Parameter visitParameter(Concrete.Parameter parameter, Concrete.Parameter wideParameter) {
        //noinspection DuplicatedCode
        if (parameter.getType() == null) {
            return (Concrete.Parameter) myFactory.param(parameter.isExplicit(), parameter.getRefList().get(0));
        } else if (wideParameter.getRefList().stream().anyMatch(Objects::nonNull)) {
            return (Concrete.Parameter) myFactory.param(parameter.isExplicit(), parameter.getRefList(), ((Concrete.TypeParameter) parameter).type.accept(this, ((Concrete.TypeParameter) wideParameter).type));
        } else {
            return (Concrete.Parameter) myFactory.param(parameter.isExplicit(), ((Concrete.TypeParameter) parameter).type.accept(this, ((Concrete.TypeParameter) wideParameter).type));
        }
    }

    private List<Concrete.Parameter> visitParameters(List<? extends Concrete.Parameter> parameters, List<? extends Concrete.Parameter> wideParams) {
        assert parameters.size() == wideParams.size();
        var newParams = new ArrayList<Concrete.Parameter>();
        for (int i = 0; i < parameters.size(); ++i) {
            newParams.add(visitParameter(parameters.get(i), wideParams.get(i)));
        }
        return newParams;
    }

    @Override
    public Concrete.Expression visitLam(Concrete.LamExpression expr, Concrete.SourceNode params) {
        var wideExpr = (Concrete.LamExpression) params;
        var newParams = visitParameters(expr.getParameters(), wideExpr.getParameters());
        var newPatterns = new ArrayList<Concrete.Pattern>();
        if (expr instanceof Concrete.PatternLamExpression) {
            for (int i = 0; i < ((Concrete.PatternLamExpression) expr).getPatterns().size(); ++i) {
                var pattern = ((Concrete.PatternLamExpression) expr).getPatterns().get(i);
                if (pattern != null) {
                    newPatterns.add(visitPattern(pattern, ((Concrete.PatternLamExpression) wideExpr).getPatterns().get(i)));
                }
            }
        }
        return Concrete.PatternLamExpression.make(expr.getData(), newParams, newPatterns, expr.getBody().accept(this, wideExpr.getBody()));
    }

    @Override
    public Concrete.Expression visitPi(Concrete.PiExpression expr, Concrete.SourceNode params) {
        var wideExpression = (Concrete.PiExpression) params;
        return (Concrete.Expression) myFactory.pi(visitParameters(expr.getParameters(), wideExpression.getParameters()), expr.getCodomain().accept(this, wideExpression.getCodomain()));
    }

    @Override
    public Concrete.Expression visitUniverse(Concrete.UniverseExpression expr, Concrete.SourceNode params) {
        return (Concrete.Expression) myFactory.universe(expr.getPLevel(), expr.getHLevel());
    }

    @Override
    public Concrete.Expression visitHole(Concrete.HoleExpression expr, Concrete.SourceNode params) {
        return (Concrete.Expression) myFactory.hole();
    }

    @Override
    public Concrete.Expression visitApplyHole(Concrete.ApplyHoleExpression expr, Concrete.SourceNode params) {
        return new Concrete.ApplyHoleExpression(expr.getData());
    }

    @Override
    public Concrete.Expression visitGoal(Concrete.GoalExpression expr, Concrete.SourceNode params) {
        var wideExpression = (Concrete.GoalExpression) params;
        if (expr.expression != null) {
            return myFactory.goal(expr.getName(), expr.expression.accept(this, wideExpression.getExpression()));
        }
        return myFactory.goal(expr.getName(), null);
    }

    @Override
    public Concrete.Expression visitTuple(Concrete.TupleExpression expr, Concrete.SourceNode params) {
        Concrete.TupleExpression wideExpr;
        if (params instanceof Concrete.TypedExpression) {
            wideExpr = (Concrete.TupleExpression)((Concrete.TypedExpression) params).getExpression();
        } else {
            wideExpr = (Concrete.TupleExpression) params;
        }
        var newFields = new ArrayList<Concrete.Expression>();
        for (int i = 0; i < expr.getFields().size(); ++i) {
            newFields.add(expr.getFields().get(i).accept(this, wideExpr.getFields().get(i)));
        }
        return (Concrete.Expression) myFactory.tuple(newFields.toArray(new Concrete.Expression[0]));
    }

    @Override
    public Concrete.Expression visitSigma(Concrete.SigmaExpression expr, Concrete.SourceNode params) {
        var wideExpr = (Concrete.SigmaExpression) params;
        var newParams = visitParameters(expr.getParameters(), wideExpr.getParameters());
        return (Concrete.Expression) myFactory.sigma(newParams);
    }

    @Override
    public Concrete.Expression visitBinOpSequence(Concrete.BinOpSequenceExpression expr, Concrete.SourceNode params) {
        var wideExpr = (Concrete.BinOpSequenceExpression) params;
        if (expr.getSequence().size() == 1 && expr.getClauses() == null) {
            return expr.getSequence().get(0).expression.accept(this, wideExpr.getSequence().get(0).expression);
        }

        var newExpressions = new ArrayList<Concrete.BinOpSequenceElem>();
        for (int i = 0; i < expr.getSequence().size(); ++i) {
            var currentElem = expr.getSequence().get(i);
            newExpressions.add(new Concrete.BinOpSequenceElem(currentElem.expression.accept(this, wideExpr.getSequence().get(i).expression), currentElem.fixity, currentElem.isExplicit));
        }
        List<Concrete.FunctionClause> newClauses;
        if (expr.getClauses() != null) {
            newClauses = visitClauses(expr.getClauseList(), wideExpr.getClauseList());
        } else {
            newClauses = Collections.emptyList();
        }
        return new Concrete.BinOpSequenceExpression(expr.getData(), newExpressions, new Concrete.FunctionClauses(expr.getData(), newClauses));
    }

    private Concrete.Pattern visitPattern(Concrete.Pattern pattern, Concrete.Pattern widePattern) {
        Concrete.Pattern newPattern;
        if (pattern instanceof Concrete.NamePattern) {
            Concrete.NamePattern namePattern = (Concrete.NamePattern) pattern;
            newPattern = new Concrete.NamePattern(pattern.getData(), pattern.isExplicit(), ((Concrete.NamePattern) pattern).getReferable(), null);
            if (namePattern.type != null) {
                ((Concrete.NamePattern)newPattern).type = namePattern.type.accept(this, ((Concrete.NamePattern) widePattern).type);
            }
        } else if (pattern instanceof Concrete.ConstructorPattern || pattern instanceof Concrete.TuplePattern) {
            var innerPatterns = new ArrayList<Concrete.Pattern>();
            for (int i = 0; i < pattern.getPatterns().size(); ++i) {
                innerPatterns.add(visitPattern(pattern.getPatterns().get(i), widePattern.getPatterns().get(i)));
            }
            if (pattern instanceof Concrete.ConstructorPattern) {
                newPattern = new Concrete.ConstructorPattern(pattern.getData(), pattern.isExplicit(), ((Concrete.ConstructorPattern) pattern).getConstructor(), innerPatterns, pattern.getAsReferable());
            } else {
                newPattern = new Concrete.TuplePattern(pattern.getData(), pattern.isExplicit(), innerPatterns, pattern.getAsReferable());
            }
        } else {
            newPattern = new Concrete.NumberPattern(pattern.getData(), ((Concrete.NumberPattern)pattern).getNumber(), pattern.getAsReferable());
        }

        if (newPattern.getAsReferable() != null && newPattern.getAsReferable().type != null) {
            newPattern.getAsReferable().type = newPattern.getAsReferable().type.accept(this, Objects.requireNonNull(widePattern.getAsReferable()).type);
        }
        return newPattern;
    }


    private Concrete.Clause visitClause(Concrete.Clause clause, Concrete.Clause wideClause) {
        var newPatterns = new ArrayList<Concrete.Pattern>();
        if (clause.getPatterns() != null) {
            for (int i = 0; i < clause.getPatterns().size(); ++i) {
                newPatterns.add(visitPattern(clause.getPatterns().get(i), wideClause.getPatterns().get(i)));
            }
        }
        if (clause instanceof Concrete.ConstructorClause) {
            return new Concrete.ConstructorClause(clause.getData(), newPatterns, ((Concrete.ConstructorClause) clause).getConstructors());
        } else {
            assert clause instanceof Concrete.FunctionClause;
            return new Concrete.FunctionClause(clause.getData(), newPatterns, ((Concrete.FunctionClause) clause).expression.accept(this, wideClause.getExpression()));
        }
    }


    public List<Concrete.FunctionClause> visitClauses(List<? extends Concrete.FunctionClause> clauses, List<Concrete.FunctionClause> wideClauses) {
        List<Concrete.FunctionClause> newClauses = new ArrayList<>();
        for (int i = 0; i < clauses.size(); i++) {
            newClauses.add((Concrete.FunctionClause) visitClause(clauses.get(i), wideClauses.get(i)));
        }
        return newClauses;
    }

    @Override
    public Concrete.Expression visitCase(Concrete.CaseExpression expr, Concrete.SourceNode params) {
        var wideExpr = (Concrete.CaseExpression)params;
        var arguments = new ArrayList<Concrete.CaseArgument>();
        var newCase = new Concrete.CaseExpression(expr.getData(), expr.isSCase(), arguments, null, null, visitClauses(expr.getClauses(), wideExpr.getClauses()));
        for (int i = 0; i < expr.getArguments().size(); i++) {
            Concrete.CaseArgument caseArg = expr.getArguments().get(i);
            var newArg = caseArg.expression.accept(this, wideExpr.getArguments().get(i).expression);
            Concrete.Expression newType;
            if (caseArg.type != null) {
                newType = caseArg.type.accept(this, wideExpr.getArguments().get(i).type);
            } else {
                newType = null;
            }
            arguments.add(new Concrete.CaseArgument(newArg, caseArg.referable, newType));
        }

        if (expr.getResultType() != null) {
            newCase.setResultType(expr.getResultType().accept(this, params));
        }
        if (expr.getResultTypeLevel() != null) {
            newCase.setResultTypeLevel(expr.getResultTypeLevel().accept(this, params));
        }
        return newCase;
    }

    @Override
    public Concrete.Expression visitEval(Concrete.EvalExpression expr, Concrete.SourceNode params) {
        return new Concrete.EvalExpression(expr.getData(), expr.isPEval(), expr.getExpression().accept(this, ((Concrete.EvalExpression)params).getExpression()));
    }

    @Override
    public Concrete.Expression visitProj(Concrete.ProjExpression expr, Concrete.SourceNode params) {
        return new Concrete.ProjExpression(expr.getData(), expr.getExpression().accept(this, ((Concrete.ProjExpression)params).getExpression()), expr.getField());
    }

    @Override
    public Concrete.Expression visitClassExt(Concrete.ClassExtExpression expr, Concrete.SourceNode params) {
        Concrete.ClassExtExpression wideExpr = (Concrete.ClassExtExpression) params;
        return Concrete.ClassExtExpression.make(expr.getData(), expr.getBaseClassExpression().accept(this, wideExpr.getBaseClassExpression()), new Concrete.Coclauses(expr.getData(), visitClassElements(expr.getStatements(), wideExpr.getStatements())));
    }

    private Concrete.ClassFieldImpl visitClassFieldImpl(Concrete.ClassFieldImpl classFieldImpl, Concrete.ClassFieldImpl wideField) {
        var newFieldImpl = new Concrete.ClassFieldImpl(classFieldImpl.getData(), classFieldImpl.getImplementedField(), null, new Concrete.Coclauses(classFieldImpl.getData(), visitClassElements(classFieldImpl.getSubCoclauseList(), wideField.getSubCoclauseList())), classFieldImpl.isDefault());
        if (classFieldImpl.implementation != null) {
            newFieldImpl.implementation = classFieldImpl.implementation.accept(this, wideField.implementation);
        }
        return newFieldImpl;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Concrete.ClassElement> T visitClassElement(T element, T wideElement) {
        if (element instanceof Concrete.ClassFieldImpl) {
            return (T) visitClassFieldImpl((Concrete.ClassFieldImpl) element, (Concrete.ClassFieldImpl) wideElement);
        } else if (element instanceof Concrete.OverriddenField) {
            Concrete.OverriddenField field = (Concrete.OverriddenField) element;
            Concrete.OverriddenField wideField = (Concrete.OverriddenField) wideElement;
            var parameters = visitParameters(field.getParameters(), wideField.getParameters()).stream().map(param -> ((Concrete.TypeParameter) param)).collect(Collectors.toList());
            var newField = new Concrete.OverriddenField(element.getData(), field.getOverriddenField(), parameters, field.getResultType().accept(this, ((Concrete.OverriddenField) wideElement).getResultType()), null);
            if (field.getResultTypeLevel() != null) {
                newField.setResultTypeLevel(field.getResultTypeLevel().accept(this, wideField.getResultTypeLevel()));
            }
        }
        return null;
    }

    private <T extends Concrete.ClassElement> List<T> visitClassElements(List<T> elements, List<T> wideElements) {
        List<T> classElements = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            classElements.add(visitClassElement(elements.get(i), wideElements.get(i)));
        }
        return classElements;
    }

    @Override
    public Concrete.Expression visitNew(Concrete.NewExpression expr, Concrete.SourceNode params) {
        return new Concrete.NewExpression(expr.getData(), expr.getExpression().accept(this, ((Concrete.NewExpression)params).getExpression()));
    }

    protected Concrete.LetClause visitLetClause(Concrete.LetClause clause, Concrete.LetClause wideClause) {
        var newPattern = visitPattern(clause.getPattern(), wideClause.getPattern());
        var newParameters = visitParameters(clause.getParameters(), wideClause.getParameters());
        var newResultType = clause.getResultType() == null ? null : clause.getResultType().accept(this, wideClause.getResultType());
        var newTerm = clause.term.accept(this, wideClause.term);
        return new Concrete.LetClause(newParameters, newResultType, newTerm, newPattern);
    }

    @Override
    public Concrete.Expression visitLet(Concrete.LetExpression expr, Concrete.SourceNode params) {
        var wideExpression = (Concrete.LetExpression) params;
        var clauses = new ArrayList<Concrete.LetClause>();
        for (int i = 0; i < expr.getClauses().size(); i++) {
            clauses.add(visitLetClause(expr.getClauses().get(i), wideExpression.getClauses().get(i)));
        }
        return new Concrete.LetExpression(expr.getData(), expr.isHave(), expr.isStrict(), clauses, expr.getExpression().accept(this, wideExpression.getExpression()));
    }

    @Override
    public Concrete.Expression visitTyped(Concrete.TypedExpression expr, Concrete.SourceNode params) {
        var wideTypedExpression = (Concrete.TypedExpression)params;
        return new Concrete.TypedExpression(expr.getData(), expr.getExpression().accept(this, wideTypedExpression.getExpression()), expr.getType().accept(this, wideTypedExpression.getType()));
    }
}
