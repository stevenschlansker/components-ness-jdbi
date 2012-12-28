package com.nesscomputing.jdbi.exception;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.skife.jdbi.v2.StatementContext;

public class UniqueConstraintViolationException extends ConsistencyViolationException
{
    private static final Pattern PG_UNIQ_PATTERN = Pattern.compile("ERROR: duplicate key value violates unique constraint \"(\\w+)\"");
    private static final long serialVersionUID = 1L;
    private final String constraintName;

    public UniqueConstraintViolationException(String constraintName, String message, Throwable cause, StatementContext ctx)
    {
        super(message, cause, ctx);
        this.constraintName = constraintName;
    }

    public String getConstraintName()
    {
        return constraintName;
    }

    static class UniqueConstraintInspector implements ExceptionInspector
    {
        @Override
        public String getSqlStatePrefix()
        {
            return "23505";
        }

        @Override
        public void inspect(SQLException ex, String oldMessage, Throwable realCause, StatementContext ctx)
        {
            Matcher matcher = PG_UNIQ_PATTERN.matcher(ex.getMessage());

            if (!matcher.find()) {
                return;
            }

            throw new UniqueConstraintViolationException(matcher.group(1), oldMessage, realCause, ctx);
        }
    }
}
