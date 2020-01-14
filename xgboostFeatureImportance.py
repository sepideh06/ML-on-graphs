from numpy import loadtxt
from xgboost import XGBClassifier
from xgboost import plot_importance
from matplotlib import pyplot
import numpy as np

data = loadtxt('traintest.csv', delimiter=",")


dataset=np.array(data)
dataset = dataset.astype(float)


X = dataset[:,0:5]
Y = dataset[:,5]

# fit model no training data
model = XGBClassifier()
model.fit(X, Y)
# plot feature importance
plot_importance(model)
pyplot.show()

print(model.feature_importances_)
pyplot.bar(range(len(model.feature_importances_)), model.feature_importances_)
pyplot.show()



# use feature importance for feature selection
from numpy import sort
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score,f1_score
from sklearn.feature_selection import SelectFromModel
# split data into X and y
train = loadtxt('train.csv', delimiter=",")
test = loadtxt('test.csv', delimiter=",")

train=np.array(train)
test=np.array(test)
train = train.astype(float)
test = test.astype(float)





X_train = train[:,0:5]
y_train = train[:,5]


X_test = test[:,0:5]
y_test = test[:,5]
# fit model on all training data
model = XGBClassifier()
model.fit(X_train, y_train)
# make predictions for test data and evaluate
y_pred = model.predict(X_test)
predictions = [round(value) for value in y_pred]
accuracy = accuracy_score(y_test, predictions)
print("Accuracy: %.2f%%" % (accuracy * 100.0))
fscore = f1_score(y_test, predictions)
print("Accuracy: %.2f%%" % (accuracy * 100.0))
print("Fscore: %.2f%%" % (fscore * 100.0))
# Fit model using each importance as a threshold
thresholds = sort(model.feature_importances_)
for thresh in thresholds:
        # select features using threshold
        selection = SelectFromModel(model, threshold=thresh, prefit=True)
        select_X_train = selection.transform(X_train)
        # train model
        selection_model = XGBClassifier()
        selection_model.fit(select_X_train, y_train)
        # eval model
        select_X_test = selection.transform(X_test)
        y_pred = selection_model.predict(select_X_test)
        predictions = [round(value) for value in y_pred]
        accuracy = accuracy_score(y_test, predictions)
        fscore = f1_score(y_test, predictions)
        print("Thresh=%.3f, n=%d, Accuracy: %.2f%%" % (thresh, select_X_train.shape[1], accuracy*100.0))
        print("Thresh=%.3f, n=%d, F-score: %.2f%%" % (thresh, select_X_train.shape[1], fscore*100.0))