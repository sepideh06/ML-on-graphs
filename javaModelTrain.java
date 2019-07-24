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

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import ml.dmlc.xgboost4j.java.DMatrix;

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
		                    .get(0).asLong();
		    	 long nodeI2 = session2.run( "CREATE (p:testingData {TestID:{testID}}) RETURN id(p)", Values.parameters("testID", idTest))
		                    .single()
		                    .get(0).asLong();
		    	 
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
		
		 
		 Random random = new Random();
		 try (Session session = driver.session()) {
			 //Integer numEdges = Integer.parseInt(session.run("MATCH (a:page)-[r:LINKS_TO]->() RETURN COUNT(r)").next().get("COUNT(r)").toString());
			 //System.out.println("num" + " " + numEdges.toString());
			 //while (skip<numEdges){
			 //StatementResult listOfIds = session.run("MATCH (a:page)-[r:LINKS_TO]->(b:page) RETURN distinct a.PageID,b.PageID SKIP {skip} LIMIT 1000; ", Values.parameters("skip",skip));
			 StatementResult listOfIds = session.run("MATCH (a:page)-[r:LINKS_TO]->(b:page) RETURN distinct a.PageID,b.PageID;");
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
				 
				 
		//Now we create nodes for train and test
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

	public void CreateModel() throws XGBoostError, IOException 
	{
		Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "123"));
		
		String[] arrayTest = new String[]{};
		String[] arrayTrain = new String[]{};
		List<String> lstTrain = new ArrayList<String>();
		List<String> lstTest = new ArrayList<String>();
		
		 try (Session session = driver.session()) {
			 List<Record> listOfTestData = session.run("MATCH (a:Test) RETURN  a.data AS data").list();
			 List<Record> listOfTrainData = session.run("MATCH (a:Train) RETURN  a.data AS data").list();

			 arrayTest = listOfTestData.get(0).get("data").toString().split(" ");
			 arrayTrain = listOfTrainData.get(0).get("data").toString().split(" ");
			 
			 String label = "";
             for(int i= 0;i < arrayTest.length;i++)
             {
            	 String[] str = arrayTest[i].replace("\"", "").replace("[", "").replace("]", "").split(",");
            	 List<Record> embeddingPageID1 = session.run("MATCH (n:page) WHERE n.PageID = {pageID} RETURN n.Embedding AS Embedding",Values.parameters("pageID",Integer.parseInt(str[0]))).list();
            	 List<Record> embeddingPageID2 = session.run("MATCH (n:page) WHERE n.PageID = {pageID} RETURN n.Embedding AS Embedding",Values.parameters("pageID",Integer.parseInt(str[1]))).list();
            	 String[] embedArray1 = embeddingPageID1.get(0).get("Embedding").toString().replace("\"", "").split(",");
            	 String[] embedArray2 = embeddingPageID2.get(0).get("Embedding").toString().replace("\"", "").split(",");
            	 
            	 if(str[2] == "exist")
            		 label = "1";
            	 else
            		 label = "0";
            	 
            	 String[] embeddingArray1 = Arrays.copyOf(embedArray1, 301);
            	 String[] embeddingArray2 = Arrays.copyOf(embedArray2, 301);
            	 for(int k= embedArray1.length-1;k<301;k++)
            	 {
            		 embeddingArray1[k]="1";
            	 }
            	 for(int k= embedArray2.length-1;k<301;k++)
            	 {
            		 embeddingArray2[k]="1";
            	 }
            	 
            	 lstTest.add(label+" " + "1:" + str[0] + " " +  "2:" + str[1] + " " + "3:" + embeddingArray1[0]+" " +"4:"+embeddingArray1[1]+" "+
            			 "5:"+embeddingArray1[2]+" "+
            			 "6:"+embeddingArray1[3]+" "+
            			 "7:"+embeddingArray1[4]+" "+
            			 "8:"+embeddingArray1[5]+" "+
            			 "9:"+embeddingArray1[6]+" "+
            			 "10:"+embeddingArray1[7]+" "+
            			 "11:"+embeddingArray1[8]+" "+
            			 "12:"+embeddingArray1[9]+" "+
            			 "13:"+embeddingArray1[10]+" "+
            			 "14:"+embeddingArray1[11]+" "+
            			 "15:"+embeddingArray1[12]+" "+
            			 "16:"+embeddingArray1[13]+" "+
            			 "17:"+embeddingArray1[14]+" "+
            			 "18:"+embeddingArray1[15]+" "+
            			 "19:"+embeddingArray1[16]+" "+
            			 "20:"+embeddingArray1[17]+" "+
            			 "21:"+embeddingArray1[18]+" "+
            			 "22:"+embeddingArray1[19]+" "+
            			 "23:"+embeddingArray1[20]+" "+
            			 "24:"+embeddingArray1[21]+" "+
            			 "25:"+embeddingArray1[22]+" "+
            			 "26:"+embeddingArray1[23]+" "+
            			 "27:"+embeddingArray1[24]+" "+
            			 "28:"+embeddingArray1[25]+" "+
            			 "29:"+embeddingArray1[26]+" "+
            			 "30:"+embeddingArray1[27]+" "+
            			 "31:"+embeddingArray1[28]+" "+
            			 "32:"+embeddingArray1[29]+" "+
            			 "33:"+embeddingArray1[30]+" "+
            			 "34:"+embeddingArray1[31]+" "+
            			 "35:"+embeddingArray1[32]+" "+
            			 "36:"+embeddingArray1[33]+" "+
            			 "37:"+embeddingArray1[34]+" "+
            			 "38:"+embeddingArray1[35]+" "+
            			 "39:"+embeddingArray1[36]+" "+
            			 "40:"+embeddingArray1[37]+" "+
            			 "41:"+embeddingArray1[38]+" "+
            			 "42:"+embeddingArray1[39]+" "+
            			 "43:"+embeddingArray1[40]+" "+
            			 "44:"+embeddingArray1[41]+" "+
            			 "45:"+embeddingArray1[42]+" "+
            			 "46:"+embeddingArray1[43]+" "+
            			 "47:"+embeddingArray1[44]+" "+
            			 "48:"+embeddingArray1[45]+" "+
            			 "49:"+embeddingArray1[46]+" "+
            			 "50:"+embeddingArray1[47]+" "+
            			 "51:"+embeddingArray1[48]+" "+
            			 "52:"+embeddingArray1[49]+" "+
            			 "53:"+embeddingArray1[50]+" "+
            			 "54:"+embeddingArray1[51]+" "+
            			 "55:"+embeddingArray1[52]+" "+
            			 "56:"+embeddingArray1[53]+" "+
            			 "57:"+embeddingArray1[54]+" "+
            			 "58:"+embeddingArray1[55]+" "+
            			 "59:"+embeddingArray1[56]+" "+
            			 "60:"+embeddingArray1[57]+" "+
            			 "61:"+embeddingArray1[58]+" "+
            			 "62:"+embeddingArray1[59]+" "+
            			 "63:"+embeddingArray1[60]+" "+
            			 "64:"+embeddingArray1[61]+" "+
            			 "65:"+embeddingArray1[62]+" "+
            			 "66:"+embeddingArray1[63]+" "+
            			 "67:"+embeddingArray1[64]+" "+
            			 "68:"+embeddingArray1[65]+" "+
            			 "69:"+embeddingArray1[66]+" "+
            			 "70:"+embeddingArray1[67]+" "+
            			 "71:"+embeddingArray1[68]+" "+
            			 "72:"+embeddingArray1[69]+" "+
            			 "73:"+embeddingArray1[70]+" "+
            			 "74:"+embeddingArray1[71]+" "+
            			 "75:"+embeddingArray1[72]+" "+
            			 "76:"+embeddingArray1[73]+" "+
            			 "77:"+embeddingArray1[74]+" "+
            			 "78:"+embeddingArray1[75]+" "+
            			 "79:"+embeddingArray1[76]+" "+
            			 "80:"+embeddingArray1[77]+" "+
            			 "81:"+embeddingArray1[78]+" "+
            			 "82:"+embeddingArray1[79]+" "+
            			 "83:"+embeddingArray1[80]+" "+
            			 "84:"+embeddingArray1[81]+" "+
            			 "85:"+embeddingArray1[82]+" "+
            			 "86:"+embeddingArray1[83]+" "+
            			 "87:"+embeddingArray1[84]+" "+
            			 "88:"+embeddingArray1[85]+" "+
            			 "89:"+embeddingArray1[86]+" "+
            			 "90:"+embeddingArray1[87]+" "+
            			 "91:"+embeddingArray1[88]+" "+
            			 "92:"+embeddingArray1[89]+" "+
            			 "93:"+embeddingArray1[90]+" "+
            			 "94:"+embeddingArray1[91]+" "+
            			 "95:"+embeddingArray1[92]+" "+
            			 "96:"+embeddingArray1[93]+" "+
            			 "97:"+embeddingArray1[94]+" "+
            			 "98:"+embeddingArray1[95]+" "+
            			 "99:"+embeddingArray1[96]+" "+
            			 "100:"+embeddingArray1[97]+" "+
            			 "101:"+embeddingArray1[98]+" "+
            			 "102:"+embeddingArray1[99]+" "+
            			 "103:"+embeddingArray1[100]+" "+
            			 "104:"+embeddingArray1[101]+" "+
            			 "105:"+embeddingArray1[102]+" "+
            			 "106:"+embeddingArray1[103]+" "+
            			 "107:"+embeddingArray1[104]+" "+
            			 "108:"+embeddingArray1[105]+" "+
            			 "109:"+embeddingArray1[106]+" "+
            			 "110:"+embeddingArray1[107]+" "+
            			 "111:"+embeddingArray1[108]+" "+
            			 "112:"+embeddingArray1[109]+" "+
            			 "113:"+embeddingArray1[110]+" "+
            			 "114:"+embeddingArray1[111]+" "+
            			 "115:"+embeddingArray1[112]+" "+
            			 "116:"+embeddingArray1[113]+" "+
            			 "117:"+embeddingArray1[114]+" "+
            			 "118:"+embeddingArray1[115]+" "+
            			 "119:"+embeddingArray1[116]+" "+
            			 "120:"+embeddingArray1[117]+" "+
            			 "121:"+embeddingArray1[118]+" "+
            			 "122:"+embeddingArray1[119]+" "+
            			 "123:"+embeddingArray1[120]+" "+
            			 "124:"+embeddingArray1[121]+" "+
            			 "125:"+embeddingArray1[122]+" "+
            			 "126:"+embeddingArray1[123]+" "+
            			 "127:"+embeddingArray1[124]+" "+
            			 "128:"+embeddingArray1[125]+" "+
            			 "129:"+embeddingArray1[126]+" "+
            			 "130:"+embeddingArray1[127]+" "+
            			 "131:"+embeddingArray1[128]+" "+
            			 "132:"+embeddingArray1[129]+" "+
            			 "133:"+embeddingArray1[130]+" "+
            			 "134:"+embeddingArray1[131]+" "+
            			 "135:"+embeddingArray1[132]+" "+
            			 "136:"+embeddingArray1[133]+" "+
            			 "137:"+embeddingArray1[134]+" "+
            			 "138:"+embeddingArray1[135]+" "+
            			 "139:"+embeddingArray1[136]+" "+
            			 "140:"+embeddingArray1[137]+" "+
            			 "141:"+embeddingArray1[138]+" "+
            			 "142:"+embeddingArray1[139]+" "+
            			 "143:"+embeddingArray1[140]+" "+
            			 "144:"+embeddingArray1[141]+" "+
            			 "145:"+embeddingArray1[142]+" "+
            			 "146:"+embeddingArray1[143]+" "+
            			 "147:"+embeddingArray1[144]+" "+
            			 "148:"+embeddingArray1[145]+" "+
            			 "149:"+embeddingArray1[146]+" "+
            			 "150:"+embeddingArray1[147]+" "+
            			 "151:"+embeddingArray1[148]+" "+
            			 "152:"+embeddingArray1[149]+" "+
            			 "153:"+embeddingArray1[150]+" "+
            			 "154:"+embeddingArray1[151]+" "+
            			 "155:"+embeddingArray1[152]+" "+
            			 "156:"+embeddingArray1[153]+" "+
            			 "157:"+embeddingArray1[154]+" "+
            			 "158:"+embeddingArray1[155]+" "+
            			 "159:"+embeddingArray1[156]+" "+
            			 "160:"+embeddingArray1[157]+" "+
            			 "161:"+embeddingArray1[158]+" "+
            			 "162:"+embeddingArray1[159]+" "+
            			 "163:"+embeddingArray1[160]+" "+
            			 "164:"+embeddingArray1[161]+" "+
            			 "165:"+embeddingArray1[162]+" "+
            			 "166:"+embeddingArray1[163]+" "+
            			 "167:"+embeddingArray1[164]+" "+
            			 "168:"+embeddingArray1[165]+" "+
            			 "169:"+embeddingArray1[166]+" "+
            			 "170:"+embeddingArray1[167]+" "+
            			 "171:"+embeddingArray1[168]+" "+
            			 "172:"+embeddingArray1[169]+" "+
            			 "173:"+embeddingArray1[170]+" "+
            			 "174:"+embeddingArray1[171]+" "+
            			 "175:"+embeddingArray1[172]+" "+
            			 "176:"+embeddingArray1[173]+" "+
            			 "177:"+embeddingArray1[174]+" "+
            			 "178:"+embeddingArray1[175]+" "+
            			 "179:"+embeddingArray1[176]+" "+
            			 "180:"+embeddingArray1[177]+" "+
            			 "181:"+embeddingArray1[178]+" "+
            			 "182:"+embeddingArray1[179]+" "+
            			 "183:"+embeddingArray1[180]+" "+
            			 "184:"+embeddingArray1[181]+" "+
            			 "185:"+embeddingArray1[182]+" "+
            			 "186:"+embeddingArray1[183]+" "+
            			 "187:"+embeddingArray1[184]+" "+
            			 "188:"+embeddingArray1[185]+" "+
            			 "189:"+embeddingArray1[186]+" "+
            			 "190:"+embeddingArray1[187]+" "+
            			 "191:"+embeddingArray1[188]+" "+
            			 "192:"+embeddingArray1[189]+" "+
            			 "193:"+embeddingArray1[190]+" "+
            			 "194:"+embeddingArray1[191]+" "+
            			 "195:"+embeddingArray1[192]+" "+
            			 "196:"+embeddingArray1[193]+" "+
            			 "197:"+embeddingArray1[194]+" "+
            			 "198:"+embeddingArray1[195]+" "+
            			 "199:"+embeddingArray1[196]+" "+
            			 "200:"+embeddingArray1[197]+" "+
            			 "201:"+embeddingArray1[198]+" "+
            			 "202:"+embeddingArray1[199]+" "+
            			 "203:"+embeddingArray1[200]+" "+
            			 "204:"+embeddingArray1[201]+" "+
            			 "205:"+embeddingArray1[202]+" "+
            			 "206:"+embeddingArray1[203]+" "+
            			 "207:"+embeddingArray1[204]+" "+
            			 "208:"+embeddingArray1[205]+" "+
            			 "209:"+embeddingArray1[206]+" "+
            			 "210:"+embeddingArray1[207]+" "+
            			 "211:"+embeddingArray1[208]+" "+
            			 "212:"+embeddingArray1[209]+" "+
            			 "213:"+embeddingArray1[210]+" "+
            			 "214:"+embeddingArray1[211]+" "+
            			 "215:"+embeddingArray1[212]+" "+
            			 "216:"+embeddingArray1[213]+" "+
            			 "217:"+embeddingArray1[214]+" "+
            			 "218:"+embeddingArray1[215]+" "+
            			 "219:"+embeddingArray1[216]+" "+
            			 "220:"+embeddingArray1[217]+" "+
            			 "221:"+embeddingArray1[218]+" "+
            			 "222:"+embeddingArray1[219]+" "+
            			 "223:"+embeddingArray1[220]+" "+
            			 "224:"+embeddingArray1[221]+" "+
            			 "225:"+embeddingArray1[222]+" "+
            			 "226:"+embeddingArray1[223]+" "+
            			 "227:"+embeddingArray1[224]+" "+
            			 "228:"+embeddingArray1[225]+" "+
            			 "229:"+embeddingArray1[226]+" "+
            			 "230:"+embeddingArray1[227]+" "+
            			 "231:"+embeddingArray1[228]+" "+
            			 "232:"+embeddingArray1[229]+" "+
            			 "233:"+embeddingArray1[230]+" "+
            			 "234:"+embeddingArray1[231]+" "+
            			 "235:"+embeddingArray1[232]+" "+
            			 "236:"+embeddingArray1[233]+" "+
            			 "237:"+embeddingArray1[234]+" "+
            			 "238:"+embeddingArray1[235]+" "+
            			 "239:"+embeddingArray1[236]+" "+
            			 "240:"+embeddingArray1[237]+" "+
            			 "241:"+embeddingArray1[238]+" "+
            			 "242:"+embeddingArray1[239]+" "+
            			 "243:"+embeddingArray1[240]+" "+
            			 "244:"+embeddingArray1[241]+" "+
            			 "245:"+embeddingArray1[242]+" "+
            			 "246:"+embeddingArray1[243]+" "+
            			 "247:"+embeddingArray1[244]+" "+
            			 "248:"+embeddingArray1[245]+" "+
            			 "249:"+embeddingArray1[246]+" "+
            			 "250:"+embeddingArray1[247]+" "+
            			 "251:"+embeddingArray1[248]+" "+
            			 "252:"+embeddingArray1[249]+" "+
            			 "253:"+embeddingArray1[250]+" "+
            			 "254:"+embeddingArray1[251]+" "+
            			 "255:"+embeddingArray1[252]+" "+
            			 "256:"+embeddingArray1[253]+" "+
            			 "257:"+embeddingArray1[254]+" "+
            			 "258:"+embeddingArray1[255]+" "+
            			 "259:"+embeddingArray1[256]+" "+
            			 "260:"+embeddingArray1[257]+" "+
            			 "261:"+embeddingArray1[258]+" "+
            			 "262:"+embeddingArray1[259]+" "+
            			 "263:"+embeddingArray1[260]+" "+
            			 "264:"+embeddingArray1[261]+" "+
            			 "265:"+embeddingArray1[262]+" "+
            			 "266:"+embeddingArray1[263]+" "+
            			 "267:"+embeddingArray1[264]+" "+
            			 "268:"+embeddingArray1[265]+" "+
            			 "269:"+embeddingArray1[266]+" "+
            			 "270:"+embeddingArray1[267]+" "+
            			 "271:"+embeddingArray1[268]+" "+
            			 "272:"+embeddingArray1[269]+" "+
            			 "273:"+embeddingArray1[270]+" "+
            			 "274:"+embeddingArray1[271]+" "+
            			 "275:"+embeddingArray1[272]+" "+
            			 "276:"+embeddingArray1[273]+" "+
            			 "277:"+embeddingArray1[274]+" "+
            			 "278:"+embeddingArray1[275]+" "+
            			 "279:"+embeddingArray1[276]+" "+
            			 "280:"+embeddingArray1[277]+" "+
            			 "281:"+embeddingArray1[278]+" "+
            			 "282:"+embeddingArray1[279]+" "+
            			 "283:"+embeddingArray1[280]+" "+
            			 "284:"+embeddingArray1[281]+" "+
            			 "285:"+embeddingArray1[282]+" "+
            			 "286:"+embeddingArray1[283]+" "+
            			 "287:"+embeddingArray1[284]+" "+
            			 "288:"+embeddingArray1[285]+" "+
            			 "289:"+embeddingArray1[286]+" "+
            			 "290:"+embeddingArray1[287]+" "+
            			 "291:"+embeddingArray1[288]+" "+
            			 "292:"+embeddingArray1[289]+" "+
            			 "293:"+embeddingArray1[290]+" "+
            			 "294:"+embeddingArray1[291]+" "+
            			 "295:"+embeddingArray1[292]+" "+
            			 "296:"+embeddingArray1[293]+" "+
            			 "297:"+embeddingArray1[294]+" "+
            			 "298:"+embeddingArray1[295]+" "+
            			 "299:"+embeddingArray1[296]+" "+
            			 "300:"+embeddingArray1[297]+" "+
            			 "301:"+embeddingArray1[298]+" "+
            			 "302:"+embeddingArray1[299]+" "+
            			 "303:"+embeddingArray2[0]+" "+
            			 "304:"+embeddingArray2[1]+" "+
            			 "305:"+embeddingArray2[2]+" "+
            			 "306:"+embeddingArray2[3]+" "+
            			 "307:"+embeddingArray2[4]+" "+
            			 "308:"+embeddingArray2[5]+" "+
            			 "309:"+embeddingArray2[6]+" "+
            			 "310:"+embeddingArray2[7]+" "+
            			 "311:"+embeddingArray2[8]+" "+
            			 "312:"+embeddingArray2[9]+" "+
            			 "313:"+embeddingArray2[10]+" "+
            			 "314:"+embeddingArray2[11]+" "+
            			 "315:"+embeddingArray2[12]+" "+
            			 "316:"+embeddingArray2[13]+" "+
            			 "317:"+embeddingArray2[14]+" "+
            			 "318:"+embeddingArray2[15]+" "+
            			 "319:"+embeddingArray2[16]+" "+
            			 "320:"+embeddingArray2[17]+" "+
            			 "321:"+embeddingArray2[18]+" "+
            			 "322:"+embeddingArray2[19]+" "+
            			 "323:"+embeddingArray2[20]+" "+
            			 "324:"+embeddingArray2[21]+" "+
            			 "325:"+embeddingArray2[22]+" "+
            			 "326:"+embeddingArray2[23]+" "+
            			 "327:"+embeddingArray2[24]+" "+
            			 "328:"+embeddingArray2[25]+" "+
            			 "329:"+embeddingArray2[26]+" "+
            			 "330:"+embeddingArray2[27]+" "+
            			 "331:"+embeddingArray2[28]+" "+
            			 "332:"+embeddingArray2[29]+" "+
            			 "333:"+embeddingArray2[30]+" "+
            			 "334:"+embeddingArray2[31]+" "+
            			 "335:"+embeddingArray2[32]+" "+
            			 "336:"+embeddingArray2[33]+" "+
            			 "337:"+embeddingArray2[34]+" "+
            			 "338:"+embeddingArray2[35]+" "+
            			 "339:"+embeddingArray2[36]+" "+
            			 "340:"+embeddingArray2[37]+" "+
            			 "341:"+embeddingArray2[38]+" "+
            			 "342:"+embeddingArray2[39]+" "+
            			 "343:"+embeddingArray2[40]+" "+
            			 "344:"+embeddingArray2[41]+" "+
            			 "345:"+embeddingArray2[42]+" "+
            			 "346:"+embeddingArray2[43]+" "+
            			 "347:"+embeddingArray2[44]+" "+
            			 "348:"+embeddingArray2[45]+" "+
            			 "349:"+embeddingArray2[46]+" "+
            			 "350:"+embeddingArray2[47]+" "+
            			 "351:"+embeddingArray2[48]+" "+
            			 "352:"+embeddingArray2[49]+" "+
            			 "353:"+embeddingArray2[50]+" "+
            			 "354:"+embeddingArray2[51]+" "+
            			 "355:"+embeddingArray2[52]+" "+
            			 "356:"+embeddingArray2[53]+" "+
            			 "357:"+embeddingArray2[54]+" "+
            			 "358:"+embeddingArray2[55]+" "+
            			 "359:"+embeddingArray2[56]+" "+
            			 "360:"+embeddingArray2[57]+" "+
            			 "361:"+embeddingArray2[58]+" "+
            			 "362:"+embeddingArray2[59]+" "+
            			 "363:"+embeddingArray2[60]+" "+
            			 "364:"+embeddingArray2[61]+" "+
            			 "365:"+embeddingArray2[62]+" "+
            			 "366:"+embeddingArray2[63]+" "+
            			 "367:"+embeddingArray2[64]+" "+
            			 "368:"+embeddingArray2[65]+" "+
            			 "369:"+embeddingArray2[66]+" "+
            			 "370:"+embeddingArray2[67]+" "+
            			 "371:"+embeddingArray2[68]+" "+
            			 "372:"+embeddingArray2[69]+" "+
            			 "373:"+embeddingArray2[70]+" "+
            			 "374:"+embeddingArray2[71]+" "+
            			 "375:"+embeddingArray2[72]+" "+
            			 "376:"+embeddingArray2[73]+" "+
            			 "377:"+embeddingArray2[74]+" "+
            			 "378:"+embeddingArray2[75]+" "+
            			 "379:"+embeddingArray2[76]+" "+
            			 "380:"+embeddingArray2[77]+" "+
            			 "381:"+embeddingArray2[78]+" "+
            			 "382:"+embeddingArray2[79]+" "+
            			 "383:"+embeddingArray2[80]+" "+
            			 "384:"+embeddingArray2[81]+" "+
            			 "385:"+embeddingArray2[82]+" "+
            			 "386:"+embeddingArray2[83]+" "+
            			 "387:"+embeddingArray2[84]+" "+
            			 "388:"+embeddingArray2[85]+" "+
            			 "389:"+embeddingArray2[86]+" "+
            			 "390:"+embeddingArray2[87]+" "+
            			 "391:"+embeddingArray2[88]+" "+
            			 "392:"+embeddingArray2[89]+" "+
            			 "393:"+embeddingArray2[90]+" "+
            			 "394:"+embeddingArray2[91]+" "+
            			 "395:"+embeddingArray2[92]+" "+
            			 "396:"+embeddingArray2[93]+" "+
            			 "397:"+embeddingArray2[94]+" "+
            			 "398:"+embeddingArray2[95]+" "+
            			 "399:"+embeddingArray2[96]+" "+
            			 "400:"+embeddingArray2[97]+" "+
            			 "401:"+embeddingArray2[98]+" "+
            			 "402:"+embeddingArray2[99]+" "+
            			 "403:"+embeddingArray2[100]+" "+
            			 "404:"+embeddingArray2[101]+" "+
            			 "405:"+embeddingArray2[102]+" "+
            			 "406:"+embeddingArray2[103]+" "+
            			 "407:"+embeddingArray2[104]+" "+
            			 "408:"+embeddingArray2[105]+" "+
            			 "409:"+embeddingArray2[106]+" "+
            			 "410:"+embeddingArray2[107]+" "+
            			 "411:"+embeddingArray2[108]+" "+
            			 "412:"+embeddingArray2[109]+" "+
            			 "413:"+embeddingArray2[110]+" "+
            			 "414:"+embeddingArray2[111]+" "+
            			 "415:"+embeddingArray2[112]+" "+
            			 "416:"+embeddingArray2[113]+" "+
            			 "417:"+embeddingArray2[114]+" "+
            			 "418:"+embeddingArray2[115]+" "+
            			 "419:"+embeddingArray2[116]+" "+
            			 "420:"+embeddingArray2[117]+" "+
            			 "421:"+embeddingArray2[118]+" "+
            			 "422:"+embeddingArray2[119]+" "+
            			 "423:"+embeddingArray2[120]+" "+
            			 "424:"+embeddingArray2[121]+" "+
            			 "425:"+embeddingArray2[122]+" "+
            			 "426:"+embeddingArray2[123]+" "+
            			 "427:"+embeddingArray2[124]+" "+
            			 "428:"+embeddingArray2[125]+" "+
            			 "429:"+embeddingArray2[126]+" "+
            			 "430:"+embeddingArray2[127]+" "+
            			 "431:"+embeddingArray2[128]+" "+
            			 "432:"+embeddingArray2[129]+" "+
            			 "433:"+embeddingArray2[130]+" "+
            			 "434:"+embeddingArray2[131]+" "+
            			 "435:"+embeddingArray2[132]+" "+
            			 "436:"+embeddingArray2[133]+" "+
            			 "437:"+embeddingArray2[134]+" "+
            			 "438:"+embeddingArray2[135]+" "+
            			 "439:"+embeddingArray2[136]+" "+
            			 "440:"+embeddingArray2[137]+" "+
            			 "441:"+embeddingArray2[138]+" "+
            			 "442:"+embeddingArray2[139]+" "+
            			 "443:"+embeddingArray2[140]+" "+
            			 "444:"+embeddingArray2[141]+" "+
            			 "445:"+embeddingArray2[142]+" "+
            			 "446:"+embeddingArray2[143]+" "+
            			 "447:"+embeddingArray2[144]+" "+
            			 "448:"+embeddingArray2[145]+" "+
            			 "449:"+embeddingArray2[146]+" "+
            			 "450:"+embeddingArray2[147]+" "+
            			 "451:"+embeddingArray2[148]+" "+
            			 "452:"+embeddingArray2[149]+" "+
            			 "453:"+embeddingArray2[150]+" "+
            			 "454:"+embeddingArray2[151]+" "+
            			 "455:"+embeddingArray2[152]+" "+
            			 "456:"+embeddingArray2[153]+" "+
            			 "457:"+embeddingArray2[154]+" "+
            			 "458:"+embeddingArray2[155]+" "+
            			 "459:"+embeddingArray2[156]+" "+
            			 "460:"+embeddingArray2[157]+" "+
            			 "461:"+embeddingArray2[158]+" "+
            			 "462:"+embeddingArray2[159]+" "+
            			 "463:"+embeddingArray2[160]+" "+
            			 "464:"+embeddingArray2[161]+" "+
            			 "465:"+embeddingArray2[162]+" "+
            			 "466:"+embeddingArray2[163]+" "+
            			 "467:"+embeddingArray2[164]+" "+
            			 "468:"+embeddingArray2[165]+" "+
            			 "469:"+embeddingArray2[166]+" "+
            			 "470:"+embeddingArray2[167]+" "+
            			 "471:"+embeddingArray2[168]+" "+
            			 "472:"+embeddingArray2[169]+" "+
            			 "473:"+embeddingArray2[170]+" "+
            			 "474:"+embeddingArray2[171]+" "+
            			 "475:"+embeddingArray2[172]+" "+
            			 "476:"+embeddingArray2[173]+" "+
            			 "477:"+embeddingArray2[174]+" "+
            			 "478:"+embeddingArray2[175]+" "+
            			 "479:"+embeddingArray2[176]+" "+
            			 "480:"+embeddingArray2[177]+" "+
            			 "481:"+embeddingArray2[178]+" "+
            			 "482:"+embeddingArray2[179]+" "+
            			 "483:"+embeddingArray2[180]+" "+
            			 "484:"+embeddingArray2[181]+" "+
            			 "485:"+embeddingArray2[182]+" "+
            			 "486:"+embeddingArray2[183]+" "+
            			 "487:"+embeddingArray2[184]+" "+
            			 "488:"+embeddingArray2[185]+" "+
            			 "489:"+embeddingArray2[186]+" "+
            			 "490:"+embeddingArray2[187]+" "+
            			 "491:"+embeddingArray2[188]+" "+
            			 "492:"+embeddingArray2[189]+" "+
            			 "493:"+embeddingArray2[190]+" "+
            			 "494:"+embeddingArray2[191]+" "+
            			 "495:"+embeddingArray2[192]+" "+
            			 "496:"+embeddingArray2[193]+" "+
            			 "497:"+embeddingArray2[194]+" "+
            			 "498:"+embeddingArray2[195]+" "+
            			 "499:"+embeddingArray2[196]+" "+
            			 "500:"+embeddingArray2[197]+" "+
            			 "501:"+embeddingArray2[198]+" "+
            			 "502:"+embeddingArray2[199]+" "+
            			 "503:"+embeddingArray2[200]+" "+
            			 "504:"+embeddingArray2[201]+" "+
            			 "505:"+embeddingArray2[202]+" "+
            			 "506:"+embeddingArray2[203]+" "+
            			 "507:"+embeddingArray2[204]+" "+
            			 "508:"+embeddingArray2[205]+" "+
            			 "509:"+embeddingArray2[206]+" "+
            			 "510:"+embeddingArray2[207]+" "+
            			 "511:"+embeddingArray2[208]+" "+
            			 "512:"+embeddingArray2[209]+" "+
            			 "513:"+embeddingArray2[210]+" "+
            			 "514:"+embeddingArray2[211]+" "+
            			 "515:"+embeddingArray2[212]+" "+
            			 "516:"+embeddingArray2[213]+" "+
            			 "517:"+embeddingArray2[214]+" "+
            			 "518:"+embeddingArray2[215]+" "+
            			 "519:"+embeddingArray2[216]+" "+
            			 "520:"+embeddingArray2[217]+" "+
            			 "521:"+embeddingArray2[218]+" "+
            			 "522:"+embeddingArray2[219]+" "+
            			 "523:"+embeddingArray2[220]+" "+
            			 "524:"+embeddingArray2[221]+" "+
            			 "525:"+embeddingArray2[222]+" "+
            			 "526:"+embeddingArray2[223]+" "+
            			 "527:"+embeddingArray2[224]+" "+
            			 "528:"+embeddingArray2[225]+" "+
            			 "529:"+embeddingArray2[226]+" "+
            			 "530:"+embeddingArray2[227]+" "+
            			 "531:"+embeddingArray2[228]+" "+
            			 "532:"+embeddingArray2[229]+" "+
            			 "533:"+embeddingArray2[230]+" "+
            			 "534:"+embeddingArray2[231]+" "+
            			 "535:"+embeddingArray2[232]+" "+
            			 "536:"+embeddingArray2[233]+" "+
            			 "537:"+embeddingArray2[234]+" "+
            			 "538:"+embeddingArray2[235]+" "+
            			 "539:"+embeddingArray2[236]+" "+
            			 "540:"+embeddingArray2[237]+" "+
            			 "541:"+embeddingArray2[238]+" "+
            			 "542:"+embeddingArray2[239]+" "+
            			 "543:"+embeddingArray2[240]+" "+
            			 "544:"+embeddingArray2[241]+" "+
            			 "545:"+embeddingArray2[242]+" "+
            			 "546:"+embeddingArray2[243]+" "+
            			 "547:"+embeddingArray2[244]+" "+
            			 "548:"+embeddingArray2[245]+" "+
            			 "549:"+embeddingArray2[246]+" "+
            			 "550:"+embeddingArray2[247]+" "+
            			 "551:"+embeddingArray2[248]+" "+
            			 "552:"+embeddingArray2[249]+" "+
            			 "553:"+embeddingArray2[250]+" "+
            			 "554:"+embeddingArray2[251]+" "+
            			 "555:"+embeddingArray2[252]+" "+
            			 "556:"+embeddingArray2[253]+" "+
            			 "557:"+embeddingArray2[254]+" "+
            			 "558:"+embeddingArray2[255]+" "+
            			 "559:"+embeddingArray2[256]+" "+
            			 "560:"+embeddingArray2[257]+" "+
            			 "561:"+embeddingArray2[258]+" "+
            			 "562:"+embeddingArray2[259]+" "+
            			 "563:"+embeddingArray2[260]+" "+
            			 "564:"+embeddingArray2[261]+" "+
            			 "565:"+embeddingArray2[262]+" "+
            			 "566:"+embeddingArray2[263]+" "+
            			 "567:"+embeddingArray2[264]+" "+
            			 "568:"+embeddingArray2[265]+" "+
            			 "569:"+embeddingArray2[266]+" "+
            			 "570:"+embeddingArray2[267]+" "+
            			 "571:"+embeddingArray2[268]+" "+
            			 "572:"+embeddingArray2[269]+" "+
            			 "573:"+embeddingArray2[270]+" "+
            			 "574:"+embeddingArray2[271]+" "+
            			 "575:"+embeddingArray2[272]+" "+
            			 "576:"+embeddingArray2[273]+" "+
            			 "577:"+embeddingArray2[274]+" "+
            			 "578:"+embeddingArray2[275]+" "+
            			 "579:"+embeddingArray2[276]+" "+
            			 "580:"+embeddingArray2[277]+" "+
            			 "581:"+embeddingArray2[278]+" "+
            			 "582:"+embeddingArray2[279]+" "+
            			 "583:"+embeddingArray2[280]+" "+
            			 "584:"+embeddingArray2[281]+" "+
            			 "585:"+embeddingArray2[282]+" "+
            			 "586:"+embeddingArray2[283]+" "+
            			 "587:"+embeddingArray2[284]+" "+
            			 "588:"+embeddingArray2[285]+" "+
            			 "589:"+embeddingArray2[286]+" "+
            			 "590:"+embeddingArray2[287]+" "+
            			 "591:"+embeddingArray2[288]+" "+
            			 "592:"+embeddingArray2[289]+" "+
            			 "593:"+embeddingArray2[290]+" "+
            			 "594:"+embeddingArray2[291]+" "+
            			 "595:"+embeddingArray2[292]+" "+
            			 "596:"+embeddingArray2[293]+" "+
            			 "597:"+embeddingArray2[294]+" "+
            			 "598:"+embeddingArray2[295]+" "+
            			 "599:"+embeddingArray2[296]+" "+
            			 "600:"+embeddingArray2[297]+" "+
            			 "601:"+embeddingArray2[298]+" "+
            			 "602:"+embeddingArray2[299]+" ");
             }
             
             /*for(int i=0;i<300;i++)
             {
            	 int j = i + 303;
            	 System.out.println("\""+ j +":"+"\"" +"+embeddingArray2["+i+"]+"+"\"" +" "+"\"+");
             }*/
             
             List<String> lines = lstTest;
             Path file = Paths.get("test.txt.test");
             Files.write(file, lines, StandardCharsets.UTF_8);
             
             
             for(int i= 0;i < arrayTrain.length;i++)
             {
            	 String[] str = arrayTrain[i].replace("\"", "").replace("[", "").replace("]", "").split(",");
            	 List<Record> embeddingPageID1 = session.run("MATCH (n:page) WHERE n.PageID = {pageID} RETURN n.Embedding AS Embedding",Values.parameters("pageID",Integer.parseInt(str[0]))).list();
            	 List<Record> embeddingPageID2 = session.run("MATCH (n:page) WHERE n.PageID = {pageID} RETURN n.Embedding AS Embedding",Values.parameters("pageID",Integer.parseInt(str[1]))).list();
            	 String[] embedArray1 = embeddingPageID1.get(0).get("Embedding").toString().replace("\"", "").split(",");
            	 String[] embedArray2 = embeddingPageID2.get(0).get("Embedding").toString().replace("\"", "").split(",");
            	 
            	 if(str[2] == "exist")
            		 label = "1";
            	 else
            		 label = "0";
            	 
            	 String[] embeddingArray1 = Arrays.copyOf(embedArray1, 301);
            	 String[] embeddingArray2 = Arrays.copyOf(embedArray2, 301);
            	 for(int k= embedArray1.length-1;k<301;k++)
            	 {
            		 embeddingArray1[k]="1";
            	 }
            	 for(int k= embedArray2.length-1;k<301;k++)
            	 {
            		 embeddingArray2[k]="1";
            	 }
            	 
            	 lstTrain.add(label+" " + "1:" + str[0] + " " +  "2:" + str[1] + " " + "3:" + embeddingArray1[0]+ " " + "4:"+embeddingArray1[1]+" "+
            			 "5:"+embeddingArray1[2]+" "+
            			 "6:"+embeddingArray1[3]+" "+
            			 "7:"+embeddingArray1[4]+" "+
            			 "8:"+embeddingArray1[5]+" "+
            			 "9:"+embeddingArray1[6]+" "+
            			 "10:"+embeddingArray1[7]+" "+
            			 "11:"+embeddingArray1[8]+" "+
            			 "12:"+embeddingArray1[9]+" "+
            			 "13:"+embeddingArray1[10]+" "+
            			 "14:"+embeddingArray1[11]+" "+
            			 "15:"+embeddingArray1[12]+" "+
            			 "16:"+embeddingArray1[13]+" "+
            			 "17:"+embeddingArray1[14]+" "+
            			 "18:"+embeddingArray1[15]+" "+
            			 "19:"+embeddingArray1[16]+" "+
            			 "20:"+embeddingArray1[17]+" "+
            			 "21:"+embeddingArray1[18]+" "+
            			 "22:"+embeddingArray1[19]+" "+
            			 "23:"+embeddingArray1[20]+" "+
            			 "24:"+embeddingArray1[21]+" "+
            			 "25:"+embeddingArray1[22]+" "+
            			 "26:"+embeddingArray1[23]+" "+
            			 "27:"+embeddingArray1[24]+" "+
            			 "28:"+embeddingArray1[25]+" "+
            			 "29:"+embeddingArray1[26]+" "+
            			 "30:"+embeddingArray1[27]+" "+
            			 "31:"+embeddingArray1[28]+" "+
            			 "32:"+embeddingArray1[29]+" "+
            			 "33:"+embeddingArray1[30]+" "+
            			 "34:"+embeddingArray1[31]+" "+
            			 "35:"+embeddingArray1[32]+" "+
            			 "36:"+embeddingArray1[33]+" "+
            			 "37:"+embeddingArray1[34]+" "+
            			 "38:"+embeddingArray1[35]+" "+
            			 "39:"+embeddingArray1[36]+" "+
            			 "40:"+embeddingArray1[37]+" "+
            			 "41:"+embeddingArray1[38]+" "+
            			 "42:"+embeddingArray1[39]+" "+
            			 "43:"+embeddingArray1[40]+" "+
            			 "44:"+embeddingArray1[41]+" "+
            			 "45:"+embeddingArray1[42]+" "+
            			 "46:"+embeddingArray1[43]+" "+
            			 "47:"+embeddingArray1[44]+" "+
            			 "48:"+embeddingArray1[45]+" "+
            			 "49:"+embeddingArray1[46]+" "+
            			 "50:"+embeddingArray1[47]+" "+
            			 "51:"+embeddingArray1[48]+" "+
            			 "52:"+embeddingArray1[49]+" "+
            			 "53:"+embeddingArray1[50]+" "+
            			 "54:"+embeddingArray1[51]+" "+
            			 "55:"+embeddingArray1[52]+" "+
            			 "56:"+embeddingArray1[53]+" "+
            			 "57:"+embeddingArray1[54]+" "+
            			 "58:"+embeddingArray1[55]+" "+
            			 "59:"+embeddingArray1[56]+" "+
            			 "60:"+embeddingArray1[57]+" "+
            			 "61:"+embeddingArray1[58]+" "+
            			 "62:"+embeddingArray1[59]+" "+
            			 "63:"+embeddingArray1[60]+" "+
            			 "64:"+embeddingArray1[61]+" "+
            			 "65:"+embeddingArray1[62]+" "+
            			 "66:"+embeddingArray1[63]+" "+
            			 "67:"+embeddingArray1[64]+" "+
            			 "68:"+embeddingArray1[65]+" "+
            			 "69:"+embeddingArray1[66]+" "+
            			 "70:"+embeddingArray1[67]+" "+
            			 "71:"+embeddingArray1[68]+" "+
            			 "72:"+embeddingArray1[69]+" "+
            			 "73:"+embeddingArray1[70]+" "+
            			 "74:"+embeddingArray1[71]+" "+
            			 "75:"+embeddingArray1[72]+" "+
            			 "76:"+embeddingArray1[73]+" "+
            			 "77:"+embeddingArray1[74]+" "+
            			 "78:"+embeddingArray1[75]+" "+
            			 "79:"+embeddingArray1[76]+" "+
            			 "80:"+embeddingArray1[77]+" "+
            			 "81:"+embeddingArray1[78]+" "+
            			 "82:"+embeddingArray1[79]+" "+
            			 "83:"+embeddingArray1[80]+" "+
            			 "84:"+embeddingArray1[81]+" "+
            			 "85:"+embeddingArray1[82]+" "+
            			 "86:"+embeddingArray1[83]+" "+
            			 "87:"+embeddingArray1[84]+" "+
            			 "88:"+embeddingArray1[85]+" "+
            			 "89:"+embeddingArray1[86]+" "+
            			 "90:"+embeddingArray1[87]+" "+
            			 "91:"+embeddingArray1[88]+" "+
            			 "92:"+embeddingArray1[89]+" "+
            			 "93:"+embeddingArray1[90]+" "+
            			 "94:"+embeddingArray1[91]+" "+
            			 "95:"+embeddingArray1[92]+" "+
            			 "96:"+embeddingArray1[93]+" "+
            			 "97:"+embeddingArray1[94]+" "+
            			 "98:"+embeddingArray1[95]+" "+
            			 "99:"+embeddingArray1[96]+" "+
            			 "100:"+embeddingArray1[97]+" "+
            			 "101:"+embeddingArray1[98]+" "+
            			 "102:"+embeddingArray1[99]+" "+
            			 "103:"+embeddingArray1[100]+" "+
            			 "104:"+embeddingArray1[101]+" "+
            			 "105:"+embeddingArray1[102]+" "+
            			 "106:"+embeddingArray1[103]+" "+
            			 "107:"+embeddingArray1[104]+" "+
            			 "108:"+embeddingArray1[105]+" "+
            			 "109:"+embeddingArray1[106]+" "+
            			 "110:"+embeddingArray1[107]+" "+
            			 "111:"+embeddingArray1[108]+" "+
            			 "112:"+embeddingArray1[109]+" "+
            			 "113:"+embeddingArray1[110]+" "+
            			 "114:"+embeddingArray1[111]+" "+
            			 "115:"+embeddingArray1[112]+" "+
            			 "116:"+embeddingArray1[113]+" "+
            			 "117:"+embeddingArray1[114]+" "+
            			 "118:"+embeddingArray1[115]+" "+
            			 "119:"+embeddingArray1[116]+" "+
            			 "120:"+embeddingArray1[117]+" "+
            			 "121:"+embeddingArray1[118]+" "+
            			 "122:"+embeddingArray1[119]+" "+
            			 "123:"+embeddingArray1[120]+" "+
            			 "124:"+embeddingArray1[121]+" "+
            			 "125:"+embeddingArray1[122]+" "+
            			 "126:"+embeddingArray1[123]+" "+
            			 "127:"+embeddingArray1[124]+" "+
            			 "128:"+embeddingArray1[125]+" "+
            			 "129:"+embeddingArray1[126]+" "+
            			 "130:"+embeddingArray1[127]+" "+
            			 "131:"+embeddingArray1[128]+" "+
            			 "132:"+embeddingArray1[129]+" "+
            			 "133:"+embeddingArray1[130]+" "+
            			 "134:"+embeddingArray1[131]+" "+
            			 "135:"+embeddingArray1[132]+" "+
            			 "136:"+embeddingArray1[133]+" "+
            			 "137:"+embeddingArray1[134]+" "+
            			 "138:"+embeddingArray1[135]+" "+
            			 "139:"+embeddingArray1[136]+" "+
            			 "140:"+embeddingArray1[137]+" "+
            			 "141:"+embeddingArray1[138]+" "+
            			 "142:"+embeddingArray1[139]+" "+
            			 "143:"+embeddingArray1[140]+" "+
            			 "144:"+embeddingArray1[141]+" "+
            			 "145:"+embeddingArray1[142]+" "+
            			 "146:"+embeddingArray1[143]+" "+
            			 "147:"+embeddingArray1[144]+" "+
            			 "148:"+embeddingArray1[145]+" "+
            			 "149:"+embeddingArray1[146]+" "+
            			 "150:"+embeddingArray1[147]+" "+
            			 "151:"+embeddingArray1[148]+" "+
            			 "152:"+embeddingArray1[149]+" "+
            			 "153:"+embeddingArray1[150]+" "+
            			 "154:"+embeddingArray1[151]+" "+
            			 "155:"+embeddingArray1[152]+" "+
            			 "156:"+embeddingArray1[153]+" "+
            			 "157:"+embeddingArray1[154]+" "+
            			 "158:"+embeddingArray1[155]+" "+
            			 "159:"+embeddingArray1[156]+" "+
            			 "160:"+embeddingArray1[157]+" "+
            			 "161:"+embeddingArray1[158]+" "+
            			 "162:"+embeddingArray1[159]+" "+
            			 "163:"+embeddingArray1[160]+" "+
            			 "164:"+embeddingArray1[161]+" "+
            			 "165:"+embeddingArray1[162]+" "+
            			 "166:"+embeddingArray1[163]+" "+
            			 "167:"+embeddingArray1[164]+" "+
            			 "168:"+embeddingArray1[165]+" "+
            			 "169:"+embeddingArray1[166]+" "+
            			 "170:"+embeddingArray1[167]+" "+
            			 "171:"+embeddingArray1[168]+" "+
            			 "172:"+embeddingArray1[169]+" "+
            			 "173:"+embeddingArray1[170]+" "+
            			 "174:"+embeddingArray1[171]+" "+
            			 "175:"+embeddingArray1[172]+" "+
            			 "176:"+embeddingArray1[173]+" "+
            			 "177:"+embeddingArray1[174]+" "+
            			 "178:"+embeddingArray1[175]+" "+
            			 "179:"+embeddingArray1[176]+" "+
            			 "180:"+embeddingArray1[177]+" "+
            			 "181:"+embeddingArray1[178]+" "+
            			 "182:"+embeddingArray1[179]+" "+
            			 "183:"+embeddingArray1[180]+" "+
            			 "184:"+embeddingArray1[181]+" "+
            			 "185:"+embeddingArray1[182]+" "+
            			 "186:"+embeddingArray1[183]+" "+
            			 "187:"+embeddingArray1[184]+" "+
            			 "188:"+embeddingArray1[185]+" "+
            			 "189:"+embeddingArray1[186]+" "+
            			 "190:"+embeddingArray1[187]+" "+
            			 "191:"+embeddingArray1[188]+" "+
            			 "192:"+embeddingArray1[189]+" "+
            			 "193:"+embeddingArray1[190]+" "+
            			 "194:"+embeddingArray1[191]+" "+
            			 "195:"+embeddingArray1[192]+" "+
            			 "196:"+embeddingArray1[193]+" "+
            			 "197:"+embeddingArray1[194]+" "+
            			 "198:"+embeddingArray1[195]+" "+
            			 "199:"+embeddingArray1[196]+" "+
            			 "200:"+embeddingArray1[197]+" "+
            			 "201:"+embeddingArray1[198]+" "+
            			 "202:"+embeddingArray1[199]+" "+
            			 "203:"+embeddingArray1[200]+" "+
            			 "204:"+embeddingArray1[201]+" "+
            			 "205:"+embeddingArray1[202]+" "+
            			 "206:"+embeddingArray1[203]+" "+
            			 "207:"+embeddingArray1[204]+" "+
            			 "208:"+embeddingArray1[205]+" "+
            			 "209:"+embeddingArray1[206]+" "+
            			 "210:"+embeddingArray1[207]+" "+
            			 "211:"+embeddingArray1[208]+" "+
            			 "212:"+embeddingArray1[209]+" "+
            			 "213:"+embeddingArray1[210]+" "+
            			 "214:"+embeddingArray1[211]+" "+
            			 "215:"+embeddingArray1[212]+" "+
            			 "216:"+embeddingArray1[213]+" "+
            			 "217:"+embeddingArray1[214]+" "+
            			 "218:"+embeddingArray1[215]+" "+
            			 "219:"+embeddingArray1[216]+" "+
            			 "220:"+embeddingArray1[217]+" "+
            			 "221:"+embeddingArray1[218]+" "+
            			 "222:"+embeddingArray1[219]+" "+
            			 "223:"+embeddingArray1[220]+" "+
            			 "224:"+embeddingArray1[221]+" "+
            			 "225:"+embeddingArray1[222]+" "+
            			 "226:"+embeddingArray1[223]+" "+
            			 "227:"+embeddingArray1[224]+" "+
            			 "228:"+embeddingArray1[225]+" "+
            			 "229:"+embeddingArray1[226]+" "+
            			 "230:"+embeddingArray1[227]+" "+
            			 "231:"+embeddingArray1[228]+" "+
            			 "232:"+embeddingArray1[229]+" "+
            			 "233:"+embeddingArray1[230]+" "+
            			 "234:"+embeddingArray1[231]+" "+
            			 "235:"+embeddingArray1[232]+" "+
            			 "236:"+embeddingArray1[233]+" "+
            			 "237:"+embeddingArray1[234]+" "+
            			 "238:"+embeddingArray1[235]+" "+
            			 "239:"+embeddingArray1[236]+" "+
            			 "240:"+embeddingArray1[237]+" "+
            			 "241:"+embeddingArray1[238]+" "+
            			 "242:"+embeddingArray1[239]+" "+
            			 "243:"+embeddingArray1[240]+" "+
            			 "244:"+embeddingArray1[241]+" "+
            			 "245:"+embeddingArray1[242]+" "+
            			 "246:"+embeddingArray1[243]+" "+
            			 "247:"+embeddingArray1[244]+" "+
            			 "248:"+embeddingArray1[245]+" "+
            			 "249:"+embeddingArray1[246]+" "+
            			 "250:"+embeddingArray1[247]+" "+
            			 "251:"+embeddingArray1[248]+" "+
            			 "252:"+embeddingArray1[249]+" "+
            			 "253:"+embeddingArray1[250]+" "+
            			 "254:"+embeddingArray1[251]+" "+
            			 "255:"+embeddingArray1[252]+" "+
            			 "256:"+embeddingArray1[253]+" "+
            			 "257:"+embeddingArray1[254]+" "+
            			 "258:"+embeddingArray1[255]+" "+
            			 "259:"+embeddingArray1[256]+" "+
            			 "260:"+embeddingArray1[257]+" "+
            			 "261:"+embeddingArray1[258]+" "+
            			 "262:"+embeddingArray1[259]+" "+
            			 "263:"+embeddingArray1[260]+" "+
            			 "264:"+embeddingArray1[261]+" "+
            			 "265:"+embeddingArray1[262]+" "+
            			 "266:"+embeddingArray1[263]+" "+
            			 "267:"+embeddingArray1[264]+" "+
            			 "268:"+embeddingArray1[265]+" "+
            			 "269:"+embeddingArray1[266]+" "+
            			 "270:"+embeddingArray1[267]+" "+
            			 "271:"+embeddingArray1[268]+" "+
            			 "272:"+embeddingArray1[269]+" "+
            			 "273:"+embeddingArray1[270]+" "+
            			 "274:"+embeddingArray1[271]+" "+
            			 "275:"+embeddingArray1[272]+" "+
            			 "276:"+embeddingArray1[273]+" "+
            			 "277:"+embeddingArray1[274]+" "+
            			 "278:"+embeddingArray1[275]+" "+
            			 "279:"+embeddingArray1[276]+" "+
            			 "280:"+embeddingArray1[277]+" "+
            			 "281:"+embeddingArray1[278]+" "+
            			 "282:"+embeddingArray1[279]+" "+
            			 "283:"+embeddingArray1[280]+" "+
            			 "284:"+embeddingArray1[281]+" "+
            			 "285:"+embeddingArray1[282]+" "+
            			 "286:"+embeddingArray1[283]+" "+
            			 "287:"+embeddingArray1[284]+" "+
            			 "288:"+embeddingArray1[285]+" "+
            			 "289:"+embeddingArray1[286]+" "+
            			 "290:"+embeddingArray1[287]+" "+
            			 "291:"+embeddingArray1[288]+" "+
            			 "292:"+embeddingArray1[289]+" "+
            			 "293:"+embeddingArray1[290]+" "+
            			 "294:"+embeddingArray1[291]+" "+
            			 "295:"+embeddingArray1[292]+" "+
            			 "296:"+embeddingArray1[293]+" "+
            			 "297:"+embeddingArray1[294]+" "+
            			 "298:"+embeddingArray1[295]+" "+
            			 "299:"+embeddingArray1[296]+" "+
            			 "300:"+embeddingArray1[297]+" "+
            			 "301:"+embeddingArray1[298]+" "+
            			 "302:"+embeddingArray1[299]+" "+
            			 "303:"+embeddingArray2[0]+" "+
            			 "304:"+embeddingArray2[1]+" "+
            			 "305:"+embeddingArray2[2]+" "+
            			 "306:"+embeddingArray2[3]+" "+
            			 "307:"+embeddingArray2[4]+" "+
            			 "308:"+embeddingArray2[5]+" "+
            			 "309:"+embeddingArray2[6]+" "+
            			 "310:"+embeddingArray2[7]+" "+
            			 "311:"+embeddingArray2[8]+" "+
            			 "312:"+embeddingArray2[9]+" "+
            			 "313:"+embeddingArray2[10]+" "+
            			 "314:"+embeddingArray2[11]+" "+
            			 "315:"+embeddingArray2[12]+" "+
            			 "316:"+embeddingArray2[13]+" "+
            			 "317:"+embeddingArray2[14]+" "+
            			 "318:"+embeddingArray2[15]+" "+
            			 "319:"+embeddingArray2[16]+" "+
            			 "320:"+embeddingArray2[17]+" "+
            			 "321:"+embeddingArray2[18]+" "+
            			 "322:"+embeddingArray2[19]+" "+
            			 "323:"+embeddingArray2[20]+" "+
            			 "324:"+embeddingArray2[21]+" "+
            			 "325:"+embeddingArray2[22]+" "+
            			 "326:"+embeddingArray2[23]+" "+
            			 "327:"+embeddingArray2[24]+" "+
            			 "328:"+embeddingArray2[25]+" "+
            			 "329:"+embeddingArray2[26]+" "+
            			 "330:"+embeddingArray2[27]+" "+
            			 "331:"+embeddingArray2[28]+" "+
            			 "332:"+embeddingArray2[29]+" "+
            			 "333:"+embeddingArray2[30]+" "+
            			 "334:"+embeddingArray2[31]+" "+
            			 "335:"+embeddingArray2[32]+" "+
            			 "336:"+embeddingArray2[33]+" "+
            			 "337:"+embeddingArray2[34]+" "+
            			 "338:"+embeddingArray2[35]+" "+
            			 "339:"+embeddingArray2[36]+" "+
            			 "340:"+embeddingArray2[37]+" "+
            			 "341:"+embeddingArray2[38]+" "+
            			 "342:"+embeddingArray2[39]+" "+
            			 "343:"+embeddingArray2[40]+" "+
            			 "344:"+embeddingArray2[41]+" "+
            			 "345:"+embeddingArray2[42]+" "+
            			 "346:"+embeddingArray2[43]+" "+
            			 "347:"+embeddingArray2[44]+" "+
            			 "348:"+embeddingArray2[45]+" "+
            			 "349:"+embeddingArray2[46]+" "+
            			 "350:"+embeddingArray2[47]+" "+
            			 "351:"+embeddingArray2[48]+" "+
            			 "352:"+embeddingArray2[49]+" "+
            			 "353:"+embeddingArray2[50]+" "+
            			 "354:"+embeddingArray2[51]+" "+
            			 "355:"+embeddingArray2[52]+" "+
            			 "356:"+embeddingArray2[53]+" "+
            			 "357:"+embeddingArray2[54]+" "+
            			 "358:"+embeddingArray2[55]+" "+
            			 "359:"+embeddingArray2[56]+" "+
            			 "360:"+embeddingArray2[57]+" "+
            			 "361:"+embeddingArray2[58]+" "+
            			 "362:"+embeddingArray2[59]+" "+
            			 "363:"+embeddingArray2[60]+" "+
            			 "364:"+embeddingArray2[61]+" "+
            			 "365:"+embeddingArray2[62]+" "+
            			 "366:"+embeddingArray2[63]+" "+
            			 "367:"+embeddingArray2[64]+" "+
            			 "368:"+embeddingArray2[65]+" "+
            			 "369:"+embeddingArray2[66]+" "+
            			 "370:"+embeddingArray2[67]+" "+
            			 "371:"+embeddingArray2[68]+" "+
            			 "372:"+embeddingArray2[69]+" "+
            			 "373:"+embeddingArray2[70]+" "+
            			 "374:"+embeddingArray2[71]+" "+
            			 "375:"+embeddingArray2[72]+" "+
            			 "376:"+embeddingArray2[73]+" "+
            			 "377:"+embeddingArray2[74]+" "+
            			 "378:"+embeddingArray2[75]+" "+
            			 "379:"+embeddingArray2[76]+" "+
            			 "380:"+embeddingArray2[77]+" "+
            			 "381:"+embeddingArray2[78]+" "+
            			 "382:"+embeddingArray2[79]+" "+
            			 "383:"+embeddingArray2[80]+" "+
            			 "384:"+embeddingArray2[81]+" "+
            			 "385:"+embeddingArray2[82]+" "+
            			 "386:"+embeddingArray2[83]+" "+
            			 "387:"+embeddingArray2[84]+" "+
            			 "388:"+embeddingArray2[85]+" "+
            			 "389:"+embeddingArray2[86]+" "+
            			 "390:"+embeddingArray2[87]+" "+
            			 "391:"+embeddingArray2[88]+" "+
            			 "392:"+embeddingArray2[89]+" "+
            			 "393:"+embeddingArray2[90]+" "+
            			 "394:"+embeddingArray2[91]+" "+
            			 "395:"+embeddingArray2[92]+" "+
            			 "396:"+embeddingArray2[93]+" "+
            			 "397:"+embeddingArray2[94]+" "+
            			 "398:"+embeddingArray2[95]+" "+
            			 "399:"+embeddingArray2[96]+" "+
            			 "400:"+embeddingArray2[97]+" "+
            			 "401:"+embeddingArray2[98]+" "+
            			 "402:"+embeddingArray2[99]+" "+
            			 "403:"+embeddingArray2[100]+" "+
            			 "404:"+embeddingArray2[101]+" "+
            			 "405:"+embeddingArray2[102]+" "+
            			 "406:"+embeddingArray2[103]+" "+
            			 "407:"+embeddingArray2[104]+" "+
            			 "408:"+embeddingArray2[105]+" "+
            			 "409:"+embeddingArray2[106]+" "+
            			 "410:"+embeddingArray2[107]+" "+
            			 "411:"+embeddingArray2[108]+" "+
            			 "412:"+embeddingArray2[109]+" "+
            			 "413:"+embeddingArray2[110]+" "+
            			 "414:"+embeddingArray2[111]+" "+
            			 "415:"+embeddingArray2[112]+" "+
            			 "416:"+embeddingArray2[113]+" "+
            			 "417:"+embeddingArray2[114]+" "+
            			 "418:"+embeddingArray2[115]+" "+
            			 "419:"+embeddingArray2[116]+" "+
            			 "420:"+embeddingArray2[117]+" "+
            			 "421:"+embeddingArray2[118]+" "+
            			 "422:"+embeddingArray2[119]+" "+
            			 "423:"+embeddingArray2[120]+" "+
            			 "424:"+embeddingArray2[121]+" "+
            			 "425:"+embeddingArray2[122]+" "+
            			 "426:"+embeddingArray2[123]+" "+
            			 "427:"+embeddingArray2[124]+" "+
            			 "428:"+embeddingArray2[125]+" "+
            			 "429:"+embeddingArray2[126]+" "+
            			 "430:"+embeddingArray2[127]+" "+
            			 "431:"+embeddingArray2[128]+" "+
            			 "432:"+embeddingArray2[129]+" "+
            			 "433:"+embeddingArray2[130]+" "+
            			 "434:"+embeddingArray2[131]+" "+
            			 "435:"+embeddingArray2[132]+" "+
            			 "436:"+embeddingArray2[133]+" "+
            			 "437:"+embeddingArray2[134]+" "+
            			 "438:"+embeddingArray2[135]+" "+
            			 "439:"+embeddingArray2[136]+" "+
            			 "440:"+embeddingArray2[137]+" "+
            			 "441:"+embeddingArray2[138]+" "+
            			 "442:"+embeddingArray2[139]+" "+
            			 "443:"+embeddingArray2[140]+" "+
            			 "444:"+embeddingArray2[141]+" "+
            			 "445:"+embeddingArray2[142]+" "+
            			 "446:"+embeddingArray2[143]+" "+
            			 "447:"+embeddingArray2[144]+" "+
            			 "448:"+embeddingArray2[145]+" "+
            			 "449:"+embeddingArray2[146]+" "+
            			 "450:"+embeddingArray2[147]+" "+
            			 "451:"+embeddingArray2[148]+" "+
            			 "452:"+embeddingArray2[149]+" "+
            			 "453:"+embeddingArray2[150]+" "+
            			 "454:"+embeddingArray2[151]+" "+
            			 "455:"+embeddingArray2[152]+" "+
            			 "456:"+embeddingArray2[153]+" "+
            			 "457:"+embeddingArray2[154]+" "+
            			 "458:"+embeddingArray2[155]+" "+
            			 "459:"+embeddingArray2[156]+" "+
            			 "460:"+embeddingArray2[157]+" "+
            			 "461:"+embeddingArray2[158]+" "+
            			 "462:"+embeddingArray2[159]+" "+
            			 "463:"+embeddingArray2[160]+" "+
            			 "464:"+embeddingArray2[161]+" "+
            			 "465:"+embeddingArray2[162]+" "+
            			 "466:"+embeddingArray2[163]+" "+
            			 "467:"+embeddingArray2[164]+" "+
            			 "468:"+embeddingArray2[165]+" "+
            			 "469:"+embeddingArray2[166]+" "+
            			 "470:"+embeddingArray2[167]+" "+
            			 "471:"+embeddingArray2[168]+" "+
            			 "472:"+embeddingArray2[169]+" "+
            			 "473:"+embeddingArray2[170]+" "+
            			 "474:"+embeddingArray2[171]+" "+
            			 "475:"+embeddingArray2[172]+" "+
            			 "476:"+embeddingArray2[173]+" "+
            			 "477:"+embeddingArray2[174]+" "+
            			 "478:"+embeddingArray2[175]+" "+
            			 "479:"+embeddingArray2[176]+" "+
            			 "480:"+embeddingArray2[177]+" "+
            			 "481:"+embeddingArray2[178]+" "+
            			 "482:"+embeddingArray2[179]+" "+
            			 "483:"+embeddingArray2[180]+" "+
            			 "484:"+embeddingArray2[181]+" "+
            			 "485:"+embeddingArray2[182]+" "+
            			 "486:"+embeddingArray2[183]+" "+
            			 "487:"+embeddingArray2[184]+" "+
            			 "488:"+embeddingArray2[185]+" "+
            			 "489:"+embeddingArray2[186]+" "+
            			 "490:"+embeddingArray2[187]+" "+
            			 "491:"+embeddingArray2[188]+" "+
            			 "492:"+embeddingArray2[189]+" "+
            			 "493:"+embeddingArray2[190]+" "+
            			 "494:"+embeddingArray2[191]+" "+
            			 "495:"+embeddingArray2[192]+" "+
            			 "496:"+embeddingArray2[193]+" "+
            			 "497:"+embeddingArray2[194]+" "+
            			 "498:"+embeddingArray2[195]+" "+
            			 "499:"+embeddingArray2[196]+" "+
            			 "500:"+embeddingArray2[197]+" "+
            			 "501:"+embeddingArray2[198]+" "+
            			 "502:"+embeddingArray2[199]+" "+
            			 "503:"+embeddingArray2[200]+" "+
            			 "504:"+embeddingArray2[201]+" "+
            			 "505:"+embeddingArray2[202]+" "+
            			 "506:"+embeddingArray2[203]+" "+
            			 "507:"+embeddingArray2[204]+" "+
            			 "508:"+embeddingArray2[205]+" "+
            			 "509:"+embeddingArray2[206]+" "+
            			 "510:"+embeddingArray2[207]+" "+
            			 "511:"+embeddingArray2[208]+" "+
            			 "512:"+embeddingArray2[209]+" "+
            			 "513:"+embeddingArray2[210]+" "+
            			 "514:"+embeddingArray2[211]+" "+
            			 "515:"+embeddingArray2[212]+" "+
            			 "516:"+embeddingArray2[213]+" "+
            			 "517:"+embeddingArray2[214]+" "+
            			 "518:"+embeddingArray2[215]+" "+
            			 "519:"+embeddingArray2[216]+" "+
            			 "520:"+embeddingArray2[217]+" "+
            			 "521:"+embeddingArray2[218]+" "+
            			 "522:"+embeddingArray2[219]+" "+
            			 "523:"+embeddingArray2[220]+" "+
            			 "524:"+embeddingArray2[221]+" "+
            			 "525:"+embeddingArray2[222]+" "+
            			 "526:"+embeddingArray2[223]+" "+
            			 "527:"+embeddingArray2[224]+" "+
            			 "528:"+embeddingArray2[225]+" "+
            			 "529:"+embeddingArray2[226]+" "+
            			 "530:"+embeddingArray2[227]+" "+
            			 "531:"+embeddingArray2[228]+" "+
            			 "532:"+embeddingArray2[229]+" "+
            			 "533:"+embeddingArray2[230]+" "+
            			 "534:"+embeddingArray2[231]+" "+
            			 "535:"+embeddingArray2[232]+" "+
            			 "536:"+embeddingArray2[233]+" "+
            			 "537:"+embeddingArray2[234]+" "+
            			 "538:"+embeddingArray2[235]+" "+
            			 "539:"+embeddingArray2[236]+" "+
            			 "540:"+embeddingArray2[237]+" "+
            			 "541:"+embeddingArray2[238]+" "+
            			 "542:"+embeddingArray2[239]+" "+
            			 "543:"+embeddingArray2[240]+" "+
            			 "544:"+embeddingArray2[241]+" "+
            			 "545:"+embeddingArray2[242]+" "+
            			 "546:"+embeddingArray2[243]+" "+
            			 "547:"+embeddingArray2[244]+" "+
            			 "548:"+embeddingArray2[245]+" "+
            			 "549:"+embeddingArray2[246]+" "+
            			 "550:"+embeddingArray2[247]+" "+
            			 "551:"+embeddingArray2[248]+" "+
            			 "552:"+embeddingArray2[249]+" "+
            			 "553:"+embeddingArray2[250]+" "+
            			 "554:"+embeddingArray2[251]+" "+
            			 "555:"+embeddingArray2[252]+" "+
            			 "556:"+embeddingArray2[253]+" "+
            			 "557:"+embeddingArray2[254]+" "+
            			 "558:"+embeddingArray2[255]+" "+
            			 "559:"+embeddingArray2[256]+" "+
            			 "560:"+embeddingArray2[257]+" "+
            			 "561:"+embeddingArray2[258]+" "+
            			 "562:"+embeddingArray2[259]+" "+
            			 "563:"+embeddingArray2[260]+" "+
            			 "564:"+embeddingArray2[261]+" "+
            			 "565:"+embeddingArray2[262]+" "+
            			 "566:"+embeddingArray2[263]+" "+
            			 "567:"+embeddingArray2[264]+" "+
            			 "568:"+embeddingArray2[265]+" "+
            			 "569:"+embeddingArray2[266]+" "+
            			 "570:"+embeddingArray2[267]+" "+
            			 "571:"+embeddingArray2[268]+" "+
            			 "572:"+embeddingArray2[269]+" "+
            			 "573:"+embeddingArray2[270]+" "+
            			 "574:"+embeddingArray2[271]+" "+
            			 "575:"+embeddingArray2[272]+" "+
            			 "576:"+embeddingArray2[273]+" "+
            			 "577:"+embeddingArray2[274]+" "+
            			 "578:"+embeddingArray2[275]+" "+
            			 "579:"+embeddingArray2[276]+" "+
            			 "580:"+embeddingArray2[277]+" "+
            			 "581:"+embeddingArray2[278]+" "+
            			 "582:"+embeddingArray2[279]+" "+
            			 "583:"+embeddingArray2[280]+" "+
            			 "584:"+embeddingArray2[281]+" "+
            			 "585:"+embeddingArray2[282]+" "+
            			 "586:"+embeddingArray2[283]+" "+
            			 "587:"+embeddingArray2[284]+" "+
            			 "588:"+embeddingArray2[285]+" "+
            			 "589:"+embeddingArray2[286]+" "+
            			 "590:"+embeddingArray2[287]+" "+
            			 "591:"+embeddingArray2[288]+" "+
            			 "592:"+embeddingArray2[289]+" "+
            			 "593:"+embeddingArray2[290]+" "+
            			 "594:"+embeddingArray2[291]+" "+
            			 "595:"+embeddingArray2[292]+" "+
            			 "596:"+embeddingArray2[293]+" "+
            			 "597:"+embeddingArray2[294]+" "+
            			 "598:"+embeddingArray2[295]+" "+
            			 "599:"+embeddingArray2[296]+" "+
            			 "600:"+embeddingArray2[297]+" "+
            			 "601:"+embeddingArray2[298]+" "+
            			 "602:"+embeddingArray2[299]+" ");
             }
             
             List<String> liness = lstTrain;
             Path files = Paths.get("train.txt.train");
             Files.write(files, liness, StandardCharsets.UTF_8);
             
			 session.close();
		
		 
	}
		 driver.close();
	}
	
//---------------------------------------------------------------------------------------------------------------
	public void CreateSecondModel(String tag) throws XGBoostError, IOException 
	{
		Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "123"));
		
		String[] arrayTest = new String[]{};
		String[] arrayTrain = new String[]{};
		List<String> lstTrain = new ArrayList<String>();
		List<String> lstTest = new ArrayList<String>();               
		
		 try (Session session = driver.session()) {
			 List<Record> listOfTestData = session.run("MATCH (a:Test) RETURN  a.data AS data").list();
			 List<Record> listOfTrainData = session.run("MATCH (a:Train) RETURN  a.data AS data").list();

			 arrayTest = listOfTestData.get(0).get("data").toString().split(" ");
			 arrayTrain = listOfTrainData.get(0).get("data").toString().split(" ");
			 
			 
				
				long[] rowHeadersTest = new long[603*arrayTest.length];
		        float[] dataTest = new float[603*arrayTest.length];
		        int[] colIndexTest = new int[603*arrayTest.length];
		        float[] labelsTest = new float[arrayTest.length];  
		        
		        long[] rowHeadersTrain = new long[603*arrayTrain.length];
		        float[] dataTrain = new float[603*arrayTrain.length];
		        int[] colIndexTrain = new int[603*arrayTrain.length];
		        float[] labelsTrain = new float[arrayTrain.length];
		        
			 String label = "";
			 rowHeadersTest[0]=0;
			 int h = 0;
	         for(int i= 0;i < arrayTest.length ;i++)
	         {
	        	 String[] str = arrayTest[i].replace("\"", "").replace("[", "").replace("]", "").split(",");
	        	 List<Record> embeddingPageID1 = session.run("MATCH (n:page) WHERE n.PageID = {pageID} RETURN n.Embedding AS Embedding",Values.parameters("pageID",Integer.parseInt(str[0]))).list();
	        	 List<Record> embeddingPageID2 = session.run("MATCH (n:page) WHERE n.PageID = {pageID} RETURN n.Embedding AS Embedding",Values.parameters("pageID",Integer.parseInt(str[1]))).list();
	        	 String[] embedArray1 = embeddingPageID1.get(0).get("Embedding").toString().replace("\"", "").split(",");
	        	 String[] embedArray2 = embeddingPageID2.get(0).get("Embedding").toString().replace("\"", "").split(",");
	        	 
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
	        	 
	        	 
	        	 
	        	 String[] embeddingArray1 = Arrays.copyOf(embedArray1, 301);
	        	 String[] embeddingArray2 = Arrays.copyOf(embedArray2, 301);
	        	 for(int k= embedArray1.length-1;k<301;k++)
	        	 {
	        		 embeddingArray1[k]="1";
	        	 }
	        	 for(int k= embedArray2.length-1;k<301;k++)
	        	 {
	        		 embeddingArray2[k]="1";
	        	 }
	        	 
	        	 if(h < 603*arrayTest.length)
	        	 {
		        	 dataTest[h] = Float.parseFloat(label);
		        	 h = h + 1;
		        	 dataTest[h] = Float.parseFloat(str[0]);
		        	 h = h + 1;
		        	 dataTest[h] = Float.parseFloat(str[1]);
		        	 h = h + 1;
		        	 for(int k= 0;k<300;k++)
		        	 {
		        		 dataTest[h+k]=Float.parseFloat(embeddingArray1[k]);
		        	 }
		        	 h = h + 301;
		        	 //System.out.println("h..." + h);
		        	 for(int k= 0;k<300;k++)
		        	 {
		        		 if(h+k < (603*arrayTest.length))
		        		 {
		        		    dataTest[h+k]=Float.parseFloat(embeddingArray2[k]);
		        		 }
		        	 }
		        	 h = h + 301;
		        	 
		        	 
		        	 rowHeadersTest[i+1] = rowHeadersTest[i]+603;
	        	 }
	         }
	         
	         int b = 0;
	         for(int l = 0;l < dataTest.length;l++)
        	 {
	        	 if(b == 603)
	        	 {
	        		 b = 0;
        		    colIndexTrain[l]= 0;
	        	 }
	        	 else
	        	 {
	        		 colIndexTrain[l]= b;
	        	 }
	        	 b = b + 1;
        	 }
	         
	         //Traindata
	         label = "";
			 rowHeadersTrain[0]=0;
			 h = 0;
	         for(int i= 0;i < arrayTrain.length ;i++)
	         {
	        	 String[] str = arrayTrain[i].replace("\"", "").replace("[", "").replace("]", "").split(",");
	        	 List<Record> embeddingPageID1 = session.run("MATCH (n:page) WHERE n.PageID = {pageID} RETURN n.Embedding AS Embedding",Values.parameters("pageID",Integer.parseInt(str[0]))).list();
	        	 List<Record> embeddingPageID2 = session.run("MATCH (n:page) WHERE n.PageID = {pageID} RETURN n.Embedding AS Embedding",Values.parameters("pageID",Integer.parseInt(str[1]))).list();
	        	 String[] embedArray1 = embeddingPageID1.get(0).get("Embedding").toString().replace("\"", "").split(",");
	        	 String[] embedArray2 = embeddingPageID2.get(0).get("Embedding").toString().replace("\"", "").split(",");
	        	 
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
	        	 
	        	 if(h < 603*arrayTrain.length)
	        	 {
		        	 dataTrain[h] = Float.parseFloat(label);
		        	 h = h + 1;
		        	 dataTrain[h] = Float.parseFloat(str[0]);
		        	 h = h + 1;
		        	 dataTrain[h] = Float.parseFloat(str[1]);
		        	 h = h + 1;
		        	 for(int k= 0;k<300;k++)
		        	 {
		        		 dataTrain[h+k]=Float.parseFloat(embeddingArray1[k]);
		        	 }
		        	 h = h + 301;
		        	 //System.out.println("h..." + h);
		        	 for(int k= 0;k<300;k++)
		        	 {
		        		 if(h+k < (603*arrayTest.length))
		        		 {
		        		    dataTrain[h+k]=Float.parseFloat(embeddingArray2[k]);
		        		 }
		        	 }
		        	 h = h + 301;
		        	 
		        	 
		        	// rowHeadersTrain[i+1] = rowHeadersTrain[i]+603;
	        	 }
	         }
	         
	         /*
	         b = 0;
	         for(int l = 0;l < dataTrain.length;l++)
        	 {
	        	 if(b == 603)
	        	 {
	        		 b = 0;
        		    colIndexTrain[l]= 0;
	        	 }
	        	 else
	        	 {
	        		 colIndexTrain[l]= b;
	        	 }
	        	 b = b + 1;
        	 }
	         */
	         
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
	 				booster.saveModel("model.bin");
	 				// dump with feature map
	 				String[] model_dump_with_feature_map = booster.getModelDump("featureMap.txt", false);
	 			    booster = XGBoost.loadModel("model.bin");
	 			    
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
	 				SaveModelToNeo4j(session,tag,statisticInfo);
	 				
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
public void SaveModelToNeo4j(Session session,String tag,float[] statisticInfo) throws IOException
{
	String fileName = "model.bin";
	Path path = Paths.get(fileName);
	byte[] bytes = Files.readAllBytes(path);
	 if(bytes.length > 0)
	 {
		 session.run("CREATE (a:Model { tag: {tag}, modelData:{data}, accuracy:{accuracy}, precision:{precision}, errorRate:{errorRate}, recall:{recall}, Fscore:{Fscore}})",
				 Values.parameters("tag",tag, "data", bytes,"accuracy",statisticInfo[0],"precision",statisticInfo[1],"errorRate",statisticInfo[2],"recall",statisticInfo[3],"Fscore",statisticInfo[4]));
	 }
}	
}
//-------------------------------------------------------------------------------------------------------------


