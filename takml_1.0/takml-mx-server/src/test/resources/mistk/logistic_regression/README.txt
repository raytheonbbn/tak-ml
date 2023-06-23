This directory contains the MISTK model script/logistic_regression.py, which is used in unit tests. It also contains a sample binary model as well as the training and testing data that was used to build it.

The steps taken and parameters used to build this model using the MISTK REST interface are as follows:

initialize:

{ }

loadData:

{
    "train": {
        "objectInfo": {
            "name": "training_set",
            "kind": "MistkDataset"
        },
        "dataPath": "/tak-ml/server-side/takml-roger/src/test/resources/mistk/logistic_regression/train",
        "modality": "text",
        "format": "csv"
    },
    "test": {
        "objectInfo": {
            "name": "testing_set",
            "kind": "MistkDataset"
        },
        "dataPath": "/tak-ml/server-side/takml-roger/src/test/resources/mistk/logistic_regression/test",
        "modality": "text",
        "format": "csv"
    }
}

buildModel:

<none>

train:

<none>

saveModel:

/tak-ml/server-side/takml-roger/src/test/resources/mistk/logistic_regression/model
