package com.nesscomputing.jdbi.exception;

import com.google.inject.AbstractModule;

import com.nesscomputing.config.ConfigProvider;

public class NessJdbiExceptionModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind (NessJdbiExceptionPolicy.class);
        bind (JdbiExceptionConfig.class).toProvider(ConfigProvider.of(JdbiExceptionConfig.class));
    }
}
