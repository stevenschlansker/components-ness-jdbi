package com.nesscomputing.jdbi.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URI;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

import com.nesscomputing.db.postgres.junit.EmbeddedPostgresRules;
import com.nesscomputing.db.postgres.junit.EmbeddedPostgresTestDatabaseRule;

public class TestNessJdbiExceptionPolicy
{
    @Rule
    public EmbeddedPostgresTestDatabaseRule dbRule = EmbeddedPostgresRules.embeddedDatabaseRule(URI.create(""));

    private DBI dbi;
    private Dao dao;

    @Before
    public void openDbi()
    {
        dbi = new DBI(dbRule.getControl().getJdbcUri(), "postgres", "");
        dbi.setExceptionPolicy(new NessJdbiExceptionPolicy());

        dbi.inTransaction(new TransactionCallback<Void>() {
            @Override
            public Void inTransaction(Handle conn, TransactionStatus status) throws Exception
            {
                conn.execute("CREATE TABLE test (" +
                        "a INTEGER UNIQUE CONSTRAINT chkconstraint CHECK (a < 3)" +
                    ");" +
                    "CREATE TABLE referrer (" +
                        "fka INTEGER CONSTRAINT blah REFERENCES test(a)" +
                    ");");
                return null;
            }
        });

        dao = dbi.onDemand(Dao.class);
    }

    @Test
    public void testCheckConstraint()
    {
        try {
            dao.insertA(5);
            fail();
        } catch (CheckConstraintViolationException e)
        {
            assertEquals("test", e.getRelationName());
            assertEquals("chkconstraint", e.getConstraintName());
        }
    }

    @Test
    public void testForeignKey()
    {
        try {
            dao.insertA(2);
            dao.insertARef(3);
            fail();
        } catch (ForeignKeyViolationException e)
        {
            assertEquals("referrer", e.getRelationName());
            assertEquals("blah", e.getConstraintName());
            assertEquals("fka", e.getColumnName());
            assertEquals("3", e.getColumnValue());
            assertEquals("test", e.getForeignRelationName());
        }
    }

    @Test
    public void testUniqueViolation()
    {
        try {
            dao.insertA(2);
            dao.insertA(2);
            fail();
        } catch (UniqueConstraintViolationException e)
        {
            assertEquals("test_a_key", e.getConstraintName());
        }
    }

    public interface Dao
    {
        @SqlUpdate("INSERT INTO test VALUES(:a)")
        void insertA(@Bind("a") int a);

        @SqlUpdate("INSERT INTO referrer VALUES(:a)")
        void insertARef(@Bind("a") int a);
    }
}
