import os,sys

EN_SENSES = [
             'Temporal.Asynchronous.Precedence',
             'Temporal.Asynchronous.Succession',
             'Temporal.Synchrony',
             'Contingency.Cause.Reason',
             'Contingency.Cause.Result',
             'Contingency.Condition',
             'Comparison.Contrast',
             'Comparison.Concession',
             'Expansion.Conjunction',
             'Expansion.Instantiation',
             'Expansion.Restatement',
             'Expansion.Alternative',
             'Expansion.Alternative.Chosen alternative',
             'Expansion.Exception',
             'EntRel',
             ]

rare = {}
file = open("nonExp_train.txt","r")
for line in file:
    sense = line[line.rfind(' ')+1:-1]
    if(sense not in EN_SENSES):
        if(sense not in rare):
            rare[sense] = 1
        else:
            rare[sense] = rare[sense]+1
print(rare)

rare = {}
file = open("sense_train.txt","r")
for line in file:
    sense = line[line.rfind(' ')+1:-1]
    if(sense not in EN_SENSES):
        if(sense not in rare):
            rare[sense] = 1
        else:
            rare[sense] = rare[sense]+1
print(rare)
