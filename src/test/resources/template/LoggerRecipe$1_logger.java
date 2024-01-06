/*
 * Copyright 2024 the original author or authors.
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
package template;
import org.openrewrite.java.*;

@SuppressWarnings("all")
public class LoggerRecipe$1_logger {
    public LoggerRecipe$1_logger() {}

    public static JavaTemplate.Builder getTemplate() {
        return JavaTemplate
                .builder("LoggerFactory.getLogger(#{s:any(java.lang.String)})")
                .imports("org.slf4j.LoggerFactory")
                .javaParser(JavaParser.fromJavaVersion().classpath("slf4j-api"));
    }
}
