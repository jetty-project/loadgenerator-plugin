//
//  ========================================================================
//  Copyright (c) 1995-2018 Webtide LLC, Olivier Lamy
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

package com.webtide.jetty.load.generator.jenkins.result;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.webtide.jetty.load.generator.jenkins.PluginConstants;
import com.webtide.jetty.load.generator.jenkins.RunInformations;
import hudson.model.Actionable;
import hudson.model.ProminentProjectAction;
import hudson.model.Run;
import hudson.util.RunList;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mortbay.jetty.load.generator.listeners.LoadResult;
import org.mortbay.jetty.load.generator.store.ElasticResultStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
public class LoadResultProjectAction
    extends Actionable
    implements ProminentProjectAction
{

    private static final Logger LOGGER = LoggerFactory.getLogger( LoadResultProjectAction.class );

    private final transient RunList<?> builds;

    private final String elasticHostName;

    private final transient Run<?, ?> lastRun;

    public LoadResultProjectAction( RunList<?> builds, Run<?, ?> lastRun, String elasticHostName )
    {
        this.builds = builds == null ? new RunList<>() : builds;
        this.lastRun = lastRun;
        this.elasticHostName = elasticHostName;
    }

    public void doTrend( StaplerRequest req, StaplerResponse rsp )
        throws IOException, ServletException
    {

    }


    public void doTitles( StaplerRequest req, StaplerResponse rsp )
        throws IOException, ServletException
    {

    }

    @Override
    public String getIconFileName()
    {
        return PluginConstants.ICON_URL;
    }

    @Override
    public String getUrlName()
    {
        return "loadtestresult";
    }


    /**
     *
     * @return all jetty versions available in loadresult index key version value number of results
     */
    public Map<String, String> getJettyVersions()
        throws IOException {
        LOGGER.debug( "getJettyVersions" );

        ElasticHost elasticHost = ElasticHost.get( elasticHostName );
        try (ElasticResultStore elasticResultStore = elasticHost.buildElasticResultStore();
             InputStream inputStream = this.getClass().getResourceAsStream("/distinctJettyVersion.json" ))
        {
            String distinctSearchQuery = IOUtils.toString( inputStream );

            String distinctResult = elasticResultStore.search( distinctSearchQuery );

            List<Map<String,String>> versionsListMap = JsonPath.parse( distinctResult ).read( "$.aggregations.version.buckets");

            Map<String, String> versions = versionsListMap.stream() //
                .collect( Collectors.toMap( m -> m.get( "key" ), m -> String.valueOf( m.get("doc_count") ) ) );
            return versions;
        }
    }

    public void doResponseTimeTrend( StaplerRequest req, StaplerResponse rsp )
        throws IOException, ServletException
    {

        LOGGER.debug( "doResponseTimeTrend" );

        ElasticHost elasticHost = ElasticHost.get( elasticHostName );
        try (ElasticResultStore elasticResultStore = elasticHost.buildElasticResultStore())
        {
            // getting data
            List<String> resultIds = new ArrayList<>();
            this.builds.stream().forEach( o -> {
                LoadTestdResultBuildAction loadTestdResultBuildAction = o.getAction( LoadTestdResultBuildAction.class );
                if ( loadTestdResultBuildAction != null //
                    && StringUtils.isNotEmpty( loadTestdResultBuildAction.getBuildId() ) )
                {
                    resultIds.add( loadTestdResultBuildAction.getBuildId() );
                }
            } );

            List<LoadResult> loadResults = elasticResultStore.get( resultIds );

            List<RunInformations> runInformations = //
                loadResults.stream() //
                    .map( loadResult -> new RunInformations( loadResult.getUuid(), //
                                                             loadResult.getCollectorInformations() ) //
                        .jettyVersion( loadResult.getServerInfo().getJettyVersion() ) ) //
                    .collect( Collectors.toList() );

            LoadTestResultPublisher.OBJECT_MAPPER.writeValue( rsp.getWriter(), runInformations );
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
        }


    }

    @Override
    public String getDisplayName()
    {
        return "Http Load Testing Result";
    }

    @Override
    public String getSearchUrl()
    {
        return getUrlName();
    }


}
