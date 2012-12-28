package com.nesscomputing.jdbi.exception;

import java.sql.SQLException;
import java.util.List;

import com.google.common.collect.Lists;

class Unwrapper
{
    private Unwrapper() { }

    public static Iterable<SQLException> findSqlExceptions(Throwable cause)
    {
        List<SQLException> result = Lists.newArrayList();

        do {
            if (cause instanceof SQLException) {
                SQLException sqlEx = (SQLException) cause;

                do {
                    result.add(sqlEx);
                } while ( (sqlEx = sqlEx.getNextException()) != null);

            }
        } while ( (cause = cause.getCause()) != null);

        return result;
    }
}
