package org.bio2rdf.servlets;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.queryall.*;
import org.queryall.impl.*;
import org.queryall.helpers.*;

import org.apache.log4j.Logger;

import org.openrdf.model.URI;

/** 
 * 
 */

public class NamespaceProvidersServlet extends HttpServlet 
{
    public static final Logger log = Logger.getLogger(NamespaceProvidersServlet.class.getName());
    public static final boolean _TRACE = log.isTraceEnabled();
    public static final boolean _DEBUG = log.isDebugEnabled();
    public static final boolean _INFO = log.isInfoEnabled();

    
    @Override
    public void doGet(HttpServletRequest request,
                        HttpServletResponse response)
        throws ServletException, IOException 
    {
        // Settings.setServletContext(getServletConfig().getServletContext());
        
        PrintWriter out = response.getWriter();
        response.setContentType("text/html");
        
        @SuppressWarnings("unused")
		String subversionId = "$Id: $";
        
        Date currentDate = new Date();
        String now = Utilities.ISO8601UTC().format(currentDate);
        
        @SuppressWarnings("unused")
        String realHostName = request.getScheme() + "://" + request.getServerName() + (request.getServerPort() == 80 ? "" : ":"+ request.getServerPort())+"/";
        
        Map<URI, Provider> allProviders = Settings.getAllProviders();
        
        Map<URI, NamespaceEntry> allNamespaceEntries = Settings.getAllNamespaceEntries();
        
        Map<URI, NormalisationRule> allRdfRules = Settings.getAllNormalisationRules();
        
        Map<URI, RuleTest> allRdfRuleTests = Settings.getAllRuleTests();
        
        Map<URI, Profile> allProfiles = Settings.getAllProfiles();
        
        Map<URI, Collection<Provider>> providersByNamespace = new Hashtable<URI, Collection<Provider>>();
        
        Map<URI, Collection<Provider>> providersByQueryKey = new Hashtable<URI, Collection<Provider>>();
        
        Map<String, Collection<Provider>> allQueryTypesByNamespace = new Hashtable<String, Collection<Provider>>();
        
        StringBuilder rdfStrings = new StringBuilder();
        
        int overallQueryTypeProviders = 0;
        int overallNamespaceProviders = 0;
        int overallQueryTypeByNamespaceProviders = 0;
        
        for(URI nextKey : allProviders.keySet())
        {
            Provider nextProvider = allProviders.get(nextKey);
            
            for(URI nextQueryKey : nextProvider.getIncludedInCustomQueries())
            {
                if(!providersByQueryKey.containsKey(nextQueryKey))
                {
                    Collection<Provider> queryProviders = Settings.getProvidersForQueryType(nextQueryKey);
                    
                    // TODO: implement interface Comparable for Provider
                    // Collections.sort(queryProviders)
                    
                    providersByQueryKey.put(nextQueryKey, queryProviders);
                    
                    overallQueryTypeProviders += queryProviders.size();
                }
            }
            
            for(URI nextNamespace : nextProvider.getNamespaces())
            {
                if(nextNamespace != null && !providersByNamespace.containsKey(nextNamespace))
                {
                    Collection<Collection<URI>> nextNamespacesList = new HashSet<Collection<URI>>();
                    Collection<URI> nextNamespaces = new HashSet<URI>(4);
                    nextNamespaces.add(nextNamespace);
                    nextNamespacesList.add(nextNamespaces);
                    
                    Collection<Provider> namespaceProviders = Settings.getProvidersForNamespaceUris(nextNamespacesList, QueryTypeImpl.queryNamespaceMatchAny);
                    
                    providersByNamespace.put(nextNamespace, namespaceProviders);
                    
                    overallNamespaceProviders += namespaceProviders.size();
                    
                    for(Provider nextNamespaceProvider : namespaceProviders)
                    {
                        for(URI nextQueryKey : nextNamespaceProvider.getIncludedInCustomQueries())
                        {
                            if(!allQueryTypesByNamespace.containsKey(nextQueryKey.stringValue() + " " + nextNamespace.stringValue()))
                            {
                                Collection<Collection<URI>> nextQueryTypesByNamespacesList = new HashSet<Collection<URI>>();
                                Collection<URI> nextQueryTypesByNamespaces = new HashSet<URI>(4);
                                nextQueryTypesByNamespaces.add(nextNamespace);
                                nextQueryTypesByNamespacesList.add(nextQueryTypesByNamespaces);
                                
                                Collection<Provider> queryTypesByNamespace = Settings.getProvidersForQueryTypeForNamespaceUris(nextQueryKey, nextQueryTypesByNamespacesList, QueryTypeImpl.queryNamespaceMatchAny);
                                
                                allQueryTypesByNamespace.put(nextQueryKey + " " + nextNamespace, queryTypesByNamespace);
                                
                                overallQueryTypeByNamespaceProviders += queryTypesByNamespace.size();
                            }
                        }
                    }
                }
            }
        }
        
        
        out.write("<br />Number of namespaces that are known = " + allNamespaceEntries.size()+"<br />\n");
        out.write("<br />Number of namespaces that have providers = " + providersByNamespace.size()+"<br />\n");
        out.write("<br />Number of query titles = " + providersByQueryKey.size()+"<br />\n");
        out.write("<br />Number of providers = " + allProviders.size()+"<br />\n");
        out.write("<br />Number of rdf normalisation rules = " + allRdfRules.size()+"<br />\n");
        out.write("<br />Number of rdf normalisation rule tests = " + allRdfRuleTests.size()+"<br />\n");
        out.write("<br />Number of profiles = " + allProfiles.size()+"<br />\n");
        
        out.write("<br />Number of namespace provider options = " + overallNamespaceProviders+"<br />\n");
        out.write("<br />Number of query title provider options = " + overallQueryTypeProviders+"<br />\n");
        out.write("<br />Number of query title and namespace combinations = " + allQueryTypesByNamespace.size()+"<br />\n");
        out.write("<br />Number of query title and namespace combination provider options = " + overallQueryTypeByNamespaceProviders+"<br /><br />\n");
        
        out.write("Raw complete namespace Collection<br />\n");
        
        for(URI nextUniqueNamespace : providersByNamespace.keySet())
        {
            out.write(nextUniqueNamespace.stringValue()+",");
        }
        
        for(URI nextUniqueNamespace : providersByNamespace.keySet())
        {
            if(nextUniqueNamespace == null)
                continue;
            
            out.write("<h3><span class='debug'>Namespace="+nextUniqueNamespace.stringValue()+"</span></h3>\n");
            
            Collection<Provider> providersForNextNamespace = providersByNamespace.get(nextUniqueNamespace);
            
            Collection<URI> implementedQueriesForNextNamespace = new HashSet<URI>();
            
            for(Provider nextProviderForNextNamespace : providersForNextNamespace)
            {
                for(URI nextIncludedQuery : nextProviderForNextNamespace.getIncludedInCustomQueries())
                {
                    if(!implementedQueriesForNextNamespace.contains(nextIncludedQuery))
                    {
                        implementedQueriesForNextNamespace.add(nextIncludedQuery);
                    }
                }
            }
            
            out.write("Queries for this namespace ("+implementedQueriesForNextNamespace.size()+")");
            
            if(implementedQueriesForNextNamespace.size() > 0)
            {
                out.write("<ol>");
            }
            
            for(URI nextImplementedQuery : implementedQueriesForNextNamespace)
            {
                out.write("<li>"+nextImplementedQuery.stringValue()+"</li>");
            }
            
            if(implementedQueriesForNextNamespace.size() > 0)
            {
                out.write("</ol>");
            }
        }
        
        for(URI nextUniqueQuery : providersByQueryKey.keySet())
        {
            if(nextUniqueQuery == null)
                continue;
            
            out.write("<h3><span class='debug'>Query="+nextUniqueQuery.stringValue()+"</span></h3>\n");
            
            Collection<Provider> providersForNextQuery = providersByQueryKey.get(nextUniqueQuery);
            
            List<URI> implementedNamespacesForNextQuery = new ArrayList<URI>();
            
            for(Provider nextProviderForNextQuery : providersForNextQuery)
            {
                for(URI nextIncludedNamespace : nextProviderForNextQuery.getNamespaces())
                {
                    if(!implementedNamespacesForNextQuery.contains(nextIncludedNamespace))
                    {
                        implementedNamespacesForNextQuery.add(nextIncludedNamespace);
                    }
                }
            }
            
            out.write("Namespaces for this query ("+implementedNamespacesForNextQuery.size()+")");
            
            if(implementedNamespacesForNextQuery.size() > 0)
            {
                out.write("<ol>");
            }
            
            for(URI nextImplementedNamespace : implementedNamespacesForNextQuery)
            {
                out.write("<li>"+nextImplementedNamespace.stringValue()+"</li>");
            }
            
            if(implementedNamespacesForNextQuery.size() > 0)
            {
                out.write("</ol>");
            }
            
            out.write("RDF N3 compatible list:<br /> \n");
            
            if(implementedNamespacesForNextQuery.size() > 0 && Settings.getURICollectionPropertiesFromConfig("autogenerateIncludeStubList").contains(nextUniqueQuery))
            {
                StringBuilder sb = new StringBuilder();
                
                String shortQueryName = Settings.AUTOGENERATED_QUERY_PREFIX
                    + 
                    Utilities.md5(
                        Utilities.percentEncode(nextUniqueQuery.stringValue())
                        + Settings.getStringPropertyFromConfig("separator")
                        + now
                        + Settings.getStringPropertyFromConfig("separator")
                    )
                    + Settings.AUTOGENERATED_QUERY_SUFFIX;
                
                String queryName = Settings.getDefaultHostAddress() 
                    + Settings.DEFAULT_RDF_QUERY_NAMESPACE
                    + Settings.getStringPropertyFromConfig("separator")
                    + shortQueryName;
                
                String shortProviderName = Settings.AUTOGENERATED_PROVIDER_PREFIX
                    +
                    Utilities.md5(
                        Utilities.percentEncode(nextUniqueQuery.stringValue())
                        +Settings.getStringPropertyFromConfig("separator")
                        +now
                        +Settings.getStringPropertyFromConfig("separator")
                        )
                    +Settings.AUTOGENERATED_PROVIDER_SUFFIX;
                    
                String providerName = Settings.getDefaultHostAddress() 
                    + Settings.DEFAULT_RDF_PROVIDER_NAMESPACE
                    + Settings.getStringPropertyFromConfig("separator")
                    + shortProviderName ;
                
                StringBuilder namespacesForThisQuery = new StringBuilder();
                
                for(int nextPosition = 0; nextPosition < implementedNamespacesForNextQuery.size(); nextPosition++) 
                {
                    if(nextPosition != 0)
                        namespacesForThisQuery.append(", ");
                    namespacesForThisQuery.append("<"+implementedNamespacesForNextQuery.get(nextPosition)+"> ");
                }
                
                sb.append(Utilities.xmlEncodeString("<"+queryName+"> a <http://purl.org/queryall/query:Query> ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/queryall/query:isPageable> \"false\"^^<http://www.w3.org/2001/XMLSchema#boolean> ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/dc/elements/1.1/title> \""+Utilities.ntriplesEncode(shortQueryName)+"\" ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/queryall/query:handleAllNamespaces> \"false\"^^<http://www.w3.org/2001/XMLSchema#boolean> ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/queryall/query:isNamespaceSpecific> \"true\"^^<http://www.w3.org/2001/XMLSchema#boolean> ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/queryall/query:namespaceMatchMethod> <http://purl.org/queryall/query:namespaceMatchAny> ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/queryall/query:includeDefaults> \"false\"^^<http://www.w3.org/2001/XMLSchema#boolean> ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/queryall/query:inputRegex> \""+Utilities.ntriplesEncode(Settings.getStringPropertyFromConfig("plainNamespaceAndIdentifierRegex"))+"\" ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/queryall/query:templateString> \"\" ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/queryall/query:queryUriTemplateString> \"${defaultHostAddress}${input_1}${defaultSeparator}${input_2}\" ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/queryall/query:standardUriTemplateString> \"${defaultHostAddress}${input_1}${defaultSeparator}${input_2}\" ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/queryall/query:outputRdfXmlString> \"\" ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/queryall/query:inRobotsTxt> \"false\"^^<http://www.w3.org/2001/XMLSchema#boolean> ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/queryall/profile:profileIncludeExcludeOrder> <http://purl.org/queryall/profile:excludeThenInclude> ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/queryall/query:namespaceToHandle> "+namespacesForThisQuery.toString()+" ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/queryall/query:hasPublicIdentifierIndex> \"1\"^^<http://www.w3.org/2001/XMLSchema#int> ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/queryall/query:hasNamespaceInputIndex> \"1\"^^<http://www.w3.org/2001/XMLSchema#int> ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/queryall/query:includeQueryType> <"+nextUniqueQuery+"> .")+"<br />\n<br />\n");
                
                sb.append(Utilities.xmlEncodeString("<"+providerName+"> a <http://purl.org/queryall/provider:Provider> ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/queryall/provider:Title> \""+Utilities.ntriplesEncode(shortProviderName)+"\" ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/queryall/provider:resolutionStrategy> <http://purl.org/queryall/provider:proxy> ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/queryall/provider:resolutionMethod> <http://purl.org/queryall/provider:nocommunication> ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/queryall/provider:isDefaultSource> \"false\"^^<http://www.w3.org/2001/XMLSchema#boolean> ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/queryall/profile:profileIncludeExcludeOrder> <http://purl.org/queryall/profile:excludeThenInclude> ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/queryall/provider:includedInQuery> <"+queryName+"> ;")+"<br />\n");
                sb.append(Utilities.xmlEncodeString("<http://purl.org/queryall/provider:handlesNamespace> "+namespacesForThisQuery.toString()+" . ")+"<br /><br />\n");
                
                out.write(sb.toString()+"<br /> \n");
                
                rdfStrings.append(sb);
            }
        }
        
        if(log.isDebugEnabled())
        {
        out.write("<h2>Consistency analysis:</h2>");
        for(URI nextUniqueNamespace : providersByNamespace.keySet())
        {
            for(URI nextUniqueQueryTitle : providersByQueryKey.keySet())
            {
                Collection<QueryType> queriesForNextTitle = Settings.getCustomQueriesByUri(nextUniqueQueryTitle);
                
                if(queriesForNextTitle.size() == 0)
                {
                    // log.error("No query type definitions were found for the query title "+nextUniqueQueryTitle);
                    out.write("<span class='error'>No query type definitions were found for the query title "+nextUniqueQueryTitle+"</span><br />\n");
                }
                else if(queriesForNextTitle.size() == 1)
                {
                    Collection<Provider> queryTypesForNamespace;
                    
                    queryTypesForNamespace = Settings.getProvidersForQueryType(nextUniqueQueryTitle);
                    
                    // We use QueryType.handleAllNamespaces to detect whether we are conceivably missing query type providers for any of the discovered namespaces
                    // if(queriesForNextTitle.get(0).handleAllNamespaces && queryTypesForNamespace.size() == 0)
                    // {
                        // if(log.isTraceEnabled())
                        // {
                            // // log.warn("No provider was found for a namespace for a querytitle that is defined to be able to handle all namespaces nextUniqueQueryTitle="+nextUniqueQueryTitle+" nextUniqueNamespace="+nextUniqueNamespace);
                            // out.write("<span class='warn'>No provider was found for a namespace for a querytitle that is defined to be able to handle all namespaces nextUniqueQueryTitle="+nextUniqueQueryTitle+" nextUniqueNamespace="+nextUniqueNamespace+" </span><br />\n");
                        // }
                    // }
                    // else 
                    if(queryTypesForNamespace.size() > 0)
                    {
                        if(log.isDebugEnabled())
                        {
                            out.write("<span class='info'>Provider found for namespace and query : nextUniqueQueryTitle="+nextUniqueQueryTitle+" nextUniqueNamespace="+nextUniqueNamespace+"</span><br />\n");
                            // log.debug("Provider found for namespace and query : nextUniqueQueryTitle="+nextUniqueQueryTitle+" nextUniqueNamespace="+nextUniqueNamespace);
                        }
                        
                        for(Provider nextQueryNamespaceProvider : queryTypesForNamespace)
                        {
                            if(nextQueryNamespaceProvider.getEndpointUrls() != null)
                            {
                                for(String nextEndpointUrl : nextQueryNamespaceProvider.getEndpointUrls())
                                {
                                    if(log.isDebugEnabled())
                                    {
                                        out.write("<li><span class='debug'><a href='"+nextEndpointUrl+"'>"+nextEndpointUrl);
                                        
                                        if(nextQueryNamespaceProvider.getEndpointMethod().equals(ProviderImpl.providerHttpPostSparql.stringValue()) && nextQueryNamespaceProvider.getUseSparqlGraph())
                                        {
                                            out.write(" graph="+nextQueryNamespaceProvider.getSparqlGraphUri());
                                        }
                                        
                                        out.write("</a></span></li>\n");
                                    }
                                }
                            }
                            else if(nextQueryNamespaceProvider.getEndpointMethod().equals(ProviderImpl.providerNoCommunication.stringValue()))
                            {
                                if(log.isDebugEnabled())
                                {
                                    out.write("<li><span class='debug'>No communication required</span></li><br />\n");
                                }
                            }
                            else
                            {
                                if(log.isDebugEnabled())
                                {
                                    out.write("<li><span class='debug'>No endpoint URL's found for a particular provider</span></li><br />\n");
                                }
                            }
                        }
                    }
                    else
                    {
                        // Enable this to get some rarely meaningful notices about queries which are not designed to handle all namespaces
                        // if(log.isDebugEnabled())
                        // {
                            // log.debug("No provider was found for a namespace for a particular queryTitle nextUniqueQueryTitle="+nextUniqueQueryTitle+" nextUniqueNamespace="+nextUniqueNamespace);
                            // out.write("<span class='debug'>No provider was found for a namespace for a particular queryTitle nextUniqueQueryTitle="+nextUniqueQueryTitle+" nextUniqueNamespace="+nextUniqueNamespace+" </span><br />\n");				
                        // }
                    }
                }
                else
                {
                    log.warn("More than one query type definition was found for the query title "+nextUniqueQueryTitle + " number found="+queriesForNextTitle.size());
                    out.write("<span class='error'>More than one query type definition was found for the query title "+nextUniqueQueryTitle+"</span><br />\n");
                }
            }
        }
        }
        out.write("<a id=\"rdfoutput\">Complete RDF Output</a><br/>\n");
        out.write(rdfStrings.toString());
    
  }
  
}
