package example;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.neo4j.procedure.UserFunction;

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

public class javaModelTrain {
    
    @Context
    public GraphDatabaseService db;

	@Procedure(value = "example.splitData", mode=Mode.WRITE)
        @Description("it splits data into train and test")
	 public void splitData(@Name("cutoff") double cutoff2, @Name("tag") String tag) {
                float cutoff = (float)cutoff2;
		 //This should return the id of train node, the id of test node
		 Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "123"));
		 List<String> train = new ArrayList<>();
		 List<String> test = new ArrayList<>();
		 ArrayList<Integer> pageIDs = new ArrayList<>();
		 List<Pair<Integer, Integer>> linkedPageIDs = new ArrayList<Pair<Integer, Integer>>();
		 Integer skip=0;
		
		 
		 Random random = new Random();
		 try (Session session = driver.session()) {
			 //Integer numEdges = Integer.parseInt(session.run("MATCH (a:page)-[r:LINKS_TO]->() RETURN COUNT(r)").next().get("COUNT(r)").toString());
			 //System.out.println("num" + " " + numEdges.toString());
			 //while (skip<numEdges){
			 //StatementResult listOfIds = session.run("MATCH (a:page)-[r:LINKS_TO]->(b:page) RETURN distinct a.PageID,b.PageID SKIP {skip} LIMIT 1000; ", Values.parameters("skip",skip));
			 StatementResult listOfIds = session.run("MATCH (a:page)-[r:LINKS_TO]->(b:page) RETURN distinct a.PageID,b.PageID;");
			 listOfIds.stream().forEach(cand->{
				 
				 
				 if(pageIDs.contains(cand.get("a.PageID"))== false)
					 pageIDs.add(Integer.parseInt(cand.get("a.PageID").toString().replace("\"","")));
				 
				 if(pageIDs.contains(cand.get("b.PageID"))== false)
					 pageIDs.add(Integer.parseInt(cand.get("b.PageID").toString().replace("\"","")));
					 
				 linkedPageIDs.add(new Pair<Integer, Integer>(Integer.parseInt(cand.get("a.PageID").toString().replace("\"","")), Integer.parseInt(cand.get("b.PageID").toString().replace("\"","")))); 
					 
				 String pairToInsert=cand.get("a.PageID").toString().replace("\"","");
				 
				 pairToInsert+=","+cand.get("b.PageID").toString().replace("\"","")+","+"exist";
				 
				 if (random.nextFloat()<=cutoff){
					 train.add(pairToInsert);
				 }
				 else{
					 test.add(pairToInsert);
				 }
				 });
			 
			 //System.out.println(skip);
			 //System.out.println("Enter");
			 //skip+=1000;
			 //}
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
			 sameCommunityflag = ComparingPageCommunityId(dicPageCommunity, Integer.parseInt(pairId.getValue(0).toString().replace("\"","")), Integer.parseInt(pairId.getValue(1).toString().replace("\"","")));
	            if (pairId.getValue(0) != pairId.getValue(1) && linkedPageIDs.contains(pairId) == false && sameCommunityflag == true)
	            	lstNonExistPagelinks.add(new Pair<Integer, Integer>(Integer.parseInt(pairId.getValue(0).toString().replace("\"","")), Integer.parseInt(pairId.getValue(1).toString().replace("\"",""))));
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
				 for (Pair<Integer, Integer> pairId : testNonExist)
					{
				          test.add(pairId.getValue(0).toString()+","+pairId.getValue(1).toString()+","+"nonexist");
					}
				         
				 //System.out.println("Train..." + train);
				 //System.out.println("Test..." + test);
				 
				 
		//Now we create nodes for train and test
				 CreateNodesTrainTest(tag,train,test);		
	   }
	   
//-------------------------------------------------------------------------------------------------------
    
public void CreateNodesTrainTest(String tag,List<String> train,List<String> test)
{
	//Now we create train and test
	 String[] results = new String[2];
	 //This should return the id of train node, the id of test node
	 Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "123"));
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

//-----------------------------------------------------------------------------------------------------

public Map<String, List<String>> GetCommunityIds(ArrayList<Integer> PageIDs)
{
	Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "123"));
	Integer skip=0;
	Map<String, List<String>> dicPageCommunity = new HashMap<String, List<String>>();
	 
	try (Session session = driver.session()) {
		 Integer numEdges = Integer.parseInt(session.run("MATCH (a:page)-[r:LINKS_TO]->() RETURN COUNT(r)").next().get("COUNT(r)").toString().replace("\"",""));
		 System.out.println(numEdges);
		 while (skip<numEdges){
		 StatementResult listOfIds = session.run("MATCH (a:page) RETURN a.PageID,a.CommunityID SKIP {skip} LIMIT 1000; ", Values.parameters("skip",skip));
		 listOfIds.stream().forEach(cand->{
			 
		            if (PageIDs.contains(Integer.parseInt(cand.get("a.PageID").toString().replace("\"",""))))
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
//-------------------------------------------------------------------------------------------------------------------------------------------------------------
 
	@Procedure(value = "example.CreateSecondModel", mode=Mode.WRITE)
        @Description("create model")
	public void CreateSecondModel(@Name("tag") String tag) throws XGBoostError, IOException 
	{
		Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "123"));
		
		String[] arrayTest = new String[]{};
		String[] arrayTrain = new String[]{};
		List<String> lstTrain = new ArrayList<String>();
		List<String> lstTest = new ArrayList<String>();               
		String testtraniTag = "";
		 try (Session session = driver.session()) {
			 
			 List<Record>  testDataTag = session.run("MATCH (a:Test) RETURN  a.tag as tag").list();
			 testtraniTag = testDataTag.get(0).get("tag").toString();
			 
			 List<Record> listOfTestData = session.run("MATCH (a:Test) RETURN  a.data AS data").list();
			 List<Record> listOfTrainData = session.run("MATCH (a:Train) RETURN  a.data AS data").list();

			 arrayTest = listOfTestData.get(0).get("data").toString().split(" ");
			 arrayTrain = listOfTrainData.get(0).get("data").toString().split(" ");
			 
				
			float[] rowHeadersTest = new float[603*arrayTest.length];
		        float[] dataTest = new float[603*arrayTest.length];
		        int[] colIndexTest = new int[603*arrayTest.length];
		        float[] labelsTest = new float[arrayTest.length];  
		        
		        float[] rowHeadersTrain = new float[603*arrayTrain.length];
		        float[] dataTrain = new float[603*arrayTrain.length];
		        int[] colIndexTrain = new int[603*arrayTrain.length];
		        float[] labelsTrain = new float[arrayTrain.length];
		        
			 String label = "";
			 //rowHeadersTest[0]=0;
			 int h = 0;
	         for(int i= 0;i < arrayTest.length ;i++)
	         {

	        	 String[] str = arrayTest[i].replace("\"", "").replace("[", "").replace("]", "").split(",");
	        	 List<Record> embeddingPageID1 = session.run("MATCH (n:page) WHERE n.PageID = {pageID} RETURN n.Embedding AS Embedding",Values.parameters("pageID",str[0])).list();
	        	 List<Record> embeddingPageID2 = session.run("MATCH (n:page) WHERE n.PageID = {pageID} RETURN n.Embedding AS Embedding",Values.parameters("pageID",str[1])).list();

	        	 String[] embedArray1 = new String[] {};
                         String[] embedArray2 = new String[] {};
		         if(embeddingPageID1.size() > 0)
		         {
		             embedArray1 = embeddingPageID1.get(0).get("Embedding").toString().replace("\"", "").split(",");
		         }
		         if(embeddingPageID2.size() > 0)
		         {
		             embedArray2 = embeddingPageID2.get(0).get("Embedding").toString().replace("\"", "").split(",");
		         }


	        	 if(str[2].replace(" ", "").contains("nonexist"))
	        	 {
	        	     label = "0";
	        	     labelsTest[i] = 0;
	        	 }
	        	 else
	        	 {
	        		 label = "1";
	        		 labelsTest[i] = 1;
	        	 }
	        	 
	        	
	        	 
	        	 String[] embeddingArray1 = Arrays.copyOf(embedArray1, 300);
	        	 String[] embeddingArray2 = Arrays.copyOf(embedArray2, 300);
	        	 for(int k= embedArray1.length-1;k<300;k++)
	        	 {
	        		 embeddingArray1[k]="1";
	        	 }
	        	 for(int k= embedArray2.length-1;k<300;k++)
	        	 {
	        		 embeddingArray2[k]="1";
	        	 }
	        	
	        	 if(h <= (603*arrayTest.length)-603)
	        	 {
		        	 dataTest[h] = Float.parseFloat(label);
		        	 h = h + 1;
		        	 dataTest[h] = Float.parseFloat(str[0]);
		        	 h = h + 1;
		        	 dataTest[h] = Float.parseFloat(str[1]);
		        	 h = h + 1;
		        	 for(int k= 0;k<300;k++)
		        	 {
					if(h+k <= (603*arrayTest.length)-600)
					{
		        		 	dataTest[h+k]=Float.parseFloat(embeddingArray1[k]);
					}
		        	 }
		        	 h = h + 301;
		        	 //System.out.println("h..." + h);
		        	 for(int k= 0;k<300;k++)
		        	 {
		        		 if(h+k <= (603*arrayTest.length)-300)
		        		 {
		        		    dataTest[h+k]=Float.parseFloat(embeddingArray2[k]);
		        		 }
		        	 }
		        	 h = h + 301;
		        	 
		        	 
		        	 //rowHeadersTest[i+1] = rowHeadersTest[i]+603;
	        	 }
	         }
	       
	         
	         //Traindata
	         label = "";
			 //rowHeadersTrain[0]=0;
			 h = 0;
	         for(int i= 0;i < arrayTrain.length ;i++)
	         {
	        	 String[] str = arrayTrain[i].replace("\"", "").replace("[", "").replace("]", "").split(",");
	        	 List<Record> embeddingPageID1 = session.run("MATCH (n:page) WHERE n.PageID = {pageID} RETURN n.Embedding AS Embedding",Values.parameters("pageID",str[0])).list();
	        	 List<Record> embeddingPageID2 = session.run("MATCH (n:page) WHERE n.PageID = {pageID} RETURN n.Embedding AS Embedding",Values.parameters("pageID",str[1])).list();
	        	 
			 String[] embedArray1 = new String[] {};
                         String[] embedArray2 = new String[] {};
		         if(embeddingPageID1.size() > 0)
		         {
		             embedArray1 = embeddingPageID1.get(0).get("Embedding").toString().replace("\"", "").split(",");
		         }
		         if(embeddingPageID2.size() > 0)
		         {
		             embedArray2 = embeddingPageID2.get(0).get("Embedding").toString().replace("\"", "").split(",");
		         }

	        	 
	        	 if(str[2].replace(" ", "").contains("nonexist"))
	        	 {
	        		 label = "0";
	        		 labelsTrain[i] = 0;
	        	 }
	        	 else
	        	 {
	        		 label = "1";
	        		 labelsTrain[i] = 1;
	        	 }
	        	  
	        	 String[] embeddingArray1 = Arrays.copyOf(embedArray1, 300);
	        	 String[] embeddingArray2 = Arrays.copyOf(embedArray2, 300);
	        	 for(int k= embedArray1.length-1;k<300;k++)
	        	 {
	        		 embeddingArray1[k]="1";
	        	 }
	        	 for(int k= embedArray2.length-1;k<300;k++)
	        	 {
	        		 embeddingArray2[k]="1";
	        	 }
	        	 
	        	 if(h <= (603*arrayTrain.length)-603)
	        	 {
		        	 dataTrain[h] = Float.parseFloat(label);
		        	 h = h + 1;
		        	 dataTrain[h] = Float.parseFloat(str[0]);
		        	 h = h + 1;
		        	 dataTrain[h] = Float.parseFloat(str[1]);
		        	 h = h + 1;
		        	 for(int k= 0;k<300;k++)
		        	 {	
					if(h+k <= (603*arrayTrain.length)-600)
					{
		        		  dataTrain[h+k]=Float.parseFloat(embeddingArray1[k]);
					}
		        	 }
		        	 h = h + 301;
		        	 //System.out.println("h..." + h);
		        	 for(int k= 0;k<300;k++)
		        	 {
		        		 if(h+k <= (603*arrayTrain.length)-300)
		        		 {
		        		    dataTrain[h+k]=Float.parseFloat(embeddingArray2[k]);
		        		 }
		        	 }
		        	 h = h + 301;
		        	 
		        	 
		        	// rowHeadersTrain[i+1] = rowHeadersTrain[i]+603;
	        	 }
	         }
	         
	            
	         int numColumn = 603;
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
	 				//booster.saveModel("model.bin");
	 				// dump with feature map
	 				//String[] model_dump_with_feature_map = booster.getModelDump("featureMap.txt", false);
	 			       //booster = XGBoost.loadModel("model.bin");
	 			    
	 				float[][] predicts = booster.predict(testMat);
	 				System.out.println("predicts..." + predicts[0][0] + " for class 1 and "+predicts[0][1]+"for class 0... The real label was:"+testMat.getLabel()[0]);
	 				System.out.println(exmp.checkPredicts(predicts, predicts));
	 				// predict leaf
	 				//float[][] leafPredicts = booster.predictLeaf(testMat, 0);
	 				
	 				float[] statisticInfo = confusionMatrix(testMat,predicts);
	 				System.out.println("accuracy:"+statisticInfo[0] + "...."+
	 						"precision:"+statisticInfo[1]+"....."+
	 						"errorRate:"+statisticInfo[2]+"...."+
	 						"recall:"+statisticInfo[3]+"....."+
	 						"FScore:"+statisticInfo[4]);
	 				SaveModelToNeo4j(booster,session,tag,statisticInfo,testtraniTag);
	 				
	         session.close();
	 		         
			 
			}
				 driver.close();
			} 
			
//-----------------------------------------------------------------------------------------------------------
    
	public float[] confusionMatrix(DMatrix testMat,float[][] predicts) throws XGBoostError
	{
		float[] statisticInfo= new float[5];
		float noNo = 0; float noYes = 0; float yesNo = 0; float yesYes = 0;
		float predictedValue = 2;
		for(int i = 0;i < predicts.length;i++)
		{
			if(predicts[i][0] > predicts[i][1])
				predictedValue = 1;
			else
				predictedValue = 0;
		    if(testMat.getLabel()[i] == 1 && predictedValue == 1)
		    	yesYes =+ 1;
		    else if(testMat.getLabel()[i] == 0 && predictedValue == 0)
		    	noNo =+ 1;
		    else if(testMat.getLabel()[i] == 0 && predictedValue == 1)
		    	noYes =+ 1;
		    else if(testMat.getLabel()[i] == 1 && predictedValue == 0)
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
//------------------------------------------------------------------------------------------------------------
	public static boolean checkPredicts(float[][] fPredicts, float[][] sPredicts) {
	    if (fPredicts.length != sPredicts.length) {
	      return false;
	    }

	    for (int i = 0; i < fPredicts.length; i++) {
	      if (!Arrays.equals(fPredicts[i], sPredicts[i])) {
	        return false;
	      }
	    }

	    return true;
	}

//-------------------------------------------------------------------------------------------------------------
public void SaveModelToNeo4j(Booster booster,Session session,String tag,float[] statisticInfo,String testtraniTag) throws IOException, XGBoostError
{
	String fileName = "model.bin";
	booster.saveModel(fileName);
	Path path = Paths.get(fileName);
	byte[] bytes = Files.readAllBytes(path);
	 if(bytes.length > 0)
	 {
		 session.run("CREATE (a:Model { tag: {tag}, modelData:{data}, accuracy:{accuracy}, precision:{precision}, errorRate:{errorRate}, recall:{recall}, Fscore:{Fscore}, testtrainModelTag:{testtrainModelTag}})",
				 Values.parameters("tag",tag, "data", bytes,"accuracy",statisticInfo[0],"precision",statisticInfo[1],"errorRate",statisticInfo[2],"recall",statisticInfo[3],"Fscore",statisticInfo[4],"testtrainModelTag",testtraniTag));
	 }
}	

//-------------------------------------------------------------------------------------------------------------
public static String result = "";
@Description("ModelInference")
@Procedure(value = "example.ModelInference")
public Stream<Output> ModelInference(@Name("modelTag") String modelTag,@Name("dataForInference") String dataForInference)
{
	 //String fileName = "E:\\neo4j-community-3.5.4-windows\\neo4j-community-3.5.4\\data\\databases\\graph.db";
	 //File file = new File(fileName);
	 //db = new GraphDatabaseFactory().newEmbeddedDatabase(file);
	 Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "123"));
	 Map<String, Object> params = new HashMap<>();
     params.put("modelTag", modelTag );
     String query = "MATCH (a:Model { tag: $modelTag } ) RETURN  a.modelData,a.accuracy,a.precision,a.errorRate,a.recall,a.Fscore";


		
		String[] arrayTest = new String[]{};
		List<String> lstTest = new ArrayList<String>();               
		
		 try (Session session = driver.session()) {
			 StatementResult modelData = session.run("MATCH (a:Model { tag: {modelTag} } ) RETURN  a.modelData,a.accuracy,a.precision,a.errorRate,a.recall,a.Fscore",Values.parameters("modelTag",modelTag));
			 modelData.stream().forEach(cand->{
				 
				 byte[] bytes = cand.get("a.modelData").asByteArray();				 
				 String FILEPATH = "model.bin";
				 File file = new File(FILEPATH);
				 try {
					OutputStream os = new FileOutputStream(file);
					os.write(bytes); 
					Booster booster = XGBoost.loadModel(FILEPATH);
					
					  int numColumn = 603;
					  String data = "1,0,32,0.087403275,-0.1401191,-0.37970066,0.21424966,-0.21106939,-0.071578234,0.29007334,-0.09687779,-0.083295956,-0.17524725,-0.049041435,0.20308562,-0.0633825,0.09500676,0.014067516,-0.15991588,0.029578676,-0.021118382,-0.095649354,0.36918557,-0.1400644,0.22167416,-0.0361364,-0.01855282,-0.23654626,-0.006939014,-0.04870526,0.060528364,0.19537103,0.21499686,-0.17977078,0.18759787,-0.29048625,0.082479015,0.31667626,-0.16755064,-0.09201702,0.008555263,-0.14196561,1.8244982E-4,0.012281895,0.052782405,0.01563395,0.028985396,-0.0073877894,-0.16300027,0.051467713,-0.18841101,0.030682148,0.025490664,-0.14368598,-0.13351351,-0.19150294,-0.22361302,0.1483035,-0.35647333,0.040899914,-0.3216449,-0.031828854,0.15434682,0.15936351,-0.15053545,0.081928976,0.06918708,-0.08036085,-0.22769625,0.13178773,0.14428039,0.17200421,6.377287E-4,0.03495024,-0.21943325,0.11627227,0.19229273,-0.026772106,0.09776288,0.19008188,0.27389735,-0.118230425,0.19077559,-0.13990386,-0.41353655,-0.1688183,-0.22912197,-0.26606923,-0.12624969,0.055988256,0.25784823,-0.18816416,-0.07186795,0.15597242,0.031450946,0.26177394,-0.1436014,-0.09810102,-0.096318685,0.15913218,0.24569987,0.02085275,0.4553627,0.13252716,0.34842458,-0.16264941,-0.25008884,0.32904068,0.09047052,-0.17742573,0.052140523,0.23115589,0.11962244,-0.063084796,-0.02267891,-0.1377259,-0.2535846,0.010072301,-0.07212299,-0.19796945,-0.079813346,-0.10298111,0.09481236,0.0071426914,-0.20330966,-0.11087199,0.055079103,-0.15144576,-0.040806532,-0.1974537,0.22337146,-0.11445954,0.30032602,-0.028555786,0.13154547,-7.7396753E-4,-0.052426595,0.19975871,-0.15348832,-0.26321527,0.10395167,-0.22734313,0.2184941,-0.020975614,0.26962826,0.20057207,-0.16136694,0.035221267,-0.122128166,0.051410805,3.7997123E-4,-0.040190488,0.040642917,0.15032496,-0.23899865,0.014533219,-0.19630438,0.009750552,0.121692926,0.27994493,0.03367935,0.13440134,-0.25655577,0.11253273,0.16038842,-0.07183555,-0.19502051,0.19113415,0.20074515,-0.03755195,-0.090271294,0.044890624,0.2275806,-0.054289345,-0.37455449,-0.33670565,0.007423345,0.2682921,0.12323669,-0.0401897,-0.080933355,-0.16911803,-0.027470721,-0.16884083,-0.3367943,-0.12911344,0.09340341,-0.13730296,0.27559164,0.14725901,-0.30747667,0.07225456,-0.044905376,0.019628918,-0.21173139,0.18363541,0.28235024,-0.022419209,-0.009698287,-0.06551786,0.17665888,-0.10588954,-0.16904771,-0.02738593,-0.046320472,0.035360023,0.024506515,0.12232303,-0.1981883,-0.017106298,0.07534499,0.022042358,-0.07882471,0.18869692,0.023687536,0.332867,0.2532411,0.16277105,-0.12660009,0.07892565,-0.31738734,-0.18418832,-0.026556233,-0.40504384,-0.12038041,-0.07331709,0.028991982,0.09024644,0.17815723,0.01368281,-0.21386008,-0.054795504,-0.04856475,-0.107461035,-0.0782615,-0.24691008,-0.22936387,-0.04444888,0.065044135,0.22836989,-0.06341053,-0.016403178,0.18678308,0.09799131,-0.0128325485,0.023131674,-0.014177471,-0.07314447,0.18852864,-0.18244757,-0.028014988,0.032171648,0.08982187,0.014910772,0.09434714,-0.108259566,0.16220476,-0.029621007,-0.1034523,0.09148636,-0.12565093,0.066604495,-0.06028354,0.14299797,0.38231584,-0.092961706,0.2992439,-0.102887906,-0.044390913,-0.2686872,0.14672072,-0.07234594,0.1240975,0.10284076,0.009233785,0.09146663,-0.15674107,-0.117059804,-0.14869303,0.0039980807,0.17785735,0.19957137,0.17402637,0.12671103,-0.03162459,-0.25027126,-0.0030004645,-0.0054609464,-0.049140092,0.10355901,0.07905755,-0.05853097,0.018626938,-0.045393586,-0.07830246,0.13586073,-0.036386903,-0.34316733,0.14405511,0.031384185,0.18877971,-0.031880397,-0.07542809,0.013613634,0.09363844,0.0013715625,-0.059811365,-0.0350758,0.03128532,0.12782718,-0.025399625,-0.03397279,-0.013243599,-6.187558E-4,-0.032406803,0.04611862,-0.07523146,-0.027348991,-0.17525372,-0.04637273,0.033380147,0.045335747,0.07693737,-0.27242345,0.19229534,-0.047392234,0.22093697,-0.029982127,-0.18683979,-0.061149262,-0.018281728,0.038479757,0.11745518,-0.07413845,0.17141148,-0.14268485,0.0732625,-0.013789244,0.011985321,-0.017756492,-0.098321654,-0.123629086,-0.109298326,0.12927127,-0.06698985,-0.03633902,0.007931817,0.013836905,0.023574747,-0.097720794,-0.1016795,0.056898687,0.08997635,0.13355464,-0.012130976,-0.0392825,0.16216332,-0.08017628,-0.056106426,-0.022683635,0.034825746,0.047451988,-0.12167515,0.024465367,0.033839926,0.08857593,-0.06273982,0.19057609,-0.040801093,0.042603184,0.042427465,-0.16631694,0.04326553,0.0736224,-0.038506065,0.12483467,-0.014433414,-0.16474147,0.010605611,0.13611412,-0.03872183,0.06143287,-0.0321076,0.0638555,0.083415754,-0.02441997,-0.10543179,0.037350193,-0.043026507,-0.09827198,0.07042189,0.15833065,-0.068502754,-0.11389594,0.05453974,0.007871833,-0.1062846,0.03430406,0.12290166,0.059938468,0.0854035,-0.06232687,-0.030131064,0.031420697,-0.069966346,0.21522453,-0.18475726,0.12572181,-0.047085363,-0.03656407,0.09466579,-0.08937465,-0.1700937,-0.14088574,-0.17292067,0.020638142,0.084054396,-0.09251538,0.07200217,0.02230359,0.046488978,0.03036815,-0.028120458,0.095736936,0.060803372,-0.016822748,0.12020072,-0.11936447,-0.016120147,0.089216076,0.15080765,-0.075126626,0.01059626,0.0046393536,0.11253951,-0.002814386,-0.077508934,-0.036801748,-0.060010485,-0.13309428,-0.050314277,0.13978346,0.004540298,0.11535987,0.067038104,0.0026174095,0.14797598,0.038850356,0.025356468,0.032861985,0.008919679,0.11000283,0.045697443,0.0015607625,-0.18422988,-0.107192844,-0.02733969,-0.027062492,-0.0055292305,-0.063161984,0.13030618,-0.17140485,0.040545803,0.10021284,0.015334591,-0.028107878,0.15073994,0.14394705,-0.072321914,0.057004888,0.013681658,0.13162172,0.02136939,-0.10023061,0.009150222,-0.11236283,0.013239682,-0.068089165,0.1440911,0.021191712,0.054188907,-0.2250091,-0.050924554,-0.1944449,-0.020738821,-0.06269479,0.0736179,0.0820092,0.13782936,-0.09776199,-0.03459872,0.063452944,0.053491652,0.18270263,-0.14691004,0.035343543,0.06274601,-0.18027061,0.021676306,0.23874046,0.058983658,-0.19823828,-0.028842945,0.053072378,-0.06137558,0.09607974,0.017820988,0.067951575,-0.05042611,-0.077659085,0.037259612,0.117032595,0.12362273,0.08917691,-0.04172038,0.12160517,0.032278005,-0.11215273,-0.021120075,-0.06637502,-0.2885927,-0.10663117,-0.0660766,0.10903639,0.0057737455,-0.10635422,0.020670533,0.08295192,0.09083721,0.045869056,-0.09301614,0.098536894,-0.07714153,0.037384085,-0.14564505,0.10836565,-0.022202052,0.08209545,-0.097585924,-0.11246575,-0.13158062,-0.20881653,-0.13200197,-0.05788873,5.223304E-4,-0.07553891,0.042926457,-0.050039932,-0.05903341,0.02855834,0.027960908,-0.08322275,-0.13648874,0.044465262,-0.13136216,-0.0011229273,-0.012010569,-0.079966255,0.16074866,-0.20133904,-0.054057896,-0.05769839,0.010110892,0.016592648,0.14895493,0.15915239,0.093094885,-0.047184356,0.14294846,0.15156853,0.12331214,-0.029729886,0.12182158,-0.15676503,0.1889922,0.07674611,-0.15650256,-0.089633346,-0.17607336,0.073384024,0.17696083,-0.0035502724,0.11268789,-0.019143328,-0.0569446,0.017624635,0.052507233,-0.059785105,0.05556257,0.10889074,0.16886479,-0.13064201,0.052040037,0.0731478,-0.15059358,-0.009592466,0.090363875,0.036062934,-0.12833895,-0.07113528,0.09610449,-0.0037511736,-0.0674723";
					  String[] str = data.replace("\"", "").replace("[", "").replace("]", "").replace(" ", "").split(",");
					  float[] floatArray = new float[603];
					  float[]  testlabel = new float[1];
					  for(int i = 0;i<str.length-1;i++)
					  {
						  floatArray[i] = Float.parseFloat(str[i]);
					  }
				      DMatrix testMat = new DMatrix(floatArray,1, numColumn);
				      testlabel[0] = floatArray[0];
				      testMat.setLabel(testlabel);

				      float[][] predicts = booster.predict(testMat);
		 		      result = "predicts for nodeID " + str[1]  + " and " + str[2] + " and label 1 is " + predicts[0][0] + "..."  + 
		 		       "predicts for nodeID " + str[1]  + " and " + str[2] + " and label 0 is " + predicts[0][1] + "...";   
					
					os.close();
					
				} catch (IOException | XGBoostError e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			 });
			 session.close();
		 }
	  javaModelTrain.Output  res = new javaModelTrain.Output(result);
	//InputStream in = org.apache.commons.io.IOUtils.toInputStream(result, "UTF-8");
		 driver.close();
      List<Output> resultList = new ArrayList<Output>();
      resultList.add(new Output(result));
      return resultList.stream();	
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
