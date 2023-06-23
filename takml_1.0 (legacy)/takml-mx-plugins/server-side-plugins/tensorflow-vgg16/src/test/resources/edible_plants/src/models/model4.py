
from __future__ import absolute_import, division, print_function, unicode_literals

from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Conv2D, Flatten, Dropout, MaxPooling2D, GlobalAveragePooling2D
from tensorflow.keras.preprocessing.image import ImageDataGenerator

import tensorflow as tf
print(tf.version.VERSION)
import csv

import os
import numpy as np
import matplotlib.pyplot as plt
import math
from models.utils import *
from tensorflow.keras.models import load_model


AUTOTUNE = tf.data.experimental.AUTOTUNE

class PlantClassification:
    
    
    def __init__(self, dict_params, for_training = False):
        '''
        for_training param: specify if you intend to train the model
        
        To train the model, the following params have to be specified:
        + taxon_file, train_data_folder, validation_data_folder, batch_size, train_output_directory, img_size
        
        To execute the model without training, the following params have to be specified:
        + taxon_file, img_size
        + optional: train_output_directory (to specify the default location of trained model and/or weights)
        '''
        if not for_training:
            self.taxon_file = dict_params["taxon_file"]
            self.img_size = dict_params["img_size"]
            self.train_output_directory = dict_params.get("train_output_directory")
        else:
            self.taxon_file = dict_params["taxon_file"]
            self.train_data_folder = dict_params["train_data_folder"]
            self.validation_data_folder = dict_params["validation_data_folder"]
            self.batch_size = dict_params["batch_size"]
            self.train_output_directory = dict_params["train_output_directory"]
            self.img_size = dict_params["img_size"]

#        Example:
#         list_of_taxon_ids=['123','456','789']
#         label_map={'123': 0, '456': 1, '789': 2}
    
        # list_of_taxon_ids
        self.label_map = {}
        self.list_of_taxon_ids = []
        class_index = 0
        with open(self.taxon_file, 'r') as f:
            csv_reader = csv.DictReader(f)
            for row in csv_reader:
                taxon_id = row["taxon_id"]
                self.list_of_taxon_ids.append(taxon_id)
                self.label_map[taxon_id] = class_index
                class_index += 1
        print("List of taxons: {}".format(self.list_of_taxon_ids))
        print("Number of classes: {}".format(len(self.list_of_taxon_ids)))
        
        if self.train_output_directory is not None:
            # Checkpoint default location
            self.checkpoint_dir = self.train_output_directory + "/checkpoints/"
    
            # Model default location
            model_dir = self.train_output_directory + "/models/"
            if not os.path.exists(model_dir):
                os.mkdir(model_dir)
            self.saved_model = model_dir + "my_model.h5";
        else:
            self.checkpoint_dir = None
            self.saved_model = None
      
    def _process_image(self, filename):
        image = tf.io.read_file(filename)
        image = tf.image.decode_jpeg(image, channels=3)
        image = tf.cast(image, tf.float32)
        image = (image/127.5) - 1
        image = tf.image.resize(image, (self.img_size, self.img_size))
        return image
    
    def _process_image_from_bytes(self, byte_array):
        image = tf.image.decode_jpeg(byte_array, channels=3)
        image = tf.cast(image, tf.float32)
        image = (image/127.5) - 1
        image = tf.image.resize(image, (self.img_size, self.img_size))
        return image
    
    def _process_image_and_label(self, x, y):
        image = self._process_image(x)
        return image, y

    def _create_dataset_pipeline(self, x, y, shuffle = True, shuffle_buffer_size = 500):
        ds = tf.data.Dataset.from_tensor_slices((x,y))
        ds = ds.map(self._process_image_and_label)
        if shuffle:
            ds = ds.shuffle(buffer_size=shuffle_buffer_size)
        ds = ds.repeat()
        ds = ds.batch(self.batch_size)
        ds = ds.prefetch(buffer_size=AUTOTUNE)
        return ds
    
    def _create_dataset_pipeline_for_prediction(self, x, batch_size):
        ds = tf.data.Dataset.from_tensor_slices(x)
        ds = ds.map(self._process_image)
        ds = ds.batch(batch_size)
        ds = ds.prefetch(buffer_size=AUTOTUNE)
        return ds
        

    def load_data(self, shuffle_buffer_size = 500):
        # train data
        self.list_of_filenames_train = []
        self.list_of_label_indices_train = []
        
        for taxon_id in self.list_of_taxon_ids:
            species_folder = os.path.join(self.train_data_folder, taxon_id)
            filenames = os.listdir(species_folder)
            for filename in filenames:
                self.list_of_filenames_train.append(os.path.join(species_folder, filename))
                self.list_of_label_indices_train.append(self.label_map[str(taxon_id)])
            print("Training set: Class {} has {} images".format(taxon_id, len(filenames)))
        
        self.train_dataset = self._create_dataset_pipeline(self.list_of_filenames_train, self.list_of_label_indices_train, shuffle = True, shuffle_buffer_size = shuffle_buffer_size)
        
        # validation data
        self.list_of_filenames_validation = []
        self.list_of_label_indices_validation = []
        
        for taxon_id in self.list_of_taxon_ids:
            species_folder = os.path.join(self.validation_data_folder, taxon_id)
            filenames = os.listdir(species_folder)
            for filename in filenames:
                self.list_of_filenames_validation.append(os.path.join(species_folder, filename))
                self.list_of_label_indices_validation.append(self.label_map[str(taxon_id)])
            print("Validation set: Class {} has {} images".format(taxon_id, len(filenames)))
        
        self.validation_dataset= self._create_dataset_pipeline(self.list_of_filenames_validation, self.list_of_label_indices_validation, shuffle = False)

    def create_model(self):
        IMG_SHAPE = (self.img_size, self.img_size, 3)
        VGG16_MODEL = tf.keras.applications.VGG16(input_shape=IMG_SHAPE,
                                                include_top=False,
                                                weights='imagenet')
        
        VGG16_MODEL.trainable=False
        
        global_average_layer = GlobalAveragePooling2D()
        prediction_layer = Dense(len(self.list_of_taxon_ids),activation='softmax')
        self.model = Sequential([
            VGG16_MODEL,
            global_average_layer,
            prediction_layer
        ])
        
#         self.model = Sequential([
#             VGG16_MODEL,
#             Flatten(),
#             Dense(units = 2048, activation='relu'),
#             Dropout(0.2),
#             Dense(units = 2048, activation='relu'),
#             Dropout(0.2),
#             Dense(units = len(self.list_of_taxon_ids), activation='softmax')
#         ])
        
#         self.model = Sequential([
#             VGG16_MODEL,
#             GlobalAveragePooling2D(),
#             Dense(units = 2048, activation='relu'),
#             Dropout(0.2),
#             Dense(units = len(self.list_of_taxon_ids), activation='softmax')
#         ])
        
        self.model.compile(optimizer='adam', 
              loss=tf.keras.losses.sparse_categorical_crossentropy,
              metrics=["accuracy"])
        
         
    def summarize_model(self):
        self.model.summary()
        
    def train(self, epochs, use_multiprocessing = False, workers = 1, validate_after_each_epoch = True):
        # Create a callback that saves the model's weights    
        checkpoint_file = self.checkpoint_dir + "cp-{epoch:04d}.ckpt"    
        cp_callback = tf.keras.callbacks.ModelCheckpoint(filepath=checkpoint_file,
                                                         save_weights_only=True,
                                                         verbose=1)
        
        if validate_after_each_epoch:
            self.history = self.model.fit(self.train_dataset,
                epochs=epochs,
                steps_per_epoch= math.ceil(len(self.list_of_filenames_train)/float(self.batch_size)),
                use_multiprocessing = use_multiprocessing,
                workers = workers,
                validation_steps = math.ceil(len(self.list_of_filenames_validation)/float(self.batch_size)),
                validation_data= self.validation_dataset,
                callbacks=[cp_callback]
                )
        else:
            self.history = self.model.fit(self.train_dataset,
                epochs=epochs,
                steps_per_epoch= math.ceil(len(self.list_of_filenames_train)/float(self.batch_size)),
                use_multiprocessing = use_multiprocessing,
                workers = workers,
                callbacks=[cp_callback]
                )
        
    def show_history_of_training(self):
        if self.history is None:
            print("No training history is available")
        else:
            print("history.history: {}".format(self.history.history))
            plt.plot(self.history.history['accuracy'])
            plt.plot(self.history.history['val_accuracy'])
            plt.title('model accuracy')
            plt.ylabel('accuracy')
            plt.xlabel('epoch')
            plt.legend(['train', 'test'], loc='upper left')
            plt.show()
        
    def load_weights(self, filename = None):
        if filename is None:
            # Load latest weights from the default location
            latest = tf.train.latest_checkpoint(self.checkpoint_dir)
            self.model.load_weights(latest)
        else:
            self.model.load_weights(filename)
                     
    def save_model(self, filename = None):
        if filename is None:
            #Save at default location
            self.model.save(self.saved_model)    
        else:
            self.model.save(filename)
                   
     
    def load_model(self, filename = None):
        if filename is None:
            #Load model from a default location
            self.model = load_model(self.saved_model) 
        else:
            self.model= load_model(filename) 
        
     
    def evaluate(self):
        loss0,accuracy0 = self.model.evaluate(self.validation_dataset, 
                                              steps = math.ceil(len(self.list_of_filenames_validation)/float(self.batch_size)))
        print("Evaluation loss: {:.2f}".format(loss0))
        print("Evaluation accuracy: {:.2f}".format(accuracy0))
    
        
    def predict_by_filename(self, filename):
        x = self._process_image(filename) 
        x = np.expand_dims(x, axis=0) # expand to [1, image_width, image_height, number_of_channels]
        prediction = self.model.predict(x)
        prediction = np.argmax(prediction)
        return prediction
    
    def predict_batch(self, folder, batch_size = 1, use_multiprocessing = False, workers = 1): # ??? How to know the order of files in the predictions 
        list_of_filenames = [os.path.join(folder, filename) for filename in os.listdir(folder)]
        prediction_dataset= self._create_dataset_pipeline_for_prediction(list_of_filenames, batch_size)
        predictions = self.model.predict(prediction_dataset,
                                        verbose=1,
                                        use_multiprocessing=use_multiprocessing,
                                        workers=workers
                                        )
        predictions = np.argmax(predictions, axis = 1)
        return predictions
    
    def predict_by_jpg_bytes(self, byte_array):
        x = self._process_image_from_bytes(byte_array) 
        x = np.expand_dims(x, axis=0) # expand to [1, image_width, image_height, number_of_channels]
        prediction = self.model.predict(x)
        prediction = np.argmax(prediction)
        return prediction
    
    def get_label_map(self):
        return self.label_map
    
    def get_classnames(self):
        return self.list_of_taxon_ids
    
    
