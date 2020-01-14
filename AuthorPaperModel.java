package example;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bouncycastle.util.Arrays.Iterator;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.procedure.*;

import example.javaModelTrain.Output;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import ml.dmlc.xgboost4j.java.DMatrix;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.TransactionWork;
import org.neo4j.driver.v1.Values;
import static org.neo4j.driver.v1.Values.parameters;


public class AuthorPaperModel {
	@Context
    public GraphDatabaseService db;

	@Procedure(value = "example.splitAuthorData", mode=Mode.WRITE)
    @Description("it splits data into train and test")
	 public void splitAuthorData(@Name("tag") String tag) throws IOException {
		 //This should return the id of train node, the id of test node
		 Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "neo4j"));
		 List<String> train = new ArrayList<>();
		 List<String> test = new ArrayList<>();
		 ArrayList<String> allAuthorIDsTrain = new ArrayList<>();
		 ArrayList<String> allAuthorIDsTest = new ArrayList<>();
		 List<Pair<String, String>> linkedAuthorIDs = new ArrayList<Pair<String, String>>();
		 Map<String, Set<String>> groundTruth = new HashMap<String, Set<String>>();
		 Map<String, Set<String>> groundTruthTest = new HashMap<String, Set<String>>();
		 Integer skip=0;
		
		 long startTime = 0;
		 long stopTime = 0;
		 startTime = System.currentTimeMillis();
		 
		 int counter = 0; //i write this counter into a file and it just shows me that the programming is running.	 
		 File file = new File("splitDataInDB.csv");
		 if(!file.exists()){
			  file.createNewFile();
			}else{
				FileOutputStream writer = new FileOutputStream("splitDataInDB.csv");
	            writer.write(("").getBytes());
	            writer.close();
			}
		 Path p = Paths.get("splitDataInDB.csv"); 
		 	 
		 Random random = new Random();
		 try (Session session = driver.session()) {
			 //Integer numEdges = Integer.parseInt(session.run("MATCH (a:page)-[r:LINKS_TO]->() RETURN COUNT(r)").next().get("COUNT(r)").toString());
			 //System.out.println("num" + " " + numEdges.toString());
			 //while (skip<numEdges){
			 //StatementResult listOfIds = session.run("MATCH (a:page)-[r:LINKS_TO]->(b:page) RETURN distinct a.PageID,b.PageID SKIP {skip} LIMIT 1000; ", Values.parameters("skip",skip));
			 StatementResult authorsTrain = session.run("MATCH (n:train)<-[rel1:AUTHORED_BY]-(m:train) RETURN DISTINCT n.AuthorID");
                         StatementResult authorsTest = session.run("MATCH (n:test)<-[rel1:AUTHORED_BY]-(m:test) RETURN DISTINCT n.AuthorID");
			 
                         StatementResult linksTrain = session.run("MATCH (n2:train)<-[rel1:AUTHORED_BY]-(m:paper)-[rel:AUTHORED_BY]->(n:train) WHERE n.AuthorID<n2.AuthorID AND m.Year=1990 RETURN DISTINCT n.AuthorID, n2.AuthorID;");
			 StatementResult linksTest = session.run("MATCH (n2:test)<-[rel1:AUTHORED_BY]-(m:paper)-[rel:AUTHORED_BY]->(n:test) WHERE n.AuthorID<n2.AuthorID AND  m.Year=1991 RETURN DISTINCT n.AuthorID, n2.AuthorID;");
			 
					 
			 linksTrain.stream().forEach(cand->{
				 
				 Set<String> hash_Set = new HashSet<String>();
				 String author1,author2;
				 author1=cand.get("n.AuthorID").toString().replace("\"","");
				 author2=cand.get("n2.AuthorID").toString().replace("\"","");
				 
				 if(groundTruth.containsKey(author1))
				 {
					 hash_Set = groundTruth.get(author1);
					 if(!hash_Set.contains(author2))
			 				hash_Set.add(author2);
					        train.add(author1+","+author2+",exists");
				 }
				 else {
				     hash_Set = new HashSet<String>();
				     hash_Set.add(author2);
				     train.add(author1+","+author2+",exists");
					 groundTruth.put(author1,hash_Set);
				 }}
				);
			 

			linksTest.stream().forEach(cand->{
				 
				 Set<String> hash_Set = new HashSet<String>();
				 String author1,author2;
				 author1=cand.get("n.AuthorID").toString().replace("\"","");
				 author2=cand.get("n2.AuthorID").toString().replace("\"","");
				 if(groundTruthTest.containsKey(author1))
				 {
					 hash_Set = groundTruthTest.get(author1);
					 if(!hash_Set.contains(author2))
			 				hash_Set.add(author2);	
					        test.add(author1+","+author2+",exists");
				 }
				 else {
				     hash_Set = new HashSet<String>();
				     hash_Set.add(author2);
				     test.add(author1+","+author2+",exists");
				     groundTruthTest.put(author1,hash_Set);
				 }}
				);
				 
			 
			   authorsTrain.stream().forEach(cand->{
				    allAuthorIDsTrain.add(cand.get("n.AuthorID").toString().replace("\"",""));			
				 });

			   authorsTest.stream().forEach(cand->{
				    allAuthorIDsTest.add(cand.get("n.AuthorID").toString().replace("\"",""));			
				 });
			   

			 session.close();
		 }
		 //Generate non exists
         Integer trainExists=train.size();
         Integer testExists=test.size();
         Integer i1, i2;
         while(train.size()<2*trainExists) {
        	 i1= random.nextInt(allAuthorIDsTrain.size());
        	 i2= random.nextInt(allAuthorIDsTrain.size());
        	 String author1,author2;
			 if (allAuthorIDsTrain.get(i1).compareTo(allAuthorIDsTrain.get(i2))<1) {
				 author1=allAuthorIDsTrain.get(i1);
				 author2=allAuthorIDsTrain.get(i2);
			 }
			 else {
				 author1=allAuthorIDsTrain.get(i2);
				 author2=allAuthorIDsTrain.get(i1);
			 }
        	 //Check if author1-2 exists in ground truth, if it does not exist, add to train
			 if (!(groundTruth.containsKey(author1)&&groundTruth.get(author1).contains(author2))) {
				 train.add(author1+","+author2+",nonexists");
			 }
			 
         }
         while(test.size()<2*testExists) {
        	 i1= random.nextInt(allAuthorIDsTest.size());
        	 i2= random.nextInt(allAuthorIDsTest.size());
        	 String author1,author2;
			 if (allAuthorIDsTest.get(i1).compareTo(allAuthorIDsTest.get(i2))<1) {
				 author1=allAuthorIDsTest.get(i1);
				 author2=allAuthorIDsTest.get(i2);
			 }
			 else {
				 author1=allAuthorIDsTest.get(i2);
				 author2=allAuthorIDsTest.get(i1);
			 }
        	 //Check if author1-2 exists in ground truth, if it does not exist, add to train
			 if (!(groundTruthTest.containsKey(author1)&&groundTruthTest.get(author1).contains(author2))) {
				 test.add(author1+","+author2+",nonexists");
			 }
         }
		 driver.close();
		 //Now we create nodes for train and test
 	     
		 stopTime = System.currentTimeMillis();
 		 String splitDataInBDTime = "Spliting Data inside neo4j takes..." + (stopTime - startTime) + "ms";
 	     Files.write(p, splitDataInBDTime.getBytes(), StandardOpenOption.APPEND);
 	     CreateNodesTrainTest(tag,train,test);					
	   }
	   
//-------------------------------------------------------------------------------------------------------
    
public void CreateNodesTrainTest(String tag,List<String> train,List<String> test)
{
	//Now we create train and test
	 String[] results = new String[2];
	 //This should return the id of train node, the id of test node
	 Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "neo4j"));
	 Integer skip=0;
	 
	 System.out.println("Train..." + train.size());
	 System.out.println("Test..." + test.size()); 
	 
	 
	 try (Session session = driver.session()) {
		 if(train.size() > 0)
		 {
			 session.run("CREATE (a:Train { tag: {tag}, data:{data} })", Values.parameters("tag",tag, "data", train));
			 session.run("CREATE (a:Test { tag: {tag}, data:{data}})", Values.parameters("tag",tag, "data", test));
		 }
		 /*
		 skip = 1000000;
		 while (skip<train.size()){
			 session.run("MATCH (a:Train { tag: {tag}) SET n.data=n.data+{data}", Values.parameters("tag",tag, "data", train.subList(skip, skip+1000000)));
			 skip+=1000;
		 }
		 
		 skip = 1000000;
		 while (skip<test.size()){
			 session.run("MATCH (a:Test { tag: {tag}) SET n.data=n.data+{data}", Values.parameters("tag",tag, "data", test.subList(skip, skip+1000000)));
			 skip+=1000;
		 }*/
    
		 session.close();
	 }
	 driver.close();
	 
}
//----------------------------------------------------------------------------------------------------
public static String creationTrainingTime = "";

@Procedure(value = "example.CreateAuthorPaperModel", mode=Mode.WRITE)
@Description("create model")
public Stream<Output> CreateAuthorPaperModel(@Name("tag") String tag,@Name("modelTag") String modelTag,@Name("CommonNeighbours") Boolean CommonNeighbours,@Name("ShortestPathInCoAuthorGraph") Boolean ShortestPathInCoAuthorGraph,
		@Name("SecondDegreeShortestPathInCoAuthorGraph") Boolean SecondDegreeShortestPathInCoAuthorGraph,@Name("ShortestPathInCitationGraph") Boolean ShortestPathInCitationGraph,
		@Name("SecondDegreeShortestPathInCitationGraph") Boolean SecondDegreeShortestPathInCitationGraph)
		throws XGBoostError, IOException 
{
	Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "neo4j"));
	
	String[] arrayTest = new String[]{};
	String[] arrayTrain = new String[]{}; 
	String[] arrTest = new String[1000];
	String[] arrTrain = new String[1000];
	//String testtraniTag = "";
	String label = "";
	
	int numberOfParameters = 0;
	if(CommonNeighbours)
	{numberOfParameters = numberOfParameters + 1;}	
	if(ShortestPathInCoAuthorGraph)
	{numberOfParameters = numberOfParameters + 1;}
	if(SecondDegreeShortestPathInCoAuthorGraph)
	{numberOfParameters = numberOfParameters + 1;}
	if(ShortestPathInCitationGraph)
	{numberOfParameters = numberOfParameters + 1;}
	if(SecondDegreeShortestPathInCitationGraph)
	{numberOfParameters = numberOfParameters + 1;}
	
	 long startTime = 0;
	 long stopTime = 0;
	 startTime = System.currentTimeMillis();
	 
	 int counter = 0; //i write this counter into a file and it just shows me that the programming is running.	 
	 File file = new File("counterIn.csv");
	 if(!file.exists()){
		  file.createNewFile();
		}else{
			FileOutputStream writer = new FileOutputStream("counterIn.csv");
            writer.write(("").getBytes());
            writer.close();
		}
	 Path p = Paths.get("counterIn.csv"); 
	 Files.write(p, Integer.toString(counter).getBytes(), StandardOpenOption.APPEND);
	 //------------------------------------------------------------------------------------------
	 
	 
	 try (Session session = driver.session()) {
		 
		 Map<String, Object> parameters = new HashMap<String, Object>();
		 parameters.put("tag", tag);
		 
		 List<Record> listOfTestData = session.run("MATCH (a:Test) Where a.tag = {tag} RETURN  a.data AS data",parameters).list();
		 List<Record> listOfTrainData = session.run("MATCH (a:Train) Where a.tag = {tag} RETURN  a.data AS data",parameters).list();

		 
		 arrTest = listOfTestData.get(0).get("data").toString().split(" ");
		 arrTrain = listOfTrainData.get(0).get("data").toString().split(" ");
		 
		 List<String> arrayListTest = new ArrayList<String>();
		 List<String> arrayListTrain = new ArrayList<String>(); 
		 
		//-------------------------------select 1000 nodes for test....500 exists and 500 non-exists
			 for(int t= 0;t < arrTest.length ;t++)
	         {
				 String[] str = arrTest[t].replace("\"", "").replace("[", "").replace("]", "").split(",");
				 if(str[2].replace(" ", "").contains("nonexists") && arrayListTest.size() < 500 && arrayListTest.contains(arrTest[t].replace("\"", "").replace("[", "").replace("]", ""))== false)
					{
						arrayListTest.add(arrTest[t].replace("\"", "").replace("[", "").replace("]", ""));
						
					}
	         }
		 
		 for(int t= 0;t < arrTest.length ;t++)
         {
			 String[] str = arrTest[t].replace("\"", "").replace("[", "").replace("]", "").split(",");
		 
			 if(str[2].replace(" ", "").contains("exist") && arrayListTest.size() < 1000 && arrayListTest.contains(arrTest[t].replace("\"", "").replace("[", "").replace("]", ""))== false)
			 {
					 arrayListTest.add(arrTest[t].replace("\"", "").replace("[", "").replace("]", ""));
			 }
         }
		//-------------------------------select 1000 nodes for test....500 exists and 500 non-exists
		 for(int t= 0;t < arrTrain.length ;t++)
         {
			 String[] str = arrTrain[t].replace("\"", "").replace("[", "").replace("]", "").split(",");
			 if(str[2].replace(" ", "").contains("nonexists") && arrayListTrain.size() < 500 && arrayListTrain.contains(arrTrain[t].replace("\"", "").replace("[", "").replace("]", ""))== false)
				{
						 arrayListTrain.add(arrTrain[t].replace("\"", "").replace("[", "").replace("]", ""));
				}
         }
		 
		 for(int t= 0;t < arrTrain.length ;t++)
         {
			 String[] str = arrTrain[t].replace("\"", "").replace("[", "").replace("]", "").split(",");
			 if(str[2].replace(" ", "").contains("exist") && arrayListTrain.size() < 1000  && arrayListTrain.contains(arrTrain[t].replace("\"", "").replace("[", "").replace("]", ""))== false)
				{
						 arrayListTrain.add(arrTrain[t].replace("\"", "").replace("[", "").replace("]", ""));
				}
         }
		 //------------------------------------------------------------------------------------------------------------------
		 arrayTest = arrayListTest.stream().toArray(String[]::new);
		 arrayTrain = arrayListTrain.stream().toArray(String[]::new);	 		 
		 //----------------------------------------------------------------------------------------------------------------------------------------
		 //They will be used for creating DMatrix
		 float[] dataTest = new float[(numberOfParameters+1)*arrayTest.length];
		 float[] dataTrain = new float[(numberOfParameters+1)*arrayTrain.length];
		 float[] labelsTest = new float[arrayTest.length];
		 float[] labelsTrain = new float[arrayTrain.length]; 
		 
		 int h = 0;
         for(int i= 0;i < arrayTest.length ;i++)
         {
        	 String[] arrayOfTestData = arrayTest[i].replace("\"", "").replace("[", "").replace("]", "").split(",");
        	 parameters = new HashMap<String, Object>();
    		 parameters.put("authorId1", arrayOfTestData[0].toString());
    		 parameters.put("authorId2", arrayOfTestData[1].toString());
    	       	
         	//Fill final dataTest array for passing to ModelCreationFunction
 		         if(arrayOfTestData[2].replace(" ", "").contains("nonexist"))
 		       	 {
 		       	     label = "0";
 		       	     labelsTest[i] = 0;
 		       	 }
 		       	 else
 		       	 {
 		       		 label = "1";
 		       		 labelsTest[i] = 1;
 		       	 }
 		         dataTest[h] = Float.parseFloat(label);
 		         h=h+1;
 		        // dataTest[h] = Float.parseFloat(arrayOfTestData[0]);
	        	// h = h + 1;
	        	// dataTest[h] = Float.parseFloat(arrayOfTestData[1]);
	        	// h = h + 1;
    		 
    		 
        	 //Feature Calculation
        	 
        	if(CommonNeighbours)
        	{
        		List<Record> commonNighboursList = session.run("MATCH (start:author{AuthorID:{authorId1}})<-[rel1:AUTHORED_BY]-(m:test)-[rel2:AUTHORED_BY]->"
	    		 		+ "(p:test)<-[rel3:AUTHORED_BY]-(q:test)-[rel4:AUTHORED_BY]->(end:author{AuthorID:{authorId2}}) WHERE p<>end AND p<>start  "
	    		 		+ "RETURN count(DISTINCT p) as score",parameters).list();
        		 if(commonNighboursList.size() > 0)
		         {
        			 Float normalizedValue = Float.parseFloat(commonNighboursList.get(0).get("score").toString().replace("\"", "").toString())/31;
        					 
        			 dataTest[h] = normalizedValue;
		         }
        		 else
        		 {
        			 dataTest[h] = Float.parseFloat("0");
        		 }
        		 h = h + 1;
        	}
        	
        	if(ShortestPathInCoAuthorGraph)
        	{
        		List<Record> shortestPathList = session.run("MATCH (start:author{AuthorID:{authorId1}}),"
        				+ "(end:author{AuthorID:{authorId2}}) \r\n" + 
        				 "CALL algo.shortestPath(start, end, 'cost',{write:false,nodeQuery:'test',relationshipQuery:'AUTHORED_BY'}) "
        				+ "YIELD totalCost RETURN totalCost" + 
        				"",parameters).list();
        		 if(shortestPathList.size() > 0)
		         {
        			    Float normalizedValue = Float.parseFloat(shortestPathList.get(0).get("totalCost").toString().replace("\"", "").toString())-(-1)/(52-(-1));
        			 	dataTest[h] = normalizedValue;
		         }
        		 else
        		 {
        			 dataTest[h] = Float.parseFloat("0");
        		 }
        		 h = h + 1;
        	}
        	
        	if(SecondDegreeShortestPathInCoAuthorGraph)
        	{
        		List<Record> SecondDegreeShortestPathInCoAurthorList = session.run("MATCH (start:author{AuthorID:{authorId1}}),"
        				+ "(end:author{AuthorID:{authorId2}}) \r\n" + 
        				"CALL algo.kShortestPaths.stream(start, end, 2, 'cost' ,"
        				+ "{write:false,nodeQuery:'test',relationshipQuery:'AUTHORED_BY'})\r\n" + 
        				"YIELD index,nodeIds, costs\r\n" + 
        				"RETURN reduce(acc = 0.0, cost in costs | acc + cost) AS totalCost \r\n" + 
        				"ORDER BY totalCost DESC LIMIT 1\r\n" + 
        				"",parameters).list();
        		 if(SecondDegreeShortestPathInCoAurthorList.size() > 0)
		         {
        			    Float normalizedValue = Float.parseFloat(SecondDegreeShortestPathInCoAurthorList.get(0).get("totalCost").toString().replace("\"", "").toString())-(-1) / 52-(-1);
        			 	dataTest[h] = normalizedValue;
		         }
        		 else
        		 {
        			 dataTest[h] = Float.parseFloat("0");
        		 }
        		 h = h + 1;
        	}
        	
        	if(ShortestPathInCitationGraph)
        	{
        		List<Record> shortestPathCitationList = session.run("MATCH (start:author{AuthorID:{authorId1}}),"
        				+ "(end:author{AuthorID:{authorId2}}) \r\n" + 
        				 "CALL algo.shortestPath(start, end, 'cost',{write:false,nodeQuery:'test'}) "
        				+ "YIELD totalCost RETURN totalCost" + 
        				"",parameters).list();
        		 if(shortestPathCitationList.size() > 0)
		         {
        			 Float normalizedValue = (Float.parseFloat(shortestPathCitationList.get(0).get("totalCost").toString().replace("\"", "").toString())-(-1))/(34-(-1));
        			 dataTest[h] = normalizedValue;  				 
		         }
        		 else
        		 {
        			 dataTest[h] = Float.parseFloat("0");
        		 }
        		 h = h + 1;
        	}
        	
        	if(SecondDegreeShortestPathInCitationGraph)
        	{
        		List<Record> SecondDegreeShortestPathInCitationList = session.run("MATCH (start:author{AuthorID:{authorId1}}),"
        				+ "(end:author{AuthorID:{authorId2}}) \r\n" + 
        				"CALL algo.kShortestPaths.stream(start, end, 2, 'cost' ,"
        				+ "{write:false,nodeQuery:'test'})\r\n" + 
        				"YIELD index,nodeIds, costs\r\n" + 
        				"RETURN reduce(acc = 0.0, cost in costs | acc + cost) AS totalCost \r\n" + 
        				"ORDER BY totalCost DESC LIMIT 1\r\n" + 
        				"",parameters).list();
        		 if(SecondDegreeShortestPathInCitationList.size() > 0)
		         {
        			 Float normalizedValue = (Float.parseFloat(SecondDegreeShortestPathInCitationList.get(0).get("totalCost").toString().replace("\"", "").toString())-(-1))/(34-(-1));
        			 dataTest[h] = normalizedValue;     			 	
		         }	 
        		 else
        		 {
        			 dataTest[h] = Float.parseFloat("0");
        		 }
        		 h = h + 1;
        	}
        	
        	counter = counter + 1;
        	Files.write(p, Integer.toString(counter).getBytes(), StandardOpenOption.APPEND);
         }//End of Calculation For Test Data
         
         
         
         
         h = 0;
         for(int i= 0;i < arrayTrain.length ;i++)
         {
        	 String[] arrayOfTrainData = arrayTrain[i].replace("\"", "").replace("[", "").replace("]", "").split(",");
        	 parameters = new HashMap<String, Object>();
    		 parameters.put("authorId1", arrayOfTrainData[0].toString());
    		 parameters.put("authorId2", arrayOfTrainData[1].toString());
    		 
         	//Fill final dataTest array for passing to ModelCreationFunction
 		         if(arrayOfTrainData[2].replace(" ", "").contains("nonexist"))
 		       	 {
 		       	     label = "0";
 		       	     labelsTrain[i] = 0;
 		       	 }
 		       	 else
 		       	 {
 		       		 label = "1";
 		       		 labelsTrain[i] = 1;
 		       	 }
 		         dataTrain[h] = Float.parseFloat(label);
 		         h=h+1;
 		        // dataTrain[h] = Float.parseFloat(arrayOfTrainData[0]);
	        	// h = h + 1;
	        	// dataTrain[h] = Float.parseFloat(arrayOfTrainData[1]);
	        	// h = h + 1;
    		 
    		 
        	 //Feature Calculation
        	 
        	if(CommonNeighbours)
        	{
        		List<Record> commonNighboursList = session.run("MATCH (start:author{AuthorID:{authorId1}})<-[rel1:AUTHORED_BY]-(m:train)-[rel2:AUTHORED_BY]->"
	    		 		+ "(p:train)<-[rel3:AUTHORED_BY]-(q:train)-[rel4:AUTHORED_BY]->(end:author{AuthorID:{authorId2}}) WHERE p<>end AND p<>start  "
	    		 		+ "RETURN count(DISTINCT p) as score",parameters).list();
        		 if(commonNighboursList.size() > 0)
		         {
        			 Float normalizedValue = Float.parseFloat(commonNighboursList.get(0).get("score").toString().replace("\"", "").toString())/15;
        			 dataTrain[h] = normalizedValue;
		         }
        		 else
        		 {
        			 dataTrain[h] = Float.parseFloat("0");
        		 }
        		 h = h + 1;
        	}
        	
        	if(ShortestPathInCoAuthorGraph)
        	{
        		List<Record> shortestPathList = session.run("MATCH (start:author{AuthorID:{authorId1}}),"
        				+ "(end:author{AuthorID:{authorId2}}) \r\n" + 
        				 "CALL algo.shortestPath(start, end, 'cost',{write:false,nodeQuery:'train',relationshipQuery:'AUTHORED_BY'}) "
        				+ "YIELD totalCost RETURN totalCost" + 
        				"",parameters).list();
			        		 
        		 if(shortestPathList.size() > 0)
		         {		        
        			 Float normalizedValue = (Float.parseFloat(shortestPathList.get(0).get("totalCost").toString().replace("\"", "").toString())-(-1))/(34-(-1));
        			 dataTrain[h] = normalizedValue;
		         }
        		 else
        		 {
        			 dataTrain[h] = Float.parseFloat("0");
        		 }
        		 h = h + 1;
        	}
        	
        	if(SecondDegreeShortestPathInCoAuthorGraph)
        	{
        		List<Record> SecondDegreeShortestPathInCoAurthorList = session.run("MATCH (start:author{AuthorID:{authorId1}}),"
        				+ "(end:author{AuthorID:{authorId2}}) \r\n" + 
        				"CALL algo.kShortestPaths.stream(start, end, 2, 'cost' ,"
        				+ "{write:false,nodeQuery:'train',relationshipQuery:'AUTHORED_BY'})\r\n" + 
        				"YIELD index,nodeIds, costs\r\n" + 
        				"RETURN reduce(acc = 0.0, cost in costs | acc + cost) AS totalCost \r\n" + 
        				"ORDER BY totalCost DESC LIMIT 1\r\n" + 
        				"",parameters).list();
	        	if(SecondDegreeShortestPathInCoAurthorList.size() > 0)
	        	{
	        		 Float normalizedValue = (Float.parseFloat(SecondDegreeShortestPathInCoAurthorList.get(0).get("totalCost").toString().replace("\"", "").toString())-(-1))/(34-(-1));
        			 dataTrain[h] = normalizedValue;
		         }
        		 else
        		 {
        			 dataTrain[h] = Float.parseFloat("0");
        		 }
        		 h = h + 1;
        	}
        	
        	if(ShortestPathInCitationGraph)
        	{
        		List<Record> shortestPathCitationList = session.run("MATCH (start:author{AuthorID:{authorId1}}),"
        				+ "(end:author{AuthorID:{authorId2}}) \r\n" + 
        				 "CALL algo.shortestPath(start, end, 'cost',{write:false,nodeQuery:'train'}) "
        				+ "YIELD totalCost RETURN totalCost" + 
        				"",parameters).list();
        		 if(shortestPathCitationList.size() > 0)
		         {
        			 Float normalizedValue = (Float.parseFloat(shortestPathCitationList.get(0).get("totalCost").toString().replace("\"", "").toString())-(-1))/(37-(-1));
        			 dataTrain[h]  = normalizedValue;
		         }
        		 else
        		 {
        			 dataTrain[h] = Float.parseFloat("0");
        		 }
        		 h = h + 1;
        	}
        	
        	if(SecondDegreeShortestPathInCitationGraph)
        	{
        		List<Record> SecondDegreeShortestPathInCitationList = session.run("MATCH (start:author{AuthorID:{authorId1}}),"
        				+ "(end:author{AuthorID:{authorId2}}) \r\n" + 
        				"CALL algo.kShortestPaths.stream(start, end, 2, 'cost' ,"
        				+ "{write:false,nodeQuery:'train'})\r\n" + 
        				"YIELD index,nodeIds, costs\r\n" + 
        				"RETURN reduce(acc = 0.0, cost in costs | acc + cost) AS totalCost \r\n" + 
        				"ORDER BY totalCost DESC LIMIT 1\r\n" + 
        				"",parameters).list();
        		 if(SecondDegreeShortestPathInCitationList.size() > 0)
        		 {
        			 Float normalizedValue = (Float.parseFloat(SecondDegreeShortestPathInCitationList.get(0).get("totalCost").toString().replace("\"", "").toString())-(-1))/(37-(-1));
        			 dataTrain[h] = normalizedValue;
        		 }
        		 else
        		 {
        			 dataTrain[h] = Float.parseFloat("0");
        		 }
        		 h = h + 1;
        	}
        	
        	
        	counter = counter + 1;
        	Files.write(p, Integer.toString(counter).getBytes(), StandardOpenOption.APPEND);
         }//End of Calculation For Train Data
         
        /*  for(int i=0;i < dataTrain.length;i++)
         {
        	 System.out.println(dataTrain[i]);
         }*/
         stopTime = System.currentTimeMillis();
 		 creationTrainingTime = "Feature Calculation inside neo4j takes..." + (stopTime - startTime) + "ms";
 	     Files.write(p, creationTrainingTime.getBytes(), StandardOpenOption.APPEND);
         
         //Model Creation
 	     startTime = System.currentTimeMillis();
         int numColumn = numberOfParameters + 1;
         //DMatrix testMat = new DMatrix(rowHeadersTest, colIndexTest, dataTest, DMatrix.SparseType.CSR, numColumn);
         //DMatrix trainMat = new DMatrix(rowHeadersTrain, colIndexTrain, dataTrain, DMatrix.SparseType.CSR, numColumn);
         DMatrix testMat = new DMatrix(dataTest,arrayTest.length, numColumn);
         DMatrix trainMat = new DMatrix(dataTrain,arrayTrain.length , numColumn);
         testMat.setLabel(labelsTest);
         trainMat.setLabel(labelsTrain);
      
 		Map<String, Object> params = new HashMap<String, Object>() {
 			  {
 			    put("eta", 1.0);
 			    put("max_depth", 2);
 			    //put("silent", 1);
 			    put("objective" , "multi:softprob"); 
 			    //put("objective", "binary:logistic");
 			    //put("eval_metric", "logloss");
 			    put("eval_metric", "mlogloss");
 			    //put("eval_metric", "merror");
 			    put("num_class", 2);
 			  }
 			};
 			
 			
 			Map<String, DMatrix> watches = new HashMap<String, DMatrix>() {
 				  {
 				    put("train", trainMat);
 				    put("test", testMat);
 				  }
 				};
 				int nround = 2;
 				Booster booster = XGBoost.train(trainMat, params, nround, watches, null, null);
 			    
 				float[][] predicts = booster.predict(testMat);
 				//System.out.println("predicts " + predicts[0][0] + " for class 1 and " + predicts[0][1] + " for class 0... The real label was:"+testMat.getLabel()[0]);
 				
 				float[] statisticInfo = confusionMatrix(testMat,predicts);
 				/*System.out.println("accuracy:"+statisticInfo[0] + "...."+
 						"precision:"+statisticInfo[1]+"....."+
 						"errorRate:"+statisticInfo[2]+"...."+
 						"recall:"+statisticInfo[3]+"....."+
 						"FScore:"+statisticInfo[4]);*/
 				SaveModelToNeo4j(booster,session,tag,statisticInfo,modelTag);
         
         
		stopTime = System.currentTimeMillis();
		creationTrainingTime = "Model Creation and training,inside neo4j takes..." + (stopTime - startTime) + "ms";
	 	Files.write(p, creationTrainingTime.getBytes(), StandardOpenOption.APPEND);
        session.close();		 
		}
	 
      javaModelTrain.Output  res = new javaModelTrain.Output(creationTrainingTime);
      List<Output> resultList = new ArrayList<Output>();
      resultList.add(new Output(creationTrainingTime));
	  driver.close();
      return resultList.stream();	
}

//------------------------------------------------------------------------------------------------------

public float[] confusionMatrix(DMatrix testMat,float[][] predicts) throws XGBoostError
{
	float[] statisticInfo= new float[5];
	float noNo = 0; float noYes = 0; float yesNo = 0; float yesYes = 0;
	float predictedLabel = 2;
	for(int i = 0;i < predicts.length;i++)
	{
		if(predicts[i][0] > predicts[i][1])
			predictedLabel = 1;
		else
			predictedLabel = 0;
	    if(testMat.getLabel()[i] == 1 && predictedLabel == 1)
	    	yesYes =+ 1;
	    else if(testMat.getLabel()[i] == 0 && predictedLabel == 0)
	    	noNo =+ 1;
	    else if(testMat.getLabel()[i] == 0 && predictedLabel == 1)
	    	noYes =+ 1;
	    else if(testMat.getLabel()[i] == 1 && predictedLabel == 0)
	    	yesNo =+ 1;		    
	}
	//(TP+TN)/total 
	float accuracy = (noNo + yesYes) /(noNo+yesYes+yesNo+noYes);
	// TP/predicted yes
	float precision = yesYes / (noYes + yesYes);
	float errorRate = 1-accuracy;
	// TP/actual yes 
	float recall = yesYes / (yesYes + yesNo);
	float FScore = 2*((precision*recall)/(precision+recall));
	
	statisticInfo[0] = accuracy;
	statisticInfo[1] = precision;
	statisticInfo[2] = errorRate;
	statisticInfo[3] = recall;
	statisticInfo[4] = FScore;
	return statisticInfo;
}
//-----------------------------------------------------------------------------------------------------------------------------------------
public void SaveModelToNeo4j(Booster booster,Session session,String tag,float[] statisticInfo,String modelTag) throws IOException, XGBoostError
{
String fileName = "model.bin";
booster.saveModel(fileName);
Path path = Paths.get(fileName);
byte[] bytes = Files.readAllBytes(path);

 if(bytes.length > 0)
 {
	 session.run("CREATE (a:Model { tag: {modelTag}, modelData:{data}, accuracy:{accuracy}, precision:{precision}, errorRate:{errorRate}, recall:{recall}, Fscore:{Fscore}, testtrainModelTag:{testtrainModelTag}})",
			 Values.parameters("modelTag",modelTag, "data", bytes,"accuracy",statisticInfo[0],"precision",statisticInfo[1],"errorRate",statisticInfo[2],"recall",statisticInfo[3],"Fscore",statisticInfo[4],"testtrainModelTag",tag));
 }
}	
//-------------------------------------------------------------------------------------------------------------------------------------------
public static String result = "";
@Description("ModelInference")
@Procedure(value = "example.AuthorPaperModelInference")
public Stream<Output> AuthorPaperModelInference(@Name("modelTag") String modelTag) throws IOException
{
	ArrayList<String> allAuthorIDs1991 = new ArrayList<>();
	 String sampleNumer = "100";
	 Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "neo4j"));
	 Map<String, Object> params = new HashMap<>();
     params.put("modelTag", modelTag );
     //String query = "MATCH (a:Model { tag: $modelTag } ) RETURN  a.modelData,a.accuracy,a.precision,a.errorRate,a.recall,a.Fscore";   
     Random random = new Random();	 
     
     long startTime = 0;
	 long stopTime = 0;
	 	 
	 File file = new File("INInferenceFile.csv");
	 if(!file.exists()){
		  file.createNewFile();
		}else{
			FileOutputStream writer = new FileOutputStream("INInferenceFile.csv");
            writer.write(("").getBytes());
            writer.close();
		}
	 	Path p = Paths.get("INInferenceFile.csv"); 
	 
	 
		 try (Session session = driver.session()) {
			 
			 //Get all the authors in 1991
			 StatementResult linksTest = session.run("MATCH (n2:test)<-[rel1:AUTHORED_BY]-(m:paper)-[rel:AUTHORED_BY]->(n:test) WHERE m.Year=1991 RETURN DISTINCT n.AuthorID, n2.AuthorID;");
			 linksTest.stream().forEach(cand->{
				 allAuthorIDs1991.add(cand.get("n.AuthorID").toString().replace("\"",""));	
				 allAuthorIDs1991.add(cand.get("n2.AuthorID").toString().replace("\"",""));
				 });		 
			 
			 //Randomly select 25,50,75,100 authors
	         Integer j,i1,i2;
	         j = 0;
	         Integer sampleNum = Integer.parseInt(sampleNumer);
	         List<Pair<String, String>> linkedAuthorIDs = new ArrayList<Pair<String, String>>();
	         while(j < sampleNum) {
	        	 i1= random.nextInt(allAuthorIDs1991.size());
	        	 i2= random.nextInt(allAuthorIDs1991.size());
	        	 if(i1 != i2 && linkedAuthorIDs.contains(new Pair<String, String>(allAuthorIDs1991.get(i1), allAuthorIDs1991.get(i2))) == false &&
	        		linkedAuthorIDs.contains(new Pair<String, String>(allAuthorIDs1991.get(i2), allAuthorIDs1991.get(i1))) == false)
	        	{
	        	    linkedAuthorIDs.add(new Pair<String, String>(allAuthorIDs1991.get(i1), allAuthorIDs1991.get(i2))); 	
	        	    j = j + 1;
				 }				 
	         }
	         
	         //Calculation Features
	         float[] dataTest = new float[(6)*linkedAuthorIDs.size()];
	    	 float[] labelsTest = new float[linkedAuthorIDs.size()];
	         int h = 0;
	         startTime = System.currentTimeMillis();
	         for(int i= 0;i < linkedAuthorIDs.size() ;i++)
	         {
	        	 HashMap<String, Object> parameters = new HashMap<String, Object>();
	    		 parameters.put("authorId1", linkedAuthorIDs.get(i).getValue(0).toString());
	    		 parameters.put("authorId2", linkedAuthorIDs.get(i).getValue(1).toString());
	    	       	
	    		 //Lable
	 		         dataTest[h] = 0;
	 		         h=h+1;
	 		         labelsTest[i] = 0;
	 		         
	        	 //Feature Calculation	        	 
	        	//CommonNeighbours)
	        	
	        		List<Record> commonNighboursList = session.run("MATCH (start:author{AuthorID:{authorId1}})<-[rel1:AUTHORED_BY]-(m:test)-[rel2:AUTHORED_BY]->"
		    		 		+ "(p:test)<-[rel3:AUTHORED_BY]-(q:test)-[rel4:AUTHORED_BY]->(end:author{AuthorID:{authorId2}}) WHERE p<>end AND p<>start  "
		    		 		+ "RETURN count(DISTINCT p) as score",parameters).list();
	        		 if(commonNighboursList.size() > 0)
			         {
	        			 Float normalizedValue = Float.parseFloat(commonNighboursList.get(0).get("score").toString().replace("\"", "").toString())/31;
	        					 
	        			 dataTest[h] = normalizedValue;
			         }
	        		 else
	        		 {
	        			 dataTest[h] = Float.parseFloat("0");
	        		 }
	        		 h = h + 1;
	        	
	        	
	        	//ShortestPathInCoAuthorGraph)
	        	
	        		List<Record> shortestPathList = session.run("MATCH (start:author{AuthorID:{authorId1}}),"
	        				+ "(end:author{AuthorID:{authorId2}}) \r\n" + 
	        				 "CALL algo.shortestPath(start, end, 'cost',{write:false,nodeQuery:'test',relationshipQuery:'AUTHORED_BY'}) "
	        				+ "YIELD totalCost RETURN totalCost" + 
	        				"",parameters).list();
	        		 if(shortestPathList.size() > 0)
			         {
	        			    Float normalizedValue = Float.parseFloat(shortestPathList.get(0).get("totalCost").toString().replace("\"", "").toString())-(-1)/(52-(-1));
	        			 	dataTest[h] = normalizedValue;
			         }
	        		 else
	        		 {
	        			 dataTest[h] = Float.parseFloat("0");
	        		 }
	        		 h = h + 1;
	        	
	        	
	        	//SecondDegreeShortestPathInCoAuthorGraph)
	        	
	        		List<Record> SecondDegreeShortestPathInCoAurthorList = session.run("MATCH (start:author{AuthorID:{authorId1}}),"
	        				+ "(end:author{AuthorID:{authorId2}}) \r\n" + 
	        				"CALL algo.kShortestPaths.stream(start, end, 2, 'cost' ,"
	        				+ "{write:false,nodeQuery:'test',relationshipQuery:'AUTHORED_BY'})\r\n" + 
	        				"YIELD index,nodeIds, costs\r\n" + 
	        				"RETURN reduce(acc = 0.0, cost in costs | acc + cost) AS totalCost \r\n" + 
	        				"ORDER BY totalCost DESC LIMIT 1\r\n" + 
	        				"",parameters).list();
	        		 if(SecondDegreeShortestPathInCoAurthorList.size() > 0)
			         {
	        			    Float normalizedValue = Float.parseFloat(SecondDegreeShortestPathInCoAurthorList.get(0).get("totalCost").toString().replace("\"", "").toString())-(-1) / 52-(-1);
	        			 	dataTest[h] = normalizedValue;
			         }
	        		 else
	        		 {
	        			 dataTest[h] = Float.parseFloat("0");
	        		 }
	        		 h = h + 1;
	        	
	        	
	        	//ShortestPathInCitationGraph)
	        	
	        		List<Record> shortestPathCitationList = session.run("MATCH (start:author{AuthorID:{authorId1}}),"
	        				+ "(end:author{AuthorID:{authorId2}}) \r\n" + 
	        				 "CALL algo.shortestPath(start, end, 'cost',{write:false,nodeQuery:'test'}) "
	        				+ "YIELD totalCost RETURN totalCost" + 
	        				"",parameters).list();
	        		 if(shortestPathCitationList.size() > 0)
			         {
	        			 Float normalizedValue = (Float.parseFloat(shortestPathCitationList.get(0).get("totalCost").toString().replace("\"", "").toString())-(-1))/(34-(-1));
	        			 dataTest[h] = normalizedValue;  				 
			         }
	        		 else
	        		 {
	        			 dataTest[h] = Float.parseFloat("0");
	        		 }
	        		 h = h + 1;
	        	
	        	
	        	//SecondDegreeShortestPathInCitationGraph)
	        	
	        		List<Record> SecondDegreeShortestPathInCitationList = session.run("MATCH (start:author{AuthorID:{authorId1}}),"
	        				+ "(end:author{AuthorID:{authorId2}}) \r\n" + 
	        				"CALL algo.kShortestPaths.stream(start, end, 2, 'cost' ,"
	        				+ "{write:false,nodeQuery:'test'})\r\n" + 
	        				"YIELD index,nodeIds, costs\r\n" + 
	        				"RETURN reduce(acc = 0.0, cost in costs | acc + cost) AS totalCost \r\n" + 
	        				"ORDER BY totalCost DESC LIMIT 1\r\n" + 
	        				"",parameters).list();
	        		 if(SecondDegreeShortestPathInCitationList.size() > 0)
			         {
	        			 Float normalizedValue = (Float.parseFloat(SecondDegreeShortestPathInCitationList.get(0).get("totalCost").toString().replace("\"", "").toString())-(-1))/(34-(-1));
	        			 dataTest[h] = normalizedValue;     			 	
			         }	 
	        		 else
	        		 {
	        			 dataTest[h] = Float.parseFloat("0");
	        		 }
	        		 h = h + 1;
	        	
	        	
	        	
	         }//End of Calculation For Test Data
	         stopTime = System.currentTimeMillis();
	         String inferenceTime = "Model Inference feature Creation " + (stopTime - startTime) + "ms" + "\n";
			 Files.write(p, inferenceTime.getBytes(), StandardOpenOption.APPEND);
	         
	         
	         
	     	//repeat 10 times this proccess for 100 samples
	         long modelInferenceTime = 0;
			 for (int k= 0 ; k <10; k++)
			 {
				 startTime = System.currentTimeMillis();
		  //Model Inference....
			 StatementResult modelData = session.run("MATCH (a:Model { tag: {modelTag} } ) RETURN  a.modelData",Values.parameters("modelTag",modelTag));
			 modelData.stream().forEach(cand->{
				 
				 byte[] bytes = cand.get("a.modelData").asByteArray();				 
				 String FILEPATH = "model.bin";
				 File file1 = new File(FILEPATH);
				 try {
					OutputStream os = new FileOutputStream(file1);
					os.write(bytes); 
					Booster booster = XGBoost.loadModel(FILEPATH);
					
				
					      DMatrix testMat = new DMatrix(dataTest,linkedAuthorIDs.size(), 6);
					      testMat.setLabel(labelsTest);
	
					      float[][] predicts = booster.predict(testMat);
			 		      /*result += "prediction for nodeID " + str[1]  + " and " + str[2] + " and label 1 is " + predicts[0][0] + "..."  + 
			 		       "prediction for nodeID " + str[1]  + " and " + str[2] + " and label 0 is " + predicts[0][1] + "..." + "\n";   
			 		     System.out.println(result);*/
					 }
			    catch (IOException | XGBoostError e)
			    {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			 });
			 
			 stopTime = System.currentTimeMillis();
			 
			 modelInferenceTime = modelInferenceTime + (stopTime - startTime);			
			 }
			 inferenceTime = "Model Inference " + " For " + sampleNumer + " takes "  + (modelInferenceTime/10) + "ms" + "\n";
			 Files.write(p, inferenceTime.getBytes(), StandardOpenOption.APPEND);
			 			 
			 
			 
			//repeat 10 times this proccess for 75 samples
	         modelInferenceTime = 0;
	         float[] dataTest75 = new float[6*75];
	         float[] labelsTest75 = new float[75];
	         for(int o = 0; o < 75;o++)
	         {
	        	 dataTest75[o] = dataTest[o];	
	         }
	         for(int o = 0; o < 75;o++)
	         {
	        	 labelsTest75[o] = labelsTest[o];	
	         }
	         
			 for (int k= 0 ; k <10; k++)
			 {
		  //Model Inference....
			startTime = System.currentTimeMillis();
			 StatementResult modelData = session.run("MATCH (a:Model { tag: {modelTag} } ) RETURN  a.modelData",Values.parameters("modelTag",modelTag));
			 modelData.stream().forEach(cand->{
				 
				 byte[] bytes = cand.get("a.modelData").asByteArray();				 
				 String FILEPATH = "model.bin";
				 File file1 = new File(FILEPATH);
				 try {
					OutputStream os = new FileOutputStream(file1);
					os.write(bytes); 
					Booster booster = XGBoost.loadModel(FILEPATH);
					
				
					      DMatrix testMat = new DMatrix(dataTest75,75, 6);
					      testMat.setLabel(labelsTest75);
	
					      float[][] predicts = booster.predict(testMat);
					 }
			    catch (IOException | XGBoostError e)
			    {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			 });
			 
			 stopTime = System.currentTimeMillis();
			 
			 modelInferenceTime = modelInferenceTime + (stopTime - startTime);			
			 }
			 inferenceTime = "Model Inference " + " For " + "75 samples" + " takes "  + (modelInferenceTime/10) + "ms" + "\n";
			 Files.write(p, inferenceTime.getBytes(), StandardOpenOption.APPEND);
			 
			 
			//repeat 10 times this proccess for 50 samples
	         modelInferenceTime = 0;
	         float[] dataTest50 = new float[6*50];
	         float[] labelsTest50 = new float[50];
	         for(int o = 0; o < 50;o++)
	         {
	        	 dataTest50[o] = dataTest[o];	
	         }
	         for(int o = 0; o < 50;o++)
	         {
	        	 labelsTest50[o] = labelsTest[o];	
	         }
	         
			 for (int k= 0 ; k <10; k++)
			 {
		  //Model Inference....
		     startTime = System.currentTimeMillis();
			 StatementResult modelData = session.run("MATCH (a:Model { tag: {modelTag} } ) RETURN  a.modelData",Values.parameters("modelTag",modelTag));
			 modelData.stream().forEach(cand->{
				 
				 byte[] bytes = cand.get("a.modelData").asByteArray();				 
				 String FILEPATH = "model.bin";
				 File file1 = new File(FILEPATH);
				 try {
					OutputStream os = new FileOutputStream(file1);
					os.write(bytes); 
					Booster booster = XGBoost.loadModel(FILEPATH);
					
				
					      DMatrix testMat = new DMatrix(dataTest50,50, 6);
					      testMat.setLabel(labelsTest50);
	
					      float[][] predicts = booster.predict(testMat);
					 }
			    catch (IOException | XGBoostError e)
			    {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			 });
			 
			 stopTime = System.currentTimeMillis();
			 
			 modelInferenceTime = modelInferenceTime + (stopTime - startTime);			
			 }
			 inferenceTime = "Model Inference " + " For " + "50 samples" + " takes "  + (modelInferenceTime/10) + "ms" + "\n";
			 Files.write(p, inferenceTime.getBytes(), StandardOpenOption.APPEND);
			 
			 
			//repeat 10 times this proccess for 50 samples
	         modelInferenceTime = 0;
	         float[] dataTest25 = new float[6*25];
	         float[] labelsTest25 = new float[25];
	         for(int o = 0; o < 25;o++)
	         {
	        	 dataTest25[o] = dataTest[o];	
	         }
	         for(int o = 0; o < 25;o++)
	         {
	        	 labelsTest25[o] = labelsTest[o];	
	         }
	         
			 for (int k= 0 ; k <10; k++)
			 {
		  //Model Inference....
		     startTime = System.currentTimeMillis();
			 StatementResult modelData = session.run("MATCH (a:Model { tag: {modelTag} } ) RETURN  a.modelData",Values.parameters("modelTag",modelTag));
			 modelData.stream().forEach(cand->{
				 
				 byte[] bytes = cand.get("a.modelData").asByteArray();				 
				 String FILEPATH = "model.bin";
				 File file1 = new File(FILEPATH);
				 try {
					OutputStream os = new FileOutputStream(file1);
					os.write(bytes); 
					Booster booster = XGBoost.loadModel(FILEPATH);
					
				
					      DMatrix testMat = new DMatrix(dataTest25,25,6);
					      testMat.setLabel(labelsTest25);
	
					      float[][] predicts = booster.predict(testMat);
					 }
			    catch (IOException | XGBoostError e)
			    {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			 });
			 
			 stopTime = System.currentTimeMillis();
			 
			 modelInferenceTime = modelInferenceTime + (stopTime - startTime);			
			 }
			 inferenceTime = "Model Inference " + " For " + "25 samples" + " takes "  + (modelInferenceTime/10) + "ms" + "\n";
			 Files.write(p, inferenceTime.getBytes(), StandardOpenOption.APPEND);
					 
			 session.close();
		 
		
		  javaModelTrain.Output  res = new javaModelTrain.Output(result);
		//InputStream in = org.apache.commons.io.IOUtils.toInputStream(result, "UTF-8");
			 driver.close();
	      List<Output> resultList = new ArrayList<Output>();
	      resultList.add(new Output(result));
	      return resultList.stream();	
	}
}
//------------------------------------------------------------------------------------------------------------
 public static class Output {
	    public String out;
	    public Output(String result)
		{
			this.out = result;
	    }
}
//-------------------------------------------------------------------------------------------------------------
}





