##############################################################################
#
#    This program is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with this program.  If not, see <https://www.gnu.org/licenses/>.
#
##############################################################################

import logging
import os
import pickle 
import pandas
import base64
import json

from sklearn.linear_model import LogisticRegression
from mistk.abstract_model import AbstractModel

class ScikitLearnLogisticRegressionModel(AbstractModel):
    def __init__(self):
        AbstractModel.__init__(self)
        self._props = None
        self._hparams = None
        self._regr = None
        self._X_train = None
        self._Y_train = None
        self._X_test = None
        self._data_loaded = False
        self._predictions = None
        self._confidence = None
        self._model_file_name = 'scikit-logistic-regression-model.bin'
        self._objectives = None
  
    def do_initialize(self, objectives: list, props : dict, hparams : dict):
        self._props = props or {}
        self._hparams = hparams or {}
        self._objectives = objectives
        
        logging.info(self._props)
        
        if 'model_file_name' in self._props:
            self._model_file_name = self._props['model_file_name']
        
    def do_load_data(self, dataset_map: dict): 
        # check for and load training data and/or test data
        if 'train' not in dataset_map and 'test' not in dataset_map:
            raise RuntimeError('No datasets provided')
        if 'train' in dataset_map:
            dataset = dataset_map['train']
            data_array = self.read_dataset(dataset.data_path + '/data.csv')
            # NOTE this model is coded to this particular (arbitrary) dataset format
            self._X_train = data_array[:, 0:-1]
            self._Y_train = data_array[:, -1]
        if 'test' in dataset_map:
            dataset = dataset_map['test']
            data_array = self.read_dataset(dataset.data_path + '/data.csv')
            self._X_test = data_array[:, 0:-1]

    def do_build_model(self, path=None):
        if path:
            # if we got a path to a saved model then load it
            path = os.path.join(path, self._model_file_name)
            if os.path.exists(path):
                logging.debug("Loading model " + path)
                
                with open(path, mode='rb') as reader:
                    # we decided to use python pickle to save and load
                    # model checkpoints in this model, but other models
                    # are free to use any format they desire
                    self._regr = pickle.load(reader)
                assert isinstance(self._regr, LogisticRegression)
            else:
                self._regr = LogisticRegression()
        else:
            self._regr = LogisticRegression()

    def do_train(self):
        # train with our previously loaded data
        self._regr.fit(self._X_train, self._Y_train)
        self.update_status({"samples_fit": len(self._X_train)})
    
    def do_save_model(self, path):
        path = os.path.join(path, self._model_file_name)
        logging.info("Saving model to " + path)
        
        # just saving a simple 'pickled' model to disk
        with open(path, mode='wb') as writer:
            writer.write(pickle.dumps(self._regr))
            
    def do_pause(self):
        # this model doesn't support 'pause'
        raise NotImplementedError()

    def do_resume_training(self):
        raise NotImplementedError()
    
    def do_resume_predict(self):
        raise NotImplementedError()
    
    def do_predict(self):
        # predict with our previously loaded data
        self._predictions = self._regr.predict(self._X_test)
        self._confidence = self._regr.predict_proba(self._X_test)[:,1]
        self.update_status({"samples_predicted": len(self._X_test)})
    
    def do_save_predictions(self, dataPath):
        dataPath = os.path.join(dataPath, "predictions.csv")
        logging.info("Saving predictions to " + dataPath)
        
        with open(dataPath, mode='w') as writer:
            for i in range(self._predictions.shape[0]):
                # write out a results csv that can be evaluated
                writer.write(str(i) + "," + str(self._predictions[i])
                    + "," + str(self._confidence[i]) + "\n")
                
    def do_stream_predict(self, data_map: dict):
        predictions = {}
        for key, value in data_map.items():
            logging.debug('Predicting class for key ' + key)
            data_row = json.loads(base64.b64decode(value))
            prediction = self._regr.predict([data_row])
            # Convert from int64 to int to be able to encode it
            predictions[key] = int(prediction[0])
        return predictions

    def do_terminate(self):
        pass

    def do_reset(self):
        pass
        
    def read_dataset(self, data_path):
        logging.debug("Loading dataset from %s", data_path)
        with open(data_path) as reader:
            dataframe = pandas.read_csv(reader, header=None)
        return dataframe.values
