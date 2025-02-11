/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;

public class UnnecessaryExplicitTypeArguments extends Recipe {

    @Override
    public String getDisplayName() {
        return "Unnecessary explicit type arguments";
    }

    @Override
    public String getDescription() {
        return "When explicit type arguments are inferrable by the compiler, they may be removed.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (m.getMethodType() != null) {
                    Object enclosing = getCursor().dropParentUntil(J.class::isInstance).getValue();
                    JavaType enclosingType = null;

                    if (enclosing instanceof Expression) {
                        enclosingType = ((Expression) enclosing).getType();
                    } else if (enclosing instanceof NameTree) {
                        enclosingType = ((NameTree) enclosing).getType();
                    } else if (enclosing instanceof J.Return) {
                        Object e = getCursor().dropParentUntil(p -> p instanceof J.MethodDeclaration || p instanceof J.Lambda).getValue();
                        if (e instanceof J.MethodDeclaration) {
                            J.MethodDeclaration methodDeclaration = (J.MethodDeclaration) e;
                            if (methodDeclaration.getReturnTypeExpression() != null) {
                                enclosingType = methodDeclaration.getReturnTypeExpression().getType();
                            }
                        } else if (e instanceof J.Lambda) {
                            enclosingType = ((J.Lambda) e).getType();
                        }
                    }

                    if (enclosingType != null &&
                            m.getMethodType().getResolvedSignature() != null &&
                            enclosingType.equals(m.getMethodType().getResolvedSignature().getReturnType())) {
                        m = m.withTypeParameters(null);
                    }
                }

                return m;
            }
        };
    }
}
