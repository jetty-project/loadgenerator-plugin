//import org.eclipse.jetty.load.generator.profile.Resource
//import org.eclipse.jetty.load.generator.profile.ResourceProfile

return new ResourceProfile(new Resource( "index.html",
                             new Resource( "/style.css",
                               new Resource( "/logo.gif" ),
                               new Resource( "/spacer.png" )
                             ),
                             new Resource( "/fancy.css" ),
                             new Resource( "/script.js",
                                           new Resource( "/library.js" ),
                                           new Resource( "/morestuff.js" )
                            ),
                            new Resource( "/anotherScript.js" ),
                            new Resource( "/iframeContents.html" ),
                            new Resource( "/moreIframeContents.html" ),
                            new Resource( "/favicon.ico" )
    )
);
