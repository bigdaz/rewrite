/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.asFullyQualified
import org.openrewrite.java.asGeneric
import org.openrewrite.java.hasElementType

interface MethodInvocationTest {
    private fun J.CompilationUnit.allInvs() = classes[0].body
        .statements
        .filterIsInstance<J.VariableDeclarations>()
        .map { it.variables[0].initializer as J.MethodInvocation }

    @Test
    fun methodInvocation(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                Integer m = foo ( 0, 1, 2 );
    
                public Integer foo(Integer n, Integer... ns) { return n; }
            }
        """)[0]

        val (inv) = a.allInvs()

        // check assumptions about the call site
        assertEquals("foo", inv.simpleName)
        assertEquals("java.lang.Integer", inv.type.asFullyQualified()?.fullyQualifiedName)
        assertEquals(listOf(JavaType.Primitive.Int, JavaType.Primitive.Int, JavaType.Primitive.Int),
            inv.arguments.filterIsInstance<J.Literal>().map { it.type })

        val effectParams = inv.methodType!!.resolvedSignature!!.paramTypes
        assertEquals("java.lang.Integer", effectParams[0].asFullyQualified()?.fullyQualifiedName)
        assertTrue(effectParams[1].hasElementType("java.lang.Integer"))

        // for non-generic method signatures, resolvedSignature and genericSignature match
        assertEquals(inv.methodType!!.resolvedSignature, inv.methodType!!.genericSignature)

        assertEquals("A", inv.methodType?.declaringType?.fullyQualifiedName)

        assertEquals("foo ( 0, 1, 2 )", inv.printTrimmed())
    }

    @Test
    fun genericMethodInvocation(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                Integer o = generic ( 0, 1, 2 );
                Integer p = this . < Integer > generic ( 0, 1, 2 );
    
                public <TTTT> TTTT generic(TTTT n, TTTT... ns) { return n; }
            }
        """)[0]

        val (genericInv, explicitGenericInv) = a.allInvs()

        listOf(genericInv, explicitGenericInv).forEach { test: J.MethodInvocation ->
            // check assumptions about the call site
            assertEquals("java.lang.Integer", test.type.asFullyQualified()?.fullyQualifiedName)
            assertEquals(listOf(JavaType.Primitive.Int, JavaType.Primitive.Int, JavaType.Primitive.Int),
                test.arguments.filterIsInstance<J.Literal>().map { it.type })

            val effectiveParams = test.methodType!!.resolvedSignature!!.paramTypes
            assertEquals("java.lang.Integer", effectiveParams[0].asFullyQualified()?.fullyQualifiedName)
            assertTrue(effectiveParams[1].hasElementType("java.lang.Integer"))

            val methType = test.methodType!!.genericSignature!!
            assertEquals("java.lang.Object", methType.returnType.asGeneric()?.fullyQualifiedName)
            assertEquals("java.lang.Object", methType.paramTypes[0].asGeneric()?.fullyQualifiedName)
            assertTrue(methType.paramTypes[1].hasElementType("java.lang.Object"))
        }

        assertEquals("this . < Integer > generic ( 0, 1, 2 )", explicitGenericInv.printTrimmed())
    }

    @Test
    fun staticMethodInvocation(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                Integer n = staticFoo ( 0 );
                Integer o = staticFoo ( );
    
                public static int staticFoo(int... args) { return 0; }
            }
        """)[0]

        val (staticInv, parameterlessStaticInv) = a.allInvs()

        assertEquals("staticFoo", staticInv.simpleName)
        assertEquals("A", staticInv.methodType?.declaringType?.fullyQualifiedName)
        assertEquals("staticFoo ( 0 )", staticInv.printTrimmed())
        assertEquals("staticFoo ( )", parameterlessStaticInv.printTrimmed())
    }

    @Test
    fun methodThatDoesNotExist(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                Integer n = doesNotExist();
            }
        """)[0]

        val inv = a.classes[0].body.statements.filterIsInstance<J.VariableDeclarations>().first().variables[0]
            .initializer as J.MethodInvocation
        assertNull(inv.methodType?.declaringType)
        assertNull(inv.type)
        assertNull(inv.type)
    }
}
