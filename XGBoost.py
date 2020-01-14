from sklearn.metrics import accuracy_score, classification_report, confusion_matrix, roc_auc_score, f1_score
import pandas as pd
import numpy as np
from xgboost import XGBClassifier
import sys
from numpy import loadtxt
import matplotlib.pyplot as pyplot
from numpy import sort
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score,f1_score
from sklearn.feature_selection import SelectFromModel


train = loadtxt('train1.csv', delimiter=",")
test = loadtxt('test1.csv', delimiter=",")

train=np.array(train)
test=np.array(test)
train = train.astype(float)
test = test.astype(float)


train_X = train[:,0:1]
train_Y = train[:,1]


test_X = test[:,0:1]
test_Y = test[:,1]


xgb = XGBClassifier(max_depth=12,
                        objective='multi:softprob',
                        n_estimators=1000,
                        learning_rate = 0.001,
                        num_class = 2)
eval_set = [(train_X, train_Y), (test_X, test_Y)]
xgb.fit(train_X, train_Y, early_stopping_rounds=15, eval_metric=["merror","mlogloss"], eval_set=eval_set, verbose=True)


y_pred = xgb.predict(test_X)
predictions = [round(value) for value in y_pred]


"""
accuracy = accuracy_score(test_Y, predictions)
fscore = f1_score(test_Y, predictions)
print("Accuracy: %.2f%%" % (accuracy * 100.0))
print("Fscore: %.2f%%" % (fscore * 100.0))
"""

results = xgb.evals_result()
epochs = len(results['validation_0']['merror'])
epochs = len(results['validation_0']['mlogloss'])
x_axis = range(0, epochs)
# plot log loss
fig, ax = pyplot.subplots()
ax.plot(x_axis, results['validation_0']['mlogloss'], label='Train')
ax.plot(x_axis, results['validation_1']['mlogloss'], label='Test')
ax.legend()
pyplot.ylabel('mLog Loss')
pyplot.title('XGBoost mLog Loss')
pyplot.show()
# plot classification error

fig, ax = pyplot.subplots()
ax.plot(x_axis, results['validation_0']['merror'], label='Train')
ax.plot(x_axis, results['validation_1']['merror'], label='Test')
ax.legend()
pyplot.ylabel('Classification Error')
pyplot.title('XGBoost Classification Error')
pyplot.show()

print(xgb.feature_importances_)
thresholds = sort(xgb.feature_importances_)
for thresh in thresholds:
        # select features using threshold
        selection = SelectFromModel(xgb, threshold=thresh, prefit=True)
        select_X_train = selection.transform(train_X)
        # train model
        selection_model = XGBClassifier()
        selection_model.fit(select_X_train, train_Y)
        # eval model
        select_X_test = selection.transform(test_X)
        y_pred = selection_model.predict(select_X_test)
        predictions = [round(value) for value in y_pred]
        accuracy = accuracy_score(test_Y, predictions)
        fscore = f1_score(test_Y, predictions)
        print("Thresh=%.3f, n=%d, Accuracy: %.2f%%" % (thresh, select_X_train.shape[1], accuracy*100.0))
        print("Thresh=%.3f, n=%d, F-score: %.2f%%" % (thresh, select_X_train.shape[1], fscore*100.0))