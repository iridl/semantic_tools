package iri.wms;

import java.io.*;
import java.net.URL;
import java.io.PrintWriter;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import net.sf.saxon.*;
import javax.xml.transform.*;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.TransformerFactory;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import info.aduna.xml.XMLWriter;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.Binding;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.BindingSet;
import org.openrdf.query.impl.MapBindingSet;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import opendap.semantics.IRISail.IRIHTTPRepository;
import opendap.semantics.IRISail.XMLfromRDF;

import iri.semantics.RepositoryAps;

/**
 * Servlet for catalogue listing.
 */
public class SerfRecords extends HttpServlet implements javax.servlet.Servlet {
    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(SerfRecords.class);

    /**
     * @see HttpServlet#HttpServlet()
     */
    public SerfRecords() {
        super();

    }

    /**
     * Generate directory listings or xml documents according to the user request.
     * When a user points the browser to xmldocs, it returns the listing of directories;
     * When points to xmldocs/serfrecords, a list of documents is shown, in turn the individual
     * xml documents is shown.  
     * @see javax.servlet.http.HttpServlet#doGet(HttpServletRequest request,
     * HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse res)
            throws ServletException, IOException {
        
        IRIHTTPRepository repo = null;
        String repositoryID = null;
        
        String pathURI = request.getRequestURI();
        log.info("pathURI= " + pathURI);
        if (pathURI.endsWith("/")) {
            pathURI = pathURI.substring(0, pathURI.lastIndexOf("/"));
        }
        log.info("pathURI= " + pathURI);
        String[] urlwords = pathURI.split ("\\/");
        System.out.println (urlwords.length);
        for (int i=0; i < urlwords.length; i++)
        	  System.out.println (urlwords[i]); 
        String serverName = request.getServerName();
        
        //if(serverName.equals("iridlc6") || serverName.equals("iridlc1")){
        serverName = "iridl.ldeo.columbia.edu";
        
        //}
        String scheme = request.getScheme();

        String requestUrl = scheme + "://" + serverName + pathURI;
        log.info("scheme+ServerName+RequestURI " + requestUrl);
               
        RepositoryConnection con = null;
        //String sesameURL = "http://iridlc6:8480/openrdf-sesame";
        String sesameURL = "http://iridlc1:8180/openrdf-sesame";
        //String sesameURL = "http://iridl2p.ldeo.columbia.edu/openrdf-sesame";
        // if the requestURL is one or 2 levels open xmldocs to get hasRepos, 
        // else reduce requestURL to 2 levels, open xmldocs to get hasRepos.
        repositoryID = "xmldocs";
        if (urlwords.length < 4) {
        	try {
				repo = setupIRIHTTPRepository(sesameURL, repositoryID);
			} catch (RepositoryException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
            try {
				con = repo.getConnection();
			} catch (RepositoryException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        }else{
            String longpathURI = request.getRequestURI();
            if (longpathURI.endsWith("/")) {
                longpathURI = pathURI.substring(0, pathURI.lastIndexOf("/"));
            }
            log.info("longpathURI= " + longpathURI);
            String[] longurlwords = longpathURI.split ("\\/");        	        	
            String reposrequestUrl = scheme + "://" + serverName + "/xmldocs/" + longurlwords[2];
        	try {
				repo = setupIRIHTTPRepository(sesameURL, repositoryID);
			} catch (RepositoryException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
            try {
				con = repo.getConnection();
			} catch (RepositoryException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
            String queryStrRepos = "select distinct vdir,repos "
     	        + "FROM "
                + "{vdir} vdoc:hasRepository {repos} "
                + "WHERE vdir=<"
                + reposrequestUrl
                + "> "
                + "USING NAMESPACE "
                + "vdoc=<http://iridl.ldeo.columbia.edu/ontologies/vdoc.owl#> ";
   
            log.debug("queryStrRepos = " + queryStrRepos);

            HashMap <String,String>  queryResultRepos = RepositoryAps.runQuery(repo, queryStrRepos);
            Set <String> reposKey = queryResultRepos.keySet();
            Iterator <String> itRepos = reposKey.iterator();
            String altrepos = repositoryID;
            while (itRepos.hasNext()) {
              altrepos = itRepos.next();
              System.out.print(altrepos + ": ");
            }
            if (altrepos.compareTo(repositoryID) != 0) {
            	// for each virtualDir, open the appropriate repository
                 System.out.print(altrepos + ": ");
                 try {
                      con.close();
                      log.info("Connection to the repository is closed.");
                      repo.shutDown();
                      log.info("The repository is shutdown.");
                 } catch (RepositoryException e) {
                      log.error("Caught RepositoryException in doGet Msg:"
                              + e.getMessage());
                 }
                 repositoryID = altrepos;
                 try {
					repo = setupIRIHTTPRepository(sesameURL, repositoryID);
				} catch (RepositoryException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                 try {
					con = repo.getConnection();
				} catch (RepositoryException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        }
        
        log.info("requestUrl = " + requestUrl);
        ValueFactory f = repo.getValueFactory();
        URI sub = f.createURI(requestUrl);
        URI prd = f.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        Value virtualDirectory = (Value) f
                .createURI("http://iridl.ldeo.columbia.edu/ontologies/vdoc.owl#VirtualDirectory");
        Value virtualXmlDocument = (Value) f
                .createURI("http://iridl.ldeo.columbia.edu/ontologies/vdoc.owl#VirtualXmlDocument");
        Value virtualXmlReport = (Value) f
        .createURI("http://iridl.ldeo.columbia.edu/ontologies/vdoc.owl#VirtualXmlReport");
        Value virtualHTMLReport = (Value) f
        .createURI("http://iridl.ldeo.columbia.edu/ontologies/vdoc.owl#VirtualHTMLReport");

        Statement virtualDirectoryStatement = f.createStatement(sub, prd, virtualDirectory);
        Statement virtualXmlDocumentStatement = f.createStatement(sub, prd, virtualXmlDocument);
        Statement virtualXmlReportStatement = f.createStatement(sub, prd, virtualXmlReport);
        Statement virtualHTMLReportStatement = f.createStatement(sub, prd, virtualHTMLReport);
        
        try {
            if (con.hasStatement(virtualDirectoryStatement, true)) {
                log.info("Processing virtualDirectory ...");
                
                String queryStrRepos = "select distinct vdir,repos "
         	        + "FROM "
                    + "{vdir} vdoc:hasRepository {repos} "
                    + "WHERE vdir=<"
                    + requestUrl
                    + "> "
                    + "USING NAMESPACE "
                    + "vdoc=<http://iridl.ldeo.columbia.edu/ontologies/vdoc.owl#> ";
       
                log.debug("queryStrRepos = " + queryStrRepos);

                HashMap <String,String>  queryResultRepos = RepositoryAps.runQuery(repo, queryStrRepos);
                Set <String> reposKey = queryResultRepos.keySet();
                Iterator <String> itRepos = reposKey.iterator();
                String altrepos = repositoryID;
                while (itRepos.hasNext()) {
                  altrepos = itRepos.next();
                  System.out.print(altrepos + ": ");
                }
                if (altrepos.compareTo(repositoryID) != 0) {
                	// for each virtualDir, open the appropriate repository
                     System.out.print(altrepos + ": ");
                     try {
                          con.close();
                          log.info("Connection to the repository is closed.");
                          repo.shutDown();
                          log.info("The repository is shutdown.");
                     } catch (RepositoryException e) {
                          log.error("Caught RepositoryException in doGet Msg:"
                                  + e.getMessage());
                     }
                     repositoryID = altrepos;
                     repo = setupIRIHTTPRepository(sesameURL, repositoryID);
                     con = repo.getConnection();
                }
       
                String queryStr = "select distinct dir,file "
                        + "FROM "
                        + "{dir} rdf:type {vdoc:VirtualDirectory} ; vdoc:contains {file} "
                        + "WHERE dir=<"
                        + requestUrl
                        + "> "
                        + "AND isURI(file) "
                        + "USING NAMESPACE "
                        + "vdoc=<http://iridl.ldeo.columbia.edu/ontologies/vdoc.owl#>";

                log.debug("queryStr = " + queryStr);
                //FileOutputStream ofs = new FileOutputStream("outputfile");
                HashMap<String, String> queryResult = RepositoryAps.runQuery(repo, queryStr);
                
                res.setContentType("text/html");
                //res.setCharacterEncoding("utf-8");
                //PrintWriter out = res.getWriter();
                
                //suitable for writing binary data in the response. 
                //The servlet container does not encode the binary data. 
                ServletOutputStream out = res.getOutputStream();
                
                
                Map<String,String> queryResultKeys = new HashMap<String,String>();
                String qfile = null;
                String base = null;
                String qfileName = null;
                Set <String> queryResultKey = queryResult.keySet();
                String dirLocalName = "xmldocs";
                Iterator<String> itQueryResultKey = queryResultKey.iterator();
                
                while(itQueryResultKey.hasNext()){
                    qfile = itQueryResultKey.next();
                    String dirFullName = queryResult.get(qfile);
                    dirLocalName = dirFullName.substring(dirFullName.lastIndexOf("/") +1);
                    base = qfile.substring(0, qfile.lastIndexOf("/"));
                    qfileName = qfile.substring(qfile.lastIndexOf("/") + 1);
                    queryResultKeys.put(qfileName, qfile);
                }
                log.debug("dirLocalName = " + dirLocalName);
                SortedSet<String> sortedset= new TreeSet<String>(queryResultKeys.keySet());
                Iterator<String> it = sortedset.iterator();
                out.println("<html>");
                out.println("<body>");
                if(!dirLocalName.equals("xmldocs")){
                    dirLocalName = "xmldocs/" + dirLocalName; 
                }
                out.println("<h1> " + dirLocalName+" </h1>");
                out.println("<ul>");
                
                while (it.hasNext()) {
                 String key = it.next();
                 out.println("<li><a href=\"" + queryResultKeys.get(key) + "\">" + key + "</a>");
            }
                out.println("</ul>");
                out.println("</body>");
                out.println("</html>");
                out.flush();
            }
            
            else if (con.hasStatement(virtualHTMLReportStatement, true)) {
                log.info("Processing virtualHTMLReport ...");
                
                // check for stylesheet associated with virtualHTMLReport
                
                String queryStrXsl = "select distinct vrpt,xsl "
         	        + "FROM "
                    + "{vrpt} vocab:stylesheet {xsl} "
                    + "WHERE vrpt=<"
                    + requestUrl
                    + "> "
                    + "USING NAMESPACE "
                    + "vocab=<http://www.w3.org/1999/xhtml/vocab#> ";
                
                log.debug("queryStrReport = " + queryStrXsl);

                HashMap <String,String>  queryResultXsl = RepositoryAps.runQuery(repo, queryStrXsl);
                Set <String> xslKey = queryResultXsl.keySet();
                Iterator <String> itXsl = xslKey.iterator();
                String qxsl = null;
                while (itXsl.hasNext()) {
                  qxsl = itXsl.next();
                  log.info("Request for Xsl = " + qxsl);
                }

                String queryStrReport = "select distinct vrpt,rptqry "
         	        + "FROM "
                    + "{vrpt} vdoc:hasSerqlReportQuery {rptqry} "
                    + "WHERE vrpt=<"
                    + requestUrl
                    + "> "
                    + "USING NAMESPACE "
                    + "vdoc=<http://iridl.ldeo.columbia.edu/ontologies/vdoc.owl#> ";
       
                log.debug("queryStrReport = " + queryStrReport);

                HashMap <String,String>  queryResultReport = RepositoryAps.runQuery(repo, queryStrReport);
                Set <String> reportKey = queryResultReport.keySet();
                Iterator <String> itReport = reportKey.iterator();
                String qreport = null;
                while (itReport.hasNext()) {
                  qreport = itReport.next();
                  log.info("Request for Report = " + qreport);
                }

                // use saxon to apply the xsl2 to the XML and deliver HTML
                // use the stylesheet attached to the VirtualXmlReport or default stylesheet
                ByteArrayOutputStream xdoc = new ByteArrayOutputStream();
                SPARQLResultsXMLWriter sparqlWriter = new SPARQLResultsXMLWriter(xdoc);
                                

                TupleQuery tupleQuery;
				try {
					tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL, qreport);
					// when there are bindings add them to the tupleQuery
	                // set up a ValueFactory to convert strings from request params to values
	                ValueFactory factory = repo.getValueFactory();
	                // look for bindings
	                Enumeration en = request.getParameterNames();
	                while (en.hasMoreElements()) {
	                    String pname = (String) en.nextElement();
	                    tupleQuery.setBinding(pname,factory.createLiteral(request.getParameter(pname)));
	                    log.info(pname + " = " + request.getParameter(pname));            
	                }                
                    try {
						tupleQuery.evaluate(sparqlWriter);
						// log.info(xdoc.toString());
				        // Create a transform factory instance.
				        TransformerFactory tfactory  = TransformerFactory.newInstance();
				        URL urlxsl = new URL(qxsl);
				        InputStream xslIS = urlxsl.openStream();
				        StreamSource xslSource = new StreamSource(xslIS);

					    try {
							Transformer  transformer = tfactory.newTransformer(xslSource);
					        InputStream  xmlIS       =
					            new ByteArrayInputStream(xdoc.toString().getBytes());
					        StreamSource xmlSource   = new StreamSource(xmlIS);

					        // Transform the source XML to System.out.
					        try {
								transformer.transform(xmlSource, new StreamResult(res.getOutputStream()));
							} catch (TransformerException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

					    } catch (TransformerConfigurationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				 			     				                     
					} catch (QueryEvaluationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (TupleQueryResultHandlerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (MalformedQueryException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

            }
            else if (con.hasStatement(virtualXmlReportStatement, true)) {
                log.info("Processing virtualxmlReport ...");
                
                // check for stylesheet associated with virtualxmlReport
                
                String queryStrXsl = "select distinct vrpt,xsl "
         	        + "FROM "
                    + "{vrpt} vocab:stylesheet {xsl} "
                    + "WHERE vrpt=<"
                    + requestUrl
                    + "> "
                    + "USING NAMESPACE "
                    + "vocab=<http://www.w3.org/1999/xhtml/vocab#> ";
                
                log.debug("queryStrReport = " + queryStrXsl);

                HashMap <String,String>  queryResultXsl = RepositoryAps.runQuery(repo, queryStrXsl);
                Set <String> xslKey = queryResultXsl.keySet();
                Iterator <String> itXsl = xslKey.iterator();
                String qxsl = null;
                while (itXsl.hasNext()) {
                  qxsl = itXsl.next();
                  log.info("Request for Xsl = " + qxsl);
                }

                String queryStrReport = "select distinct vrpt,rptqry "
         	        + "FROM "
                    + "{vrpt} vdoc:hasSerqlReportQuery {rptqry} "
                    + "WHERE vrpt=<"
                    + requestUrl
                    + "> "
                    + "USING NAMESPACE "
                    + "vdoc=<http://iridl.ldeo.columbia.edu/ontologies/vdoc.owl#> ";
       
                log.debug("queryStrReport = " + queryStrReport);

                HashMap <String,String>  queryResultReport = RepositoryAps.runQuery(repo, queryStrReport);
                Set <String> reportKey = queryResultReport.keySet();
                Iterator <String> itReport = reportKey.iterator();
                String qreport = null;
                while (itReport.hasNext()) {
                  qreport = itReport.next();
                  log.info("Request for Report = " + qreport);
                }

                // use sesame method to execute query and return XML
                
                ServletOutputStream xdoc = res.getOutputStream();
                MyXMLWriter writer = new MyXMLWriter(xdoc);
                
                // use the stylesheet attached to the VirtualXmlReport or default stylesheet
                if (qxsl != null) {
                	writer.setStylesheetRef(qxsl); 
                }else{
                	writer.setStylesheetRef("http://iridl.ldeo.columbia.edu/ontologies/xslt/result-to-html.xsl");
                }
                SPARQLResultsXMLWriter sparqlWriter = new SPARQLResultsXMLWriter(writer);
                
                TupleQuery tupleQuery;
				try {
					tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL, qreport);
					// when there are bindings add them to the tupleQuery
	                // set up a ValueFactory to convert strings from request params to values
	                ValueFactory factory = repo.getValueFactory();
	                // look for bindings
	                Enumeration en = request.getParameterNames();
	                while (en.hasMoreElements()) {
	                    String pname = (String) en.nextElement();
	                    tupleQuery.setBinding(pname,factory.createLiteral(request.getParameter(pname)));
	                    log.info(pname + " = " + request.getParameter(pname));            
	                }                
                    try {
						tupleQuery.evaluate(sparqlWriter);
					} catch (QueryEvaluationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (TupleQueryResultHandlerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (MalformedQueryException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

            }
            else if (con.hasStatement(virtualXmlDocumentStatement, true)) {
                log.info("Processing virtualxmlDocument ...");

                String queryStr = "select distinct file,element,uri,class,targetns "
                        + "FROM "
                        + "{file} rdf:type {vdoc:VirtualXmlDocument}; "
                        + "vdoc:hasOuterElement {element} rdfs:range {class} "
                        + ",{file} element {uri}, "
                        + "[{element} rdfs:isDefinedBy {} xsd:targetNamespace {targetns}] " 
                        + "WHERE file=<"
                        + requestUrl
                        + "> "
                        + "USING NAMESPACE "
                        + "rdfs = <http://www.w3.org/2000/01/rdf-schema#>, "
                        + "vdoc=<http://iridl.ldeo.columbia.edu/ontologies/vdoc.owl#> ";

                XMLfromRDF xmlfromrdf = new XMLfromRDF(con);
                //XMLfromRDF xmlfromrdf = new XMLfromRDF(con, requestUrl,requestUrl);
                xmlfromrdf.getXMLfromRDF(requestUrl, queryStr,f);

                Document doc = xmlfromrdf.getDoc();
                
                XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
                
                res.setContentType("text/xml");
                res.setCharacterEncoding("utf-8");
                PrintWriter out = res.getWriter();
                //ServletOutputStream out = res.getOutputStream();
                
                if(doc != null){
                	try {
                    
                        outputter.output(doc, out);  
                        out.flush();
                	} catch (IOException e) {
                    log.error("Caught IOException Msg:" +e.getMessage() );
                	}
                	}else{
                    out.println("Document is empty!");
                    log.error("Document is empty!");
                	}
            	}
            
        } catch (RepositoryException e) {
            log.error("Caught RepositoryException " + e.getMessage());
        } catch (InterruptedException e) {
            log.error("Caught InterruptedException " + e.getMessage());
        } finally {
            if (con != null) {
                try {
                    con.close();
                    log.info("Connection to the repository is closed.");
                    repo.shutDown();
                    log.info("The repository is shutdown.");
                } catch (RepositoryException e) {
                    log.error("Caught RepositoryException in doGet Msg:"
                            + e.getMessage());
                }
            }
        }
    }


	/*
     * (non-Java-doc)
     * @see javax.servlet.http.HttpServlet#doPost(HttpServletRequest request,
     * HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);

    }

    private IRIHTTPRepository setupIRIHTTPRepository(String sesameURL,
            String repositoryID) throws RepositoryException,
            InterruptedException {

        log.info("Connect to server " + sesameURL + " ...");
        IRIHTTPRepository repository = new IRIHTTPRepository(sesameURL,
                repositoryID);

        log.info("Initialize repository " + repositoryID + " ...");
        repository.initialize();

        log.info("Semantic Repository Ready.");

        if (Thread.currentThread().isInterrupted())
            throw new InterruptedException(
                    "Thread.currentThread.isInterrupted() returned 'true'.");

        return repository;

    }

}
