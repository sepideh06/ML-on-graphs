import pandas as pd
import random
from timeit import default_timer as timer
import itertools
from py2neo import Graph, Node, Relationship


dicPageIDName = {}
lstPageIDPageID = []
dicPageIDNeighboursCount = {}
training_data_exist = []
training_data_nonexist = []
dicPageComunity = {}

graph = Graph(password = "123")
#-------------------------------------------------------------------------------------------------------------------------------------
def SeperateAndStoreTrainTestData():

        lstpageIDs = []
        lstNonExistPagelinks = []
        lstbothPageID = []

        dfPageInfo = pd.DataFrame(graph.data("MATCH (a:page) RETURN a.PageID, a.Name,a.CommunityID,a.Embedding"))
        #print(df1.iloc[0][0]) CommunityID
        #print(df1.iloc[0][1]) Embedding
        #print(df1.iloc[0][2]) Name
        #print(df1.iloc[0][3]) PageID

        dfLinkedPages = pd.DataFrame(graph.data("MATCH (a:page)-[r:LINKS_TO]-(b:page) RETURN distinct a.PageID,b.PageID"))

        for j in range(0, len(dfLinkedPages)):
            if dfLinkedPages.iloc[j][0] not in lstpageIDs:
                lstpageIDs.append(dfLinkedPages.iloc[j][0])
            if dfLinkedPages.iloc[j][1] not in lstpageIDs:
                lstpageIDs.append(dfLinkedPages.iloc[j][1])
            lstbothPageID.append([dfLinkedPages.iloc[j][0],dfLinkedPages.iloc[j][1]])

        print("Existing links are being extracted:")
        print("number of all existing links:" + str(len(lstbothPageID)))
        # read comunity of each page
        for j in range(0, len(dfPageInfo)):
            if (dfPageInfo.iloc[j][3] in lstpageIDs):
                pageIdList = []
                if (dicPageComunity.get(dfPageInfo.iloc[j][0]) == None):
                    pageIdList = [dfPageInfo.iloc[j][3]]
                else:
                    pageIdList = dicPageComunity[dfPageInfo.iloc[j][0]]
                    pageIdList = pageIdList + [dfPageInfo.iloc[j][3]]
                dicPageComunity[dfPageInfo.iloc[j][0]] = pageIdList


        # Cartesian product for noneexisting links
        print("Total number of all created links(exist,non-exist) before applying rule:" + " " + str(len(list(itertools.product(lstpageIDs, lstpageIDs)))))
        for i in itertools.product(lstpageIDs, lstpageIDs):
            sameCommunityflag, communityId = ComparingPageCommunityId(dicPageComunity, i[0], i[1])
            if (i[0] != i[1] and ([i[0],i[1]] not in lstbothPageID) and (sameCommunityflag == True)):
                lstNonExistPagelinks.append([i[0],i[1]])
        print("total size of non-exist links which fall into same community:" + " " + str(len(lstNonExistPagelinks)))
        #print(lstNonExistPagelinks)
        # number of links in each community(exist and not exist links in each community)

        Numberoflinksfallinthesamecomunity = {}
        NumberoflinksNotfallinthesamecomunity = {}

        for key in dicPageComunity.keys():
            for i in itertools.product(dicPageComunity[key], dicPageComunity[key]):
                # if pageIds in training data contains i
                if ([i[0],i[1]] in lstbothPageID):
                    if (i[0] != i[1]):
                        if (Numberoflinksfallinthesamecomunity.get(key) == None):
                            Numberoflinksfallinthesamecomunity[key] = 0
                        else:
                            Numberoflinksfallinthesamecomunity[key] = Numberoflinksfallinthesamecomunity.get(key) + 1
                else:
                    if (i[0] != i[1]):
                        if (NumberoflinksNotfallinthesamecomunity.get(key) == None):
                            NumberoflinksNotfallinthesamecomunity[key] = 0
                        else:
                            NumberoflinksNotfallinthesamecomunity[key] = NumberoflinksNotfallinthesamecomunity.get(key) + 1

        # print("Number of all links:" + " " + str(len(lstbothPageID)))
        print("Number of links fall in the same comunity:" + " " + str(
            Numberoflinksfallinthesamecomunity))
        print("Number of non-exist links fall in the same comunity:" + " " + str(
            NumberoflinksNotfallinthesamecomunity))
       #----------------------------------------------------------------------------------------------------------------
        #Here i sepearet train & test data
        # I already have a list of connected pages(in lstbothPageID) and a list of non-existing in(lstNonExistPagelinks)

        splitRatio = 0.67
        print('Train & Test data is being seperated:')
        #before seperation i would like to add label (exist,non-exist) to our data to diffrentiate
        for i in range(0,len(lstbothPageID)):
            lstbothPageID[i].append('exist')

        for i in range(0,len(lstNonExistPagelinks)):
            lstNonExistPagelinks[i].append('nonexist')

        train0, test0 = splitDataset(lstbothPageID, splitRatio)
        train1, test1 = splitDataset(lstNonExistPagelinks, splitRatio)

        train = train0 + train1
        test = test0 + test1

        print("size of test data:" + " " + str(len(test)))
        print("size of train data:" + " " + str(len(train)))

        #---------------------------------------------------------------------------------------------------------------
        #Here we will update the label of edges to 'exist' or 'non-exist' and to 'train' or 'test' and
        #We will add non-exist links as well

        print('Relationships & properties are being added or updated:')
        StoreTrainTestDataIntoNeo4j(test,train)

# for rel in graph.match(rel_type="LINKS_TO", bidirectional=False):
# print(str(rel.start_node()["PageID"]) + "  : " + str(rel.end_node()["PageID"]))
#----------------------------------------------------------------------------------------------------------------------------------------
def ComparingPageCommunityId(pageCommunityDictionary,pageId1,pageId2):

    communityId1 = ''
    communityId2 = ''
    for key,value in pageCommunityDictionary.items():
        if(pageId1 in value):
            communityId1 = key
        if(pageId2 in value):
             communityId2 = key
    if(communityId1 == communityId2):
        return True,communityId1
    else:
        return False,-1
#--------------------------------------------------------------------------------------------------------------------------------------------
def StoreTrainTestDataIntoNeo4j(test,train):

    for i in range(0,len(train)):
        if train[i][2] == 'exist':
            updateRelationshipaddProperty(train[i],'train')
        else:
            addNewRelationshipaddProperty(train[i],'train')

    for i in range(0, len(test)):
        if test[i][2] == 'exist':
            updateRelationshipaddProperty(test[i], 'test')
        else:
            addNewRelationshipaddProperty(test[i], 'test')
#-----------------------------------------------------------------------------------------------------------------------------------------------
def updateRelationshipaddProperty(row,edgeType):
    #update relationship
    # add a property to relationship which can be  'train' or 'test'
    graph.run("MATCH (n:page {PageID:{PageID1}})-[r:LINKS_TO]->(m:page {PageID:{PageID2}}) CREATE (n)- [r2:exist { edgeType:{EdgeType}}]-> (m) SET r2 = r WITH r DELETE r",PageID1=int(row[0]),PageID2=int(row[1]),EdgeType=edgeType)
#------------------------------------------------------------------------------------------------------------------------------------------------
def addNewRelationshipaddProperty(row,edgeType):
    # create new relationship
    # add a property to relationship which can be  'train' or 'test'
    graph.run("MATCH(page1: page {PageID: {PageID1}}), (page2: page {PageID: {PageID2}}) CREATE (page1)- [r: nonexist {edgeType:{EdgeType}}]-> (page2)",PageID1=int(row[0]), PageID2=int(row[1]),EdgeType=edgeType)
#------------------------------------------------------------------------------------------------------------------------------------------------
"""randomly Splitting our dataset with a ratio of 67% train and 33% test """
def splitDataset(training_data,splitRatio):
	trainSize = int(len(training_data) * splitRatio)
	trainSet = []
	copy = list(training_data)
	while len(trainSet) < trainSize:
		index = random.randrange(len(copy))
		trainSet.append(copy.pop(index))

	return [trainSet, copy]
#--------------------------------------------------------------------------------------------------------------------------------------------
print("Process started")
start = timer()
tx = graph.begin()

SeperateAndStoreTrainTestData()

tx.commit()
end = timer()
print("Process completed, duration: %.2f seconds" % (end - start))



