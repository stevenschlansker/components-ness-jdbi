package com.nesscomputing.jdbi.exception;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.skife.jdbi.v2.StatementContext;

public class CheckConstraintViolationException extends ConsistencyViolationException
{
    private static final Pattern PG_CHK_PATTERN = Pattern.compile("ERROR: [\\w ]+ for relation \"(\\w+)\" violates check constraint \"(\\w+)\"");
    private static final long serialVersionUID = 1L;
    private final String relationName;
    private final String constraintName;

    public CheckConstraintViolationException(String relationName, String constraintName, String message, Throwable cause, StatementContext ctx)
    {
        super(message, cause, ctx);
        this.relationName = relationName;
        this.constraintName = constraintName;
    }

    public String getRelationName()
    {
        return relationName;
    }

    public String getConstraintName()
    {
        return constraintName;
    }

    static class CheckConstraintInspector implements ExceptionInspector
    {
        @Override
        public String getSqlStatePrefix()
        {
            return "23514";
        }

        @Override
        public void inspect(SQLException ex, String oldMessage, Throwable realCause, StatementContext ctx)
        {
            Matcher matcher = PG_CHK_PATTERN.matcher(ex.getMessage());

            if (!matcher.find()) {
                return;
            }

            throw new CheckConstraintViolationException(matcher.group(1), matcher.group(2), oldMessage, realCause, ctx);
        }
    }
}
