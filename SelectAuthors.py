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

nltk.download('stopwords')


#---------------------------------------------------------------------------------------------------------------------------
lstPaperIDs = []
lstPaperAuthorAffiliation=[]
def GetAllPapersIds():

    global lstPaperIDs
    with open('Feature.csv') as f:
        lines = f.readlines()
        for row in lines:
            AuthorID1, AuthorID2, CommonNeighbour, ShortestPathAuthorGraph, SecondShortestPathAuthorGraph, ShortestPathCitationGraph, SecondShortestPathCitationGraph, Label = row.split(",")
            lstPaperIDs.append(PaperID)
    print("Finished selection from Papers.csv")
    f.closed
#--------------------------------------------------------------------------------------------------------------------------
def GetAllPaperAuthorAffiliation():

    global lstPaperIDs
    global lstPaperAuthorAffiliation
    with open('PaperAuthorAffiliation.csv') as f:
        lines = f.readlines()
        for row in lines:
            if(len(row.strip()) > 0 ):
                PaperID,AuthorID,Type = row.split(",")
                if(PaperID in lstPaperIDs):
                    lstPaperAuthorAffiliation.append(AuthorID)
    print("Finished selection from PaperAuthorAffiliation")
    f.closed
#--------------------------------------------------------------------------------------------------------------------------
def ReadRelatedAuthorsAndWriteToFile():


    print("Starting to write filtered Authors into FilteredAuthors.csv")
    global  lstPaperAuthorAffiliation
    with open('Authors.csv') as f:
        lines = f.readlines()
        for row in lines:
            if (len(row.strip()) > 0):
                AuthorID, Rank, Label = row.split(",")
                if(AuthorID in lstPaperAuthorAffiliation):
                    with open('FilteredAuthors.csv', 'a', encoding="UTF8") as fd:
                        fd.write(AuthorID + "\n")

    f.closed

#------------------------------------------------------------------------------------------------------------------------
GetAllPapersIds()
GetAllPaperAuthorAffiliation()
ReadRelatedAuthorsAndWriteToFile()