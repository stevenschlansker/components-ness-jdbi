package com.nesscomputing.jdbi.exception;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;

public abstract class ConsistencyViolationException extends UnableToExecuteStatementException
{
    public ConsistencyViolationException(String message, Throwable cause, StatementContext ctx)
    {
        super(message, cause, ctx);
    }

    private static final long serialVersionUID = 1L;
}
