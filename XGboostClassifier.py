import pandas as pd
import random
from timeit import default_timer as timer
import itertools
from py2neo import Graph, Node, Relationship
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix, roc_auc_score, f1_score
import pandas as pd
from sklearn.preprocessing import label_binarize
from xgboost import XGBClassifier
import pickle
import sys

graph = Graph(password = "123")

# Load dataset

print("Running")
dfTrain = pd.DataFrame(graph.data("MATCH (a:page)-[r{edgeType:'train'}]-> (b:page) RETURN DISTINCT  a.PageID,b.PageID,a.Embedding,b.Embedding,type(r),r.edgeType"))
dfTest = pd.DataFrame(graph.data("MATCH (a:page)-[r{edgeType:'test'}]-> (b:page) RETURN DISTINCT  a.PageID,b.PageID,a.Embedding,b.Embedding,type(r),r.edgeType"))
#So the expected schema for training and testing datasets is to: vertex_id1, vertex_id2, 1, 2, 3, 4, target (exists or not-exists)
used_features =['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32', '33', '34', '35', '36', '37', '38', '39', '40', '41', '42', '43', '44', '45', '46', '47', '48', '49', '50', '51', '52', '53', '54', '55', '56', '57', '58', '59', '60', '61', '62', '63', '64', '65', '66', '67', '68', '69', '70', '71', '72', '73', '74', '75', '76', '77', '78', '79', '80', '81', '82', '83', '84', '85', '86', '87', '88', '89', '90', '91', '92', '93', '94', '95', '96', '97', '98', '99', '100', '101', '102', '103', '104', '105', '106', '107', '108', '109', '110', '111', '112', '113', '114', '115', '116', '117', '118', '119', '120', '121', '122', '123', '124', '125', '126', '127', '128', '129', '130', '131', '132', '133', '134', '135', '136', '137', '138', '139', '140', '141', '142', '143', '144', '145', '146', '147', '148', '149', '150', '151', '152', '153', '154', '155', '156', '157', '158', '159', '160', '161', '162', '163', '164', '165', '166', '167', '168', '169', '170', '171', '172', '173', '174', '175', '176', '177', '178', '179', '180', '181', '182', '183', '184', '185', '186', '187', '188', '189', '190', '191', '192', '193', '194', '195', '196', '197', '198', '199', '200', '201', '202', '203', '204', '205', '206', '207', '208', '209', '210', '211', '212', '213', '214', '215', '216', '217', '218', '219', '220', '221', '222', '223', '224', '225', '226', '227', '228', '229', '230', '231', '232', '233', '234', '235', '236', '237', '238', '239', '240', '241', '242', '243', '244', '245', '246', '247', '248', '249', '250', '251', '252', '253', '254', '255', '256', '257', '258', '259', '260', '261', '262', '263', '264', '265', '266', '267', '268', '269', '270', '271', '272', '273', '274', '275', '276', '277', '278', '279', '280', '281', '282', '283', '284', '285', '286', '287', '288', '289', '290', '291', '292', '293', '294', '295', '296', '297', '298', '299', '300', '301', '302', '303', '304', '305', '306', '307', '308', '309', '310', '311', '312', '313', '314', '315', '316', '317', '318', '319', '320', '321', '322', '323', '324', '325', '326', '327', '328', '329', '330', '331', '332', '333', '334', '335', '336', '337', '338', '339', '340', '341', '342', '343', '344', '345', '346', '347', '348', '349', '350', '351', '352', '353', '354', '355', '356', '357', '358', '359', '360', '361', '362', '363', '364', '365', '366', '367', '368', '369', '370', '371', '372', '373', '374', '375', '376', '377', '378', '379', '380', '381', '382', '383', '384', '385', '386', '387', '388', '389', '390', '391', '392', '393', '394', '395', '396', '397', '398', '399', '400', '401', '402', '403', '404', '405', '406', '407', '408', '409', '410', '411', '412', '413', '414', '415', '416', '417', '418', '419', '420', '421', '422', '423', '424', '425', '426', '427', '428', '429', '430', '431', '432', '433', '434', '435', '436', '437', '438', '439', '440', '441', '442', '443', '444', '445', '446', '447', '448', '449', '450', '451', '452', '453', '454', '455', '456', '457', '458', '459', '460', '461', '462', '463', '464', '465', '466', '467', '468', '469', '470', '471', '472', '473', '474', '475', '476', '477', '478', '479', '480', '481', '482', '483', '484', '485', '486', '487', '488', '489', '490', '491', '492', '493', '494', '495', '496', '497', '498', '499', '500', '501', '502', '503', '504', '505', '506', '507', '508', '509', '510', '511', '512', '513', '514', '515', '516', '517', '518', '519', '520', '521', '522', '523', '524', '525', '526', '527', '528', '529', '530', '531', '532', '533', '534', '535', '536', '537', '538', '539', '540', '541', '542', '543', '544', '545', '546', '547', '548', '549', '550', '551', '552', '553', '554', '555', '556', '557', '558', '559', '560', '561', '562', '563', '564', '565', '566', '567', '568', '569', '570', '571', '572', '573', '574', '575', '576', '577', '578', '579', '580', '581', '582', '583', '584', '585', '586', '587', '588', '589', '590', '591', '592', '593', '594', '595', '596', '597', '598', '599','600']

dfTrain.columns = ["Embedding1","PageID1","Embedding2","PageID2","EdgeType","TYPE"]
dfTest.columns = ["Embedding1","PageID1","Embedding2","PageID2","EdgeType","TYPE"]

"""str1 = dfTrain.iloc[0]["Embedding1"].split(",")
for s in range(0,299):
    print(str('pageEmbed1[')+str(s)+']')
for s in range(0,299):
    print(str('pageEmbed2[')+str(s)+']')"""
dataTrain = []
dataTest = []

for i in (0,len(dfTrain)-1):
    pageEmbed1 = dfTrain.iloc[i]["Embedding1"].split(",")
    pageEmbed2 =  dfTrain.iloc[i]["Embedding2"].split(",")
    for i in range(len(pageEmbed1),300):
        pageEmbed1.append(i)
    for i in range(len(pageEmbed2),300):
        pageEmbed2.append(i)
    # dataTrain.extend(pageEmbed1)
    # dataTrain.extend(pageEmbed2)
    dataTrain.append([dfTrain.iloc[i]["PageID1"],dfTrain.iloc[i]["PageID2"],pageEmbed1[0],pageEmbed1[1],pageEmbed1[2],pageEmbed1[3]
,pageEmbed1[4],pageEmbed1[5],pageEmbed1[6],pageEmbed1[7],pageEmbed1[8],pageEmbed1[9],pageEmbed1[10],pageEmbed1[11],pageEmbed1[12]
,pageEmbed1[13],pageEmbed1[14],pageEmbed1[15],pageEmbed1[16],pageEmbed1[17],pageEmbed1[18],pageEmbed1[19],pageEmbed1[20],pageEmbed1[21]
,pageEmbed1[22],pageEmbed1[23],pageEmbed1[24],pageEmbed1[25],pageEmbed1[26],pageEmbed1[27],pageEmbed1[28],pageEmbed1[29],pageEmbed1[30]
,pageEmbed1[31],pageEmbed1[32],pageEmbed1[33],pageEmbed1[34],pageEmbed1[35],pageEmbed1[36],pageEmbed1[37],pageEmbed1[38],pageEmbed1[39]
,pageEmbed1[40],pageEmbed1[41],pageEmbed1[42],pageEmbed1[43],pageEmbed1[44],pageEmbed1[45],pageEmbed1[46],pageEmbed1[47],pageEmbed1[48]
,pageEmbed1[49],pageEmbed1[50],pageEmbed1[51],pageEmbed1[52],pageEmbed1[53],pageEmbed1[54],pageEmbed1[55],pageEmbed1[56],pageEmbed1[57]
,pageEmbed1[58],pageEmbed1[59],pageEmbed1[60],pageEmbed1[61],pageEmbed1[62],pageEmbed1[63],pageEmbed1[64],pageEmbed1[65],pageEmbed1[66]
,pageEmbed1[67],pageEmbed1[68],pageEmbed1[69],pageEmbed1[70],pageEmbed1[71],pageEmbed1[72],pageEmbed1[73],pageEmbed1[74],pageEmbed1[75]
,pageEmbed1[76],pageEmbed1[77],pageEmbed1[78],pageEmbed1[79],pageEmbed1[80],pageEmbed1[81],pageEmbed1[82],pageEmbed1[83],pageEmbed1[84]
,pageEmbed1[85],pageEmbed1[86],pageEmbed1[87],pageEmbed1[88],pageEmbed1[89],pageEmbed1[90],pageEmbed1[91],pageEmbed1[92],pageEmbed1[93]
,pageEmbed1[94],pageEmbed1[95],pageEmbed1[96],pageEmbed1[97],pageEmbed1[98],pageEmbed1[99],pageEmbed1[100],pageEmbed1[101],pageEmbed1[102]
,pageEmbed1[103],pageEmbed1[104],pageEmbed1[105],pageEmbed1[106],pageEmbed1[107],pageEmbed1[108],pageEmbed1[109],pageEmbed1[110],pageEmbed1[111]
,pageEmbed1[112],pageEmbed1[113],pageEmbed1[114],pageEmbed1[115],pageEmbed1[116],pageEmbed1[117],pageEmbed1[118],pageEmbed1[119],pageEmbed1[120]
,pageEmbed1[121],pageEmbed1[122],pageEmbed1[123],pageEmbed1[124],pageEmbed1[125],pageEmbed1[126],pageEmbed1[127],pageEmbed1[128],pageEmbed1[129],pageEmbed1[130],pageEmbed1[131]
,pageEmbed1[132],pageEmbed1[133],pageEmbed1[134],pageEmbed1[135],pageEmbed1[136],pageEmbed1[137],pageEmbed1[138],pageEmbed1[139],pageEmbed1[140],pageEmbed1[141],pageEmbed1[142],pageEmbed1[143],pageEmbed1[144],pageEmbed1[145],pageEmbed1[146],pageEmbed1[147],pageEmbed1[148],pageEmbed1[149],pageEmbed1[150],pageEmbed1[151],pageEmbed1[152],pageEmbed1[153],pageEmbed1[154]
,pageEmbed1[155],pageEmbed1[156],pageEmbed1[157],pageEmbed1[158],pageEmbed1[159],pageEmbed1[160],pageEmbed1[161],pageEmbed1[162],pageEmbed1[163]
,pageEmbed1[164],pageEmbed1[165],pageEmbed1[166],pageEmbed1[167],pageEmbed1[168],pageEmbed1[169],pageEmbed1[170],pageEmbed1[171],pageEmbed1[172],pageEmbed1[173]
,pageEmbed1[174],pageEmbed1[175],pageEmbed1[176],pageEmbed1[177],pageEmbed1[178],pageEmbed1[179],pageEmbed1[180],pageEmbed1[181],pageEmbed1[182]
,pageEmbed1[183],pageEmbed1[184],pageEmbed1[185],pageEmbed1[186],pageEmbed1[187],pageEmbed1[188],pageEmbed1[189],pageEmbed1[190],pageEmbed1[191],pageEmbed1[192]
,pageEmbed1[193],pageEmbed1[194],pageEmbed1[195],pageEmbed1[196],pageEmbed1[197],pageEmbed1[198],pageEmbed1[199],pageEmbed1[200],pageEmbed1[201],pageEmbed1[202]
,pageEmbed1[203],pageEmbed1[204],pageEmbed1[205],pageEmbed1[206],pageEmbed1[207],pageEmbed1[208],pageEmbed1[209],pageEmbed1[210],pageEmbed1[211],pageEmbed1[212]
,pageEmbed1[213],pageEmbed1[214],pageEmbed1[215],pageEmbed1[216],pageEmbed1[217],pageEmbed1[218],pageEmbed1[219],pageEmbed1[220],pageEmbed1[221],pageEmbed1[222]
,pageEmbed1[223],pageEmbed1[224],pageEmbed1[225],pageEmbed1[226],pageEmbed1[227],pageEmbed1[228],pageEmbed1[229],pageEmbed1[230],pageEmbed1[231]
,pageEmbed1[232],pageEmbed1[233],pageEmbed1[234],pageEmbed1[235],pageEmbed1[236],pageEmbed1[237],pageEmbed1[238],pageEmbed1[239],pageEmbed1[240],pageEmbed1[241],pageEmbed1[242],pageEmbed1[243],pageEmbed1[244]
,pageEmbed1[245],pageEmbed1[246],pageEmbed1[247],pageEmbed1[248],pageEmbed1[249],pageEmbed1[250],pageEmbed1[251],pageEmbed1[252],pageEmbed1[253],pageEmbed1[254]
,pageEmbed1[255],pageEmbed1[256],pageEmbed1[257],pageEmbed1[258],pageEmbed1[259],pageEmbed1[260],pageEmbed1[261],pageEmbed1[262],pageEmbed1[263],pageEmbed1[264]
,pageEmbed1[265],pageEmbed1[266],pageEmbed1[267],pageEmbed1[268],pageEmbed1[269],pageEmbed1[270],pageEmbed1[271],pageEmbed1[272],pageEmbed1[273],pageEmbed1[274],pageEmbed1[275]
,pageEmbed1[276],pageEmbed1[277],pageEmbed1[278],pageEmbed1[279],pageEmbed1[280],pageEmbed1[281],pageEmbed1[282],pageEmbed1[283],pageEmbed1[284],pageEmbed1[285]
,pageEmbed1[286],pageEmbed1[287],pageEmbed1[288],pageEmbed1[289],pageEmbed1[290],pageEmbed1[291],pageEmbed1[292],pageEmbed1[293],pageEmbed1[294],pageEmbed1[295]
,pageEmbed1[296],pageEmbed1[297],pageEmbed1[298],pageEmbed1[299],pageEmbed2[0],pageEmbed2[1],pageEmbed2[2],pageEmbed2[3]
,pageEmbed2[4],pageEmbed2[5],pageEmbed2[6],pageEmbed2[7],pageEmbed2[8],pageEmbed2[9],pageEmbed2[10],pageEmbed2[11],pageEmbed2[12]
,pageEmbed2[13],pageEmbed2[14],pageEmbed2[15],pageEmbed2[16],pageEmbed2[17],pageEmbed2[18],pageEmbed2[19],pageEmbed2[20],pageEmbed2[21]
,pageEmbed2[22],pageEmbed2[23],pageEmbed2[24],pageEmbed2[25],pageEmbed2[26],pageEmbed2[27],pageEmbed2[28],pageEmbed2[29],pageEmbed2[30]
,pageEmbed2[31],pageEmbed2[32],pageEmbed2[33],pageEmbed2[34],pageEmbed2[35],pageEmbed2[36],pageEmbed2[37],pageEmbed2[38],pageEmbed2[39]
,pageEmbed2[40],pageEmbed2[41],pageEmbed2[42],pageEmbed2[43],pageEmbed2[44],pageEmbed2[45],pageEmbed2[46],pageEmbed2[47],pageEmbed2[48]
,pageEmbed2[49],pageEmbed2[50],pageEmbed2[51],pageEmbed2[52],pageEmbed2[53],pageEmbed2[54],pageEmbed2[55],pageEmbed2[56],pageEmbed2[57]
,pageEmbed2[58],pageEmbed2[59],pageEmbed2[60],pageEmbed2[61],pageEmbed2[62],pageEmbed2[63],pageEmbed2[64],pageEmbed2[65],pageEmbed2[66]
,pageEmbed2[67],pageEmbed2[68],pageEmbed2[69],pageEmbed2[70],pageEmbed2[71],pageEmbed2[72],pageEmbed2[73],pageEmbed2[74],pageEmbed2[75]
,pageEmbed2[76],pageEmbed2[77],pageEmbed2[78],pageEmbed2[79],pageEmbed2[80],pageEmbed2[81],pageEmbed2[82],pageEmbed2[83],pageEmbed2[84]
,pageEmbed2[85],pageEmbed2[86],pageEmbed2[87],pageEmbed2[88],pageEmbed2[89],pageEmbed2[90],pageEmbed2[91],pageEmbed2[92],pageEmbed2[93]
,pageEmbed2[94],pageEmbed2[95],pageEmbed2[96],pageEmbed2[97],pageEmbed2[98],pageEmbed2[99],pageEmbed2[100],pageEmbed2[101],pageEmbed2[102]
,pageEmbed2[103],pageEmbed2[104],pageEmbed2[105],pageEmbed2[106],pageEmbed2[107],pageEmbed2[108],pageEmbed2[109],pageEmbed2[110],pageEmbed2[111]
,pageEmbed2[112],pageEmbed2[113],pageEmbed2[114],pageEmbed2[115],pageEmbed2[116],pageEmbed2[117],pageEmbed2[118],pageEmbed2[119],pageEmbed2[120]
,pageEmbed2[121],pageEmbed2[122],pageEmbed2[123],pageEmbed2[124],pageEmbed2[125],pageEmbed2[126],pageEmbed2[127],pageEmbed2[128],pageEmbed2[129],pageEmbed2[130],pageEmbed2[131]
,pageEmbed2[132],pageEmbed2[133],pageEmbed2[134],pageEmbed2[135],pageEmbed2[136],pageEmbed2[137],pageEmbed2[138],pageEmbed2[139],pageEmbed2[140],pageEmbed2[141],pageEmbed2[142],pageEmbed2[143],pageEmbed2[144],pageEmbed2[145],pageEmbed2[146],pageEmbed2[147],pageEmbed2[148],pageEmbed2[149],pageEmbed2[150],pageEmbed2[151],pageEmbed2[152],pageEmbed2[153],pageEmbed2[154]
,pageEmbed2[155],pageEmbed2[156],pageEmbed2[157],pageEmbed2[158],pageEmbed2[159],pageEmbed2[160],pageEmbed2[161],pageEmbed2[162],pageEmbed2[163]
,pageEmbed2[164],pageEmbed2[165],pageEmbed2[166],pageEmbed2[167],pageEmbed2[168],pageEmbed2[169],pageEmbed2[170],pageEmbed2[171],pageEmbed2[172],pageEmbed2[173]
,pageEmbed2[174],pageEmbed2[175],pageEmbed2[176],pageEmbed2[177],pageEmbed2[178],pageEmbed2[179],pageEmbed2[180],pageEmbed2[181],pageEmbed2[182]
,pageEmbed2[183],pageEmbed2[184],pageEmbed2[185],pageEmbed2[186],pageEmbed2[187],pageEmbed2[188],pageEmbed2[189],pageEmbed2[190],pageEmbed2[191],pageEmbed2[192]
,pageEmbed2[193],pageEmbed2[194],pageEmbed2[195],pageEmbed2[196],pageEmbed2[197],pageEmbed2[198],pageEmbed2[199],pageEmbed2[200],pageEmbed2[201],pageEmbed2[202]
,pageEmbed2[203],pageEmbed2[204],pageEmbed2[205],pageEmbed2[206],pageEmbed2[207],pageEmbed2[208],pageEmbed2[209],pageEmbed2[210],pageEmbed2[211],pageEmbed2[212]
,pageEmbed2[213],pageEmbed2[214],pageEmbed2[215],pageEmbed2[216],pageEmbed2[217],pageEmbed2[218],pageEmbed2[219],pageEmbed2[220],pageEmbed2[221],pageEmbed2[222]
,pageEmbed2[223],pageEmbed2[224],pageEmbed2[225],pageEmbed2[226],pageEmbed2[227],pageEmbed2[228],pageEmbed2[229],pageEmbed2[230],pageEmbed2[231]
,pageEmbed2[232],pageEmbed2[233],pageEmbed2[234],pageEmbed2[235],pageEmbed2[236],pageEmbed2[237],pageEmbed2[238],pageEmbed2[239],pageEmbed2[240],pageEmbed2[241],pageEmbed2[242],pageEmbed2[243],pageEmbed2[244]
,pageEmbed2[245],pageEmbed2[246],pageEmbed2[247],pageEmbed2[248],pageEmbed2[249],pageEmbed2[250],pageEmbed2[251],pageEmbed2[252],pageEmbed2[253],pageEmbed2[254]
,pageEmbed2[255],pageEmbed2[256],pageEmbed2[257],pageEmbed2[258],pageEmbed2[259],pageEmbed2[260],pageEmbed2[261],pageEmbed2[262],pageEmbed2[263],pageEmbed2[264]
,pageEmbed2[265],pageEmbed2[266],pageEmbed2[267],pageEmbed2[268],pageEmbed2[269],pageEmbed2[270],pageEmbed2[271],pageEmbed2[272],pageEmbed2[273],pageEmbed2[274],pageEmbed2[275]
,pageEmbed2[276],pageEmbed2[277],pageEmbed2[278],pageEmbed2[279],pageEmbed2[280],pageEmbed2[281],pageEmbed2[282],pageEmbed2[283],pageEmbed2[284],pageEmbed2[285]
,pageEmbed2[286],pageEmbed2[287],pageEmbed2[288],pageEmbed2[289],pageEmbed2[290],pageEmbed2[291],pageEmbed2[292],pageEmbed2[293],pageEmbed2[294],pageEmbed2[295]
,pageEmbed2[296],pageEmbed2[297],pageEmbed2[298],pageEmbed2[299],dfTrain.iloc[i]["TYPE"]])


for i in (0,len(dfTest)-1):
    pageEmbed1 = dfTest.iloc[i]["Embedding1"].split(",")
    pageEmbed2 =  dfTest.iloc[i]["Embedding2"].split(",")
    for i in range(len(pageEmbed1),300):
        pageEmbed1.append(i)
    for i in range(len(pageEmbed2),300):
        pageEmbed2.append(i)
    dataTest.append([dfTest.iloc[i]["PageID1"],dfTest.iloc[i]["PageID2"],pageEmbed1[0],pageEmbed1[1],pageEmbed1[2],pageEmbed1[3]
,pageEmbed1[4],pageEmbed1[5],pageEmbed1[6],pageEmbed1[7],pageEmbed1[8],pageEmbed1[9],pageEmbed1[10],pageEmbed1[11],pageEmbed1[12]
,pageEmbed1[13],pageEmbed1[14],pageEmbed1[15],pageEmbed1[16],pageEmbed1[17],pageEmbed1[18],pageEmbed1[19],pageEmbed1[20],pageEmbed1[21]
,pageEmbed1[22],pageEmbed1[23],pageEmbed1[24],pageEmbed1[25],pageEmbed1[26],pageEmbed1[27],pageEmbed1[28],pageEmbed1[29],pageEmbed1[30]
,pageEmbed1[31],pageEmbed1[32],pageEmbed1[33],pageEmbed1[34],pageEmbed1[35],pageEmbed1[36],pageEmbed1[37],pageEmbed1[38],pageEmbed1[39]
,pageEmbed1[40],pageEmbed1[41],pageEmbed1[42],pageEmbed1[43],pageEmbed1[44],pageEmbed1[45],pageEmbed1[46],pageEmbed1[47],pageEmbed1[48]
,pageEmbed1[49],pageEmbed1[50],pageEmbed1[51],pageEmbed1[52],pageEmbed1[53],pageEmbed1[54],pageEmbed1[55],pageEmbed1[56],pageEmbed1[57]
,pageEmbed1[58],pageEmbed1[59],pageEmbed1[60],pageEmbed1[61],pageEmbed1[62],pageEmbed1[63],pageEmbed1[64],pageEmbed1[65],pageEmbed1[66]
,pageEmbed1[67],pageEmbed1[68],pageEmbed1[69],pageEmbed1[70],pageEmbed1[71],pageEmbed1[72],pageEmbed1[73],pageEmbed1[74],pageEmbed1[75]
,pageEmbed1[76],pageEmbed1[77],pageEmbed1[78],pageEmbed1[79],pageEmbed1[80],pageEmbed1[81],pageEmbed1[82],pageEmbed1[83],pageEmbed1[84]
,pageEmbed1[85],pageEmbed1[86],pageEmbed1[87],pageEmbed1[88],pageEmbed1[89],pageEmbed1[90],pageEmbed1[91],pageEmbed1[92],pageEmbed1[93]
,pageEmbed1[94],pageEmbed1[95],pageEmbed1[96],pageEmbed1[97],pageEmbed1[98],pageEmbed1[99],pageEmbed1[100],pageEmbed1[101],pageEmbed1[102]
,pageEmbed1[103],pageEmbed1[104],pageEmbed1[105],pageEmbed1[106],pageEmbed1[107],pageEmbed1[108],pageEmbed1[109],pageEmbed1[110],pageEmbed1[111]
,pageEmbed1[112],pageEmbed1[113],pageEmbed1[114],pageEmbed1[115],pageEmbed1[116],pageEmbed1[117],pageEmbed1[118],pageEmbed1[119],pageEmbed1[120]
,pageEmbed1[121],pageEmbed1[122],pageEmbed1[123],pageEmbed1[124],pageEmbed1[125],pageEmbed1[126],pageEmbed1[127],pageEmbed1[128],pageEmbed1[129],pageEmbed1[130],pageEmbed1[131]
,pageEmbed1[132],pageEmbed1[133],pageEmbed1[134],pageEmbed1[135],pageEmbed1[136],pageEmbed1[137],pageEmbed1[138],pageEmbed1[139],pageEmbed1[140],pageEmbed1[141],pageEmbed1[142],pageEmbed1[143],pageEmbed1[144],pageEmbed1[145],pageEmbed1[146],pageEmbed1[147],pageEmbed1[148],pageEmbed1[149],pageEmbed1[150],pageEmbed1[151],pageEmbed1[152],pageEmbed1[153],pageEmbed1[154]
,pageEmbed1[155],pageEmbed1[156],pageEmbed1[157],pageEmbed1[158],pageEmbed1[159],pageEmbed1[160],pageEmbed1[161],pageEmbed1[162],pageEmbed1[163]
,pageEmbed1[164],pageEmbed1[165],pageEmbed1[166],pageEmbed1[167],pageEmbed1[168],pageEmbed1[169],pageEmbed1[170],pageEmbed1[171],pageEmbed1[172],pageEmbed1[173]
,pageEmbed1[174],pageEmbed1[175],pageEmbed1[176],pageEmbed1[177],pageEmbed1[178],pageEmbed1[179],pageEmbed1[180],pageEmbed1[181],pageEmbed1[182]
,pageEmbed1[183],pageEmbed1[184],pageEmbed1[185],pageEmbed1[186],pageEmbed1[187],pageEmbed1[188],pageEmbed1[189],pageEmbed1[190],pageEmbed1[191],pageEmbed1[192]
,pageEmbed1[193],pageEmbed1[194],pageEmbed1[195],pageEmbed1[196],pageEmbed1[197],pageEmbed1[198],pageEmbed1[199],pageEmbed1[200],pageEmbed1[201],pageEmbed1[202]
,pageEmbed1[203],pageEmbed1[204],pageEmbed1[205],pageEmbed1[206],pageEmbed1[207],pageEmbed1[208],pageEmbed1[209],pageEmbed1[210],pageEmbed1[211],pageEmbed1[212]
,pageEmbed1[213],pageEmbed1[214],pageEmbed1[215],pageEmbed1[216],pageEmbed1[217],pageEmbed1[218],pageEmbed1[219],pageEmbed1[220],pageEmbed1[221],pageEmbed1[222]
,pageEmbed1[223],pageEmbed1[224],pageEmbed1[225],pageEmbed1[226],pageEmbed1[227],pageEmbed1[228],pageEmbed1[229],pageEmbed1[230],pageEmbed1[231]
,pageEmbed1[232],pageEmbed1[233],pageEmbed1[234],pageEmbed1[235],pageEmbed1[236],pageEmbed1[237],pageEmbed1[238],pageEmbed1[239],pageEmbed1[240],pageEmbed1[241],pageEmbed1[242],pageEmbed1[243],pageEmbed1[244]
,pageEmbed1[245],pageEmbed1[246],pageEmbed1[247],pageEmbed1[248],pageEmbed1[249],pageEmbed1[250],pageEmbed1[251],pageEmbed1[252],pageEmbed1[253],pageEmbed1[254]
,pageEmbed1[255],pageEmbed1[256],pageEmbed1[257],pageEmbed1[258],pageEmbed1[259],pageEmbed1[260],pageEmbed1[261],pageEmbed1[262],pageEmbed1[263],pageEmbed1[264]
,pageEmbed1[265],pageEmbed1[266],pageEmbed1[267],pageEmbed1[268],pageEmbed1[269],pageEmbed1[270],pageEmbed1[271],pageEmbed1[272],pageEmbed1[273],pageEmbed1[274],pageEmbed1[275]
,pageEmbed1[276],pageEmbed1[277],pageEmbed1[278],pageEmbed1[279],pageEmbed1[280],pageEmbed1[281],pageEmbed1[282],pageEmbed1[283],pageEmbed1[284],pageEmbed1[285]
,pageEmbed1[286],pageEmbed1[287],pageEmbed1[288],pageEmbed1[289],pageEmbed1[290],pageEmbed1[291],pageEmbed1[292],pageEmbed1[293],pageEmbed1[294],pageEmbed1[295]
,pageEmbed1[296],pageEmbed1[297],pageEmbed1[298],pageEmbed1[299],pageEmbed2[0],pageEmbed2[1],pageEmbed2[2],pageEmbed2[3]
,pageEmbed2[4],pageEmbed2[5],pageEmbed2[6],pageEmbed2[7],pageEmbed2[8],pageEmbed2[9],pageEmbed2[10],pageEmbed2[11],pageEmbed2[12]
,pageEmbed2[13],pageEmbed2[14],pageEmbed2[15],pageEmbed2[16],pageEmbed2[17],pageEmbed2[18],pageEmbed2[19],pageEmbed2[20],pageEmbed2[21]
,pageEmbed2[22],pageEmbed2[23],pageEmbed2[24],pageEmbed2[25],pageEmbed2[26],pageEmbed2[27],pageEmbed2[28],pageEmbed2[29],pageEmbed2[30]
,pageEmbed2[31],pageEmbed2[32],pageEmbed2[33],pageEmbed2[34],pageEmbed2[35],pageEmbed2[36],pageEmbed2[37],pageEmbed2[38],pageEmbed2[39]
,pageEmbed2[40],pageEmbed2[41],pageEmbed2[42],pageEmbed2[43],pageEmbed2[44],pageEmbed2[45],pageEmbed2[46],pageEmbed2[47],pageEmbed2[48]
,pageEmbed2[49],pageEmbed2[50],pageEmbed2[51],pageEmbed2[52],pageEmbed2[53],pageEmbed2[54],pageEmbed2[55],pageEmbed2[56],pageEmbed2[57]
,pageEmbed2[58],pageEmbed2[59],pageEmbed2[60],pageEmbed2[61],pageEmbed2[62],pageEmbed2[63],pageEmbed2[64],pageEmbed2[65],pageEmbed2[66]
,pageEmbed2[67],pageEmbed2[68],pageEmbed2[69],pageEmbed2[70],pageEmbed2[71],pageEmbed2[72],pageEmbed2[73],pageEmbed2[74],pageEmbed2[75]
,pageEmbed2[76],pageEmbed2[77],pageEmbed2[78],pageEmbed2[79],pageEmbed2[80],pageEmbed2[81],pageEmbed2[82],pageEmbed2[83],pageEmbed2[84]
,pageEmbed2[85],pageEmbed2[86],pageEmbed2[87],pageEmbed2[88],pageEmbed2[89],pageEmbed2[90],pageEmbed2[91],pageEmbed2[92],pageEmbed2[93]
,pageEmbed2[94],pageEmbed2[95],pageEmbed2[96],pageEmbed2[97],pageEmbed2[98],pageEmbed2[99],pageEmbed2[100],pageEmbed2[101],pageEmbed2[102]
,pageEmbed2[103],pageEmbed2[104],pageEmbed2[105],pageEmbed2[106],pageEmbed2[107],pageEmbed2[108],pageEmbed2[109],pageEmbed2[110],pageEmbed2[111]
,pageEmbed2[112],pageEmbed2[113],pageEmbed2[114],pageEmbed2[115],pageEmbed2[116],pageEmbed2[117],pageEmbed2[118],pageEmbed2[119],pageEmbed2[120]
,pageEmbed2[121],pageEmbed2[122],pageEmbed2[123],pageEmbed2[124],pageEmbed2[125],pageEmbed2[126],pageEmbed2[127],pageEmbed2[128],pageEmbed2[129],pageEmbed2[130],pageEmbed2[131]
,pageEmbed2[132],pageEmbed2[133],pageEmbed2[134],pageEmbed2[135],pageEmbed2[136],pageEmbed2[137],pageEmbed2[138],pageEmbed2[139],pageEmbed2[140],pageEmbed2[141],pageEmbed2[142],pageEmbed2[143],pageEmbed2[144],pageEmbed2[145],pageEmbed2[146],pageEmbed2[147],pageEmbed2[148],pageEmbed2[149],pageEmbed2[150],pageEmbed2[151],pageEmbed2[152],pageEmbed2[153],pageEmbed2[154]
,pageEmbed2[155],pageEmbed2[156],pageEmbed2[157],pageEmbed2[158],pageEmbed2[159],pageEmbed2[160],pageEmbed2[161],pageEmbed2[162],pageEmbed2[163]
,pageEmbed2[164],pageEmbed2[165],pageEmbed2[166],pageEmbed2[167],pageEmbed2[168],pageEmbed2[169],pageEmbed2[170],pageEmbed2[171],pageEmbed2[172],pageEmbed2[173]
,pageEmbed2[174],pageEmbed2[175],pageEmbed2[176],pageEmbed2[177],pageEmbed2[178],pageEmbed2[179],pageEmbed2[180],pageEmbed2[181],pageEmbed2[182]
,pageEmbed2[183],pageEmbed2[184],pageEmbed2[185],pageEmbed2[186],pageEmbed2[187],pageEmbed2[188],pageEmbed2[189],pageEmbed2[190],pageEmbed2[191],pageEmbed2[192]
,pageEmbed2[193],pageEmbed2[194],pageEmbed2[195],pageEmbed2[196],pageEmbed2[197],pageEmbed2[198],pageEmbed2[199],pageEmbed2[200],pageEmbed2[201],pageEmbed2[202]
,pageEmbed2[203],pageEmbed2[204],pageEmbed2[205],pageEmbed2[206],pageEmbed2[207],pageEmbed2[208],pageEmbed2[209],pageEmbed2[210],pageEmbed2[211],pageEmbed2[212]
,pageEmbed2[213],pageEmbed2[214],pageEmbed2[215],pageEmbed2[216],pageEmbed2[217],pageEmbed2[218],pageEmbed2[219],pageEmbed2[220],pageEmbed2[221],pageEmbed2[222]
,pageEmbed2[223],pageEmbed2[224],pageEmbed2[225],pageEmbed2[226],pageEmbed2[227],pageEmbed2[228],pageEmbed2[229],pageEmbed2[230],pageEmbed2[231]
,pageEmbed2[232],pageEmbed2[233],pageEmbed2[234],pageEmbed2[235],pageEmbed2[236],pageEmbed2[237],pageEmbed2[238],pageEmbed2[239],pageEmbed2[240],pageEmbed2[241],pageEmbed2[242],pageEmbed2[243],pageEmbed2[244]
,pageEmbed2[245],pageEmbed2[246],pageEmbed2[247],pageEmbed2[248],pageEmbed2[249],pageEmbed2[250],pageEmbed2[251],pageEmbed2[252],pageEmbed2[253],pageEmbed2[254]
,pageEmbed2[255],pageEmbed2[256],pageEmbed2[257],pageEmbed2[258],pageEmbed2[259],pageEmbed2[260],pageEmbed2[261],pageEmbed2[262],pageEmbed2[263],pageEmbed2[264]
,pageEmbed2[265],pageEmbed2[266],pageEmbed2[267],pageEmbed2[268],pageEmbed2[269],pageEmbed2[270],pageEmbed2[271],pageEmbed2[272],pageEmbed2[273],pageEmbed2[274],pageEmbed2[275]
,pageEmbed2[276],pageEmbed2[277],pageEmbed2[278],pageEmbed2[279],pageEmbed2[280],pageEmbed2[281],pageEmbed2[282],pageEmbed2[283],pageEmbed2[284],pageEmbed2[285]
,pageEmbed2[286],pageEmbed2[287],pageEmbed2[288],pageEmbed2[289],pageEmbed2[290],pageEmbed2[291],pageEmbed2[292],pageEmbed2[293],pageEmbed2[294],pageEmbed2[295]
,pageEmbed2[296],pageEmbed2[297],pageEmbed2[298],pageEmbed2[299],dfTest.iloc[i]["TYPE"]])


train = pd.DataFrame(dataTrain, columns = ['PageID1', 'PageID2','1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32', '33', '34', '35', '36', '37', '38', '39', '40', '41', '42', '43', '44', '45', '46', '47', '48', '49', '50', '51', '52', '53', '54', '55', '56', '57', '58', '59', '60', '61', '62', '63', '64', '65', '66', '67', '68', '69', '70', '71', '72', '73', '74', '75', '76', '77', '78', '79', '80', '81', '82', '83', '84', '85', '86', '87', '88', '89', '90', '91', '92', '93', '94', '95', '96', '97', '98', '99', '100', '101', '102', '103', '104', '105', '106', '107', '108', '109', '110', '111', '112', '113', '114', '115', '116', '117', '118', '119', '120', '121', '122', '123', '124', '125', '126', '127', '128', '129', '130', '131', '132', '133', '134', '135', '136', '137', '138', '139', '140', '141', '142', '143', '144', '145', '146', '147', '148', '149', '150', '151', '152', '153', '154', '155', '156', '157', '158', '159', '160', '161', '162', '163', '164', '165', '166', '167', '168', '169', '170', '171', '172', '173', '174', '175', '176', '177', '178', '179', '180', '181', '182', '183', '184', '185', '186', '187', '188', '189', '190', '191', '192', '193', '194', '195', '196', '197', '198', '199', '200', '201', '202', '203', '204', '205', '206', '207', '208', '209', '210', '211', '212', '213', '214', '215', '216', '217', '218', '219', '220', '221', '222', '223', '224', '225', '226', '227', '228', '229', '230', '231', '232', '233', '234', '235', '236', '237', '238', '239', '240', '241', '242', '243', '244', '245', '246', '247', '248', '249', '250', '251', '252', '253', '254', '255', '256', '257', '258', '259', '260', '261', '262', '263', '264', '265', '266', '267', '268', '269', '270', '271', '272', '273', '274', '275', '276', '277', '278', '279', '280', '281', '282', '283', '284', '285', '286', '287', '288', '289', '290', '291', '292', '293', '294', '295', '296', '297', '298', '299', '300', '301', '302', '303', '304', '305', '306', '307', '308', '309', '310', '311', '312', '313', '314', '315', '316', '317', '318', '319', '320', '321', '322', '323', '324', '325', '326', '327', '328', '329', '330', '331', '332', '333', '334', '335', '336', '337', '338', '339', '340', '341', '342', '343', '344', '345', '346', '347', '348', '349', '350', '351', '352', '353', '354', '355', '356', '357', '358', '359', '360', '361', '362', '363', '364', '365', '366', '367', '368', '369', '370', '371', '372', '373', '374', '375', '376', '377', '378', '379', '380', '381', '382', '383', '384', '385', '386', '387', '388', '389', '390', '391', '392', '393', '394', '395', '396', '397', '398', '399', '400', '401', '402', '403', '404', '405', '406', '407', '408', '409', '410', '411', '412', '413', '414', '415', '416', '417', '418', '419', '420', '421', '422', '423', '424', '425', '426', '427', '428', '429', '430', '431', '432', '433', '434', '435', '436', '437', '438', '439', '440', '441', '442', '443', '444', '445', '446', '447', '448', '449', '450', '451', '452', '453', '454', '455', '456', '457', '458', '459', '460', '461', '462', '463', '464', '465', '466', '467', '468', '469', '470', '471', '472', '473', '474', '475', '476', '477', '478', '479', '480', '481', '482', '483', '484', '485', '486', '487', '488', '489', '490', '491', '492', '493', '494', '495', '496', '497', '498', '499', '500', '501', '502', '503', '504', '505', '506', '507', '508', '509', '510', '511', '512', '513', '514', '515', '516', '517', '518', '519', '520', '521', '522', '523', '524', '525', '526', '527', '528', '529', '530', '531', '532', '533', '534', '535', '536', '537', '538', '539', '540', '541', '542', '543', '544', '545', '546', '547', '548', '549', '550', '551', '552', '553', '554', '555', '556', '557', '558', '559', '560', '561', '562', '563', '564', '565', '566', '567', '568', '569', '570', '571', '572', '573', '574', '575', '576', '577', '578', '579', '580', '581', '582', '583', '584', '585', '586', '587', '588', '589', '590', '591', '592', '593', '594', '595', '596', '597', '598', '599','600','target'])
test = pd.DataFrame(dataTest, columns = ['PageID1', 'PageID2','1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32', '33', '34', '35', '36', '37', '38', '39', '40', '41', '42', '43', '44', '45', '46', '47', '48', '49', '50', '51', '52', '53', '54', '55', '56', '57', '58', '59', '60', '61', '62', '63', '64', '65', '66', '67', '68', '69', '70', '71', '72', '73', '74', '75', '76', '77', '78', '79', '80', '81', '82', '83', '84', '85', '86', '87', '88', '89', '90', '91', '92', '93', '94', '95', '96', '97', '98', '99', '100', '101', '102', '103', '104', '105', '106', '107', '108', '109', '110', '111', '112', '113', '114', '115', '116', '117', '118', '119', '120', '121', '122', '123', '124', '125', '126', '127', '128', '129', '130', '131', '132', '133', '134', '135', '136', '137', '138', '139', '140', '141', '142', '143', '144', '145', '146', '147', '148', '149', '150', '151', '152', '153', '154', '155', '156', '157', '158', '159', '160', '161', '162', '163', '164', '165', '166', '167', '168', '169', '170', '171', '172', '173', '174', '175', '176', '177', '178', '179', '180', '181', '182', '183', '184', '185', '186', '187', '188', '189', '190', '191', '192', '193', '194', '195', '196', '197', '198', '199', '200', '201', '202', '203', '204', '205', '206', '207', '208', '209', '210', '211', '212', '213', '214', '215', '216', '217', '218', '219', '220', '221', '222', '223', '224', '225', '226', '227', '228', '229', '230', '231', '232', '233', '234', '235', '236', '237', '238', '239', '240', '241', '242', '243', '244', '245', '246', '247', '248', '249', '250', '251', '252', '253', '254', '255', '256', '257', '258', '259', '260', '261', '262', '263', '264', '265', '266', '267', '268', '269', '270', '271', '272', '273', '274', '275', '276', '277', '278', '279', '280', '281', '282', '283', '284', '285', '286', '287', '288', '289', '290', '291', '292', '293', '294', '295', '296', '297', '298', '299', '300', '301', '302', '303', '304', '305', '306', '307', '308', '309', '310', '311', '312', '313', '314', '315', '316', '317', '318', '319', '320', '321', '322', '323', '324', '325', '326', '327', '328', '329', '330', '331', '332', '333', '334', '335', '336', '337', '338', '339', '340', '341', '342', '343', '344', '345', '346', '347', '348', '349', '350', '351', '352', '353', '354', '355', '356', '357', '358', '359', '360', '361', '362', '363', '364', '365', '366', '367', '368', '369', '370', '371', '372', '373', '374', '375', '376', '377', '378', '379', '380', '381', '382', '383', '384', '385', '386', '387', '388', '389', '390', '391', '392', '393', '394', '395', '396', '397', '398', '399', '400', '401', '402', '403', '404', '405', '406', '407', '408', '409', '410', '411', '412', '413', '414', '415', '416', '417', '418', '419', '420', '421', '422', '423', '424', '425', '426', '427', '428', '429', '430', '431', '432', '433', '434', '435', '436', '437', '438', '439', '440', '441', '442', '443', '444', '445', '446', '447', '448', '449', '450', '451', '452', '453', '454', '455', '456', '457', '458', '459', '460', '461', '462', '463', '464', '465', '466', '467', '468', '469', '470', '471', '472', '473', '474', '475', '476', '477', '478', '479', '480', '481', '482', '483', '484', '485', '486', '487', '488', '489', '490', '491', '492', '493', '494', '495', '496', '497', '498', '499', '500', '501', '502', '503', '504', '505', '506', '507', '508', '509', '510', '511', '512', '513', '514', '515', '516', '517', '518', '519', '520', '521', '522', '523', '524', '525', '526', '527', '528', '529', '530', '531', '532', '533', '534', '535', '536', '537', '538', '539', '540', '541', '542', '543', '544', '545', '546', '547', '548', '549', '550', '551', '552', '553', '554', '555', '556', '557', '558', '559', '560', '561', '562', '563', '564', '565', '566', '567', '568', '569', '570', '571', '572', '573', '574', '575', '576', '577', '578', '579', '580', '581', '582', '583', '584', '585', '586', '587', '588', '589', '590', '591', '592', '593', '594', '595', '596', '597', '598', '599','600','target'])


train["target"] = label_binarize(train["target"], classes=["nonexist", "exist" ])
test["target"] = label_binarize(test["target"], classes=["nonexist", "exist" ])
train_labels = train["target"]
test_labels = test["target"]


print("XGBoost")
xgb = XGBClassifier()
# Train our classifier
xgb.fit(train[used_features].values, train_labels)

"""
# save model to file
pickle.dump(xgb, open("pima.pickle.dat", "wb"))

# some time later...

# load model from file
loaded_model = pickle.load(open("pima.pickle.dat", "rb"))
"""


# Predict and evaluate
preds = xgb.predict(test[used_features].values)
print("Number of mislabeled points out of a total {} points : {}, performance {:05.2f}%"
      .format(
          test.shape[0],
          (test["target"] != preds).sum(),
          100*(1-(test["target"] != preds).sum()/test.shape[0])
))

print("test_label" + " " + str(test_labels))

cm = confusion_matrix(test_labels, preds)
print (accuracy_score(test_labels, preds))
print (classification_report(test_labels, preds))
print(cm)
scr= f1_score(test_labels, preds)
print("F1 score: ",scr)
tn, fp, fn, tp = cm.ravel()
print("True positive",tp)
print("False positive",fp)
print("False negative",fn)
print("True negative",tn)
precision = tp/(tp+fp)
recall=tp/(tp+fn)
accuracy=(tp+tn)/(tp+tn+fp+fn)
f1_score = (2*precision*recall)/(precision+recall)
print("Precision", precision)
print("Recall", recall)
print("Accuracy", accuracy)
print("F1_Score ", f1_score)
