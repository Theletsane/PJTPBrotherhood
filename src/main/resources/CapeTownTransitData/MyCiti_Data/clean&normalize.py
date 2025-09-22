import csv

def remove_stops_without_name(input_file, output_file):
    """
    Reads a CSV and writes only rows with a non-empty NAME field.
    """
    with open(input_file, newline='', encoding="utf-8") as infile:
        reader = csv.DictReader(infile)
        fieldnames = reader.fieldnames

        with open(output_file, "w", newline='', encoding="utf-8") as outfile:
            writer = csv.DictWriter(outfile, fieldnames=fieldnames)
            writer.writeheader()

            for row in reader:
                if row["NAME"].strip():  # keep only rows with non-empty name
                    writer.writerow(row)

def remove_duplicate_stops(input_file, output_file):
    """
    Reads a CSV and removes duplicate stops based on NAME (case-insensitive).
    Keeps the first occurrence of each NAME.
    """
    seen = set()

    with open(input_file, newline='', encoding="utf-8") as infile:
        reader = csv.DictReader(infile)
        fieldnames = reader.fieldnames

        with open(output_file, "w", newline='', encoding="utf-8") as outfile:
            writer = csv.DictWriter(outfile, fieldnames=fieldnames)
            writer.writeheader()

            for row in reader:
                name = row["NAME"].strip()
                name_key = name.lower()  # case-insensitive comparison
                if name and name_key not in seen:
                    seen.add(name_key)
                    writer.writerow(row)



if __name__ == "__main__":
    #remove_stops_without_name("myciti-bus-stops.csv","namelss_stops_removed.csv")
    remove_duplicate_stops("namelss_stops_removed.csv","no_dup_nameless_stops.csv")