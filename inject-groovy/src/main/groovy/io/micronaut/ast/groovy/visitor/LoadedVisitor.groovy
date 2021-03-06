/*
 * Copyright 2017-2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.ast.groovy.visitor

import groovy.transform.CompileStatic
import io.micronaut.ast.groovy.utils.AstAnnotationUtils
import io.micronaut.core.annotation.AnnotationMetadata
import io.micronaut.core.annotation.Internal
import io.micronaut.inject.visitor.TypeElementVisitor
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.GenericsType
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.Variable
import org.codehaus.groovy.control.SourceUnit

/**
 * Used to store a reference to an underlying {@link TypeElementVisitor} and
 * optionally invoke the visit methods on the visitor if it matches the
 * element being visited by the AST transformation.
 *
 * @author James Kleeh
 * @since 1.0
 */
@Internal
@CompileStatic
class LoadedVisitor {

    private final SourceUnit sourceUnit
    private final TypeElementVisitor visitor
    private final String classAnnotation
    private final String elementAnnotation

    LoadedVisitor( SourceUnit source, TypeElementVisitor visitor) {
        this.sourceUnit = source
        this.visitor = visitor
        ClassNode classNode = ClassHelper.make(visitor.getClass())
        ClassNode definition = classNode.getAllInterfaces().find {
            it.name == TypeElementVisitor.class.name
        }
        GenericsType[] generics = definition.getGenericsTypes()
        classAnnotation = generics[0].type.name
        elementAnnotation = generics[1].type.name
    }

    TypeElementVisitor getVisitor() {
        visitor
    }


    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        LoadedVisitor that = (LoadedVisitor) o

        if (visitor.getClass() != that.getClass() ) return false

        return true
    }

    int hashCode() {
        return visitor.getClass().hashCode()
    }

    @Override
    String toString() {
        visitor.toString()
    }
    /**
     * @param classNode The class node
     * @return True if the class node should be visited
     */
    boolean matches(ClassNode classNode) {
        if (classAnnotation == ClassHelper.OBJECT) {
            return true
        }
        AnnotationMetadata annotationMetadata = AstAnnotationUtils.getAnnotationMetadata(sourceUnit, classNode)
        return annotationMetadata.hasStereotype(classAnnotation)
    }

    /**
     * @param annotationMetadata The annotation data
     * @return True if the element should be visited
     */
    boolean matches(AnnotationMetadata annotationMetadata) {
        if (elementAnnotation == ClassHelper.OBJECT) {
            return true
        }
        return annotationMetadata.hasStereotype(elementAnnotation)
    }

    /**
     * Invoke the underlying visitor for the given node.
     *
     * @param annotatedNode The node to visit
     * @param annotationMetadata The annotation data for the node
     * @param visitorContext the Groovy visitor context
     */
    void visit(AnnotatedNode annotatedNode, AnnotationMetadata annotationMetadata, GroovyVisitorContext visitorContext) {
        switch (annotatedNode.getClass()) {
            case FieldNode:
            case PropertyNode:
                visitor.visitField(new GroovyFieldElement((Variable) annotatedNode, annotationMetadata), visitorContext)
                break
            case ConstructorNode:
                visitor.visitConstructor(new GroovyConstructorElement(sourceUnit, (ConstructorNode) annotatedNode, annotationMetadata), visitorContext)
                break
            case MethodNode:
                visitor.visitMethod(new GroovyMethodElement(sourceUnit, (MethodNode) annotatedNode, annotationMetadata), visitorContext)
                break
            case ClassNode:
                ClassNode cn = (ClassNode) annotatedNode
                if (cn.isEnum()) {

                } else {
                    visitor.visitClass(new GroovyClassElement(sourceUnit, cn, annotationMetadata), visitorContext)
                }
                break
        }
    }

    void start(GroovyVisitorContext visitorContext) {
        visitor.start(visitorContext)
    }

    void finish(GroovyVisitorContext visitorContext) {
        visitor.finish(visitorContext)
    }
}
