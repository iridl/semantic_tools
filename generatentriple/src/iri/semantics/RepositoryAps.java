/**
 * 
 */
package iri.semantics;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.ntriples.NTriplesWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author haibo
 *
 */
public class RepositoryAps {
    private static Logger log = LoggerFactory.getLogger(RepositoryAps.class);
    
    public static void runConstruct(Repository repo, String constructFile, FileOutputStream ofs)  {
        RepositoryConnection con = null;
        try{
        con = repo.getConnection();
            
            
            long beforeConstruct = new Date().getTime();
            
            log.debug("Read in the query from the file = " + constructFile);    
                
            FileInputStream fstream = new FileInputStream(constructFile);
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            String qString = "";
            //Read File Line By Line
            while ((strLine = br.readLine()) != null)   {
                
                qString = qString + " " + strLine;
            }
            log.debug("Query string: "+ qString);
                
            //Close the input stream
            in.close();
            NTriplesWriter ntw = new NTriplesWriter(ofs);
            con.prepareGraphQuery(QueryLanguage.SERQL, qString).evaluate(ntw);
            long afterConstruct = new Date().getTime();
            double timeElapsed = (afterConstruct - beforeConstruct)/1000.0;
            log.info("Total time in running the construct rule:"+ timeElapsed +"seconds");
        }catch (Exception e){//Catch exception if any
            log.error("In runConstruct caught Exception: " + e.getMessage());
        }finally{
            try {
                con.close();
            } catch (RepositoryException e) {
                log.error("In runConstruct caught RepositoryException: " + e.getMessage());   
               
            }
        }
    }
    
    public static HashMap<String, String>  runQuery(Repository repo, String queryStr)  {
        RepositoryConnection con = null;
        HashMap<String, String>  queryResult = new HashMap<String, String>();
        try{
        con = repo.getConnection();
            
            
            log.debug("Read in the query from the file = " + queryStr);    
            
            TupleQuery tupq = con.prepareTupleQuery(QueryLanguage.SERQL, queryStr);
            TupleQueryResult result = tupq.evaluate();
            List<String> bindingNames = result.getBindingNames();
            while(result.hasNext()){
                BindingSet bindingSet = result.next();
                Value bindingValue0 = bindingSet.getValue(bindingNames.get(0));
                
                Value bindingValue1 = bindingSet.getValue(bindingNames.get(1));
                
                queryResult.put(bindingValue1.stringValue(),bindingValue0.stringValue());
                
            }
            
        }catch (Exception e){//Catch exception if any
            log.error("In runQuery caught Exception: " + e.getMessage());
        }finally{
            try {
                con.close();
            } catch (RepositoryException e) {
                log.error("In runQuery caught RepositoryException: " + e.getMessage());   
            }
        }
        return queryResult;
    }
}
