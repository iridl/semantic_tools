package iri.semantics;

import option.Options;
import option.Options.Multiplicity;
import option.Options.Separator;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ontotext.trree.owlim_ext.SailImpl;

import opendap.logging.LogUtil;
import opendap.semantics.IRISail.IRIHTTPRepository;
import opendap.semantics.IRISail.IRISailRepository;
import opendap.semantics.IRISail.RepositoryOps;


public class GenerateNTriples {
    private Logger log;

    //private Repository owlse2;
    
    private String resourcePath;
    private String catalogCacheDirectory;
    private String owlim_storage_folder;
    private Element _config;
    private String loadfromtrig;
    private ReentrantReadWriteLock _repositoryLock;
    public GenerateNTriples() {
        log = LoggerFactory.getLogger(this.getClass());
       
        //owlse2 = null;
        _repositoryLock = new ReentrantReadWriteLock();
        resourcePath = "./";
        catalogCacheDirectory = "./wms-cache/";
        owlim_storage_folder ="owlim-storage";
        _config = null;
        loadfromtrig = null;
    }

    public static void main(String[] args) {
        long startTime, endTime;
        double elapsedTime;
        String workingDir = System.getProperty("user.dir");
        LogUtil.initLogging(workingDir);
        
        GenerateNTriples catalog = new GenerateNTriples();
        startTime = new Date().getTime();
        boolean xmlfromhttp = false; //StartingPoint is a http file? default=false
        
        Options opt = new Options(args,1, 11);
        opt.getSet().addOption("cache", Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
        opt.getSet().addOption("ruleset", Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
        opt.getSet().addOption("login", Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
        opt.getSet().addOption("pswd", Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
        opt.getSet().addOption("notimport", Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
        opt.getSet().addOption("sesame", Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
        opt.getSet().addOption("repository", Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
        opt.getSet().addOption("xmlfromhttp", Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
        opt.getSet().addOption("loadfromtrig", Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
        opt.getSet().addOption("construct", Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
        opt.getSet().addOption("constructoutput", Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
        opt.getSet().addOption("dropChoice", Separator.EQUALS, Multiplicity.ZERO_OR_ONE);
       
        if (!opt.check()) {
            catalog.log
            .error("argument error ! Usage: java -jar generatentriples.jar [-login=user -pswd=password] " +
            		"[-cache=CacheDirectory -ruleset=Ruleset] [-notimport=notimportfile] " +
            		"[-sesame=serverURL -repository=repositoryID] " +
            		"[-dropChoice=flushRepositoryOnDrop/dropWithMemoryStore/dropWithMemoryStoreDeleteRepository] " +
            		"[-xmlfromhttp=true/false] [-loadfromtrig=fullpatthtothetrigfile]" +
            		"[-construct=fileofconstructrule] [-constructoutput=fullpatthtothefileholdsconstructruleresults]" +
            		"config_files/owl_files ");
          System.exit(1);
        }
        
        
        
        String[] infile = new String[opt.getSet().getData().size()];
        catalog.log.debug("data_sizw="+opt.getSet().getData().size());
        
        for(int j = 0; j< opt.getSet().getData().size(); j++){
                    catalog.log.info("j=" + j);
            infile[j]= opt.getSet().getData().get(j);
            catalog.log.debug("infile["+j+"] = " +infile[j]);
            catalog.log.info("file="+ infile[j]);
        }  
        if (args.length < 1) {
            catalog.log
                    .error("Usage: java -jar generatentriples.jar [-login=user -pswd=password] [-cache=CacheDirectory] [-notimport=notimportfile] [-sesame=serverURL -repository=repositoryID]" +
                    		"[-loadfromtrig=fullpatthtothetrigfile] " +
                            "[-construct=fileofconstructrule] [-constructoutput=fullpatthtothefileholdsconstructruleresults] config_file/owl_file");
            catalog.log
                    .error("Example: java -jar generatentriples.jar [-login=user -pswd=password] [-cache=CacheDirectory] [-notimport=notimportfile] [-sesame=serverURL -repository=repositoryID] " +
                    		"[-loadfromtrig=fullpatthtothetrigfile] " +
                    "[-construct=fileofconstructrule] [-constructoutput=fullpatthtothefileholdsconstructruleresults] config_file/owl_file");
            catalog.log
                    .error("Or: java -jar generatentriples.jar [-login=user -pswd=password] [-cache=CacheDirectory] [-notimport=notimportfile] [-sesame=serverURL -repository=repositoryID] " +
                    		"[-loadfromtrig=fullpatthtothetrigfile] " +
                    "[construct=fileofconstructrule] [-constructoutput=fullpatthtothefileholdsconstructruleresults] " +
                    		"http://iri.columbia.edu/~haibo/opendaptest/datasetcoveragelist.owl");
            catalog.log
            .error("Or: java -jar generatentriples.jar [-login=user -pswd=password] [-cache=CacheDirectory] [-notimport=notimportfile] [-sesame=serverURL -repository=repositoryID] " +
                    "[-loadfromtrig=fullpatthtothetrigfile] " +
                    "[-construct=fileofconstructrule] [-constructoutput=fullpatthtothefileholdsconstructruleresults] " +        
            "[-xmlfromhttp=true] http://iridl.ldeo.columbia.edu/ontologies/datalibrary.xml");
            System.exit(1);

        }

        try {
            catalog.resourcePath = "/data/haibo/wmsjar/";
            //catalog.catalogCacheDirectory = workingDir;
            Repository repository= null;
            String configFileName = null;
            boolean dropWithMemoryStoreDeleteRepository = false;
                        
            if (opt.getSet().isSet("dropChoice")) {
                
                String dropChoice = opt.getSet().getOption("dropChoice").getResultValue(0);
                if(dropChoice.equalsIgnoreCase("flushRepositoryOnDrop")){
                RepositoryOps.setFlushRepositoryOnDrop();
                }
                    
                if(dropChoice.equalsIgnoreCase("dropWithMemoryStore")){
                    RepositoryOps.setDropWithMemoryStore();
                }
                if(dropChoice.equalsIgnoreCase("dropWithMemoryStoreDeleteRepository")){
                    RepositoryOps.setDropWithMemoryStoreDeleteDir();
                    catalog.log.info("drop = " + dropChoice);  
                    dropWithMemoryStoreDeleteRepository = true;
                }
            }
            
            if (opt.getSet().isSet("xmlfromhttp")) {
                
                String xmlfromhttpStr = opt.getSet().getOption("xmlfromhttp").getResultValue(0);
                if (xmlfromhttpStr.equals("true")){                
                    xmlfromhttp = true ;
                }
            }
            Vector<String> importURLs = new Vector<String>();
            for(int i = 0; i<opt.getSet().getData().size(); i++){
                configFileName = infile[i];
                
            //configFileName = infile[0];
                if (configFileName.endsWith("xml") && xmlfromhttp == false) {
                    
                    Element olfsConfig = Util.getDocumentRoot(configFileName);

                    catalog.log.debug("main() using config file: " + configFileName);
                    catalog._config = (Element) olfsConfig.getDescendants(
                            new ElementFilter("WcsCatalog")).next();
                    //catalog.processConfig(catalog._config, catalog.catalogCacheDirectory,
                    catalog.processConfig(catalog._config,workingDir, catalog.resourcePath);
                    catalog.log.debug("main(): Getting RDF imports.");
                    importURLs = catalog.getRdfImports(catalog._config);
                } else {
                    importURLs.add(configFileName);

                }
            }
            
            catalog.log.debug("main() using config file: " + configFileName);
            
            String loginStr ="rdfscan";
            String passwordStr = "6804450";
            if (opt.getSet().isSet("login") && opt.getSet().isSet("pswd")) {
                // React to option -login and -pswd
                loginStr = opt.getSet().getOption("login").getResultValue(0);
                passwordStr =  opt.getSet().getOption("pswd").getResultValue(0);;
              }
            
            if (opt.getSet().isSet("cache")) {
                // React to option -cache
                catalog.catalogCacheDirectory = opt.getSet().getOption("cache").getResultValue(0);
                if(!catalog.catalogCacheDirectory.endsWith("/")){
                    catalog.catalogCacheDirectory = catalog.catalogCacheDirectory + "/";
                }
                  catalog.log.info("Persistence is writen to " + catalog.catalogCacheDirectory);
              }
            final String login =loginStr;
            final String password =passwordStr;
            

            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication (login, password.toCharArray());
                }
            });
            Vector <String> notImport = new Vector<String> ();
            if (opt.getSet().isSet("notimport")) {
                
                File notImportFile = new File(opt.getSet().getOption("notimport").getResultValue(0));
                                
                notImport = catalog.getNotImport(notImportFile);
              }
            
            if (opt.getSet().isSet("loadfromtrig")) {
                
                catalog.loadfromtrig = opt.getSet().getOption("loadfromtrig").getResultValue(0);
                
              }
            
            if (opt.getSet().isSet("sesame") && opt.getSet().isSet("repository")) {
               
                String sesameserver = opt.getSet().getOption("sesame").getResultValue(0);
                String repositoryID =  opt.getSet().getOption("repository").getResultValue(0);;
                catalog.updateIRIHTTPRepository(importURLs, notImport, sesameserver, repositoryID );
            }else{  // we are creating a repository in the cache dir, and ruleset is an optional parameter
            	String RuleSet = "owl-max-optimized";
            	if (opt.getSet().isSet("ruleset")) {
                   RuleSet = opt.getSet().getOption("ruleset").getResultValue(0);
            	}
                repository =  catalog.setupIRISailRepository(RuleSet);
                try {
                    
                    Vector<String> startingPoints = importURLs;
                    
                    //log.info("updateIRISailRepository: Updating Repository...");
                    catalog.updateIRISailRepository(dropWithMemoryStoreDeleteRepository, repository, startingPoints, notImport);
                    
                }
                finally {
                   repository.shutDown();
                }
            
            
            }
            
            //save construct rule results into a file
            if (opt.getSet().isSet("construct") && opt.getSet().isSet("constructoutput")) {
                String constructInFile;
                String constructOutFile;
                repository =  catalog.setupIRISailRepository();
                constructInFile = opt.getSet().getOption("construct").getResultValue(0);
                constructOutFile =  opt.getSet().getOption("constructoutput").getResultValue(0);;
                FileOutputStream out = new FileOutputStream(constructOutFile);
                if(repository != null){
                RepositoryAps.runConstruct(repository, constructInFile, out);
                }
                repository.shutDown();
            }
            
            endTime = new Date().getTime();
            elapsedTime = (endTime - startTime) / 1000.0;
            catalog.log.debug("Completed catalog update in " + elapsedTime + " seconds.");
            catalog.log.debug("########################################################################################");
            catalog.log.debug("########################################################################################");
             
                        
            
            
        } catch (RepositoryException e) {
            catalog.log.error("Caught RepositoryException in main(): "
                    + e.getMessage());

        } catch (MalformedURLException e) {
            catalog.log.error("Caught MalformedURLException in main(): "
                    + e.getMessage()); 
        } catch (IOException e) {
            catalog.log.error("Caught IOException in main(): "
                    + e.getMessage());  
        } catch (JDOMException e) {
            catalog.log.error("Caught JDOMException in main(): "
                    + e.getMessage());  
        } catch (InterruptedException e) {
            catalog.log.error("Caught InterruptedException in main(): "
                    + e.getMessage());  
        } catch (RDFParseException e) {
            catalog.log.error("Caught RDFParseException in main(): "
                    + e.getMessage()); 
        } 
    }
    private Vector<String> getNotImport (File notImportFile){
               
        FileInputStream fis = null;
        
        DataInputStream dis = null;
        Vector <String> notImport = new Vector<String> ();
        try {
          fis = new FileInputStream(notImportFile);

          // Here BufferedInputStream is added for fast reading.
          
          dis = new DataInputStream(fis);
          //bis = new BufferedInputStream(dis);
          BufferedReader br = new BufferedReader(new InputStreamReader(dis));
          // dis.available() returns 0 if the file does not have more lines.
          
          String strLine;
          //Read File Line By Line
          while ((strLine = br.readLine()) != null)   {
            // Print the content on the console
            log.debug (strLine);
            notImport.add(strLine); 
          }
          // dispose all the resources after using them.
          fis.close();
          br.close();
          dis.close();

        } catch (FileNotFoundException e) {
          log.error("Caught FileNotFoundException Msg: "+e.getMessage());
        } catch (IOException e) {
          log.error("IOException Msg: "+e.getMessage());
        }
        return notImport;
    }
    /**
     * @throws IOException 
     * @throws RDFParseException *****************************************************/
    public void updateIRIHTTPRepository(Vector<String> importURLs, Vector<String> notImport,String sesameURL, String repositoryID)  throws RepositoryException, InterruptedException, RDFParseException, IOException{

        IRIHTTPRepository repository = setupIRIHTTPRepository(sesameURL, repositoryID);
        try {
            
            Vector<String> startingPoints = importURLs;

            log.info("updateCatalog(): Updating Repository...");
            if (updateRepository(repository, startingPoints, notImport)) {
                 RepositoryOps.updateExternalInference(repository, notImport, resourcePath, catalogCacheDirectory, loadfromtrig);            
                  

            }
            else {
                log.info("updateCatalog(): The repository was unchanged, nothing else to do.");
            }
        }
        finally {
            repository.shutDown();
        }

    }  


    public void updateIRISailRepository(boolean dropWithMemoryStoreDeleteRepository, Repository repository, Vector<String> importURLs, Vector<String> notImport)  throws RepositoryException, InterruptedException, RDFParseException, IOException{

        Lock repositoryWriteLock = _repositoryLock.writeLock();
        try {
            repositoryWriteLock.lock();
            Vector<String> startingPoints = importURLs;

            //log.info("updateIRISailRepository: Updating Repository...");
            //boolean repositoryChanged = updateRepository(repository, startingPoints, notImport);
            boolean repositoryChanged = RepositoryOps.updateSemanticRepository(repository, startingPoints, notImport, 
                    resourcePath, catalogCacheDirectory, loadfromtrig);
            if (repositoryChanged) {
                String workingDir = "./";
                File memoryStore = new File(workingDir + "_MemoryStore_");
                
                if(memoryStore.exists() && dropWithMemoryStoreDeleteRepository){ //assume _MemoryStore_ is generated by dropping with memory store
                repository.shutDown();
                
                File repoPath = new File(catalogCacheDirectory);
                RepositoryOps.deleteDirectory(repoPath);
                //repository = RepositoryOps.setupOwlimSailRepository(catalogCacheDirectory, loadfromtrig);
                repository =setupIRISailRepository();
                RepositoryOps.loadFromMem(repository);
                RepositoryOps.deleteDirectory(memoryStore);
                }
                
                RepositoryOps.updateExternalInference(repository, notImport, resourcePath, catalogCacheDirectory, loadfromtrig);            
            
            }
            else {
                log.info("updateIRISailRepository(): The repository was unchanged, nothing else to do.");
            }
            String filename = catalogCacheDirectory + "owlimMaxRepository.trig";
            log.debug("updateRepository(): Dumping Semantic Repository to: " + filename);
            RepositoryOps.dumpRepository(repository, filename);
            filename = catalogCacheDirectory + "owlimMaxRepository.nt";
            log.debug("updateRepository(): Dumping Semantic Repository to: " + filename);
            RepositoryOps.dumpRepository(repository, filename);   
        }
        finally {
            
            repositoryWriteLock.unlock();

            log.info("updateIRISailRepository(): Catalog update complete.");
        }

    }    
    
    public boolean updateRepository(Repository repository, Vector <String> startingPoints, Vector <String> notImport) throws RepositoryException, InterruptedException, RDFParseException, IOException{

        boolean repositoryChanged = RepositoryOps.updateSemanticRepository(repository, startingPoints, notImport,resourcePath, catalogCacheDirectory, loadfromtrig);

        return repositoryChanged;
    }
  
    private IRIHTTPRepository setupIRIHTTPRepository(String sesameURL, String repositoryID)
            throws RepositoryException, InterruptedException {

        log.info("Connect to server " + sesameURL + " ...");
        IRIHTTPRepository repository = new IRIHTTPRepository(sesameURL, repositoryID);

        log.info("Initialize repository " + repositoryID + " ...");
        repository.initialize();

        log.info("Semantic Repository Ready.");

        if(Thread.currentThread().isInterrupted())
            throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");
        
        return repository;

    } 
    private Repository setupIRISailRepository() throws RepositoryException, InterruptedException, RDFParseException, IOException {


        log.info("Setting up Semantic Repository.");

        //OWLIM Sail Repository (inferencing makes this somewhat slow)
        SailImpl owlimSail = new com.ontotext.trree.owlim_ext.SailImpl();
        Repository repository = new IRISailRepository(owlimSail); //owlim inferencing



        log.info("Configuring Semantic Repository.");
        File storageDir = new File(catalogCacheDirectory); //define local copy of repository
        owlimSail.setDataDir(storageDir);
        log.debug("Semantic Repository Data directory set to: "+ catalogCacheDirectory);
        // prepare config
        owlimSail.setParameter("storage-folder", owlim_storage_folder);
        log.debug("Semantic Repository 'storage-folder' set to: "+owlim_storage_folder);

        // Choose the operational ruleset
        String ruleset;
        //ruleset = "owl-horst-optimized";
        ruleset = "owl-max-optimized";

        owlimSail.setParameter("ruleset", ruleset);
                
        // switches on few performance "optimizations of the RDFS and OWL inference
        owlimSail.setParameter("partialRdfs", "true");
        
        log.info("Semantic Repository 'ruleset' set to: "+ ruleset);

        log.info("Intializing Semantic Repository.");

        // Initialize repository
        repository.initialize(); //needed
        
        //trig file (full path) to load into the repository. This is a work around
        //to the persistent bug in owlim
        if (loadfromtrig != null){
        String inTrigFile = loadfromtrig;
        RepositoryOps.clearRepository(repository);
        RepositoryOps.loadRepositoryFromTrigFile(repository, inTrigFile);
        String filename = catalogCacheDirectory + "afterloadingfromtrigfile.trig";
        log.debug("updateRepository(): Dumping Semantic Repository to: " + filename);
        RepositoryOps.dumpRepository(repository, filename);
        }
        
        log.info("Semantic Repository Ready.");

        if(Thread.currentThread().isInterrupted())
            throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");

        return repository;

    }

    private Repository setupIRISailRepository(String ruleset) throws RepositoryException, InterruptedException, RDFParseException, IOException {


        log.info("Setting up Semantic Repository.");

        //OWLIM Sail Repository (inferencing makes this somewhat slow)
        SailImpl owlimSail = new com.ontotext.trree.owlim_ext.SailImpl();
        Repository repository = new IRISailRepository(owlimSail); //owlim inferencing



        log.info("Configuring Semantic Repository.");
        File storageDir = new File(catalogCacheDirectory); //define local copy of repository
        owlimSail.setDataDir(storageDir);
        log.debug("Semantic Repository Data directory set to: "+ catalogCacheDirectory);
        // prepare config
        owlimSail.setParameter("storage-folder", owlim_storage_folder);
        log.debug("Semantic Repository 'storage-folder' set to: "+owlim_storage_folder);

        // Choose the operational ruleset
        //String ruleset;
        //ruleset = "owl-horst-optimized";
        //ruleset = "owl-max-optimized";

        owlimSail.setParameter("ruleset", ruleset);
                
        // switches on few performance "optimizations of the RDFS and OWL inference
        owlimSail.setParameter("partialRdfs", "true");
        
        log.info("Semantic Repository 'ruleset' set to: "+ ruleset);

        log.info("Intializing Semantic Repository.");

        // Initialize repository
        repository.initialize(); //needed
        
        //trig file (full path) to load into the repository. This is a work around
        //to the persistent bug in owlim
        if (loadfromtrig != null){
        String inTrigFile = loadfromtrig;
        RepositoryOps.clearRepository(repository);
        RepositoryOps.loadRepositoryFromTrigFile(repository, inTrigFile);
        String filename = catalogCacheDirectory + "afterloadingfromtrigfile.trig";
        log.debug("updateRepository(): Dumping Semantic Repository to: " + filename);
        RepositoryOps.dumpRepository(repository, filename);
        }
        
        log.info("Semantic Repository Ready.");

        if(Thread.currentThread().isInterrupted())
            throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");

        return repository;

    }

    
    private void processConfig(Element config, String defaultCacheDirectory,
            String defaultResourcePath) {

        Element e;
        File file;

        /**
         * ######################################################## Process
         * configuration.
         */
        catalogCacheDirectory = defaultCacheDirectory;
        e = config.getChild("CacheDirectory");
        if (e != null)
            catalogCacheDirectory = e.getTextTrim();
        if (catalogCacheDirectory != null && catalogCacheDirectory.length() > 0
                && !catalogCacheDirectory.endsWith("/"))
            catalogCacheDirectory += "/";

        file = new File(catalogCacheDirectory);
        /*if (!file.exists()) {
            if (!file.mkdirs()) {
                log.error("Unable to create cache directory: "
                                + cacheDirectory);
                if (!cacheDirectory.equals(defaultCacheDirectory)) {
                    file = new File(defaultCacheDirectory);
                    if (!file.exists()) {
                        if (!file.mkdirs()) {
                            log.error("Unable to create cache directory: "
                                    + defaultCacheDirectory);
                            log.error("Process probably doomed...");
                        }
                    }
                } else {
                    log.error("Process probably doomed...");
                }

            }
        }*/
        log.info("Using cacheDirectory: " + catalogCacheDirectory);

        resourcePath = defaultResourcePath;
        e = config.getChild("ResourcePath");
        if (e != null)
            resourcePath = e.getTextTrim();

        if (resourcePath != null && resourcePath.length() > 0
                && !resourcePath.endsWith("/"))
            resourcePath += "/";

        file = new File(this.resourcePath);
        if (!file.exists()) {
            log.error("Unable to locate resource directory: " + resourcePath);
            file = new File(defaultResourcePath);
            if (!file.exists()) {
                log.error("Unable to locate default resource directory: "
                        + defaultResourcePath);
                log.error("Process probably doomed...");
            }

        }

        log.info("Using resourcePath: " + resourcePath);

    }




    private Vector<String> getRdfImports(Element config) {

        Vector<String> rdfImports = new Vector<String>();
        Element e;
        String s;

        /**
         * Load individual dataset references
         */
        Iterator i = config.getChildren("dataset").iterator();
        String datasetURL;
        while (i.hasNext()) {
            e = (Element) i.next();
            datasetURL = e.getTextNormalize();

            if (!datasetURL.endsWith(".rdf")) {

                if (datasetURL.endsWith(".ddx") | datasetURL.endsWith(".dds")
                        | datasetURL.endsWith(".das")) {
                    datasetURL = datasetURL.substring(0, datasetURL
                            .lastIndexOf("."));
                }
                datasetURL += ".rdf";
            }
            rdfImports.add(datasetURL);
            log.info("Added dataset reference " + datasetURL
                    + " to RDF imports list.");
        }

        /**
         * Load RDF Imports
         */
        i = config.getChildren("RdfImport").iterator();
        while (i.hasNext()) {
            e = (Element) i.next();
            s = e.getTextNormalize();
            rdfImports.add(s);
            log.info("Added reference " + s + " to RDF imports list.");
        }

        return rdfImports;

    }
   

  
}
