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
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.bouncycastle.util.Arrays.Iterator;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.procedure.*;

import ml.dmlc.xgboost4j.java.XGBoostError;

public class FeatureDistribution {
	
	public void FeatureDistributionCalculation(String tag) throws IOException {
		 //This should return the id of train node, the id of test node
		 Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "neo4j"));
		 String[] arrayTest = new String[]{};
		 String[] arrayTrain = new String[]{};
		 String[] arrTest = new String[1000];
		 String[] arrTrain = new String[1000];
		 String testtraniTag = "";
		 Integer skip=0;
		 long startTime = 0;
		 long stopTime = 0;
		 startTime = System.currentTimeMillis();
		 
		 Random random = new Random();
		 try (Session session = driver.session()) {
			 
			 Map<String, Object> parameters = new HashMap<String, Object>();
    		 parameters.put("tag", tag);
			 
			 List<Record> listOfTestData = session.run("MATCH (a:Test) Where a.tag = {tag} RETURN  a.data AS data",parameters).list();
			 List<Record> listOfTrainData = session.run("MATCH (a:Train) Where a.tag = {tag} RETURN  a.data AS data",parameters).list();

			 
			 arrTest = listOfTestData.get(0).get("data").toString().split(" ");
			 arrTrain = listOfTrainData.get(0).get("data").toString().split(" ");
			 
			 List<String> arrayListTest = new ArrayList<String>();
			 List<String> arrayListTrain = new ArrayList<String>(); 
			 
			//-------------------------------randomly select 1000 nodes for test....500 exists and 500 non-exists
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
			//-------------------------------randomly select 1000 nodes for test....500 exists and 500 non-exists
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
			 //---------------------------------------------------------------------------------------------------------------------
			 
			 
			 
			 Map<Float, Integer> commonNeighbourTest = new HashMap<Float, Integer>();
			 Map<Float,Integer> ShortestPathInCoAuthorGraph = new HashMap<Float, Integer>();
			 Map<Float,Integer> SecondShortestPathInCoAuthorGraph = new HashMap<Float, Integer>();
			 Map<Float,Integer> ShortestPathInCitationGraph = new HashMap<Float, Integer>();
			 Map<Float,Integer> SecondShortestPathInCitationGraph = new HashMap<Float, Integer>();
			 
			 Float commonNeighbour = (float)-1;
			 Float ShortestPath = (float)-1;		 
			 Float secondShortestPath = (float)-1;
			 Float ShortestPathCitation = (float)-1;
			 Float secondShortestPathCitation = (float)-1;
			 
			 
			/* File file = new File("Test.csv");
			 if(!file.exists()){
				  file.createNewFile();
				}else{
					FileOutputStream writer = new FileOutputStream("Test.csv");
		            writer.write(("").getBytes());
		            writer.close();
				}*/
			 /*Path p = Paths.get("Test.csv"); 
			 String s = "AuthorID1,AuthorID2,CommonNeighbour,ShortestPathAuthorGraph,SecondShortestPathAuthorGraph,ShortestPathCitationGraph,SecondShortestPathCitationGraph,Label";
			 Files.write(p, s.getBytes(), StandardOpenOption.APPEND);*/
			 
			// System.out.println("Started calculating Features for Authors in Test List......");
	       /*  for(int i= 0;i < arrayTest.length ;i++)
	         {
	        	 String[] arrayOfTestData = arrayTest[i].replace("\"", "").replace("[", "").replace("]", "").split(",");
	        	 parameters = new HashMap<String, Object>();
	    		 parameters.put("authorId1", arrayOfTestData[0].toString());
	    		 parameters.put("authorId2", arrayOfTestData[1].toString());
	    	       	

	        	 //Feature Calculation.....................................................................................
	        	//CommonNeighbour
	    		 List<Record> commonNighboursList = session.run("MATCH (start:author{AuthorID:{authorId1}})<-[rel1:AUTHORED_BY]-(m:test)-[rel2:AUTHORED_BY]->"
	    		 		+ "(p:test)<-[rel3:AUTHORED_BY]-(q:test)-[rel4:AUTHORED_BY]->(end:author{AuthorID:{authorId2}}) WHERE p<>end AND p<>start  "
	    		 		+ "RETURN count(DISTINCT p) as score",parameters).list();
	        		 if(commonNighboursList.size() > 0)
			         {
	        			 	//System.out.println("commonNighboursList"+ " " + commonNighboursList.get(0).get("score").toString().replace("\"", "").toString());
	        			     commonNeighbour = Float.parseFloat(commonNighboursList.get(0).get("score").toString().replace("\"", "").toString());
		        			 if(commonNeighbourTest.containsKey(commonNeighbour))
		    				 {
		        				 commonNeighbourTest.put(commonNeighbour,commonNeighbourTest.get(commonNeighbour)+1);
		    				 }
		        			 else
		        			 {
		    				     commonNeighbourTest.put(commonNeighbour,1);
		        			 }
		    				 
			         }
	        	
	        		 //ShortestPathInCoAuthorGraph............................................................................
	        		List<Record> shortestPathList = session.run("MATCH (start:author{AuthorID:{authorId1}}),"
	        				+ "(end:author{AuthorID:{authorId2}}) \r\n" + 
	        				 "CALL algo.shortestPath(start, end, 'cost',{write:false,nodeQuery:'test',relationshipQuery:'AUTHORED_BY'}) "
	        				+ "YIELD totalCost RETURN totalCost" + 
	        				"",parameters).list();
	        		 if(shortestPathList.size() > 0)
			         {
	        			 	//System.out.println("ShortestPathInCoAuthorGraph"+ " " + shortestPathList.get(0).get("totalCost").toString().replace("\"", "").toString());
	        			 ShortestPath = Float.parseFloat(shortestPathList.get(0).get("totalCost").toString().replace("\"", "").toString());
	        			 if(ShortestPathInCoAuthorGraph.containsKey(ShortestPath))
	    				 {
	        				 ShortestPathInCoAuthorGraph.put(ShortestPath,ShortestPathInCoAuthorGraph.get(ShortestPath)+1);
	    				 }
	        			 else
	        			 {
	        				 ShortestPathInCoAuthorGraph.put(ShortestPath,1);
	        			 }
	    				 
			         }
	        		 
	        		 //SecondDegreeShortestPathInCoAuthorGraph.............................................................
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
	        			 	//System.out.println("SecondDegreeShortestPathInCoAuthorGraph"+ " " + SecondDegreeShortestPathInCoAurthorList.get(0).get("totalCost").toString().replace("\"", "").toString());
	        			 	secondShortestPath = Float.parseFloat(SecondDegreeShortestPathInCoAurthorList.get(0).get("totalCost").toString().replace("\"", "").toString());
	        			 	if(SecondShortestPathInCoAuthorGraph.containsKey(secondShortestPath))
		    				 {
		        				 SecondShortestPathInCoAuthorGraph.put(secondShortestPath,SecondShortestPathInCoAuthorGraph.get(secondShortestPath) + 1);
		    				 }
		        			 else
		        			 {
		        				 SecondShortestPathInCoAuthorGraph.put(secondShortestPath,1);
		        			 }
			         }
	        		 
	        		 //ShortestPathInCitationGraph........................................................................................
	        		 List<Record> shortestPathCitationList = session.run("MATCH (start:author{AuthorID:{authorId1}}),"
		        				+ "(end:author{AuthorID:{authorId2}}) \r\n" + 
		        				 "CALL algo.shortestPath(start, end, 'cost',{write:false,nodeQuery:'test'}) "
		        				+ "YIELD totalCost RETURN totalCost" + 
		        				"",parameters).list();
		        		 if(shortestPathCitationList.size() > 0)
				         {
		        			// System.out.println("shortestPathCitationList"+ " " + shortestPathCitationList.get(0).get("totalCost").toString().replace("\"", "").toString());
		        			 ShortestPathCitation = Float.parseFloat(shortestPathCitationList.get(0).get("totalCost").toString().replace("\"", "").toString());
		        			 if(ShortestPathInCitationGraph.containsKey(ShortestPathCitation))
		    				 {
		        				 ShortestPathInCitationGraph.put(ShortestPathCitation,ShortestPathInCitationGraph.get(ShortestPathCitation)+1);
		    				 }
		        			 else
		        			 {
		        				 ShortestPathInCitationGraph.put(ShortestPathCitation,1);
		        			 }
		    				 
				         }
		        		 
		            //SecondShortestpathInCitationGraph		 
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
			        			 	//System.out.println("SecondDegreeShortestPathInCitationList"+ " " + SecondDegreeShortestPathInCitationList.get(0).get("totalCost").toString().replace("\"", "").toString());
			        			 	secondShortestPathCitation = Float.parseFloat(SecondDegreeShortestPathInCitationList.get(0).get("totalCost").toString().replace("\"", "").toString());
			        			 	if(SecondShortestPathInCitationGraph.containsKey(secondShortestPathCitation))
				    				 {
			        			 		SecondShortestPathInCitationGraph.put(secondShortestPathCitation,SecondShortestPathInCitationGraph.get(secondShortestPathCitation) + 1);
				    				 }
				        			 else
				        			 {
				        				 SecondShortestPathInCitationGraph.put(secondShortestPathCitation,1);
				        			 }
					         }	 
			        		 
			        		 //Update The File Test.csv
			        		 s = System.lineSeparator() + arrayOfTestData[0]+ "," + arrayOfTestData[1] + "," + commonNeighbour.toString()+ "," + ShortestPath.toString() + "," + secondShortestPath.toString()+ "," + ShortestPathCitation.toString()+ "," + secondShortestPathCitation.toString()+ "," + arrayOfTestData[2] ;
			        		 Files.write(p, s.getBytes(), StandardOpenOption.APPEND);
	         }
	         
	         System.out.println("Finished calculating Features for Authors in Test List......");
			 
	         file = new File("testCommonNeighbour.csv");
			 if(!file.exists()){
				  file.createNewFile();
				}else{
					FileOutputStream writer = new FileOutputStream("testCommonNeighbour.csv");
		            writer.write(("").getBytes());
		            writer.close();
				}
			 
			 file = new File("testShortestPathCoAuthorGraph.csv");
			 if(!file.exists()){
				  file.createNewFile();
				}else{
					FileOutputStream writer = new FileOutputStream("testShortestPathCoAuthorGraph.csv");
		            writer.write(("").getBytes());
		            writer.close();
				}
			 
			 file = new File("testSecondShortestPathCoAuthorGraph.csv");
			 if(!file.exists()){
				  file.createNewFile();
				}else{
					FileOutputStream writer = new FileOutputStream("testSecondShortestPathCoAuthorGraph.csv");
		            writer.write(("").getBytes());
		            writer.close();
				}
			 
			 file = new File("testShortestPathCoCitationGraph.csv");
			 if(!file.exists()){
				  file.createNewFile();
				}else{
					FileOutputStream writer = new FileOutputStream("testShortestPathCoCitationGraph.csv");
		            writer.write(("").getBytes());
		            writer.close();
				}
			 
			 file = new File("testSecondShortestPathCitationGraph.csv");
			 if(!file.exists()){
				  file.createNewFile();
				}else{
					FileOutputStream writer = new FileOutputStream("testSecondShortestPathCitationGraph.csv");
		            writer.write(("").getBytes());
		            writer.close();
				}
			 
			 
			//WriteToFile testCommonNeighbour HashMap
	         Path testCommonNeighbour = Paths.get("testCommonNeighbour.csv"); 
	         for(Map.Entry<Float,Integer> m :commonNeighbourTest.entrySet()){
	            	 String content = m.getKey()+","+m.getValue()+ System.lineSeparator();
	            	 Files.write(testCommonNeighbour, content.getBytes(), StandardOpenOption.APPEND);
	             }
	             
	         //WriteToFile ShortestPathInCoAuthorGraph HashMap
	         Path testShortestPathCoAuthorGraph = Paths.get("testShortestPathCoAuthorGraph.csv");
             for(Map.Entry<Float,Integer> m :ShortestPathInCoAuthorGraph.entrySet()){
            	 String content = m.getKey()+","+m.getValue() + System.lineSeparator();
            	 Files.write(testShortestPathCoAuthorGraph, content.getBytes(), StandardOpenOption.APPEND);
             }
	             
	           //WriteToFile SecondShortestPathInCoAuthorGraph HashMap
             Path testSecondShortestPathCoAuthorGraph = Paths.get("testSecondShortestPathCoAuthorGraph.csv");
             for(Map.Entry<Float,Integer> m :SecondShortestPathInCoAuthorGraph.entrySet()){
            	 String content = m.getKey()+","+m.getValue() + System.lineSeparator();
            	 Files.write(testSecondShortestPathCoAuthorGraph, content.getBytes(), StandardOpenOption.APPEND);
             }
		             
		             
	           //WriteToFile ShortestPathInCitationGraph HashMap
             Path testShortestPathCoCitationGraph = Paths.get("testShortestPathCoCitationGraph.csv");		     
             for(Map.Entry<Float,Integer> m :ShortestPathInCitationGraph.entrySet()){
            	 String content = m.getKey()+","+m.getValue() + System.lineSeparator();
            	 Files.write(testShortestPathCoCitationGraph, content.getBytes(), StandardOpenOption.APPEND);
             }    
		             
		             
	             //WriteToFile ShortestPathInCitationGraph HashMap
             Path testSecondShortestPathCitationGraph = Paths.get("testSecondShortestPathCitationGraph.csv");
             for(Map.Entry<Float,Integer> m :SecondShortestPathInCitationGraph.entrySet()){
            	 String content = m.getKey()+","+m.getValue() + System.lineSeparator();
            	 Files.write(testSecondShortestPathCitationGraph, content.getBytes(), StandardOpenOption.APPEND);
             }        
             
             */
             
             //-----------------------------------------------------------------------------------------------------------------------------------------------
             //Generate Same result for Train data as well----------------------------------------------------------------------------------------------------
             //-----------------------------------------------------------------------------------------------------------------------------------------------

             Map<Float,Integer>  commonNeighbourTrain = new HashMap<Float, Integer>();
			 ShortestPathInCoAuthorGraph = new HashMap<Float, Integer>();
			 SecondShortestPathInCoAuthorGraph = new HashMap<Float, Integer>();
			 ShortestPathInCitationGraph = new HashMap<Float, Integer>();
			 SecondShortestPathInCitationGraph = new HashMap<Float, Integer>();
			 
			 commonNeighbour = (float)-1;
			 ShortestPath = (float)-1;		 
			 secondShortestPath = (float)-1;
			 ShortestPathCitation = (float)-1;
			 secondShortestPathCitation = (float)-1;
			 
			 
			File file = new File("Train1.csv");
			 if(!file.exists()){
				  file.createNewFile();
				}else{
					FileOutputStream writer = new FileOutputStream("Train1.csv");
		            writer.write(("").getBytes());
		            writer.close();
				}
			 Path p = Paths.get("Train1.csv"); 
			 String  s = "AuthorID1,AuthorID2,CommonNeighbour,ShortestPathAuthorGraph,SecondShortestPathAuthorGraph,ShortestPathCitationGraph,SecondShortestPathCitationGraph,Label";
			 Files.write(p, s.getBytes(), StandardOpenOption.APPEND);
			 
			 System.out.println("Started calculating Features for Authors in Train List......");
	         for(int i= 0;i < arrayTrain.length ;i++)
	         {
	        	 String[] arrayOfTrainData = arrayTrain[i].replace("\"", "").replace("[", "").replace("]", "").split(",");
	        	 parameters = new HashMap<String, Object>();
	    		 parameters.put("authorId1", arrayOfTrainData[0].toString());
	    		 parameters.put("authorId2", arrayOfTrainData[1].toString());
	    	       	

	        	 //Feature Calculation.....................................................................................
	        	//CommonNeighbour
	    		 List<Record> commonNighboursList = session.run("MATCH (start:author{AuthorID:{authorId1}})<-[rel1:AUTHORED_BY]-(m:train)-[rel2:AUTHORED_BY]->"
	    		 		+ "(p:train)<-[rel3:AUTHORED_BY]-(q:train)-[rel4:AUTHORED_BY]->(end:author{AuthorID:{authorId2}}) WHERE p<>end AND p<>start  "
	    		 		+ "RETURN count(DISTINCT p) as score",parameters).list();
	        		 if(commonNighboursList.size() > 0)
			         {
	        			 	//System.out.println("commonNighboursList"+ " " + commonNighboursList.get(0).get("score").toString().replace("\"", "").toString());
	        			     commonNeighbour = Float.parseFloat(commonNighboursList.get(0).get("score").toString().replace("\"", "").toString());
		        			 if(commonNeighbourTrain.containsKey(commonNeighbour))
		    				 {
		        				 commonNeighbourTrain.put(commonNeighbour,commonNeighbourTrain.get(commonNeighbour)+1);
		    				 }
		        			 else
		        			 {
		        				 commonNeighbourTrain.put(commonNeighbour,1);
		        			 }
		    				 
			         }
	        	
	        		 //ShortestPathInCoAuthorGraph............................................................................
	        		 //System.out.println("Authors..."+ " " + arrayOfTrainData[0].toString() + "  " +arrayOfTrainData[1].toString());
	        		 //if(Integer.parseInt(arrayOfTrainData[0].toString()) != 7  && Integer.parseInt(arrayOfTrainData[1].toString()) != 8)
	        		 //{
	        		    List<Record> shortestPathList = session.run("MATCH (start:author{AuthorID:{authorId1}}),"
	        				+ "(end:author{AuthorID:{authorId2}}) \r\n" + 
	        				 "CALL algo.shortestPath(start, end, 'cost',{write:false,nodeQuery:'train',relationshipQuery:'AUTHORED_BY'}) "
	        				+ "YIELD totalCost RETURN totalCost" + 
	        				"",parameters).list();
				        		 
				        		 if(shortestPathList.size() > 0)
						         {
				        			 //	System.out.println("ShortestPathInCoAuthorGraph"+ " " + shortestPathList.get(0).get("totalCost").toString().replace("\"", "").toString());
				        			 ShortestPath = Float.parseFloat(shortestPathList.get(0).get("totalCost").toString().replace("\"", "").toString());
				        			 if(ShortestPathInCoAuthorGraph.containsKey(ShortestPath))
				    				 {
				        				 ShortestPathInCoAuthorGraph.put(ShortestPath,ShortestPathInCoAuthorGraph.get(ShortestPath)+1);
				    				 }
				        			 else
				        			 {
				        				 ShortestPathInCoAuthorGraph.put(ShortestPath,1);
				        			 }
				    				 
						         }
	        		/* }
	        		 else
	        		 {
	        			 ShortestPath = (float)-1;
	        			 if(ShortestPathInCoAuthorGraph.containsKey(ShortestPath))
	    				 {
	        				 ShortestPathInCoAuthorGraph.put(ShortestPath,ShortestPathInCoAuthorGraph.get(ShortestPath)+1);
	    				 }
	        			 else
	        			 {
	        				 ShortestPathInCoAuthorGraph.put(ShortestPath,1);
	        			 }
	    				 
	        		 }*/
	        		 //SecondDegreeShortestPathInCoAuthorGraph.............................................................
	        		// if(Integer.parseInt(arrayOfTrainData[0].toString()) != 7  && Integer.parseInt(arrayOfTrainData[1].toString()) != 8)
	        		// {
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
		        			 	//System.out.println("SecondDegreeShortestPathInCoAuthorGraph"+ " " + SecondDegreeShortestPathInCoAurthorList.get(0).get("totalCost").toString().replace("\"", "").toString());
		        			 	secondShortestPath = Float.parseFloat(SecondDegreeShortestPathInCoAurthorList.get(0).get("totalCost").toString().replace("\"", "").toString());
		        			 	if(SecondShortestPathInCoAuthorGraph.containsKey(secondShortestPath))
			    				 {
			        				 SecondShortestPathInCoAuthorGraph.put(secondShortestPath,SecondShortestPathInCoAuthorGraph.get(secondShortestPath) + 1);
			    				 }
			        			 else
			        			 {
			        				 SecondShortestPathInCoAuthorGraph.put(secondShortestPath,1);
			        			 }
				         
		        		 }
	        		/* }
	        		 else
	        		 {
	        				secondShortestPath = (float)-1;
	        			 	if(SecondShortestPathInCoAuthorGraph.containsKey(secondShortestPath))
		    				 {
		        				 SecondShortestPathInCoAuthorGraph.put(secondShortestPath,SecondShortestPathInCoAuthorGraph.get(secondShortestPath) + 1);
		    				 }
		        			 else
		        			 {
		        				 SecondShortestPathInCoAuthorGraph.put(secondShortestPath,1);
		        			 }
	        		 }*/
	        		 
	        		 //ShortestPathInCitationGraph........................................................................................
	        		// if(Integer.parseInt(arrayOfTrainData[0].toString()) != 7  && Integer.parseInt(arrayOfTrainData[1].toString()) != 8)
	        		// {
		        		 List<Record> shortestPathCitationList = session.run("MATCH (start:author{AuthorID:{authorId1}}),"
			        				+ "(end:author{AuthorID:{authorId2}}) \r\n" + 
			        				 "CALL algo.shortestPath(start, end, 'cost',{write:false,nodeQuery:'train'}) "
			        				+ "YIELD totalCost RETURN totalCost" + 
			        				"",parameters).list();
			        		 if(shortestPathCitationList.size() > 0)
					         {
			        			 //System.out.println("shortestPathCitationList"+ " " + shortestPathCitationList.get(0).get("totalCost").toString().replace("\"", "").toString());
			        			 ShortestPathCitation = Float.parseFloat(shortestPathCitationList.get(0).get("totalCost").toString().replace("\"", "").toString());
			        			 if(ShortestPathInCitationGraph.containsKey(ShortestPathCitation))
			    				 {
			        				 ShortestPathInCitationGraph.put(ShortestPathCitation,ShortestPathInCitationGraph.get(ShortestPathCitation)+1);
			    				 }
			        			 else
			        			 {
			        				 ShortestPathInCitationGraph.put(ShortestPathCitation,1);
			        			 }
			    				 
					         }
	        		/* }
	        		 else
	        		 {
	        			 ShortestPathCitation = (float)-1;
	        			 if(ShortestPathInCitationGraph.containsKey(ShortestPathCitation))
	    				 {
	        				 ShortestPathInCitationGraph.put(ShortestPathCitation,ShortestPathInCitationGraph.get(ShortestPathCitation)+1);
	    				 }
	        			 else
	        			 {
	        				 ShortestPathInCitationGraph.put(ShortestPathCitation,1);
	        			 }
	        		 }*/
		        		 
		        		 
		            //SecondShortestpathInCitationGraph		
	        		// if(Integer.parseInt(arrayOfTrainData[0].toString()) != 7  && Integer.parseInt(arrayOfTrainData[1].toString()) != 8)
	        		// {
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
			        			 	//System.out.println("SecondDegreeShortestPathInCitationList"+ " " + SecondDegreeShortestPathInCitationList.get(0).get("totalCost").toString().replace("\"", "").toString());
			        			 	secondShortestPathCitation = Float.parseFloat(SecondDegreeShortestPathInCitationList.get(0).get("totalCost").toString().replace("\"", "").toString());
			        			 	if(SecondShortestPathInCitationGraph.containsKey(secondShortestPathCitation))
				    				 {
			        			 		SecondShortestPathInCitationGraph.put(secondShortestPathCitation,SecondShortestPathInCitationGraph.get(secondShortestPathCitation) + 1);
				    				 }
				        			 else
				        			 {
				        				 SecondShortestPathInCitationGraph.put(secondShortestPathCitation,1);
				        			 }
					         }	
	        		/* }
	        		 else
	        		 {
	        			 secondShortestPathCitation = (float)-1;
	        			 	if(SecondShortestPathInCitationGraph.containsKey(secondShortestPathCitation))
		    				 {
	        			 		SecondShortestPathInCitationGraph.put(secondShortestPathCitation,SecondShortestPathInCitationGraph.get(secondShortestPathCitation) + 1);
		    				 }
		        			 else
		        			 {
		        				 SecondShortestPathInCitationGraph.put(secondShortestPathCitation,1);
		        			 }
	        		 }*/
			        		 
			        		 //Update The File Train.csv
			        		 s = System.lineSeparator() + arrayOfTrainData[0]+ "," + arrayOfTrainData[1] + "," + commonNeighbour.toString()+ "," + ShortestPath.toString() + "," + secondShortestPath.toString()+ "," + ShortestPathCitation.toString()+ "," + secondShortestPathCitation.toString()+ "," + arrayOfTrainData[2] ;
			        		 Files.write(p, s.getBytes(), StandardOpenOption.APPEND);
	         }
	         
	   
	         System.out.println("Finished calculating Features for Authors in Train List......");
	         file = new File("trainCommonNeighbour1.csv");
			 if(!file.exists()){
				  file.createNewFile();
				}else{
					FileOutputStream writer = new FileOutputStream("trainCommonNeighbour1.csv");
		            writer.write(("").getBytes());
		            writer.close();
				}
			 
			 file = new File("trainShortestPathCoAuthorGraph1.csv");
			 if(!file.exists()){
				  file.createNewFile();
				}else{
					FileOutputStream writer = new FileOutputStream("trainShortestPathCoAuthorGraph1.csv");
		            writer.write(("").getBytes());
		            writer.close();
				}
			 
			 file = new File("trainSecondShortestPathCoAuthorGraph1.csv");
			 if(!file.exists()){
				  file.createNewFile();
				}else{
					FileOutputStream writer = new FileOutputStream("trainSecondShortestPathCoAuthorGraph1.csv");
		            writer.write(("").getBytes());
		            writer.close();
				}
			 
			 file = new File("trainShortestPathCoCitationGraph1.csv");
			 if(!file.exists()){
				  file.createNewFile();
				}else{
					FileOutputStream writer = new FileOutputStream("trainShortestPathCoCitationGraph1.csv");
		            writer.write(("").getBytes());
		            writer.close();
				}
			 
			 file = new File("trainSecondShortestPathCitationGraph1.csv");
			 if(!file.exists()){
				  file.createNewFile();
				}else{
					FileOutputStream writer = new FileOutputStream("trainSecondShortestPathCitationGraph1.csv");
		            writer.write(("").getBytes());
		            writer.close();
				}
			 
			 
			//WriteToFile testCommonNeighbour HashMap
	         Path trainCommonNeighbour = Paths.get("trainCommonNeighbour1.csv"); 
	         for(Map.Entry<Float,Integer> m :commonNeighbourTrain.entrySet()){
	            	 String content = m.getKey()+","+m.getValue()+ System.lineSeparator();
	            	 Files.write(trainCommonNeighbour, content.getBytes(), StandardOpenOption.APPEND);
	             }
	             
	         //WriteToFile ShortestPathInCoAuthorGraph HashMap
	         Path trainShortestPathCoAuthorGraph = Paths.get("trainShortestPathCoAuthorGraph1.csv");
             for(Map.Entry<Float,Integer> m :ShortestPathInCoAuthorGraph.entrySet()){
            	 String content = m.getKey()+","+m.getValue() + System.lineSeparator();
            	 Files.write(trainShortestPathCoAuthorGraph, content.getBytes(), StandardOpenOption.APPEND);
             }
	             
	           //WriteToFile SecondShortestPathInCoAuthorGraph HashMap
             Path trainSecondShortestPathCoAuthorGraph = Paths.get("trainSecondShortestPathCoAuthorGraph1.csv");
             for(Map.Entry<Float,Integer> m :SecondShortestPathInCoAuthorGraph.entrySet()){
            	 String content = m.getKey()+","+m.getValue() + System.lineSeparator();
            	 Files.write(trainSecondShortestPathCoAuthorGraph, content.getBytes(), StandardOpenOption.APPEND);
             }
		             
		             
	           //WriteToFile ShortestPathInCitationGraph HashMap
             Path trainShortestPathCoCitationGraph = Paths.get("trainShortestPathCoCitationGraph1.csv");		     
             for(Map.Entry<Float,Integer> m :ShortestPathInCitationGraph.entrySet()){
            	 String content = m.getKey()+","+m.getValue() + System.lineSeparator();
            	 Files.write(trainShortestPathCoCitationGraph, content.getBytes(), StandardOpenOption.APPEND);
             }    
		             
		             
	             //WriteToFile ShortestPathInCitationGraph HashMap
             Path trainSecondShortestPathCitationGraph = Paths.get("trainSecondShortestPathCitationGraph1.csv");
             for(Map.Entry<Float,Integer> m :SecondShortestPathInCitationGraph.entrySet()){
            	 String content = m.getKey()+","+m.getValue() + System.lineSeparator();
            	 Files.write(trainSecondShortestPathCitationGraph, content.getBytes(), StandardOpenOption.APPEND);
             }        
             
             
     		 session.close();
	         
			 }
		         
		         
			 driver.close();
	
	}
//------------------------------------------------------------------------------------------------------------------------------------------------------------	
	

}
