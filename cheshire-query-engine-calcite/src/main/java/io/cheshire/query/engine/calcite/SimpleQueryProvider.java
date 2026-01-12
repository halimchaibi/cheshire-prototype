package io.cheshire.query.engine.calcite;

import org.apache.calcite.linq4j.*;
import org.apache.calcite.linq4j.tree.Expression;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Type;
import java.util.Iterator;

public class SimpleQueryProvider implements QueryProvider {

    public static <T> Queryable<T> asQueryable(final Enumerable<T> enumerable) {
        return new AbstractQueryable<T>() {

            @Override
            public Iterator<T> iterator() {
                return new Iterator<>() {
                    private final Enumerator<T> enumerator = enumerable.enumerator();

                    @Override
                    public boolean hasNext() {
                        return enumerator.moveNext();
                    }

                    @Override
                    public T next() {
                        return enumerator.current();
                    }
                };
            }

            @Override
            public Enumerator<T> enumerator() {
                return enumerable.enumerator();
            }

            @Override
            public Type getElementType() {
                // Return Object.class as a generic element type
                return Object.class;
            }

            @Override
            public @Nullable Expression getExpression() {
                return null;
            }

            @Override
            public QueryProvider getProvider() {
                return new SimpleQueryProvider();
            }
        };
    }

    @Override
    public <T> Queryable<T> createQuery(Expression expression, Class<T> aClass) {
        throw new UnsupportedOperationException("Not supported for interpreter execution");
    }

    @Override
    public <T> Queryable<T> createQuery(Expression expression, Type type) {
        throw new UnsupportedOperationException("Not supported for interpreter execution");
    }

    @Override
    public <T> T execute(Expression expression, Class<T> aClass) {
        throw new UnsupportedOperationException("Not supported for interpreter execution");
    }

    @Override
    public <T> T execute(Expression expression, Type type) {
        throw new UnsupportedOperationException("Not supported for interpreter execution");
    }

    @Override
    public <T> Enumerator<T> executeQuery(Queryable<T> queryable) {
        return queryable.enumerator();
    }
}
