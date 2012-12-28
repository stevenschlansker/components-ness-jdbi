package com.nesscomputing.jdbi.exception;

import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;

interface ExceptionInspector
{
    String getSqlStatePrefix();
    void inspect(SQLException ex, String oldMessage, Throwable realCause, StatementContext ctx);
}
