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

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class UseOptionalOrElseTest
  implements RewriteTest {
    @Test
    void replacesWithReturnLiteral() {
        rewriteRun(
          recipeSpec -> recipeSpec.recipe(new UseOptionalOrElse()),
          java(
            """
              package my.test;
              
              class A
              {
                  public String findUserStatus() {
                  
                      Optional<String> status = Optional.empty();
                      
                      if (status.isPresent()) {
                          return status.get();
                      } else {
                          return "UNKNOWN";
                      }
                      
                  }
              }
              """,
            """
              package my.test;
              
              class A
              {
                  public String findUserStatus() {
                  
                      Optional<String> status = Optional.empty();
                      
                      return status.orElse("UNKNOWN");
                      
                  }
              }
              """
          )
        );
    }

    @Test
    void replacesWithReturnMethod() {
        rewriteRun(
          recipeSpec -> recipeSpec.recipe(new UseOptionalOrElse()),
          java(
            """
              package my.test;
              
              class A
              {
                  public String findUserStatus() {
                  
                      Optional<String> status = Optional.empty();
                      
                      if (status.isPresent()) {
                          return status.get();
                      } else {
                          return fallback();
                      }
                      
                  }
                  
                  private String fallback(){
                    return "fallback";
                  }
              }
              """,
            """
              package my.test;
              
              class A
              {
                  public String findUserStatus() {
                  
                      Optional<String> status = Optional.empty();
                      
                      return status.orElse(fallback());
                      
                  }
                  
                  private String fallback(){
                    return "fallback";
                  }
              }
              """
          )
        );
    }

//    @Test
//    void replacesWithAssignment()
//    {
//        rewriteRun(
//          recipeSpec -> recipeSpec.recipe( new UseOptionalOrElse() ),
//          java(
//            """
//            package my.test;
//
//            class A
//            {
//                public String findUserStatus() {
//
//                    Optional<String> status = Optional.empty();
//                    String result;
//
//                    if (status.isPresent()) {
//                        result = status.get();
//                    } else {
//                        result = "UNKNOWN";
//                    }
//
//                }
//            }
//            """,
//            """
//            package my.test;
//
//            class A
//            {
//                public String findUserStatus() {
//
//                    Optional<String> status = Optional.empty();
//                    String result;
//
//                    result = status.orElse("UNKNOWN");
//
//                }
//            }
//            """
//          )
//        );
//    }
//
//    @Test
//    void doNotReplaceIfStatementInIf()
//    {
//        rewriteRun(
//          recipeSpec -> recipeSpec.recipe( new UseOptionalOrElse() ),
//          java(
//            """
//            package my.test;
//
//            class A
//            {
//                public String findUserStatus() {
//
//                    Optional<String> status = Optional.empty();
//
//                    if (status.isPresent()) {
//                        System.out.println();
//                        return status.get();
//                    } else {
//                        return "UNKNOWN";
//                    }
//
//                }
//            }
//            """,
//            """
//            package my.test;
//
//            class A
//            {
//                public String findUserStatus() {
//
//                    Optional<String> status = Optional.empty();
//
//                    if (status.isPresent()) {
//                        System.out.println();
//                        return status.get();
//                    } else {
//                        return "UNKNOWN";
//                    }
//
//                }
//            }
//            """
//          )
//        );
//    }
//
//    @Test
//    void doNotReplaceIfStatementInElse()
//    {
//        rewriteRun(
//          recipeSpec -> recipeSpec.recipe( new UseOptionalOrElse() ),
//          java(
//            """
//            package my.test;
//
//            class A
//            {
//                public String findUserStatus() {
//
//                    Optional<String> status = Optional.empty();
//
//                    if (status.isPresent()) {
//                        return status.get();
//                    } else {
//                        System.out.println();
//                        return "UNKNOWN";
//                    }
//
//                }
//            }
//            """,
//            """
//            package my.test;
//
//            class A
//            {
//                public String findUserStatus() {
//
//                    Optional<String> status = Optional.empty();
//
//                    if (status.isPresent()) {
//                        return status.get();
//                    } else {
//                        System.out.println();
//                        return "UNKNOWN";
//                    }
//
//                }
//            }
//            """
//          )
//        );
//    }

    // TODO if else without curly braces, tenary operator
}
