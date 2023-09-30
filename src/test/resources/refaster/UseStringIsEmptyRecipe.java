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
package foo;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.NonNullApi;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.*;
import org.openrewrite.java.template.Primitive;
import org.openrewrite.java.template.Semantics;
import org.openrewrite.java.template.function.*;
import org.openrewrite.java.template.internal.AbstractRefasterJavaVisitor;
import org.openrewrite.java.tree.*;

import java.util.function.Supplier;
import java.util.*;


@NonNullApi
public class UseStringIsEmptyRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return "Refaster template `UseStringIsEmpty`";
    }

    @Override
    public String getDescription() {
        return "Recipe created for the following Refaster template:\n```java\npublic class UseStringIsEmpty {\n    \n    @BeforeTemplate()\n    boolean before(String s) {\n        return s.length() > 0;\n    }\n    \n    @AfterTemplate()\n    boolean after(String s) {\n        return !s.isEmpty();\n    }\n}\n```\n.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JavaVisitor<ExecutionContext> javaVisitor = new AbstractRefasterJavaVisitor() {
            final Supplier<JavaTemplate> before = memoize(() -> Semantics.expression(this, "before", (String s) -> s.length() > 0).build());
            final Supplier<JavaTemplate> after = memoize(() -> Semantics.expression(this, "after", (String s) -> !s.isEmpty()).build());

            @Override
            public J visitBinary(J.Binary elem, ExecutionContext ctx) {
                JavaTemplate.Matcher matcher;
                if ((matcher = matcher(before, getCursor())).find()) {
                    return embed(
                            apply(after, getCursor(), elem.getCoordinates().replace(), matcher.parameter(0)),
                            getCursor(),
                            ctx
                    );
                }
                return super.visitBinary(elem, ctx);
            }

        };
        return Preconditions.check(
                new UsesMethod<>("java.lang.String length(..)"),
                javaVisitor
        );
    }
}
