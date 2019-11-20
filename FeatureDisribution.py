import csv
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import random
import os
import os.path
from timeit import default_timer as timer
import nltk
import itertools


TestExistCommonNeighbour = {}
TestNonExistCommonNeighbour = {}
TestExistShortestPathAuthorGraph = {}
TestNonExistShortestPathAuthorGraph = {}
TestExistSecondShortestPathAuthorGraph ={}
TestNonExistSecondShortestPathAuthorGraph ={}
TestExistShortestPathCitationGraph = {}
TestNonExistShortestPathCitationGraph = {}
TestExistSecondShortestPathCitationGraph ={}
TestNonExistSecondShortestPathCitationGraph ={}

TrainExistCommonNeighbour = {}
TrainNonExistCommonNeighbour = {}
TrainExistShortestPathAuthorGraph = {}
TrainNonExistShortestPathAuthorGraph = {}
TrainExistSecondShortestPathAuthorGraph ={}
TrainNonExistSecondShortestPathAuthorGraph ={}
TrainExistShortestPathCitationGraph = {}
TrainNonExistShortestPathCitationGraph = {}
TrainExistSecondShortestPathCitationGraph ={}
TrainNonExistSecondShortestPathCitationGraph ={}

#---------------------------------------------------------------------------------------------------------------------------
def SplitTestExistNonExist():
    with open("FeatureCalculation1\Test.csv", "r" , encoding="utf-8") as f:
        next(f, None)
        lines = f.readlines()
        for row in lines:
            if (row):
                row = row.replace("\r", "").replace("\n", "")
            AuthorID1, AuthorID2, CommonNeighbour, ShortestPathAuthorGraph, SecondShortestPathAuthorGraph, ShortestPathCitationGraph, SecondShortestPathCitationGraph, Label = row.split(",")
            if Label == "exists":
                if (TestExistCommonNeighbour.get(CommonNeighbour) == None):
                    TestExistCommonNeighbour[CommonNeighbour] = 1
                else:
                    TestExistCommonNeighbour[CommonNeighbour] = TestExistCommonNeighbour.get(CommonNeighbour) + 1
                #-----------------------------------------------------------------------------------------------------------
                if (TestExistShortestPathAuthorGraph.get(ShortestPathAuthorGraph) == None):
                    TestExistShortestPathAuthorGraph[ShortestPathAuthorGraph] = 1
                else:
                    TestExistShortestPathAuthorGraph[ShortestPathAuthorGraph] = TestExistShortestPathAuthorGraph.get(ShortestPathAuthorGraph) + 1
                #------------------------------------------------------------------------------------------------------------
                if (TestExistSecondShortestPathAuthorGraph.get(SecondShortestPathAuthorGraph) == None):
                    TestExistSecondShortestPathAuthorGraph[SecondShortestPathAuthorGraph] = 1
                else:
                    TestExistSecondShortestPathAuthorGraph[SecondShortestPathAuthorGraph] = TestExistSecondShortestPathAuthorGraph.get(
                        SecondShortestPathAuthorGraph) + 1
                # ------------------------------------------------------------------------------------------------------------
                if (TestExistShortestPathCitationGraph.get(ShortestPathCitationGraph) == None):
                    TestExistShortestPathCitationGraph[ShortestPathCitationGraph] = 1
                else:
                    TestExistShortestPathCitationGraph[ShortestPathCitationGraph] = TestExistShortestPathCitationGraph.get(ShortestPathCitationGraph) + 1
                 #---------------------------------------------------------------------------------------------------------------
                if (TestExistSecondShortestPathCitationGraph.get(SecondShortestPathCitationGraph) == None):
                    TestExistSecondShortestPathCitationGraph[SecondShortestPathCitationGraph] = 1
                else:
                    TestExistSecondShortestPathCitationGraph[SecondShortestPathCitationGraph] = TestExistSecondShortestPathCitationGraph.get(SecondShortestPathCitationGraph) + 1


            else :
                if (TestNonExistCommonNeighbour.get(CommonNeighbour) == None):
                    TestNonExistCommonNeighbour[CommonNeighbour] = 1
                else:
                    TestNonExistCommonNeighbour[CommonNeighbour] = TestNonExistCommonNeighbour.get(CommonNeighbour) + 1
                 #-----------------------------------------------------------------------------------------------------------
                if (TestNonExistShortestPathAuthorGraph.get(ShortestPathAuthorGraph) == None):
                    TestNonExistShortestPathAuthorGraph[ShortestPathAuthorGraph] = 1
                else:
                    TestNonExistShortestPathAuthorGraph[ShortestPathAuthorGraph] = TestNonExistShortestPathAuthorGraph.get(ShortestPathAuthorGraph) + 1
                 #-----------------------------------------------------------------------------------------------------------
                if (TestNonExistSecondShortestPathAuthorGraph.get(SecondShortestPathAuthorGraph) == None):
                    TestNonExistSecondShortestPathAuthorGraph[SecondShortestPathAuthorGraph] = 1
                else:
                    TestNonExistSecondShortestPathAuthorGraph[SecondShortestPathAuthorGraph] = TestNonExistSecondShortestPathAuthorGraph.get(SecondShortestPathAuthorGraph) + 1
                # -----------------------------------------------------------------------------------------------------------
                if (TestNonExistShortestPathCitationGraph.get(ShortestPathCitationGraph) == None):
                    TestNonExistShortestPathCitationGraph[ShortestPathCitationGraph] = 1
                else:
                    TestNonExistShortestPathCitationGraph[ShortestPathCitationGraph] = TestNonExistShortestPathCitationGraph.get(ShortestPathCitationGraph) + 1
                # ---------------------------------------------------------------------------------------------------------------
                if (TestNonExistSecondShortestPathCitationGraph.get(SecondShortestPathCitationGraph) == None):
                    TestNonExistSecondShortestPathCitationGraph[SecondShortestPathCitationGraph] = 1
                else:
                    TestNonExistSecondShortestPathCitationGraph[SecondShortestPathCitationGraph] = TestNonExistSecondShortestPathCitationGraph.get(
                        SecondShortestPathCitationGraph) + 1

    f.closed

    w = csv.writer(open("FeatureCalculation1\TestExistCommonNeighbour.csv", "w", newline=''))
    values = [float(x) for x in list(TestExistCommonNeighbour.keys())]
    keyMax = max(values)
    keyMin = min(values)
    for key, val in TestExistCommonNeighbour.items():
        #w.writerow([round((float(key) - keyMin) / (keyMax - keyMin), 3), val])
        w.writerow([key, val])

    w = csv.writer(open("FeatureCalculation1\TestNonExistCommonNeighbour.csv", "w", newline=''))
    for key, val in TestNonExistCommonNeighbour.items():
        w.writerow([key, val])

    w = csv.writer(open("FeatureCalculation1\TestExistShortestPathAuthorGraph.csv", "w", newline=''))
    values = [float(x) for x in list(TestExistShortestPathAuthorGraph.keys())]
    keyMax = max(values)
    keyMin = min(values)
    for key, val in TestExistShortestPathAuthorGraph.items():
        w.writerow([key, val])

    w = csv.writer(open("FeatureCalculation1\TestNonExistShortestPathAuthorGraph.csv", "w", newline=''))
    values = [float(x) for x in list(TestNonExistShortestPathAuthorGraph.keys())]
    keyMax = max(values)
    keyMin = min(values)
    for key, val in TestNonExistShortestPathAuthorGraph.items():
        w.writerow([key, val])

    w = csv.writer(open("FeatureCalculation1\TestExistSecondShortestPathAuthorGraph.csv", "w", newline=''))
    values = [float(x) for x in list(TestExistSecondShortestPathAuthorGraph.keys())]
    keyMax = max(values)
    keyMin = min(values)
    for key, val in TestExistSecondShortestPathAuthorGraph.items():
        w.writerow([key, val])

    w = csv.writer(open("FeatureCalculation1\TestNonExistSecondShortestPathAuthorGraph.csv", "w", newline=''))
    values = [float(x) for x in list(TestNonExistSecondShortestPathAuthorGraph.keys())]
    keyMax = max(values)
    keyMin = min(values)
    for key, val in TestNonExistSecondShortestPathAuthorGraph.items():
        w.writerow([key, val])

    w = csv.writer(open("FeatureCalculation1\TestExistShortestPathCitationGraph.csv", "w", newline=''))
    values = [float(x) for x in list(TestExistShortestPathCitationGraph.keys())]
    keyMax = max(values)
    keyMin = min(values)
    for key, val in TestExistShortestPathCitationGraph.items():
        w.writerow([key, val])

    w = csv.writer(open("FeatureCalculation1\TestNonExistShortestPathCitationGraph.csv", "w", newline=''))
    values = [float(x) for x in list(TestNonExistShortestPathCitationGraph.keys())]
    keyMax = max(values)
    keyMin = min(values)
    for key, val in TestNonExistShortestPathCitationGraph.items():
        w.writerow([key, val])

    w = csv.writer(open("FeatureCalculation1\TestExistSecondShortestPathCitationGraph.csv", "w", newline=''))
    values = [float(x) for x in list(TestExistSecondShortestPathCitationGraph.keys())]
    keyMax = max(values)
    keyMin = min(values)
    for key, val in TestExistSecondShortestPathCitationGraph.items():
        w.writerow([key, val])

    w = csv.writer(open("FeatureCalculation1\TestNonExistSecondShortestPathCitationGraph.csv", "w", newline=''))
    values = [float(x) for x in list(TestNonExistSecondShortestPathCitationGraph.keys())]
    keyMax = max(values)
    keyMin = min(values)
    for key, val in TestNonExistSecondShortestPathCitationGraph.items():
        w.writerow([key, val])

#-------------------------------------------------------------------------------------------------------------------------------
'''print(TestExistCommonNeighbour)
print(TestNonExistCommonNeighbour)
print(TestExistShortestPathAuthorGraph)
print(TestNonExistShortestPathAuthorGraph)
print(TestExistSecondShortestPathAuthorGraph)
print(TestNonExistSecondShortestPathAuthorGraph)
print(TestExistShortestPathCitationGraph)
print(TestNonExistShortestPathCitationGraph)
print(TestExistSecondShortestPathCitationGraph)
print(TestNonExistSecondShortestPathCitationGraph)'''
#--------------------------------------------------------------------------------------------------------------------------------
#---------------------------------------------------------------------------------------------------------------------------
def SplitTrainExistNonExist():
    with open("FeatureCalculation1\Train.csv", "r" , encoding="utf-8") as f:
        next(f, None)
        lines = f.readlines()
        for row in lines:
            if (row):
                row = row.replace("\r", "").replace("\n", "")
            AuthorID1, AuthorID2, CommonNeighbour, ShortestPathAuthorGraph, SecondShortestPathAuthorGraph, ShortestPathCitationGraph, SecondShortestPathCitationGraph, Label = row.split(",")
            if Label == "exists":
                if (TrainExistCommonNeighbour.get(CommonNeighbour) == None):
                    TrainExistCommonNeighbour[CommonNeighbour] = 1
                else:
                    TrainExistCommonNeighbour[CommonNeighbour] = TrainExistCommonNeighbour.get(CommonNeighbour) + 1
                #-----------------------------------------------------------------------------------------------------------
                if (TrainExistShortestPathAuthorGraph.get(ShortestPathAuthorGraph) == None):
                    TrainExistShortestPathAuthorGraph[ShortestPathAuthorGraph] = 1
                else:
                    TrainExistShortestPathAuthorGraph[ShortestPathAuthorGraph] = TrainExistShortestPathAuthorGraph.get(ShortestPathAuthorGraph) + 1
                #------------------------------------------------------------------------------------------------------------
                if (TrainExistSecondShortestPathAuthorGraph.get(SecondShortestPathAuthorGraph) == None):
                    TrainExistSecondShortestPathAuthorGraph[SecondShortestPathAuthorGraph] = 1
                else:
                    TrainExistSecondShortestPathAuthorGraph[SecondShortestPathAuthorGraph] = TrainExistSecondShortestPathAuthorGraph.get(
                        SecondShortestPathAuthorGraph) + 1
                # ------------------------------------------------------------------------------------------------------------
                if (TrainExistShortestPathCitationGraph.get(ShortestPathCitationGraph) == None):
                    TrainExistShortestPathCitationGraph[ShortestPathCitationGraph] = 1
                else:
                    TrainExistShortestPathCitationGraph[ShortestPathCitationGraph] = TrainExistShortestPathCitationGraph.get(ShortestPathCitationGraph) + 1
                 #---------------------------------------------------------------------------------------------------------------
                if (TrainExistSecondShortestPathCitationGraph.get(SecondShortestPathCitationGraph) == None):
                    TrainExistSecondShortestPathCitationGraph[SecondShortestPathCitationGraph] = 1
                else:
                    TrainExistSecondShortestPathCitationGraph[SecondShortestPathCitationGraph] = TrainExistSecondShortestPathCitationGraph.get(SecondShortestPathCitationGraph) + 1


            else :
                if (TrainNonExistCommonNeighbour.get(CommonNeighbour) == None):
                    TrainNonExistCommonNeighbour[CommonNeighbour] = 1
                else:
                    TrainNonExistCommonNeighbour[CommonNeighbour] = TrainNonExistCommonNeighbour.get(CommonNeighbour) + 1
                 #-----------------------------------------------------------------------------------------------------------
                if (TrainNonExistShortestPathAuthorGraph.get(ShortestPathAuthorGraph) == None):
                    TrainNonExistShortestPathAuthorGraph[ShortestPathAuthorGraph] = 1
                else:
                    TrainNonExistShortestPathAuthorGraph[ShortestPathAuthorGraph] = TrainNonExistShortestPathAuthorGraph.get(ShortestPathAuthorGraph) + 1
                 #-----------------------------------------------------------------------------------------------------------
                if (TrainNonExistSecondShortestPathAuthorGraph.get(SecondShortestPathAuthorGraph) == None):
                    TrainNonExistSecondShortestPathAuthorGraph[SecondShortestPathAuthorGraph] = 1
                else:
                    TrainNonExistSecondShortestPathAuthorGraph[SecondShortestPathAuthorGraph] = TrainNonExistSecondShortestPathAuthorGraph.get(SecondShortestPathAuthorGraph) + 1
                # -----------------------------------------------------------------------------------------------------------
                if (TrainNonExistShortestPathCitationGraph.get(ShortestPathCitationGraph) == None):
                    TrainNonExistShortestPathCitationGraph[ShortestPathCitationGraph] = 1
                else:
                    TrainNonExistShortestPathCitationGraph[ShortestPathCitationGraph] = TrainNonExistShortestPathCitationGraph.get(ShortestPathCitationGraph) + 1
                # ---------------------------------------------------------------------------------------------------------------
                if (TrainNonExistSecondShortestPathCitationGraph.get(SecondShortestPathCitationGraph) == None):
                    TrainNonExistSecondShortestPathCitationGraph[SecondShortestPathCitationGraph] = 1
                else:
                    TrainNonExistSecondShortestPathCitationGraph[SecondShortestPathCitationGraph] = TrainNonExistSecondShortestPathCitationGraph.get(
                        SecondShortestPathCitationGraph) + 1

    f.closed

    w = csv.writer(open("FeatureCalculation1\TrainExistCommonNeighbour.csv", "w", newline=''))
    values = [float(x) for x in list(TrainExistCommonNeighbour.keys())]
    keyMax = max(values)
    keyMin = min(values)
    for key, val in TrainExistCommonNeighbour.items():
        #w.writerow([round((float(key) - keyMin) / (keyMax - keyMin), 3), val])
        w.writerow([key, val])

    w = csv.writer(open("FeatureCalculation1\TrainNonExistCommonNeighbour.csv", "w", newline=''))
    for key, val in TrainNonExistCommonNeighbour.items():
        w.writerow([key, val])

    w = csv.writer(open("FeatureCalculation1\TrainExistShortestPathAuthorGraph.csv", "w", newline=''))
    values = [float(x) for x in list(TrainExistShortestPathAuthorGraph.keys())]
    keyMax = max(values)
    keyMin = min(values)
    for key, val in TrainExistShortestPathAuthorGraph.items():
        w.writerow([key, val])

    w = csv.writer(open("FeatureCalculation1\TrainNonExistShortestPathAuthorGraph.csv", "w", newline=''))
    values = [float(x) for x in list(TrainNonExistShortestPathAuthorGraph.keys())]
    keyMax = max(values)
    keyMin = min(values)
    for key, val in TrainNonExistShortestPathAuthorGraph.items():
        w.writerow([key, val])

    w = csv.writer(open("FeatureCalculation1\TrainExistSecondShortestPathAuthorGraph.csv", "w", newline=''))
    values = [float(x) for x in list(TrainExistSecondShortestPathAuthorGraph.keys())]
    keyMax = max(values)
    keyMin = min(values)
    for key, val in TrainExistSecondShortestPathAuthorGraph.items():
        w.writerow([key, val])

    w = csv.writer(open("FeatureCalculation1\TrainNonExistSecondShortestPathAuthorGraph.csv", "w", newline=''))
    values = [float(x) for x in list(TrainNonExistSecondShortestPathAuthorGraph.keys())]
    keyMax = max(values)
    keyMin = min(values)
    for key, val in TrainNonExistSecondShortestPathAuthorGraph.items():
        w.writerow([key, val])

    w = csv.writer(open("FeatureCalculation1\TrainExistShortestPathCitationGraph.csv", "w", newline=''))
    values = [float(x) for x in list(TrainExistShortestPathCitationGraph.keys())]
    keyMax = max(values)
    keyMin = min(values)
    for key, val in TrainExistShortestPathCitationGraph.items():
        w.writerow([key, val])

    w = csv.writer(open("FeatureCalculation1\TrainNonExistShortestPathCitationGraph.csv", "w", newline=''))
    values = [float(x) for x in list(TrainNonExistShortestPathCitationGraph.keys())]
    keyMax = max(values)
    keyMin = min(values)
    for key, val in TrainNonExistShortestPathCitationGraph.items():
        w.writerow([key, val])

    w = csv.writer(open("FeatureCalculation1\TrainExistSecondShortestPathCitationGraph.csv", "w", newline=''))
    values = [float(x) for x in list(TrainExistSecondShortestPathCitationGraph.keys())]
    keyMax = max(values)
    keyMin = min(values)
    for key, val in TrainExistSecondShortestPathCitationGraph.items():
        w.writerow([key, val])

    w = csv.writer(open("FeatureCalculation1\TrainNonExistSecondShortestPathCitationGraph.csv", "w", newline=''))
    values = [float(x) for x in list(TrainNonExistSecondShortestPathCitationGraph.keys())]
    keyMax = max(values)
    keyMin = min(values)
    for key, val in TrainNonExistSecondShortestPathCitationGraph.items():
        w.writerow([key, val])
#---------------------------------------------------------------------------------------------------------------------------------------------------

def CreatingDistributionGraphForTestExistAndNonExist():

    x = []
    y = []
    #----------------------------Common Neighbour-----------------------------------------------------------------
    with open("FeatureCalculation1\TestExistCommonNeighbour.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.plot(x, y,'r', label='Exists')

    x = []
    y = []
    with open("FeatureCalculation1\TestNonExistCommonNeighbour.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.plot(x, y, 'g',label='Non-Exists')


    plt.title('Common Neighbour-Test Data-Exist&NonExist')
    plt.legend()
    plt.show()


           #-------------------------------------------Shortest Path in Co-Author Graph----------------------------------------------------------------
    x = []
    y = []
    with  open("FeatureCalculation1\TestExistShortestPathAuthorGraph.csv", 'r') as csvfile:
     plots = csv.reader(csvfile, delimiter=',')
     for row in plots:
        x.append(float(row[0]))
        y.append(int(row[1]))

    plt.clf()
    plt.plot(x, y, 'r', label='Exists')

    x = []
    y = []
    with open("FeatureCalculation1\TestNonExistShortestPathAuthorGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.plot(x, y, 'g', label='Non-Exists')
    plt.ylabel('count')
    plt.xlabel('TotalCost')

    plt.title('ShortestPath in Co-Author Graph-Test Data-Exist&NonExist')
    plt.legend()
    plt.show()

    # -------------------------------------------Second Shortest Path in Co-Author Graph----------------------------------------------------------------
    x = []
    y = []
    with  open("FeatureCalculation1\TestExistSecondShortestPathAuthorGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.clf()
    plt.plot(x, y, 'r', label='Exists')

    x = []
    y = []
    with open("FeatureCalculation1\TestNonExistSecondShortestPathAuthorGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.plot(x, y, 'g', label='Non-Exists')
    plt.ylabel('count')
    plt.xlabel('TotalCost')

    plt.title('Second ShortestPath in Co-Author Graph-Test Data-Exist&NonExist')
    plt.legend()
    plt.show()

    # -------------------------------------------Shortest Path in Citation Graph----------------------------------------------------------------
    x = []
    y = []
    with  open("FeatureCalculation1\TestExistShortestPathCitationGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.clf()
    plt.plot(x, y, 'r', label='Exists')

    x = []
    y = []
    with open("FeatureCalculation1\TestNonExistShortestPathCitationGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.plot(x, y, 'g', label='Non-Exists')
    plt.ylabel('count')
    plt.xlabel('TotalCost')

    plt.title('ShortestPath in Citation Graph-Test Data-Exist&NonExist')
    plt.legend()
    plt.show()

    # -------------------------------------------Second Shortest Path in Citation Graph----------------------------------------------------------------
    x = []
    y = []
    with  open("FeatureCalculation1\TestExistSecondShortestPathCitationGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.clf()
    plt.plot(x, y, 'r', label='Exists')

    x = []
    y = []
    with open("FeatureCalculation1\TestNonExistSecondShortestPathCitationGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.plot(x, y, 'g', label='Non-Exists')
    plt.ylabel('count')
    plt.xlabel('TotalCost')

    plt.title('Second ShortestPath in Citation Graph-Test Data-Exist&NonExist')
    plt.legend()
    plt.show()

#---------------------------------------------------------------------------------------------------------------------------------------
def CreatingDistributionGraphForTrainExistAndNonExist():

    x = []
    y = []
    #----------------------------Common Neighbour-----------------------------------------------------------------
    with open("FeatureCalculation1\TrainExistCommonNeighbour.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.plot(x, y,'r', label='Exists')

    x = []
    y = []
    with open("FeatureCalculation1\TrainNonExistCommonNeighbour.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.plot(x, y, 'g',label='Non-Exists')

    plt.title('Common Neighbour-Train Data-Exist&NonExist')
    plt.legend()
    plt.show()


           #-------------------------------------------Shortest Path in Co-Author Graph----------------------------------------------------------------
    x = []
    y = []
    with  open("FeatureCalculation1\TrainExistShortestPathAuthorGraph.csv", 'r') as csvfile:
     plots = csv.reader(csvfile, delimiter=',')
     for row in plots:
        x.append(float(row[0]))
        y.append(int(row[1]))

    plt.clf()
    plt.plot(x, y, 'r', label='Exists')

    x = []
    y = []
    with open("FeatureCalculation1\TrainNonExistShortestPathAuthorGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.plot(x, y, 'g', label='Non-Exists')
    plt.ylabel('count')
    plt.xlabel('TotalCost')

    plt.title('ShortestPath in Co-Author Graph-Train Data-Exist&NonExist')
    plt.legend()
    plt.show()

    # -------------------------------------------Second Shortest Path in Co-Author Graph----------------------------------------------------------------
    x = []
    y = []
    with  open("FeatureCalculation1\TrainExistSecondShortestPathAuthorGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.clf()
    plt.plot(x, y, 'r', label='Exists')

    x = []
    y = []
    with open("FeatureCalculation1\TrainNonExistSecondShortestPathAuthorGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.plot(x, y, 'g', label='Non-Exists')
    plt.ylabel('count')
    plt.xlabel('TotalCost')

    plt.title('Second ShortestPath in Co-Author Graph-Train Data-Exist&NonExist')
    plt.legend()
    plt.show()

    # -------------------------------------------Shortest Path in Citation Graph----------------------------------------------------------------
    x = []
    y = []
    with  open("FeatureCalculation1\TrainExistShortestPathCitationGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.clf()
    plt.plot(x, y, 'r', label='Exists')

    x = []
    y = []
    with open("FeatureCalculation1\TrainNonExistShortestPathCitationGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.plot(x, y, 'g', label='Non-Exists')
    plt.ylabel('count')
    plt.xlabel('TotalCost')

    plt.title('ShortestPath in Citation Graph-Train Data-Exist&NonExist')
    plt.legend()
    plt.show()

    # -------------------------------------------Second Shortest Path in Citation Graph----------------------------------------------------------------
    x = []
    y = []
    with  open("FeatureCalculation1\TrainExistSecondShortestPathCitationGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.clf()
    plt.plot(x, y, 'r', label='Exists')

    x = []
    y = []
    with open("FeatureCalculation1\TrainNonExistSecondShortestPathCitationGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.plot(x, y, 'g', label='Non-Exists')
    plt.ylabel('count')
    plt.xlabel('TotalCost')

    plt.title('Second ShortestPath in Citation Graph-Train Data-Exist&NonExist')
    plt.legend()
    plt.show()

#---------------------------------------------------------------------------------------------------------------------------------------
SplitTestExistNonExist()
SplitTrainExistNonExist()
CreatingDistributionGraphForTestExistAndNonExist()
CreatingDistributionGraphForTrainExistAndNonExist()