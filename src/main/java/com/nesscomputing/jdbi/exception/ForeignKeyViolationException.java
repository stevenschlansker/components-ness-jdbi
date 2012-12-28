package com.nesscomputing.jdbi.exception;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.skife.jdbi.v2.StatementContext;

public class ForeignKeyViolationException extends ConsistencyViolationException
{
    private static final Pattern PG_FK_PATTERN = Pattern.compile("ERROR: insert or update on table \"(\\w+)\" violates foreign key constraint \"(\\w+)\"");
    private static final Pattern PG_FK_DETAIL_PATTERN = Pattern.compile("Detail: Key \\((\\w+)\\)=\\(([^)]+)\\) is not present in table \"(\\w+)\"");
    private static final long serialVersionUID = 1L;
    private final String relationName;
    private final String constraintName;
    private final String columnName;
    private final String columnValue;
    private final String foreignRelationName;

    public ForeignKeyViolationException(String relationName, String constraintName, String columnName, String columnValue, String foreignRelationName, String message, Throwable cause, StatementContext ctx)
    {
        super(message, cause, ctx);
        this.relationName = relationName;
        this.constraintName = constraintName;
        this.columnName = columnName;
        this.columnValue = columnValue;
        this.foreignRelationName = foreignRelationName;
    }

    public String getRelationName()
    {
        return relationName;
    }

    public String getConstraintName()
    {
        return constraintName;
    }

    public String getColumnName()
    {
        return columnName;
    }

    public String getColumnValue()
    {
        return columnValue;
    }

    public String getForeignRelationName()
    {
        return foreignRelationName;
    }

    static class ForeignKeyInspector implements ExceptionInspector
    {
        @Override
        public String getSqlStatePrefix()
        {
            return "23503";
        }

        @Override
        public void inspect(SQLException ex, String oldMessage, Throwable realCause, StatementContext ctx)
        {
            Matcher matcher = PG_FK_PATTERN.matcher(ex.getMessage());

            if (!matcher.find()) {
                return;
            }

            Matcher detailMatcher = PG_FK_DETAIL_PATTERN.matcher(ex.getMessage());

            String colName = null;
            String colValue = null;
            String fkTable = null;

            if (detailMatcher.find()) {
                colName = detailMatcher.group(1);
                colValue = detailMatcher.group(2);
                fkTable = detailMatcher.group(3);
            }

            throw new ForeignKeyViolationException(matcher.group(1), matcher.group(2), colName, colValue, fkTable, oldMessage, realCause, ctx);
        }
    }
}
