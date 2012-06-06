/**
 * 
 */
package iri.semantics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

//import opendap.semantics.IRISail.XMLfromRDF;


import opendap.semantics.IRISail.IRISailRepository;
import opendap.semantics.IRISail.RepositoryOps;
import opendap.semantics.IRISail.XMLfromRDF;
import option.Options;
import option.Options.Multiplicity;
import option.Options.Separator;

import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.jdom.Namespace;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.rio.RDFParseException;
import org.slf4j.Logger;

import com.ontotext.trree.owlim_ext.SailImpl;

/**
 * @author haibo Liu haibo@iri.columbia.edu
 */
public class XMLfromSesame {
    private RepositoryConnection con;
    private Repository repository;
    private XMLfromRDF buildDoc;
    private Logger log;
    private String cacheDirectory;
    private String loadfromtrig;
    /**
     * constructor
     */
    public XMLfromSesame() {
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
        con = null;
        repository = null;
        buildDoc = null;
        cacheDirectory = "./";
        loadfromtrig = null;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        long startTime, endTime;
        double elapsedTime;
        HashMap<String, Vector<String>> coverageIDServer;

        XMLfromSesame catalog = new XMLfromSesame();
        startTime = new Date().getTime();

        
        Options opt = new Options(args, 2);
        opt.getSet().addOption("cache", Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
        opt.getSet().addOption("s", Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
        opt.getSet().addOption("r", Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
        opt.getSet().addOption("loadfromtrig", Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
        if (!opt.check()) {
            catalog.log
            .error("Usage: java -jar iriwms.jar [-s=serverURL -r=repositoryID] [-cache=CacheDirectory] [-loadfromtrig=fullpatthtothetrigfile]  DocRootURI topURI");
          System.exit(1);
        }
        
                 
        if (args.length < 2) {
            catalog.log
                    .error("Usage: java -jar iriwms.jar [-s=serverURL -r=repositoryID] [-cache=CacheDirectory] [-loadfromtrig=fullpatthtothetrigfile] DocRootURI topURI");
            catalog.log
                    .error("Example: java -jar iriwms.jar -cache=CacheDirectory http://www.opengis.net/wcs/1.1#CoverageDescriptions http://www.opengis.net/wcs/1.1#CoverageDescription");
            catalog.log
                    .error("Or: java -jar iriwms.jar -s=serverURL -r=repositoryID -cache=CacheDirectory http://www.opengis.net/wcs/1.1#CoverageDescriptions http://www.opengis.net/wcs/1.1#CoverageDescription");
            System.exit(1);

        }

        if (args.length > 5) {
            catalog.log
                    .error("Usage:java -jar iriwms.jar [-s=serverURL -r=repositoryID] [-cache=CacheDirectory] [-loadfromtrig=fullpatthtothetrigfile] DocRootURI topURI");
            catalog.log
                    .error("Example: java -jar iriwms.jar http://iridlc1.ldeo.columbia.edu:8180/openrdf-sesame opendap1-owl http://www.opengis.net/wcs/1.1#CoverageDescriptions http://www.opengis.net/wcs/1.1#CoverageDescription");
            System.exit(1);

        }

        String sesameURIstr = null;
        String repositoryID = null;
        String coverageRoot = opt.getSet().getData().get(0);;
        String topURIstr = opt.getSet().getData().get(1);;
        

        int rootpl = coverageRoot.lastIndexOf("#");

        String rootName = coverageRoot.substring(rootpl + 1);

        String fout = rootName + ".xml";
        try {
            if (opt.getSet().isSet("s") && opt.getSet().isSet("r")) {
               
                sesameURIstr = opt.getSet().getOption("s").getResultValue(0);
                repositoryID =  opt.getSet().getOption("r").getResultValue(0);
                catalog.setupRepository(sesameURIstr, repositoryID);
              }
            
            if (opt.getSet().isSet("cache")) {
                // React to option -cache
                catalog.cacheDirectory = opt.getSet().getOption("cache").getResultValue(0);
                
                if(!catalog.cacheDirectory.endsWith("/")){
                    catalog.cacheDirectory = catalog.cacheDirectory + "/";
                }
                File cacheDir = new File(catalog.cacheDirectory);
                if(cacheDir.exists()){
                fout = catalog.cacheDirectory + fout;
                  catalog.log.info("Coverage is writen to " + cacheDir);
                  if (opt.getSet().isSet("loadfromtrig")) {
                      catalog.loadfromtrig = opt.getSet().getOption("loadfromtrig").getResultValue(0);
                     catalog.log.info("load from trig file: " +catalog.loadfromtrig); 
                  }
                  catalog.setupRepository(catalog.cacheDirectory);
                }else{
                    catalog.log.error("Error: Cache directory does not exist! "+catalog.cacheDirectory);
                System.exit(1);
                }
              }
            
            catalog.log.debug("sesameURIstr = " + sesameURIstr);
            catalog.log.debug("repositoryID = " + repositoryID);
            catalog.log.debug("coverageRoot = " + coverageRoot);
            catalog.log.debug("topURIstr = " + topURIstr);
            
            
            
        

        coverageIDServer = catalog.getCoverageIDServerURL();
       

         catalog.extractCoverageDescrptionsFromRepository(coverageRoot,topURIstr);
        } catch (RepositoryException e) {
            catalog.log.error("Caught RepositoryException in main " + e.getMessage());
        } catch (InterruptedException e) {
            catalog.log.error("Caught InterruptedException in main " + e.getMessage());
        }finally{
         catalog.dumpCoverageDescriptionsDocument(fout);

        catalog.shutdownRepository();
        endTime = new Date().getTime();
        elapsedTime = (endTime - startTime) / 1000;
        catalog.log.info("Completed retrieving document in " + elapsedTime
                + " seconds.");
        }
    }

    private HashMap<String, Vector<String>> getCoverageIDServerURL() {
        TupleQueryResult result = null;
        HashMap<String, Vector<String>> coverageIDServer = new HashMap<String, Vector<String>>();

        String queryString = "SELECT coverageurl,coverageid "
                + "FROM "
                + "{} wcs:CoverageDescription {coverageurl} wcs:Identifier {coverageid} "
                + "USING NAMESPACE "
                + "wcs = <http://www.opengis.net/wcs/1.1#>";

        log.debug("query coverage ID and server URL: \n" + queryString);
        TupleQuery tupleQuery;
        try {
            tupleQuery = con
                    .prepareTupleQuery(QueryLanguage.SERQL, queryString);

            result = tupleQuery.evaluate();

            log.debug("Qresult: " + result.hasNext());
            List<String> bindingNames = result.getBindingNames();
            log.debug(bindingNames.toString());
            while (result.hasNext()) {
                BindingSet bindingSet = (BindingSet) result.next();
                log.debug(bindingSet.toString());
                Vector<String> coverageURL = new Vector<String>();

                if (bindingSet.getValue("coverageid") != null
                        && bindingSet.getValue("coverageurl") != null) {

                    Value valueOfcoverageid = (Value) bindingSet
                            .getValue("coverageid");
                    Value valueOfcoverageurl = (Value) bindingSet
                            .getValue("coverageurl");
                    coverageURL.addElement(valueOfcoverageurl.stringValue());

                    if (coverageIDServer.containsKey(valueOfcoverageid
                            .stringValue()))
                        coverageIDServer.get(valueOfcoverageid.stringValue())
                                .addElement(valueOfcoverageurl.stringValue());
                    else
                        coverageIDServer.put(valueOfcoverageid.stringValue(),
                                coverageURL);

                }
            }
        } catch (QueryEvaluationException e) {
            log
                    .error("Caught QueryEvaluationException in getCoverageIDServerURL: "
                            + e.getMessage());
        } catch (RepositoryException e) {

            log.error("Caught RepositoryException in getCoverageIDServerURL: "
                    + e.getMessage());
        } catch (MalformedQueryException e) {
            log
                    .error("Caught MalformedQueryException in getCoverageIDServerURL: "
                            + e.getMessage());
        }
        return coverageIDServer;

    }

    private void extractCoverageDescrptionsFromRepository(String rootElement,
            String topURI) throws InterruptedException {
        // retrieve XML from the RDF store.
        log.info("Extracting CoverageDescriptions from repository.");
        URI uri = new URIImpl(topURI);
        URI rootUri = new URIImpl(rootElement);
        Namespace topURINS =  Namespace.getNamespace(uri.getNamespace());
        //String rootElementLocalName = uri.getLocalName();
        String rootElementLocalName = rootUri.getLocalName();
        buildDoc = new XMLfromRDF(con, rootElementLocalName, topURI);
        buildDoc.getXMLfromRDF(topURI); // build a JDOM doc by querying against the
        // RDF store

    }

    private void dumpCoverageDescriptionsDocument(String filename) {
        // print out the XML
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());

        try {
            File destinationFile = new File(filename);
            FileOutputStream fos = new FileOutputStream(destinationFile);
            outputter.output(buildDoc.getDoc(), fos);
        } catch (IOException e1) {
            log.error("Caught error in dumpCoverageDescriptionsDocument: "
                    + e1.getMessage());
        }
    }

    private void setupRepository(String sesameURL, String repositoryID)
            throws RepositoryException {

        log.info("Connect to server " + sesameURL + " ...");
        repository = new HTTPRepository(sesameURL, repositoryID);

        log.info("Initialize repository " + repositoryID + " ...");
        repository.initialize();

        log.info("Open connection to the repository ...");
        con = repository.getConnection();
        log.info("Connection to the repository is opened.");
    }

    private void shutdownRepository() {
        System.out.println("Shutting down the repository ...");
        try {
            con.close();
        } catch (RepositoryException e) {

            log.error("Caught error in closing connection: " + e.getMessage());
        }
        try {
            
                repository.shutDown();   
           
                
        } catch (RepositoryException e) {

            log.error("Caught error in shuttingdown repository: "
                    + e.getMessage());
        }
        log.info("Shutdown is completed.");
    }
    private void setupRepository(String catalogCacheDirectory) throws RepositoryException, InterruptedException {


        log.info("Setting up Semantic Repository.");
        String owlim_storage_folder = "owlim-storage";
        String resourcePath = "./";
        //OWLIM Sail Repository (inferencing makes this somewhat slow)
        //SailImpl owlimSail = new com.ontotext.trree.owlim_ext.SailImpl();
        SailImpl owlimSail = new SailImpl();
        repository = new IRISailRepository(owlimSail); //owlim inferencing


        log.info("Configuring Semantic Repository.");
        File storageDir = new File(catalogCacheDirectory); //define local copy of repository
        owlimSail.setDataDir(storageDir);
        log.debug("Semantic Repository Data directory set to: " + catalogCacheDirectory);
        // prepare config
        

        owlimSail.setParameter("storage-folder", owlim_storage_folder);
        log.debug("Semantic Repository 'storage-folder' set to: " + owlim_storage_folder);

        // Choose the operational ruleset
        String ruleset;
        //ruleset = "owl-horst";
        ruleset = "owl-max-optimized";

        owlimSail.setParameter("ruleset", ruleset);
        //owlimSail.setParameter("ruleset", "owl-max");
        //owlimSail.setParameter("partialRDFs", "false");
        log.debug("Semantic Repository 'ruleset' set to: " + ruleset);


        log.info("Intializing Semantic Repository.");

        // Initialize repository
        repository.initialize();; //needed

       
      //trig file (full path) to load into the repository. This is a work around
        //to the persistent bug in owlim
        if (loadfromtrig != null && !loadfromtrig.isEmpty()){
        String inTrigFile = loadfromtrig;
        RepositoryOps.clearRepository(repository);
        try {
            RepositoryOps.loadRepositoryFromTrigFile(repository, inTrigFile);
        } catch (RDFParseException e) {
            log.error("Caught RDFParseException in setupRepository, Msg: "+e.getMessage());
        } catch (IOException e) {
            log.error("Caught IOException in setupRepository, Msg: "+e.getMessage());
        }
        String filename = catalogCacheDirectory + "afterloadingfromtrigfile.trig";
        log.debug("updateRepository(): Dumping Semantic Repository to: " + filename);
        RepositoryOps.dumpRepository(repository, filename);
        }
        con = repository.getConnection();
        log.info("Semantic Repository Ready.");

        if (Thread.currentThread().isInterrupted())
            throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");


    }

}
