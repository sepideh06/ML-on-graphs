package example;
import java.io.*;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bouncycastle.util.Arrays.Iterator;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;
import org.neo4j.driver.v1.Values;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.procedure.PerformsWrites;
import org.neo4j.procedure.UserFunction;

import static org.neo4j.driver.v1.Values.parameters;
public class javaModelTrain {
    
	 @UserFunction
	 @PerformsWrites
	 public String modelTrain(Integer idTrain, Integer idTest) {
		 Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "123"));
		 String returnStatement="";
		 try (Session session = driver.session()) {
		     StatementResult rsTrain = session.run("MATCH (a:trainingData) WHERE a.TrainID ={idTrain} RETURN a",Values.parameters( "idTrain", idTrain));
		     StatementResult rsTest = session.run("MATCH (a:testData) WHERE a.TestID ={idTest} RETURN a",Values.parameters( "idTest", idTest));
		     if (!rsTrain.hasNext()||!rsTest.hasNext()){
		    	 StatementResult exists = session.run("MATCH (a:page)-[r:LINKS_TO]-(b:page) RETURN distinct a.PageID,b.PageID");
		    	 List<String> existentThings = new ArrayList<>();
		    	 Record s = exists.peek();
		    	 String insertion= s.get("a.PageId")+","+s.get("b.PageId")+",exists";
		    	 //existentThings.forEach(it->{
		    	 //	 System.out.println(it);
		    	 //});
		    	 session.close();
		    	 Session session2 = driver.session();
		    	 long nodeId = session2.run( "CREATE (p:trainingData {TrainID:{trainID}, Data:{data}}) RETURN id(p)", Values.parameters("trainID", idTrain, "data", existentThings))
		                    .single()
		                    .get( 0 ).asLong();
		    	 long nodeI2 = session2.run( "CREATE (p:testingData {TestID:{testID}}) RETURN id(p)", Values.parameters("testID", idTest))
		                    .single()
		                    .get( 0 ).asLong();
		    	 
		    	 /* First we store a list of strings. This will be comma separated, with an exists... This is the list that we split randomly into train and test. 
		    	  * We also pass it to a set.
		    	  * Next, we need to generate the nonExists.
		    	  * For this we do a single query through all the communities, and for each community we get the nodes and then do the cartesian product inside and for each we check if in set.
		    	  * 
		    	  * Finally, we take the lists that we have produced and we
		    	  * */
		    	 
		    	 //we create both...
		    	    //We connect to Neo4j and get exists and non exists
		    	 returnStatement="We created some things";
		    	 session2.close();
	         }
		     else{
		    	 returnStatement="We did not create some things";
		    	 session.close();
		     }
		 } 
		 driver.close();
		 return returnStatement; 
		 
	   }
//-------------------------------------------------------------------------------------------------------------------------------------	 
	 
	 @UserFunction
	 @PerformsWrites
	 public String[] splitData(float cutoff, String tag) {
		 String[] results = new String[2];
		 //This should return the id of train node, the id of test node
		 Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "123"));
		 List<String> train = new ArrayList<>();
		 List<String> test = new ArrayList<>();
		 ArrayList<Integer> pageIDs = new ArrayList<>();
		 List<Pair<Integer, Integer>> linkedPageIDs = new ArrayList<Pair<Integer, Integer>>();
		 Integer skip=0;
		
		 /*
		 Random random = new Random();
		 try (Session session = driver.session()) {
			 Integer numEdges = Integer.parseInt(session.run("MATCH (a:page)-[r:LINKS_TO]->() RETURN COUNT(r)").next().get("COUNT(r)").toString());
			 System.out.println(numEdges);
			 while (skip<numEdges){
			 StatementResult listOfIds = session.run("MATCH (a:page)-[r:LINKS_TO]->(b:page) RETURN distinct a.PageID,b.PageID SKIP {skip} LIMIT 1000; ", Values.parameters("skip",skip));
			 listOfIds.stream().forEach(cand->{
				 
				 
				 if(pageIDs.contains(cand.get("a.PageID"))== false)
					 pageIDs.add(Integer.parseInt(cand.get("a.PageID").toString()));
				 
				 if(pageIDs.contains(cand.get("b.PageID"))== false)
					 pageIDs.add(Integer.parseInt(cand.get("b.PageID").toString()));
					 
				 linkedPageIDs.add(new Pair<Integer, Integer>(Integer.parseInt(cand.get("a.PageID").toString()), Integer.parseInt(cand.get("b.PageID").toString()))); 
					 
				 String pairToInsert=cand.get("a.PageID").toString();
				 
				 pairToInsert+=","+cand.get("b.PageID").toString()+","+"exist";
				 
				 if (random.nextFloat()<=cutoff){
					 train.add(pairToInsert);
				 }
				 else{
					 test.add(pairToInsert);
				 }
				 });
			 
			 System.out.println(skip);
			 //System.out.println("Enter");
			 skip+=1000;
			 }
			 session.close();
		 }
		 driver.close();
		 
		 //Create Cartesian Product
		 List<Pair<Integer, Integer>> cartesianProductList = new ArrayList<Pair<Integer, Integer>>();
		 cartesianProductList = cartesian(pageIDs);
		 
		 
		//CommunityID & list of pages assigned to it
		 Map<String, List<String>> dicPageCommunity = new HashMap<String, List<String>>();
         
         dicPageCommunity = GetCommunityIds(pageIDs); 
      
         //System.out.println("dicPageCommunity..."+dicPageCommunity);
      
      
		 //Non-Exist links
         boolean sameCommunityflag=false;

		 List<Pair<Integer, Integer>> lstNonExistPagelinks = new ArrayList<Pair<Integer, Integer>>();
		 
		 for (Pair<Integer, Integer> pairId : cartesianProductList)
		{
			 sameCommunityflag = ComparingPageCommunityId(dicPageCommunity, Integer.parseInt(pairId.getValue(0).toString()), Integer.parseInt(pairId.getValue(1).toString()));
	            if (pairId.getValue(0) != pairId.getValue(1) && linkedPageIDs.contains(pairId) == false && sameCommunityflag == true)
	            	lstNonExistPagelinks.add(new Pair<Integer, Integer>(Integer.parseInt(pairId.getValue(0).toString()), Integer.parseInt(pairId.getValue(1).toString())));
		}
	            
		 //System.out.println("Non-Exist List..."+lstNonExistPagelinks);
		 //System.out.println("Non-Exist List..."+lstNonExistPagelinks.size());
		//System.out.println("Cartesian Product List..."+cartesianProductList);
		 
		 
		 //Split non-exist links
				Integer trainSize = (int)(lstNonExistPagelinks.size() * cutoff);
				List<Pair<Integer, Integer>> trainNonExist = new ArrayList<Pair<Integer, Integer>>();
				List<Pair<Integer, Integer>> testNonExist = new ArrayList<Pair<Integer, Integer>>();
				testNonExist = lstNonExistPagelinks;
				while(trainNonExist.size() < trainSize) {
					Integer index = random.nextInt(testNonExist.size());
					trainNonExist.add(testNonExist.get(index));
					testNonExist.remove(testNonExist.get(index));
				}
				//System.out.println("Non-Exist train List..."+ trainNonExist.size());
				//System.out.println("Non-Exist test List..."+ testNonExist.size());
				
				
				
				
		//Combine non-exist & exist into trainSet & test Set
				 for (Pair<Integer, Integer> pairId : trainNonExist)
					{
				          train.add(pairId.getValue(0).toString()+","+pairId.getValue(1).toString()+","+"nonexist");
					}
				 for (Pair<Integer, Integer> pairId : trainNonExist)
					{
				          test.add(pairId.getValue(0).toString()+","+pairId.getValue(1).toString()+","+"nonexist");
					}
				         
				 //System.out.println("Train..." + train);
				 //System.out.println("Test..." + test);
			*/	 
				 
		//Now we create nodes for train and test
		 for(int i=0;i<10000000;i++)
		 {
			 train.add(Integer.toString(i) + "," + "exist");
			 test.add(Integer.toString(i) + "," + "exist");
		 }
		 
				 CreateNodesTrainTest(tag,train,test);
		 return results;
		 
	   }
//-------------------------------------------------------------------------------------------------------
public void CreateNodesTrainTest(String tag,List<String> train,List<String> test)
{
	//Now we create train and test
	 String[] results = new String[2];
	 //This should return the id of train node, the id of test node
	 Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "123"));
	 Integer skip=0;
	 
	 try (Session session = driver.session()) {
		 session.run("CREATE (a:Train { tag: {tag}, data:{data} })", Values.parameters("tag",tag, "data", train.subList(0, 1000000)));
		 session.run("CREATE (a:Test { tag: {tag}, data:{data}})", Values.parameters("tag",tag, "data", test.subList(0, 1000000)));
		 skip = 1000000;
		 while (skip<train.size()){
		 session.run("MATCH (a:Train { tag: {tag}) SET n.data=n.data+{data}", Values.parameters("tag",tag, "data", train.subList(skip, skip+1000000)));
		 }
		 skip = 1000000;
		 while (skip<test.size()){
		 session.run("MATCH (a:Test { tag: {tag}) SET n.data=n.data+{data}", Values.parameters("tag",tag, "data", test.subList(skip, skip+1000000)));
		 
		 //skip+=1000;
		 }
		 session.close();
	 }
	 driver.close();

}

//-----------------------------------------------------------------------------------------------------
	 
public Map<String, List<String>> GetCommunityIds(ArrayList<Integer> PageIDs)
{
	Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "123"));
	Integer skip=0;
	Map<String, List<String>> dicPageCommunity = new HashMap<String, List<String>>();
	 
	try (Session session = driver.session()) {
		 Integer numEdges = Integer.parseInt(session.run("MATCH (a:page)-[r:LINKS_TO]->() RETURN COUNT(r)").next().get("COUNT(r)").toString());
		 System.out.println(numEdges);
		 while (skip<numEdges){
		 StatementResult listOfIds = session.run("MATCH (a:page) RETURN a.PageID,a.CommunityID SKIP {skip} LIMIT 1000; ", Values.parameters("skip",skip));
		 listOfIds.stream().forEach(cand->{
			 
		            if (PageIDs.contains(Integer.parseInt(cand.get("a.PageID").toString())))
		            		{
				                List<String> pageIdList = new ArrayList<String>();
				                if (dicPageCommunity.get(cand.get("a.CommunityID").toString()) == null)
				                {
				                    pageIdList.add(cand.get("a.PageID").toString());
				                }
				                else
				                {
				                    pageIdList = dicPageCommunity.get(cand.get("a.CommunityID").toString());
				                    pageIdList.add(cand.get("a.PageID").toString());				                    		
				                }
				                dicPageCommunity.put(cand.get("a.CommunityID").toString(),pageIdList);
		            		}
		                    		
			 
		 });
		 
		 skip+=1000;
		 }
		 session.close();
	 }
	 driver.close();
	 
	 return dicPageCommunity;
	}


//-----------------------------------------------------------------------------------------------------
public List<Pair<Integer, Integer>> cartesian(ArrayList<Integer> array1) {

    List<Pair<Integer, Integer>> partnerPlatformPairList = new ArrayList<Pair<Integer, Integer>>();

    for (int i = 0; i < array1.size(); i++)
        for (int j = 0; j < array1.size(); j++)
        	if(partnerPlatformPairList.contains(new Pair(array1.get(i), array1.get(j))) == false)
               partnerPlatformPairList.add(new Pair<Integer, Integer>(array1.get(i), array1.get(j)));
    
    return partnerPlatformPairList;

}

//------------------------------------------------------------------------------------------------------

public boolean ComparingPageCommunityId(Map<String,List<String>> pageCommunityDictionary,Integer pageId1, Integer pageId2)
{

    String communityId1 = "";
    String communityId2 = "";
    		for (Map.Entry<String,List<String>> entry : pageCommunityDictionary.entrySet())  
    		{
				        if(entry.getValue().contains(pageId1.toString()))
				            communityId1 = entry.getKey();
				        if(entry.getValue().contains(pageId2.toString()))
				        	communityId2 = entry.getKey();
    		}
    		
    		if(communityId1 == communityId2)
                return true;
		    else
		    	return false;
}
		
}
//-------------------------------------------------------------------------------------------------------------


