package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class UseOptionalOrElseTest
  implements RewriteTest
{
    @Test
    void replacesWithReturn()
    {
        rewriteRun(
          recipeSpec -> recipeSpec.recipe( new UseOptionalOrElse() ),
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
    void replacesWithAssignment()
    {
        rewriteRun(
          recipeSpec -> recipeSpec.recipe( new UseOptionalOrElse() ),
          java(
            """
            package my.test;

            class A
            {
                public String findUserStatus() {
                
                    Optional<String> status = Optional.empty();
                    String result;
                    
                    if (status.isPresent()) {
                        result = status.get();
                    } else {
                        result = "UNKNOWN";
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
                    String result;
                    
                    result = status.orElse("UNKNOWN");
                
                }
            }
            """
          )
        );
    }

    @Test
    void doNotReplaceIfStatementInIf()
    {
        rewriteRun(
          recipeSpec -> recipeSpec.recipe( new UseOptionalOrElse() ),
          java(
            """
            package my.test;

            class A
            {
                public String findUserStatus() {
                
                    Optional<String> status = Optional.empty();
                
                    if (status.isPresent()) {
                        System.out.println();
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
                
                    if (status.isPresent()) {
                        System.out.println();
                        return status.get();
                    } else {
                        return "UNKNOWN";
                    }
                
                }
            }
            """
          )
        );
    }

    @Test
    void doNotReplaceIfStatementInElse()
    {
        rewriteRun(
          recipeSpec -> recipeSpec.recipe( new UseOptionalOrElse() ),
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
                        System.out.println();
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
                
                    if (status.isPresent()) {
                        return status.get();
                    } else {
                        System.out.println();
                        return "UNKNOWN";
                    }
                
                }
            }
            """
          )
        );
    }
}
