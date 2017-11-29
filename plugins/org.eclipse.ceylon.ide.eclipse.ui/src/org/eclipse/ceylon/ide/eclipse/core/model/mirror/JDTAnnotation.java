/********************************************************************************
 * Copyright (c) {date} Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/

package org.eclipse.ceylon.ide.eclipse.core.model.mirror;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.ElementValuePair;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

import org.eclipse.ceylon.model.loader.mirror.AnnotationMirror;

public class JDTAnnotation implements AnnotationMirror {

    private Map<String, Object> values;

    public JDTAnnotation(AnnotationBinding annotation) {
        values = new HashMap<String, Object>();
        ElementValuePair[] annotationVaues = annotation.getElementValuePairs();
        for (ElementValuePair annotationValue : annotationVaues) {
            String name = new String(annotationValue.getName());
            MethodBinding elementMethod = annotationValue.getMethodBinding();
            Object value = null;
            if (elementMethod != null) {
                value = convertValue(annotationValue.getMethodBinding().returnType, annotationValue.getValue());
            } else {
                System.err.println("WARNING: method for annotation '" + name + "' was not found");
                continue;
//                value = JDTType.UNKNOWN_TYPE;
            }
            values.put(name, value);
        }
        if (! values.containsKey("value")) {
            for (MethodBinding elementMethod : annotation.getAnnotationType().methods()) {
                if (new String(elementMethod.selector).equals("value")) {
                    Object value = convertValue(elementMethod.returnType, null);
                    if (value != null) {
                        values.put("value", value);
                    }
                }
            }
        }
    }

    public JDTAnnotation(Map<String, Object> values) {
        this.values = values;
    }

    @Override
    public Object getValue(String fieldName) {
        return values.get(fieldName);
    }

    private Object convertValue(TypeBinding returnType, Object value) {
        if(value != null && value.getClass().isArray()){
            Object[] array = (Object[])value;
            List<Object> values = new ArrayList<Object>(array.length);
            TypeBinding elementType = ((ArrayBinding)returnType).elementsType();
            for(Object val : array)
                values.add(convertValue(elementType, val));
            return values;
        }
        if(returnType.isArrayType()){
            if (value == null) {
                return Collections.emptyList();
            }
            // got a single value but expecting array
            List<Object> values = new ArrayList<Object>(1);
            TypeBinding elementType = ((ArrayBinding)returnType).elementsType();
            values.add(convertValue(elementType, value));
            return values;
        }
        if(value instanceof AnnotationBinding){
            return new JDTAnnotation((AnnotationBinding) value);
        }
        if(value instanceof TypeBinding){
            return JDTType.newJDTType((TypeBinding) value);
        }
        if(value instanceof FieldBinding){
            return new String(((FieldBinding) value).name);
        }
        if(value instanceof Constant){
            Constant constant = (Constant) value;
            return JDTUtils.fromConstant(constant);
        }
        return value;
    }

    @Override
    public Object getValue() {
        return getValue("value");
    }
    
    @Override
    public String toString() {
        return "[JDTAnnotation: "+values+"]";
    }
}
