package com.webtide.jetty.load.generator.jenkins.steps;

import hudson.Extension;
import org.eclipse.jetty.load.generator.LoadGenerator;
import org.eclipse.jetty.load.generator.profile.ResourceProfile;
import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.StaticWhitelist;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Created by olamy on 4/10/16.
 */
public class LoadGeneratorBuilderStep
    extends AbstractStepImpl
{


    private ResourceProfile resourceProfile;


    private String host;


    private int port;


    private int users;


    private String profileXmlFromFile;


    private int runningTime;


    private TimeUnit runningTimeUnit;


    private int runIteration;


    private int transactionRate;


    private LoadGenerator.Transport transport;


    private boolean secureProtocol;

    @DataBoundConstructor
    public LoadGeneratorBuilderStep( ResourceProfile resourceProfile, String host, int port, int users, String profileXmlFromFile,
                                     int runningTime, TimeUnit runningTimeUnit, int runIteration, int transactionRate,
                                     LoadGenerator.Transport transport, boolean secureProtocol )
    {
        this.resourceProfile = resourceProfile;
        this.host = host;
        this.port = port;
        this.users = users;
        this.profileXmlFromFile = profileXmlFromFile;
        this.runningTime = runningTime;
        this.runningTimeUnit = runningTimeUnit;
        this.runIteration = runIteration;
        this.transactionRate = transactionRate;
        this.transport = transport;
        this.secureProtocol = secureProtocol;
    }

    public ResourceProfile getResourceProfile()
    {
        return resourceProfile;
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public int getUsers()
    {
        return users;
    }

    public String getProfileXmlFromFile()
    {
        return profileXmlFromFile;
    }

    public int getRunningTime()
    {
        return runningTime;
    }

    public TimeUnit getRunningTimeUnit()
    {
        return runningTimeUnit;
    }

    public int getRunIteration()
    {
        return runIteration;
    }

    public int getTransactionRate()
    {
        return transactionRate;
    }

    public LoadGenerator.Transport getTransport()
    {
        return transport;
    }

    public boolean isSecureProtocol()
    {
        return secureProtocol;
    }

    @Extension
    public static class DescriptorImpl
        extends AbstractStepDescriptorImpl
    {
        public DescriptorImpl()
        {
            super( LoadGeneratorBuilderStepExecution.class );
        }

        public DescriptorImpl( Class<? extends StepExecution> executionType )
        {
            super( executionType );
        }

        @Override
        public String getFunctionName()
        {
            return "loadgenerator";
        }

        @Nonnull
        @Override
        public String getDisplayName()
        {
            return "HTTP Load Generator by Jetty";
        }
    }

    @Extension
    public static class LoadGeneratorWhileList
        extends Whitelist
    {
        private StaticWhitelist staticWhitelist;

        public LoadGeneratorWhileList()
        {
            try
            {
                try (InputStream inputStream = LoadGeneratorBuilderStep.class.getResourceAsStream(
                    "/com/webtide/jetty/load/generator/jenkins/steps/LoadGeneratorBuilderStep/loadgenerator-whilelist" ))
                {
                    try (InputStreamReader inputStreamReader = new InputStreamReader( inputStream ))
                    {
                        staticWhitelist = new StaticWhitelist( inputStreamReader );
                    }
                }
            }
            catch ( Exception e )
            {
                throw new RuntimeException( e.getMessage(), e );
            }
        }

        @Override
        public boolean permitsMethod( @Nonnull Method method, @Nonnull Object o, @Nonnull Object[] objects )
        {
            return staticWhitelist.permitsMethod( method, o, objects );
        }

        @Override
        public boolean permitsConstructor( @Nonnull Constructor<?> constructor, @Nonnull Object[] objects )
        {
            return staticWhitelist.permitsConstructor( constructor, objects );
        }

        @Override
        public boolean permitsStaticMethod( @Nonnull Method method, @Nonnull Object[] objects )
        {
            return staticWhitelist.permitsStaticMethod( method, objects );
        }

        @Override
        public boolean permitsFieldGet( @Nonnull Field field, @Nonnull Object o )
        {
            return staticWhitelist.permitsFieldGet( field, o );
        }

        @Override
        public boolean permitsFieldSet( @Nonnull Field field, @Nonnull Object o, @CheckForNull Object o1 )
        {
            return staticWhitelist.permitsFieldSet( field, o, o1 );
        }

        @Override
        public boolean permitsStaticFieldGet( @Nonnull Field field )
        {
            if (field.getType().equals( LoadGenerator.Transport.class )
                || field.getType().equals( TimeUnit.class ))
            {
                return true;
            }
            return staticWhitelist.permitsStaticFieldGet( field );
        }

        @Override
        public boolean permitsStaticFieldSet( @Nonnull Field field, @CheckForNull Object o )
        {
            return staticWhitelist.permitsStaticFieldSet( field, o );
        }
    }

}
