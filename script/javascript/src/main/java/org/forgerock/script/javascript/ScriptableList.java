/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 * Portions Copyrighted 2012-2014 ForgeRock AS
 */
package org.forgerock.script.javascript;

import java.util.Collections;
import java.util.List;

import org.forgerock.json.JsonValue;
import org.forgerock.script.scope.AbstractFactory;
import org.forgerock.script.scope.Parameter;
import org.forgerock.util.LazyList;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;

/**
 * Provides a {@code Scriptable} wrapper for a {@code List} object.
 * 
 * 
 * @author Paul C. Bryan
 */
class ScriptableList extends IdScriptableObject implements Wrapper {

    static final long serialVersionUID = 1L;

    private static final Object ARRAY_TAG = "ScriptableList";

    /**
     * Attributes of the array's length property
     */
    private int lengthAttr = DONTENUM | PERMANENT;

    /**
     * Constructor for prototype only.
     */
    private ScriptableList (){
        list = Collections.emptyList();
        parameter = null;
    }

    static void init(Scriptable scope, boolean sealed)
    {
        ScriptableList obj = new ScriptableList();
        obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    @Override
    public String getClassName()
    {
        return "ScriptableList";
    }

    @Override
    public String toString()
    {
        return null == list ? "null" : new JsonValue(list).toString();
    }

    private static final int
            Id_constructor          = 1,
            Id_toString             = 2,
            Id_toLocaleString       = 3,
            Id_toSource             = 4,
            Id_join                 = 5,
            Id_reverse              = 6,
            Id_sort                 = 7,
            Id_push                 = 8,
            Id_pop                  = 9,
            Id_shift                = 10,
            Id_unshift              = 11,
            Id_splice               = 12,
            Id_concat               = 13,
            Id_slice                = 14,
            Id_indexOf              = 15,
            Id_lastIndexOf          = 16,
            Id_every                = 17,
            Id_filter               = 18,
            Id_forEach              = 19,
            Id_map                  = 20,
            Id_some                 = 21,
            Id_reduce               = 22,
            Id_reduceRight          = 23,

    MAX_PROTOTYPE_ID        = 23,


            ConstructorId_join                 = -Id_join,
            ConstructorId_reverse              = -Id_reverse,
            ConstructorId_sort                 = -Id_sort,
            ConstructorId_push                 = -Id_push,
            ConstructorId_pop                  = -Id_pop,
            ConstructorId_shift                = -Id_shift,
            ConstructorId_unshift              = -Id_unshift,
            ConstructorId_splice               = -Id_splice,
            ConstructorId_concat               = -Id_concat,
            ConstructorId_slice                = -Id_slice,
            ConstructorId_indexOf              = -Id_indexOf,
            ConstructorId_lastIndexOf          = -Id_lastIndexOf,
            ConstructorId_every                = -Id_every,
            ConstructorId_filter               = -Id_filter,
            ConstructorId_forEach              = -Id_forEach,
            ConstructorId_map                  = -Id_map,
            ConstructorId_some                 = -Id_some,
            ConstructorId_reduce               = -Id_reduce,
            ConstructorId_reduceRight          = -Id_reduceRight,
            ConstructorId_isArray              = -24,

    Id_length        =  1,
    MAX_INSTANCE_ID  =  1;

    @Override
    protected void fillConstructorProperties(IdFunctionObject ctor)
    {
        addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_join,
                "join", 1);
        addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_reverse,
                "reverse", 0);
        addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_sort,
                "sort", 1);
        addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_push,
                "push", 1);
        addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_pop,
                "pop", 0);
        addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_shift,
                "shift", 0);
        addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_unshift,
                "unshift", 1);
        addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_splice,
                "splice", 2);
        addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_concat,
                "concat", 1);
        addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_slice,
                "slice", 2);
        addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_indexOf,
                "indexOf", 1);
        addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_lastIndexOf,
                "lastIndexOf", 1);
        addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_every,
                "every", 1);
        addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_filter,
                "filter", 1);
        addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_forEach,
                "forEach", 1);
        addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_map,
                "map", 1);
        addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_some,
                "some", 1);
        addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_reduce,
                "reduce", 1);
        addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_reduceRight,
                "reduceRight", 1);
        addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_isArray,
                "isArray", 1);
        super.fillConstructorProperties(ctor);
    }

    @Override
    protected void initPrototypeId(int id)
    {
        String s;
        int arity;
        switch (id) {
            case Id_constructor:    arity=1; s="constructor";    break;
            case Id_toString:       arity=0; s="toString";       break;
            case Id_toLocaleString: arity=0; s="toLocaleString"; break;
            case Id_toSource:       arity=0; s="toSource";       break;
            case Id_join:           arity=1; s="join";           break;
            case Id_reverse:        arity=0; s="reverse";        break;
            case Id_sort:           arity=1; s="sort";           break;
            case Id_push:           arity=1; s="push";           break;
            case Id_pop:            arity=0; s="pop";            break;
            case Id_shift:          arity=0; s="shift";          break;
            case Id_unshift:        arity=1; s="unshift";        break;
            case Id_splice:         arity=2; s="splice";         break;
            case Id_concat:         arity=1; s="concat";         break;
            case Id_slice:          arity=2; s="slice";          break;
            case Id_indexOf:        arity=1; s="indexOf";        break;
            case Id_lastIndexOf:    arity=1; s="lastIndexOf";    break;
            case Id_every:          arity=1; s="every";          break;
            case Id_filter:         arity=1; s="filter";         break;
            case Id_forEach:        arity=1; s="forEach";        break;
            case Id_map:            arity=1; s="map";            break;
            case Id_some:           arity=1; s="some";           break;
            case Id_reduce:         arity=1; s="reduce";         break;
            case Id_reduceRight:    arity=1; s="reduceRight";    break;
            default: throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(ARRAY_TAG, id, s, arity);
    }


    private static final Integer NEGATIVE_ONE = Integer.valueOf(-1);

    /** The request being wrapped. */
    private final Parameter parameter;

    /** The list being wrapped. */
    private final List<Object> list;

    public ScriptableList(final AbstractFactory.ListFactory factory) {
        if (null == factory) {
            throw new NullPointerException();
        }
        this.list = new LazyList<Object>(factory);
        this.parameter = factory.getParameter();
    }

    /**
     * Constructs a new scriptable wrapper around the specified list.
     * 
     * @param list
     *            the list to be wrapped.
     * @throws NullPointerException
     *             if the specified list is {@code null}.
     */
    public ScriptableList(final Parameter operationParameter, final List<Object> list) {
        if (null == operationParameter) {
            throw new NullPointerException();
        }
        if (null == list) {
            throw new NullPointerException();
        }
        this.list = list;
        this.parameter = operationParameter;
    }


    @Override
    protected int getMaxInstanceId()
    {
        return MAX_INSTANCE_ID;
    }

    @Override
    protected void setInstanceIdAttributes(int id, int attr) {
        if (id == Id_length) {
            lengthAttr = attr;
        }
    }

    @Override
    protected int findInstanceIdInfo(String s)
    {
        if (s.equals("length")) {
            return instanceIdInfo(lengthAttr, Id_length);
        }
        return super.findInstanceIdInfo(s);
    }

    @Override
    protected String getInstanceIdName(int id)
    {
        if (id == Id_length) { return "length"; }
        return super.getInstanceIdName(id);
    }

    @Override
    protected Object getInstanceIdValue(int id)
    {
        if (id == Id_length) {
            return ScriptRuntime.wrapNumber(getLength());
        }
        return super.getInstanceIdValue(id);
    }

    @Override
    protected void setInstanceIdValue(int id, Object value)
    {
        if (id == Id_length) {
            return;
        }
        super.setInstanceIdValue(id, value);
    }



    @Override
    public int getAttributes(int index) {
        if (index >= 0 && index < getLength() && list.get(index) != null /* or NOT_FOUND*/) {
            return EMPTY;
        }
        return super.getAttributes(index);
    }

    @Override
    protected ScriptableObject getOwnPropertyDescriptor(Context cx, Object id) {
        if (!list.isEmpty()) {
            try {
                //TODO more proper conversion
                int index = Integer.parseInt((String)id);
                Object value = list.get(index);
                if (value != null || value != NOT_FOUND) {
                return defaultIndexPropertyDescriptor(value);
                }
            } catch (IndexOutOfBoundsException e) {
                /* ignore */
            } catch (NumberFormatException e) {
                 /* ignore */
            }
        }
        return super.getOwnPropertyDescriptor(cx, id);
    }

    private ScriptableObject defaultIndexPropertyDescriptor(Object value) {
        Scriptable scope = getParentScope();
        if (scope == null) scope = this;
        ScriptableObject desc = new NativeObject();
        ScriptRuntime.setBuiltinProtoAndParent(desc, scope, TopLevel.Builtins.Object);
        desc.defineProperty("value", value, EMPTY);
        desc.defineProperty("writable", true, EMPTY);
        desc.defineProperty("enumerable", true, EMPTY);
        desc.defineProperty("configurable", true, EMPTY);
        return desc;
    }


    /*
     * @Override public String getClassName() { return "ScriptableList"; }
     */
    @Override
    public Object get(int index, Scriptable start)
    {
        if (isGetterOrSetter(null, index, false))
            return super.get(index, start);
        try {
            return Converter.wrap(parameter, list.get(index), start, list instanceof LazyList);
        } catch (IndexOutOfBoundsException e) {
            /* ignore */
        }
        return super.get(index, start);
    }

    @Override
    public boolean has(int index, Scriptable start)
    {
        if (isGetterOrSetter(null, index, false))
            return super.has(index, start);
        if ( 0 <= index && index < getLength())
            return list.get(index) != null /*NOT_FOUND*/;
        return super.has(index, start);
    }


    @Override
    public Object get(String name, Scriptable start) {
        Object o = super.get(name, start);
        return o;
        /*
         * if ("length".equals(name)) { return getLength(); } else { return
         * NOT_FOUND; }
         */
    }

    @Override
    public boolean has(String name, Scriptable start) {
        //length is only supported property
        return "length".equals(name);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        if ("length".equals(name)) {
            if (!(value instanceof Number)) {
                throw Context.reportRuntimeError("invalid array length");
            }
            int length = ((Number) value).intValue();
            if (length < 0) {
                throw Context.reportRuntimeError("invalid array length");
            }
            resize(length);
        }
    }

    @Override
    public void delete(String name) {
        // attempt to delete any property is silently ignored
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        if (index < 0) {
            throw Context.reportRuntimeError("index out of bounds");
        }
        if (list.size() < index + 1) {
            resize(index + 1); // "sparsely" allocate null elements if index
                               // exceeds size
        }
        value = Converter.convert(value);
        try {
            if (index < list.size()) {
                list.set(index, value);
            } else {
                list.add(value);
            }
        } catch (Exception e) {
            throw Context.reportRuntimeError("list prohibits modification");
        }
    }



    @Override
    public void delete(int index) {
        if (index >= 0 && index < list.size()) {
            try {
                list.set(index, null);
                // "sparse" allocation; does not remove elements
            } catch (Exception e) {
                throw Context.reportRuntimeError("list prohibits modification");
            }
        }
    }

    @Override
    public Object[] getIds() {
        Object[] result = new Object[list.size()];
        for (int n = 0; n < result.length; n++) {
            result[n] = Integer.valueOf(n);
        }
        return result;
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        if (hint == null || hint == String.class) {
            return toString();
        } else if (hint == Number.class) {
            return Double.NaN;
        } else if (hint == Boolean.class) {
            return Boolean.TRUE;
        } else {
            return this;
        }
    }

    @Override
    public boolean hasInstance(Scriptable instance) {
        return false; // no support for javascript instanceof
    }

    @Override
    public Object unwrap() {
        return list;
    }


    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj,
            Object[] args) {
        if (!f.hasTag(ARRAY_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        again:
        for (;;) {
            switch (id) {
            case ConstructorId_join:
            case ConstructorId_reverse:
            case ConstructorId_sort:
            case ConstructorId_push:
            case ConstructorId_pop:
            case ConstructorId_shift:
            case ConstructorId_unshift:
            case ConstructorId_splice:
            case ConstructorId_concat:
            case ConstructorId_slice:
            case ConstructorId_indexOf:
            case ConstructorId_lastIndexOf:
            case ConstructorId_every:
            case ConstructorId_filter:
            case ConstructorId_forEach:
            case ConstructorId_map:
            case ConstructorId_some:
            case ConstructorId_reduce:
            case ConstructorId_reduceRight: {
                if (args.length > 0) {
                    thisObj = ScriptRuntime.toObject(scope, args[0]);
                    Object[] newArgs = new Object[args.length - 1];
                    for (int i = 0; i < newArgs.length; i++)
                        newArgs[i] = args[i + 1];
                    args = newArgs;
                }
                id = -id;
                continue again;
            }

            case ConstructorId_isArray:
                return args.length > 0 && (args[0] instanceof NativeArray);

            case Id_constructor: {
                boolean inNewExpr = (thisObj == null);
                if (!inNewExpr) {
                    // IdFunctionObject.construct will set up parent, proto
                    return f.construct(cx, scope, args);
                }
                throw ScriptRuntime.throwError(cx, this, "Constructor is not implemented");
                // return jsConstructor(cx, scope, args);
            }

            case Id_toString:
                return new JsonValue(list).toString();

            case Id_toLocaleString:
                return new JsonValue(list).toString();

            case Id_toSource:
                return new JsonValue(list).toString();

            case Id_join:
                return js_join(cx, thisObj, args);

            case Id_reverse:
                throw ScriptRuntime.throwError(cx, this, "reverse is not implemented");
                // return js_reverse(cx, thisObj, args);

            case Id_sort:
                throw ScriptRuntime.throwError(cx, this, "sort is not implemented");
                // return js_sort(cx, scope, thisObj, args);

            case Id_push:
                throw ScriptRuntime.throwError(cx, this, "push is not implemented");
                // return js_push(cx, thisObj, args);

            case Id_pop:
                throw ScriptRuntime.throwError(cx, this, "pop is not implemented");
                // return js_pop(cx, thisObj, args);

            case Id_shift:
                throw ScriptRuntime.throwError(cx, this, "shift is not implemented");
                // return js_shift(cx, thisObj, args);

            case Id_unshift:
                throw ScriptRuntime.throwError(cx, this, "unshift is not implemented");
                // return js_unshift(cx, thisObj, args);

            case Id_splice:
                throw ScriptRuntime.throwError(cx, this, "splice is not implemented");
                // return js_splice(cx, scope, thisObj, args);

            case Id_concat:
                throw ScriptRuntime.throwError(cx, this, "concat is not implemented");
                // return js_concat(cx, scope, thisObj, args);

            case Id_slice:
                throw ScriptRuntime.throwError(cx, this, "slice is not implemented");
                // return js_slice(cx, thisObj, args);

            case Id_indexOf:
                return indexOfHelper(cx, thisObj, args, false);

            case Id_lastIndexOf:
                return indexOfHelper(cx, thisObj, args, true);

            case Id_every:
            case Id_filter:
            case Id_forEach:
            case Id_map:
            case Id_some:
                throw ScriptRuntime.throwError(cx, this, "some is not implemented");
                // return iterativeMethod(cx, id, scope, thisObj, args);
            case Id_reduce:
            case Id_reduceRight:
                throw ScriptRuntime.throwError(cx, this, "reduceRight is not implemented");
                // return reduceMethod(cx, id, scope, thisObj, args);
            }
            throw new IllegalArgumentException(String.valueOf(id));
        }
    }

    /**
     * Attempts to resize the list to the specified size. If growing the list,
     * "sparse" elements with {@code null} value are added.
     *
     * @param size
     *            the required size of the list.
     * @throws org.mozilla.javascript.EvaluatorException
     *             if the list could not be resized.
     */
    protected void resize(int size) {
        while (list.size() < size) {
            try {
                list.add(null);
            } catch (Exception e) {
                throw Context.reportRuntimeError("list prohibits addition of null elements");
            }
        }
        while (list.size() > size) {
            try {
                list.remove(size);
            } catch (Exception e) {
                throw Context.reportRuntimeError("list prohibits element removal");
            }
        }
    }

    public long getLength() {
        /*
         * Required because ScriptRuntime use NativeArray.getLengthProperty(cx,
         * object);
         */
        return ScriptRuntime.toUint32(list.size());
    }

    /**
     * Implements the methods "indexOf" and "lastIndexOf".
     */
    protected Object indexOfHelper(Context cx, Scriptable thisObj, Object[] args, boolean isLast) {

        if (thisObj instanceof ScriptableList) {

            ScriptableList scriptableList = (ScriptableList) thisObj;

            Object compareTo = args.length > 0 ? args[0] : Undefined.instance;

            long length = scriptableList.getLength();
            /*
             * thisObj instanceof NativeArray ? ((NativeArray)
             * thisObj).getLength() :
             * ScriptRuntime.toUint32(ScriptRuntime.getObjectProp(thisObj,
             * "length", cx));
             */
            long start;
            if (isLast) {
                // lastIndexOf
                /*
                 * From http://developer.mozilla.org/en/docs/Core_JavaScript_1
                 * .5_Reference:Objects:Array:lastIndexOf The index at which to
                 * start searching backwards. Defaults to the array's length,
                 * i.e. the whole array will be searched. If the index is
                 * greater than or equal to the length of the array, the whole
                 * array will be searched. If negative, it is taken as the
                 * offset from the end of the array. Note that even when the
                 * index is negative, the array is still searched from back to
                 * front. If the calculated index is less than 0, -1 is
                 * returned, i.e. the array will not be searched.
                 */
                if (args.length < 2) {
                    // default
                    start = length - 1;
                } else {
                    start = (long) ScriptRuntime.toInteger(args[1]);
                    if (start >= length)
                        start = length - 1;
                    else if (start < 0)
                        start += length;
                    if (start < 0)
                        return NEGATIVE_ONE;
                }
            } else {
                // indexOf
                /*
                 * From http://developer.mozilla.org/en/docs/Core_JavaScript_1
                 * .5_Reference:Objects:Array:indexOf The index at which to
                 * begin the search. Defaults to 0, i.e. the whole array will be
                 * searched. If the index is greater than or equal to the length
                 * of the array, -1 is returned, i.e. the array will not be
                 * searched. If negative, it is taken as the offset from the end
                 * of the array. Note that even when the index is negative, the
                 * array is still searched from front to back. If the calculated
                 * index is less than 0, the whole array will be searched.
                 */
                if (args.length < 2) {
                    // default
                    start = 0;
                } else {
                    start = (long) ScriptRuntime.toInteger(args[1]);
                    if (start < 0) {
                        start += length;
                        if (start < 0)
                            start = 0;
                    }
                    if (start > length - 1)
                        return NEGATIVE_ONE;
                }
            }

            if (isLast) {
                for (int i = (int) start; i >= 0; i--) {
                    if (scriptableList.list.get(i) != Scriptable.NOT_FOUND
                            && ScriptRuntime.shallowEq(scriptableList.list.get(i), compareTo)) {
                        return Long.valueOf(i);
                    }
                }
            } else {
                for (int i = (int) start; i < length; i++) {
                    if (scriptableList.list.get(i) != Scriptable.NOT_FOUND
                            && ScriptRuntime.shallowEq(scriptableList.list.get(i), compareTo)) {
                        return Long.valueOf(i);
                    }
                }
            }
            return NEGATIVE_ONE;
        } else if (thisObj instanceof NativeArray) {

            throw ScriptRuntime.throwError(cx, this,
                    "Can not delegate the method to super NativeArray");

            /*
             * case Id_indexOf: return indexOfHelper(cx, thisObj, args, false);
             * 
             * case Id_lastIndexOf: return indexOfHelper(cx, thisObj, args,
             * true); return ((NativeArray) thisObj).execIdCall(f, cx, this,
             * thisObj, args);
             */

        } else {
            throw ScriptRuntime.throwError(cx, this,
                    "Can not delegate the method to super NativeArray");
        }
    }

    /**
     * See ECMA 15.4.4.3
     */
    private String js_join(Context cx, Scriptable thisObj, Object[] args) {

        if (thisObj instanceof ScriptableList) {

            ScriptableList scriptableList = (ScriptableList) thisObj;

            long llength = scriptableList.getLength();
            int length = (int) llength;
            if (llength != length) {
                throw Context.reportRuntimeError(ScriptRuntime.getMessage1(
                        "msg.arraylength.too.big", String.valueOf(llength)));
            }
            // if no args, use "," as separator
            String separator =
                    (args.length < 1 || args[0] == Undefined.instance) ? "," : ScriptRuntime
                            .toString(args[0]);

            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Object temp : scriptableList.list) {
                if (temp != null && temp != Undefined.instance && temp != Scriptable.NOT_FOUND) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(separator);
                    }
                    sb.append(ScriptRuntime.toString(temp));
                }
            }
            return sb.toString();

        } else if (thisObj instanceof NativeArray) {

            ScriptRuntime.throwError(cx, this, "Can not delegate the method to super NativeArray");
            IdFunctionObject f = null;
            /*
             * case Id_join: return js_join(cx, thisObj, args);
             */

            return ScriptRuntime.toString(((NativeArray) thisObj).execIdCall(f, cx, this, thisObj,
                    args));

        } else {
            ScriptRuntime.throwError(cx, this, "Can not delegate the method to super NativeArray");
            return "";
        }
    }
}
