/* MethodOverloadResolver -- Resolves overloaded methods
   Copyright (C) 2009 Red Hat 

This file is part of IcedTea.

IcedTea is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

IcedTea is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with IcedTea; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */

package sun.applet;

import java.lang.reflect.Method;
import java.util.ArrayList;

/* 
 * This class resolved overloaded methods in Java objects using a cost 
 * based-approach as described here:
 *
 * http://java.sun.com/javase/6/webnotes/6u10/plugin2/liveconnect/#OVERLOADED_METHODS
 */

public class MethodOverloadResolver {

    private static boolean debugging = false;
    
    public static void main(String[] args) {
        testMethodResolver();
    }

    public static void testMethodResolver() {
        debugging = true;

        ArrayList<Object[]> list = new ArrayList<Object[]>(20);
        FooClass fc = new FooClass();
        
        // Numeric to java primitive
        // foo_i has Integer and int params
        String s1 = "foo_string_int(S,I)";
        String s1a = "foo_string_int(S,S)";
        Object[] o1 = { fc.getClass(), "foo_string_int", "blah", 42 };
        list.add(o1);
        Object[] o1a = { fc.getClass(), "foo_string_int", "blah", "42.42" };
        list.add(o1a);
        
        // Null to non-primitive type
        // foo_i is overloaded with Integer and int 
        String s2 = "foo_string_int(N)";
        Object[] o2 = { fc.getClass(), "foo_string_int", "blah", null };
        list.add(o2);
        
        // foo_jsobj is overloaded with JSObject and String params
        String s3 = "foo_jsobj(LLowCostSignatureComputer/JSObject;)";
        Object[] o3 = { fc.getClass(), "foo_jsobj", new JSObject() };
        list.add(o3);

        // foo_classtype is overloaded with Number and Integer
        String s4 = "foo_classtype(Ljava/lang/Integer;)";
        Object[] o4 = { fc.getClass(), "foo_classtype", 42 };
        list.add(o4);
        
        // foo_multiprim is overloaded with int, long and float types
        String s5 = "foo_multiprim(I)";
        String s6 = "foo_multiprim(F)";
        String s6a = "foo_multiprim(D)";
        
        Object[] o5 = { fc.getClass(), "foo_multiprim", new Integer(42) };
        Object[] o6 = { fc.getClass(), "foo_multiprim", new Float(42.42) };
        Object[] o6a = { fc.getClass(), "foo_multiprim", new Double(42.42) };
        list.add(o5);
        list.add(o6);
        list.add(o6a);
        
        // foo_float has float, String and JSObject type
        String s7 = "foo_float(I)";
        Object[] o7 = { fc.getClass(), "foo_float", new Integer(42) };
        list.add(o7);

        // foo_multiprim(float) is what this should convert
        String s8 = "foo_float(S)";
        Object[] o8 = { fc.getClass(), "foo_float", "42" };
        list.add(o8);
        
        // foo_class is overloaded with BarClass 2 and 3
        String s9 = "foo_class(LLowCostSignatureComputer/BarClass3;)";
        Object[] o9 = { fc.getClass(), "foo_class", new BarClass3() };
        list.add(o9);

        // foo_strandbyteonly takes string and byte
        String s10 = "foo_strandbyteonly(I)";
        Object[] o10 = { fc.getClass(), "foo_strandbyteonly", 42 };
        list.add(o10);

        // JSOBject to string
        String s11 = "foo_strandbyteonly(LLowCostSignatureComputer/JSObject;)";
        Object[] o11 = { fc.getClass(), "foo_strandbyteonly", new JSObject() };
        list.add(o11);

        // jsobject to string and int to float
        String s12 = "foo_str_and_float(S,I)";
        Object[] o12 = { fc.getClass(), "foo_str_and_float", new JSObject(), new Integer(42) };
        list.add(o12);
        
        // call for which no match will be found
        String s13 = "foo_int_only(JSObject)";
        Object[] o13 = { fc.getClass(), "foo_int_only", new JSObject() };
        list.add(o13);
        
        // method with no args
        String s14 = "foo_noargs()";
        Object[] o14 = { fc.getClass(), "foo_noargs" };
        list.add(o14);
        
        // method which takes a primitive bool, given a Boolean
        String s15 = "foo_boolonly()";
        Object[] o15 = { fc.getClass(), "foo_boolonly", new Boolean(true) };
        list.add(o15);

        for (Object[] o : list) {
            Object[] methodAndArgs = getMatchingMethod(o);
            if (debugging)
                if (methodAndArgs != null)
                    System.out.println("Best match: " + methodAndArgs[0] + "\n");
                else
                    System.out.println("No match found.\n");
                
        }
    }

    /* 
     * Cost based overload resolution algorithm based on cost rules specified here:
     * 
     * http://java.sun.com/javase/6/webnotes/6u10/plugin2/liveconnect/#OVERLOADED_METHODS
     */

    public static Object[] getMatchingMethod(Object[] callList) {
        Object[] ret = null;
        Class c = (Class) callList[0];
        String methodName = (String) callList[1];

        Method[] matchingMethods = getMatchingMethods(c, methodName, callList.length - 2);
        
        if (debugging)
            System.out.println("getMatchingMethod called with: " + printList(callList));

        int lowestCost = Integer.MAX_VALUE;

        ArrayList<Object> paramList = new ArrayList<Object>();

        for (Method matchingMethod : matchingMethods) {

            int methodCost = 0;
            Class[] paramTypes = matchingMethod.getParameterTypes();
            Object[] methodAndArgs = new Object[paramTypes.length + 1];
            methodAndArgs[0] = matchingMethod;

            // Figure out which of the matched methods best represents what we
            // want
            for (int i = 0; i < paramTypes.length; i++) {
                Class paramTypeClass = paramTypes[i];
                Object suppliedParam = callList[i + 2];
                Class suppliedParamClass = suppliedParam != null ? suppliedParam
                        .getClass()
                        : null;

                Object[] costAndCastedObj = getCostAndCastedObject(
                        suppliedParam, paramTypeClass);
                methodCost += (Integer) costAndCastedObj[0];
                Object castedObj = paramTypeClass.isPrimitive() ? costAndCastedObj[1]
                        : paramTypeClass.cast(costAndCastedObj[1]);
                methodAndArgs[i + 1] = castedObj;

                Class castedObjClass = castedObj == null ? null : castedObj
                        .getClass();
                Boolean castedObjIsPrim = castedObj == null ? null : castedObj
                        .getClass().isPrimitive();

                if (debugging)
                    System.out.println("Param " + i + " of method "
                            + matchingMethod + " has cost "
                            + (Integer) costAndCastedObj[0]
                            + " original param type " + suppliedParamClass
                            + " casted to " + castedObjClass + " isPrimitive="
                            + castedObjIsPrim + " value " + castedObj);
            }

            if ((methodCost > 0 && methodCost < lowestCost) || 
                 paramTypes.length == 0) {
                ret = methodAndArgs;
                lowestCost = methodCost;
            }

        }

        return ret;
    }

    private static Object[] getCostAndCastedObject(Object suppliedParam, Class paramTypeClass) {
        
        Object[] ret = new Object[2];
        Integer cost = new Integer(0);
        Object castedObj;

        Class suppliedParamClass = suppliedParam != null ? suppliedParam.getClass() : null ;

        // If this is null, there are only 2 possible cases
        if (suppliedParamClass == null) {
            castedObj = null; // if value is null.. well, it is null

            if (!paramTypeClass.isPrimitive()) {
                cost += 2; // Null to any non-primitive type
            } else {
                cost = Integer.MIN_VALUE; // Null to primitive not allowed
            }
        } else if (paramTypeClass.isPrimitive() && paramTypeClass.equals(getPrimitive(suppliedParam))) {
            cost += 1; // Numeric type to the analogous Java primitive type
            castedObj = suppliedParam; // Let auto-boxing handle it
        } else if (suppliedParamClass.equals(paramTypeClass)) {
           cost += 3; // Class type to Class type where the types are equal
           castedObj = suppliedParam;
        } else if (isNum(suppliedParam) && 
                       (paramTypeClass.isPrimitive() ||
                        java.lang.Number.class.isAssignableFrom(paramTypeClass) ||
                        java.lang.Character.class.isAssignableFrom(paramTypeClass) ||
                        java.lang.Byte.class.isAssignableFrom(paramTypeClass)
                       )
                   ) {
            cost += 4; // Numeric type to a different primitive type 
            castedObj = getNum(suppliedParam.toString(), paramTypeClass);
        } else if (suppliedParam instanceof java.lang.String &&
                    isNum(suppliedParam) &&
                        (paramTypeClass.isInstance(java.lang.Number.class) ||
                         paramTypeClass.isInstance(java.lang.Character.class) ||
                         paramTypeClass.isInstance(java.lang.Byte.class) ||
                         paramTypeClass.isPrimitive())
                   ) {
            cost += 5; // String to numeric type
            castedObj = getNum(suppliedParam.toString(), paramTypeClass);
        } else if (paramTypeClass.isAssignableFrom(suppliedParamClass)) {
            cost += 6; // Class type to superclass type;
            castedObj = paramTypeClass.cast(suppliedParam);
        } else if (paramTypeClass.equals(java.lang.String.class)) {
            cost += 7; // Any Java value to String
            castedObj = suppliedParam.toString();
        } else if (suppliedParam instanceof JSObject &&
                   paramTypeClass.equals(String.class)) {
            cost += 8; // JSObject to String
            castedObj = suppliedParam.toString();
        } else if (suppliedParam instanceof JSObject &&
                   paramTypeClass.isArray()) {
            cost += 10; // JSObject to Java array 
            castedObj = (JSObject) suppliedParam; // FIXME: Arrays not yet handled
        } else {
            cost = Integer.MIN_VALUE; // Not allowed
            castedObj = null;
        }
        
        ret[0] = cost;
        ret[1] = castedObj;

        return ret;

    }
    
    private static Method[] getMatchingMethods(Class c, String name, int paramCount) {
        Method[] allMethods = c.getMethods();
        ArrayList<Method> matchingMethods = new ArrayList(5);
        
        for (Method m: allMethods) {
            if (m.getName().equals(name) && m.getParameterTypes().length == paramCount)
                matchingMethods.add(m);
        }

        return matchingMethods.toArray(new Method[0]);
    }

    private static Class getPrimitive(Object o) {
        
        if (o instanceof java.lang.Byte) {
            return java.lang.Byte.TYPE;
        } else if (o instanceof java.lang.Character) {
            return java.lang.Character.TYPE;
        } else if (o instanceof java.lang.Short) {
            return java.lang.Short.TYPE;
        } else if (o instanceof java.lang.Integer) {
            return java.lang.Integer.TYPE;
        } else if (o instanceof java.lang.Long) {
            return java.lang.Long.TYPE;
        } else if (o instanceof java.lang.Float) {
            return java.lang.Float.TYPE;
        } else if (o instanceof java.lang.Double) {
            return java.lang.Double.TYPE;
        } else if (o instanceof java.lang.Boolean) {
            return java.lang.Boolean.TYPE;
        }

        return o.getClass();
    }

    private static boolean isNum (Object o) {
        
        if (o instanceof java.lang.Number)
            return true;
        
        if (!(o instanceof java.lang.String))
            return false;

        try {
            Long.parseLong((String) o); // whole number test
            return true;
        } catch (NumberFormatException nfe) {}
        
        try {
            Float.parseFloat((String) o); // decimal
            return true;
        } catch (NumberFormatException nfe) {}
        
        return false;
    }

    private static Object getNum (String s, Class c) throws NumberFormatException {

        Number n;
        if (s.contains("."))
            n = new Double(s);
        else
            n = new Long(s);

        // See if we need to collapse first
        if (c.equals(java.lang.Integer.class) ||
            c.equals(java.lang.Integer.TYPE)) {
            return n.intValue();
        }

        if (c.equals(java.lang.Long.class) ||
            c.equals(java.lang.Long.TYPE)) {
            return n.longValue();
        }

        if (c.equals(java.lang.Short.class) ||
            c.equals(java.lang.Short.TYPE)) {
            return n.shortValue();
        }
        
        if (c.equals(java.lang.Float.class) ||
            c.equals(java.lang.Float.TYPE)) {
            return n.floatValue();
        }
        
        if (c.equals(java.lang.Double.class) ||
            c.equals(java.lang.Double.TYPE)) {
            return n.doubleValue();
        }

        if (c.equals(java.lang.Byte.class) ||
            c.equals(java.lang.Byte.TYPE)) {
            return n.byteValue();
        }

        if (c.equals(java.lang.Character.class) ||
                c.equals(java.lang.Character.TYPE)) {
            return s.charAt(0);
        }

        return n;
    }

    private static String printList (Object[] oList) {
        
        String ret = "";
        
        ret += "{ ";
        for (Object o : oList) {
            
            String oStr = o != null ? o.toString() + " [" + o.getClass() + "]" : "null";
            
            ret += oStr;
            ret += ", ";
        }
        ret = ret.substring(0, ret.length()-2); // remove last ", "
        ret += " }";

        return ret;
    }
}

/** Begin test classes **/

class FooClass {

    // First type full match, second Class -> Primitive
    public void foo(Boolean b, int i) {

    }

    // Full match
    public void foo(Boolean b, Integer i) {

    }

    // First type full match, second Class -> Primitive ambiguity
    public void foo(Boolean b, short s) {

    }

    public void foo_string_int(String s, int i) {}

    public void foo_string_int(String s, Integer i) {}

    public void foo_jsobj(JSObject j) {}

    public void foo_jsobj(String s) {}

    public void foo_classtype(java.lang.Number num) {}

    public void foo_classtype(java.lang.Integer integer) {}

    public void foo_multiprim(int i) {}

    public void foo_multiprim(long l) {}

    public void foo_multiprim(float f) {}
    
    public void foo_multiprim(double d) {}

    public void foo_float(float f) {}
    
    public void foo_float(String s) {}
    
    public void foo_float(JSObject j) {}

    public void foo_class(BarClass1 b) {}

    public void foo_class(BarClass2 b) {}

    public void foo_strandbyteonly(String s) {}
    
    public void foo_strandbyteonly(byte b) {}
    
    public void foo_str_and_float(String s, Float f) {}
    
    public void foo_int_only (int i) {}
    
    public void foo_noargs() {}

    public void foo_boolonly(boolean b) {}
}

class BarClass1 {}
class BarClass2 extends BarClass1 {}
class BarClass3 extends BarClass2 {}
class JSObject {}