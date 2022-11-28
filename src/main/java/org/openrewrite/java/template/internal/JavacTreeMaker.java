
/*
 * Copyright (C) 2013-2020 The Project Lombok Authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.openrewrite.java.template.internal;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import org.openrewrite.internal.lang.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("SameParameterValue")
public class JavacTreeMaker {
    private final TreeMaker tm;

    public JavacTreeMaker(TreeMaker tm) {
        this.tm = tm;
    }

    public TreeMaker getUnderlyingTreeMaker() {
        return tm;
    }

    private static class SchroedingerType {
        @Nullable
        final Object value;

        private SchroedingerType(@Nullable Object value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return value == null ? -1 : value.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof SchroedingerType) {
                Object other = ((SchroedingerType) obj).value;
                return Objects.equals(value, other);
            }
            return false;
        }

        static Object getFieldCached(ConcurrentMap<String, Object> cache, String className, String fieldName) {
            Object value = cache.get(fieldName);
            if (value != null) {
                return value;
            }
            try {
                value = Permit.getField(Class.forName(className), fieldName).get(null);
            } catch (NoSuchFieldException | ClassNotFoundException | IllegalAccessException e) {
                throw Javac.sneakyThrow(e);
            }

            cache.putIfAbsent(fieldName, value);
            return value;
        }

        private static final Field NOSUCHFIELDEX_MARKER;

        static {
            try {
                NOSUCHFIELDEX_MARKER = Permit.getField(SchroedingerType.class, "NOSUCHFIELDEX_MARKER");
            } catch (NoSuchFieldException e) {
                throw Javac.sneakyThrow(e);
            }
        }

        static Object getFieldCached(ConcurrentMap<Class<?>, Field> cache, Object ref, String fieldName) throws NoSuchFieldException {
            Class<?> c = ref.getClass();
            Field field = cache.get(c);
            if (field == null) {
                try {
                    field = Permit.getField(c, fieldName);
                } catch (NoSuchFieldException e) {
                    cache.putIfAbsent(c, NOSUCHFIELDEX_MARKER);
                    throw Javac.sneakyThrow(e);
                }
                Permit.setAccessible(field);
                Field old = cache.putIfAbsent(c, field);
                if (old != null) {
                    field = old;
                }
            }

            if (field == NOSUCHFIELDEX_MARKER) {
                throw new NoSuchFieldException(fieldName);
            }
            try {
                return field.get(ref);
            } catch (IllegalAccessException e) {
                throw Javac.sneakyThrow(e);
            }
        }
    }

    public static class TypeTag extends SchroedingerType {
        private static final ConcurrentMap<String, Object> TYPE_TAG_CACHE = new ConcurrentHashMap<>();
        private static final ConcurrentMap<Class<?>, Field> FIELD_CACHE = new ConcurrentHashMap<>();
        private static final Method TYPE_TYPETAG_METHOD;

        static {
            Method m = null;
            try {
                m = Permit.getMethod(Type.class, "getTag");
            } catch (NoSuchMethodException ignored) {
            }
            TYPE_TYPETAG_METHOD = m;
        }

        private TypeTag(Object value) {
            super(value);
        }

        public static TypeTag typeTag(JCTree o) {
            try {
                return new TypeTag(getFieldCached(FIELD_CACHE, o, "typetag"));
            } catch (NoSuchFieldException e) {
                throw Javac.sneakyThrow(e);
            }
        }

        public static TypeTag typeTag(@Nullable Type t) {
            if (t == null) {
                return Javac.CTC_VOID;
            }
            try {
                return new TypeTag(getFieldCached(FIELD_CACHE, t, "tag"));
            } catch (NoSuchFieldException e) {
                if (TYPE_TYPETAG_METHOD == null) {
                    throw new IllegalStateException("Type " + t.getClass() + " has neither 'tag' nor getTag()");
                }
                try {
                    return new TypeTag(TYPE_TYPETAG_METHOD.invoke(t));
                } catch (IllegalAccessException ex) {
                    throw Javac.sneakyThrow(ex);
                } catch (InvocationTargetException ex) {
                    throw Javac.sneakyThrow(ex.getCause());
                }
            }
        }

        public static TypeTag typeTag(String identifier) {
            return new TypeTag(getFieldCached(TYPE_TAG_CACHE, "com.sun.tools.javac.code.TypeTag", identifier));
        }
    }

    public static class TreeTag extends SchroedingerType {
        private static final ConcurrentMap<String, Object> TREE_TAG_CACHE = new ConcurrentHashMap<>();
        private static final Field TAG_FIELD;
        private static final Method TAG_METHOD;

        static {
            Method m = null;
            try {
                m = Permit.getMethod(JCTree.class, "getTag");
            } catch (NoSuchMethodException ignored) {
            }

            if (m != null) {
                TAG_FIELD = null;
                TAG_METHOD = m;
            } else {
                Field f = null;
                try {
                    f = Permit.getField(JCTree.class, "tag");
                } catch (NoSuchFieldException ignored) {
                }
                TAG_FIELD = f;
                TAG_METHOD = null;
            }
        }

        private TreeTag(Object value) {
            super(value);
        }

        public static TreeTag treeTag(JCTree o) {
            try {
                if (TAG_METHOD != null) {
                    return new TreeTag(TAG_METHOD.invoke(o));
                } else {
                    return new TreeTag(TAG_FIELD.get(o));
                }
            } catch (InvocationTargetException e) {
                throw Javac.sneakyThrow(e.getCause());
            } catch (IllegalAccessException e) {
                throw Javac.sneakyThrow(e);
            }
        }

        public static TreeTag treeTag(String identifier) {
            return new TreeTag(getFieldCached(TREE_TAG_CACHE, "com.sun.tools.javac.tree.JCTree$Tag", identifier));
        }
    }
}
