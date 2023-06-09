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
import java.util.UUID;
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

                    boolean thenPartMatches = checkThenPart(variableName, ifStatement);

                    if (!thenPartMatches) {
                        return statement;
                    }



                    J.If.Else elsePart = ifStatement.getElsePart();

                    J.Block elseBody = (J.Block) elsePart.getBody();

                    if(elseBody.getStatements().size() != 1) {
                        return statement;
                    }

                    if (!(elseBody.getStatements().get(0) instanceof J.Return)) {
                        return statement;
                    }




                    J.Return returnStatement = (J.Return) elseBody.getStatements().get(0);
                    J.Literal returnExpression = (J.Literal)returnStatement.getExpression();



                    JavaTemplate javaTemplate = JavaTemplate.builder(variableName + ".orElse(" + returnExpression.getValue() + ")").build();



                    //
//  \-------J.Return | "return status.orElse("UNKNOWN")"
//  \---J.MethodInvocation | "status.orElse("UNKNOWN")"
//  |-------J.Identifier | "status"
//  |---J.Identifier | "orElse"
//  \-----------J.Literal | ""UNKNOWN""








             //       int i = 0;
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

            if (!matchesFunctionName(methodInvocation, "isPresent")) {
                return Optional.empty();
            }
            if(!isCalledWithoutArguments(methodInvocation)) {
                return Optional.empty();
            }
            Expression select = methodInvocation.getSelect().unwrap();

            if(!(select instanceof J.Identifier)){
                return Optional.empty();
            }
            String variableName = ((J.Identifier) select).getSimpleName();
            return Optional.of(variableName);
        }

        private boolean matchesFunctionName(J.MethodInvocation methodInvocation, String functionName) {
            return methodInvocation.getSimpleName().equals(functionName);
        }

        private boolean isCalledWithoutArguments(J.MethodInvocation methodInvocation) {
            return methodInvocation.getArguments().size() == 1  && (methodInvocation.getArguments().get(0) instanceof J.Empty);
        }


        boolean checkThenPart(String variableName, J.If ifStatement) {

            J.Block thenBlock = (J.Block) ifStatement.getThenPart();
            List<Statement> thenStatements = thenBlock.getStatements();
            if(thenStatements.size() != 1) {
                return false;
            }
            Statement thenStatement = thenStatements.get(0);

            if(!(thenStatement instanceof J.Return)) {
                return false;
            }


            J.Return thenReturn = (J.Return) thenStatement;
            Expression thenReturnExpression = thenReturn.getExpression();
            if(!(thenReturnExpression instanceof J.MethodInvocation)) {
                return false;
            }
            J.MethodInvocation thenReturnMethodInvocation = (J.MethodInvocation) thenReturnExpression;
            Expression select = thenReturnMethodInvocation.getSelect();
            if(!(select instanceof J.Identifier)
                    || !((J.Identifier) select).getSimpleName().equals(variableName)) {
                return false;
            }

            if (!matchesFunctionName(thenReturnMethodInvocation, "get")) {
                return false;
            }

            return isCalledWithoutArguments(thenReturnMethodInvocation);
        }
    }
}
