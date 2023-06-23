import sys

from models.model4 import *
from models.utils import TaxonLookup

if __name__ == '__main__':
    
    print(tf.__version__)

    model_file = sys.argv[1]
    data_file = sys.argv[2]
    output_file = sys.argv[3]
    taxon_file = sys.argv[4]
    taxon_lookup_file = sys.argv[5]

    params = {
        "taxon_file": taxon_file,
        "img_size": 224        
    }
    
    model = PlantClassification(params)
    model.load_model(model_file)
    
    model.summarize_model()
        
    with open(data_file, 'rb') as f:
        byte_array = f.read()
    prediction1 = model.predict_by_jpg_bytes(byte_array)
    print("Prediction by bytes: {}".format(prediction1))
    
    # From prediction (integer) to classnames (taxon_id)
    classnames = model.get_classnames()
    taxon_prediction1 = classnames[prediction1]
    
    # Look up taxon_information:
    taxon_lookup = TaxonLookup(taxon_lookup_file)
    taxon_info = taxon_lookup.lookup(taxon_prediction1)
    print("taxon_info: {}".format(taxon_info))
    print("Species: {}".format(taxon_info["species"]))
    print("genus: {}".format(taxon_info["genus"]))
    print("family: {}".format(taxon_info["family"]))
    print("order: {}".format(taxon_info["order"]))
    print("class: {}".format(taxon_info["class"]))
    print("phylum: {}".format(taxon_info["phylum"]))
    print("kingdom: {}".format(taxon_info["kingdom"]))
    print("is_edible: {}".format(taxon_info["is_edible"]))
    print("list_of_edible_parts: {}".format(taxon_info["list_of_edible_parts"]))

    f = open(output_file, "w")
    #f.write("taxon_info: {}\n".format(taxon_info))
    f.write("Species: {}\n".format(taxon_info["species"]))
    f.write("genus: {}\n".format(taxon_info["genus"]))
    f.write("family: {}\n".format(taxon_info["family"]))
    f.write("order: {}\n".format(taxon_info["order"]))
    f.write("class: {}\n".format(taxon_info["class"]))
    f.write("phylum: {}\n".format(taxon_info["phylum"]))
    f.write("kingdom: {}\n".format(taxon_info["kingdom"]))
    f.write("is_edible: {}\n".format(taxon_info["is_edible"]))
    f.write("list_of_edible_parts: {}\n".format(taxon_info["list_of_edible_parts"]))
    f.close()

    # More prediction functions
#     prediction2 = model.predict_by_filename(folder + "118850/1438168147.jpg") 
#     print("Prediction by filename: {}".format(prediction2))
#     
#     predictions = model.predict_batch(folder + "118850/", batch_size = 100) 
#     print("Predictions: {}".format(predictions))
    
    print("Done")

    
