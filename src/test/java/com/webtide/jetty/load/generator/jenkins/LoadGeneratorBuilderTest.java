//
//  ========================================================================
//  Copyright (c) 1995-2016 Webtide LLC, Olivier Lamy
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================

package com.webtide.jetty.load.generator.jenkins;

import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.Versioned;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.TaskListener;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.io.ClientConnectionFactory;
import org.eclipse.jetty.load.generator.LoadGenerator;
import org.eclipse.jetty.load.generator.responsetime.ResponseNumberPerPath;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.StatisticsServlet;
import org.eclipse.jetty.toolchain.perf.PlatformTimer;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.Trie;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class LoadGeneratorBuilderTest
{
    private static final Logger LOGGER = LoggerFactory.getLogger( LoadGeneratorBuilderTest.class );

    @Rule
    public JenkinsRule j = new JenkinsRule();

    Server server;

    ServerConnector connector;

    StatisticsHandler statisticsHandler = new StatisticsHandler();

    @Before
    public void startJetty()
        throws Exception
    {
        QueuedThreadPool serverThreads = new QueuedThreadPool();
        serverThreads.setName( "server" );
        server = new Server( serverThreads );
        server.setSessionIdManager( new HashSessionIdManager() );
        connector = new ServerConnector( server, new HttpConnectionFactory( new HttpConfiguration() ) );
        server.addConnector( connector );

        server.setHandler( statisticsHandler );

        ServletContextHandler statsContext = new ServletContextHandler( statisticsHandler, "/" );

        statsContext.addServlet( new ServletHolder( new StatisticsServlet() ), "/stats" );

        statsContext.addServlet( new ServletHolder( new TestHandler() ), "/" );

        statsContext.setSessionHandler( new SessionHandler() );

        server.start();
    }

    @Test
    public void testWithgroovyScript()
        throws Exception
    {
        FreeStyleProject project = j.createFreeStyleProject();

        InputStream inputStream =
            Thread.currentThread().getContextClassLoader().getResourceAsStream( "website_profile.groovy" );

        int iteration = 2;

        LoadGeneratorBuilder loadGeneratorBuilder =
            new LoadGeneratorBuilder( IOUtils.toString( inputStream ), "localhost", connector.getLocalPort(), //
                                      1, "", 20, TimeUnit.SECONDS, //
                                      iteration, 1, LoadGenerator.Transport.HTTP, false );

        ResponseNumberPerPath responseNumberPerPath = new ResponseNumberPerPath();

        loadGeneratorBuilder.addResponseTimeListener( responseNumberPerPath );

        project.getBuildersList().add( loadGeneratorBuilder );

        FreeStyleBuild build = project.scheduleBuild2( 0 ).get();


        if (build.getResult() != Result.SUCCESS)
        {
            LOGGER.error( "build failed: {}", IOUtils.toString( build.getLogInputStream() ) );
            Assert.assertEquals( Result.SUCCESS, build.getResult() );
        } else {
            LOGGER.error( "build {}: {}", build.getResult(), IOUtils.toString( build.getLogInputStream() ) );
        }

        System.out.println("build log: " +  IOUtils.toString( build.getLogInputStream() )  );

        LoadGeneratorBuildAction action = build.getAction( LoadGeneratorBuildAction.class );

        //action.getAllResponseInfoTimePerPath().size()

        Assert.assertEquals( 12, responseNumberPerPath.getResponseNumberPerPath().size() );
        /*
        for ( Map.Entry<String, AtomicInteger> entry : action.getPerPath().entrySet() ) // responseNumberPerPath.getResponseNumberPerPath().entrySet() )
        {
            Assert.assertEquals( "not " + iteration + " but " + entry.getValue().get() + " for path " + entry.getKey(),
                                 //
                                 entry.getValue().get(), iteration );
        }
        */

    }


    static class TestHandler
        extends HttpServlet
    {

        @Override
        protected void service( HttpServletRequest request, HttpServletResponse response )
            throws ServletException, IOException
        {

            String method = request.getMethod().toUpperCase( Locale.ENGLISH );

            HttpSession httpSession = request.getSession();

            switch ( method )
            {
                case "GET":
                {
                    response.getOutputStream().write( "Jetty rocks!!".getBytes() );
                    response.flushBuffer();
                    break;
                }
                case "POST":
                {
                    IO.copy( request.getInputStream(), response.getOutputStream() );
                    break;
                }
            }

        }
    }

    @Extension
    public static class UnitTestDecorator extends LoadGeneratorProcessClasspathDecorator
    {
        @Override
        public String decorateClasspath( String cp, TaskListener listener, FilePath slaveRoot,
                                         Launcher launcher )
            throws Exception
        {

            List<Class> classes =
                Arrays.asList( JCommander.class, LoadGenerator.class, ObjectMapper.class, Versioned.class, //
                               JsonView.class, HttpMethod.class, Trie.class, HttpClientTransport.class, //
                               ClientConnectionFactory.class, PlatformTimer.class );

            for (Class clazz : classes)
            {
                cp = cp + ( launcher.isUnix() ? ":" : ";" ) //
                    + LoadGeneratorProcessFactory.classPathEntry( slaveRoot, clazz, clazz.getSimpleName(), listener );
            }


            return cp;

        }
    }

}
