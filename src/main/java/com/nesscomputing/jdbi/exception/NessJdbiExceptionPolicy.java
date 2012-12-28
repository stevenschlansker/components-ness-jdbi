package com.nesscomputing.jdbi.exception;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.exceptions.DBIException;
import org.skife.jdbi.v2.exceptions.ExceptionPolicy;

import com.nesscomputing.config.Config;
import com.nesscomputing.jdbi.exception.CheckConstraintViolationException.CheckConstraintInspector;
import com.nesscomputing.jdbi.exception.ForeignKeyViolationException.ForeignKeyInspector;
import com.nesscomputing.jdbi.exception.UniqueConstraintViolationException.UniqueConstraintInspector;
import com.nesscomputing.logging.Log;

public class NessJdbiExceptionPolicy extends ExceptionPolicy
{
    private static final Log LOG = Log.findLog();
    private final JdbiExceptionConfig config;

    private final NavigableMap<String, List<ExceptionInspector>> inspectors;

    {
        inspectors = indexInspectors(
                new CheckConstraintInspector(),
                new ForeignKeyInspector(),
                new UniqueConstraintInspector()
            );
    }

    public NessJdbiExceptionPolicy()
    {
        this (Config.getEmptyConfig());
    }

    public NessJdbiExceptionPolicy(Config config)
    {
        this(config.getBean(JdbiExceptionConfig.class));
    }

    @Inject
    NessJdbiExceptionPolicy(JdbiExceptionConfig config)
    {
        this.config = config;
    }

    private NavigableMap<String, List<ExceptionInspector>> indexInspectors(ExceptionInspector... inspectors)
    {
        TreeMap<String, List<ExceptionInspector>> index = new TreeMap<>();

        for (ExceptionInspector inspector : inspectors) {
            String key = inspector.getSqlStatePrefix();
            List<ExceptionInspector> list = index.get(key);

            if (list == null) {
                list = new ArrayList<>();
                index.put(key, list);
            }

            list.add(inspector);
        }

        for (Entry<String, List<ExceptionInspector>> e : index.entrySet()) {
            e.setValue(Collections.unmodifiableList(e.getValue()));
        }

        return Maps.unmodifiableNavigableMap(index);
    }

    @Override
    public DBIException unableToExecuteStatement(String msg, Throwable cause, StatementContext ctx)
    {
        SQLException first = null;
        for (SQLException sqlExn : Unwrapper.findSqlExceptions(cause)) {
            if (first == null) {
                first = sqlExn;
            }

            String sqlState = sqlExn.getSQLState();
            while (!sqlState.isEmpty()) {
                tryToThrow(sqlExn, sqlState, msg, cause, ctx);
                sqlState = sqlState.substring(0, sqlState.length() - 1);
            }
        }

        if (first != null) {
            LOG.debug("Unhandled SQLState %s", first.getSQLState());
        }

        throw super.unableToExecuteStatement(msg, cause, ctx);
    }

    private void tryToThrow(SQLException sqlEx, String sqlState, String msg, Throwable cause, StatementContext ctx)
    {
        List<ExceptionInspector> list = inspectors.get(sqlState);
        if (list == null) {
            return;
        }
        for (ExceptionInspector inspector : list) {
            inspector.inspect(sqlEx, msg, cause, ctx);
        }
    }
}
