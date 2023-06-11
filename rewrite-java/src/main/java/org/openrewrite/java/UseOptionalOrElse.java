/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

import java.util.List;
import java.util.Optional;

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

    private class UseOptionOrElseVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.Block visitBlock(J.Block block, ExecutionContext executionContext) {

            J.Block newBlock = null;

            for (Statement statement : block.getStatements()) {
                if (!(statement instanceof J.If)) {
                    continue;
                }

                J.If ifStatement = (J.If) statement;

                Optional<J.Identifier> result = checkCondition(ifStatement);
                if (!result.isPresent()) {
                    continue;
                }
                String variableName = result.get().getSimpleName();
                J.Identifier variableIdentifier = result.get();

                boolean thenPartMatches = checkThenPart(variableName, ifStatement);

                if (!thenPartMatches) {
                    continue;
                }

                J.If.Else elsePart = ifStatement.getElsePart();

                J.Block elseBody = (J.Block) elsePart.getBody();

                if (elseBody.getStatements().size() != 1) {
                    continue;
                }

                if (!(elseBody.getStatements().get(0) instanceof J.Return)) {
                    continue;
                }

                J.Return returnStatement = (J.Return) elseBody.getStatements().get(0);
                Expression returnExpression = returnStatement.getExpression();

                JavaTemplate javaTemplate = JavaTemplate.builder(
                                "return #{any(java.util.Optional)}.orElse(#{any(java.lang.Object)});")
                        .contextSensitive()
                        .imports(Optional.class.getName())
                        .build();

                newBlock = javaTemplate.apply(
                        getCursor(),
                        ifStatement.getCoordinates().replace(),
                        variableIdentifier.withType(JavaType.buildType("java.util.Optional")),
                        returnExpression
                );
            }

            if (newBlock == null) {
                return super.visitBlock(block, executionContext);
            }
            return super.visitBlock(block.withStatements(newBlock.getStatements()), executionContext);
        }

        private Optional<J.Identifier> checkCondition(J.If ifStatement) {
            J.ControlParentheses<Expression> ifCondition = ifStatement.getIfCondition();
            Expression conditionExpression = ifCondition.getTree();

            if (!(conditionExpression instanceof J.MethodInvocation)) {
                return Optional.empty();
            }
            J.MethodInvocation methodInvocation = (J.MethodInvocation) conditionExpression;

            if (!matchesFunctionName(methodInvocation, "isPresent")) {
                return Optional.empty();
            }
            if (!isCalledWithoutArguments(methodInvocation)) {
                return Optional.empty();
            }
            Expression select = methodInvocation.getSelect().unwrap();

            if (!(select instanceof J.Identifier)) {
                return Optional.empty();
            }
            return Optional.of((J.Identifier) select);
        }

        private boolean matchesFunctionName(J.MethodInvocation methodInvocation, String functionName) {
            return methodInvocation.getSimpleName().equals(functionName);
        }

        private boolean isCalledWithoutArguments(J.MethodInvocation methodInvocation) {
            return methodInvocation.getArguments().size() == 1 && (methodInvocation.getArguments().get(0) instanceof J.Empty);
        }


        boolean checkThenPart(String variableName, J.If ifStatement) {

            J.Block thenBlock = (J.Block) ifStatement.getThenPart();
            List<Statement> thenStatements = thenBlock.getStatements();
            if (thenStatements.size() != 1) {
                return false;
            }
            Statement thenStatement = thenStatements.get(0);

            if (!(thenStatement instanceof J.Return)) {
                return false;
            }


            J.Return thenReturn = (J.Return) thenStatement;
            Expression thenReturnExpression = thenReturn.getExpression();
            if (!(thenReturnExpression instanceof J.MethodInvocation)) {
                return false;
            }
            J.MethodInvocation thenReturnMethodInvocation = (J.MethodInvocation) thenReturnExpression;
            Expression select = thenReturnMethodInvocation.getSelect();
            if (!(select instanceof J.Identifier)
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
