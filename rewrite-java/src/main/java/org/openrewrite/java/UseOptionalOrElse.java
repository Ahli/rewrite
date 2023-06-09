package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = true)
public class UseOptionalOrElse extends Recipe {

    @Override
    public String getDisplayName() {
        return "";
    }

    @Override
    public String getDescription() {
        return "TODO.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new UseOptionOrElseVisitor();
    }

    private class UseOptionOrElseVisitor
            extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.Block visitBlock(J.Block block, ExecutionContext executionContext) {

            List<Statement> statements = block.getStatements().stream().map(statement -> {

                if(statement instanceof J.If) {

                    J.If ifStatement = (J.If) statement;

                    Optional<String> result = checkCondition(ifStatement);
                    if(!result.isPresent()){
                        return statement;
                    }
                    String variableName = result.get();




                    
                    Statement thenPart = ifStatement.getThenPart();

                    if(!(thenPart instanceof J.Block)) {
                        return statement;
                    }
                    J.Block thenBlock = (J.Block) thenPart;
                    List<Statement> thenStatements = thenBlock.getStatements();
                    if(thenStatements.size() != 1) {
                        return statement;
                    }
                    Statement thenStatement = thenStatements.get(0);
                    if(thenStatement instanceof J.Return) {
                        J.Return thenReturn = (J.Return) thenStatement;
                        Expression thenReturnExpression = thenReturn.getExpression();
                        if(!(thenReturnExpression instanceof J.MethodInvocation)) {
                            return statement;
                        }
                        J.MethodInvocation thenReturnMethodInvocation = (J.MethodInvocation) thenReturnExpression;
                        Expression select = thenReturnMethodInvocation.getSelect();
                        if(!(select instanceof J.Identifier)
                                || !((J.Identifier) select).getSimpleName().equals(variableName.get())) {
                            return statement;
                        }






                        int j = 0;

                    } else {
                        return statement;
                    }


                    int i = 0;
                }


                return statement;
            }).collect(Collectors.toList());

            return super.visitBlock(block.withStatements(statements), executionContext);
        }

        private Optional<String> checkCondition(J.If ifStatement) {
            J.ControlParentheses<Expression> ifCondition = ifStatement.getIfCondition();
            Expression conditionExpression = ifCondition.getTree();

            if(!(conditionExpression instanceof J.MethodInvocation)) {
                return Optional.empty();
            }
            J.MethodInvocation methodInvocation = (J.MethodInvocation) conditionExpression;
            J.Identifier methodInvocationName = methodInvocation.getName();

            if(!methodInvocation.getSimpleName().equals("isPresent")){
                return Optional.empty();
            }
            if(methodInvocation.getArguments().size() != 1
                    || !(methodInvocation.getArguments().get(0) instanceof J.Empty)
            ){
                return Optional.empty();
            }
            Expression select = methodInvocation.getSelect().unwrap();

            if(!(select instanceof J.Identifier)){
                return Optional.empty();
            }
            String variableName = ((J.Identifier) select).getSimpleName();
            return Optional.of(variableName);
        }

    }
}
