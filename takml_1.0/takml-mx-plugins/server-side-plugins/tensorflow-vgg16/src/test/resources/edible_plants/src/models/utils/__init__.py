
import csv

def read_taxon_info_file(taxon_info_file):
    '''
    Read a taxon_info_file return a dictionary of taxon_ids ==> info
    '''
    taxons_info = {}
    with open(taxon_info_file, mode="r", encoding="utf-8-sig") as csv_file:
        csv_reader = csv.DictReader(csv_file)
        headers = csv_reader.fieldnames
        print("Headers: {}". format(headers))
        
        for row in csv_reader:
            taxon_id = row['taxon_id'].strip().lower()
            taxons_info[taxon_id] = row
    
    print("Taxon_lookup file: {}. Number of taxons: {}".format(taxon_info_file, len(taxons_info)))

    return taxons_info

class TaxonLookup:
    
    def __init__(self, taxon_info_file):
        self.taxons_info = read_taxon_info_file(taxon_info_file)
        
    def lookup(self, taxon_id):
        return self.taxons_info.get(taxon_id, None)
        
        
