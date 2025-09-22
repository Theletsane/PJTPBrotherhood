import csv
import re
import requests
import time

input_csv = "complete_metrorail_stations.csv"
output_csv = "TrainStation.csv"

unique_stops = {}

def clean_stop(name: str) -> str:
    if not name:
        return ""
    name = name.strip().upper()
    name = name.replace('"', '')
    name = re.sub(r"\(.*?\)", "", name).strip()
    name = re.sub(r"[^A-Z0-9\s&/-]", "", name)
    name = re.sub(r"\s+", " ", name)
    return name

def fetch_coords_from_overpass(stop_name: str):
    query = f"""
    [out:json][timeout:25];
    (
      node["highway"="bus_stop"]["name"~"{stop_name}",i](around:20000,-34.0,18.5);
      node["public_transport"="stop_position"]["name"~"{stop_name}",i](around:20000,-34.0,18.5);
      node["amenity"="taxi"]["name"~"{stop_name}",i](around:20000,-34.0,18.5);
    );
    out center 1;
    """
    try:
        response = requests.post("https://overpass-api.de/api/interpreter", data=query)
        data = response.json()
        if data.get("elements"):
            first = data["elements"][0]
            return str(first.get("lat", "MISSING")), str(first.get("lon", "MISSING"))
    except Exception as e:
        print(f" Overpass query failed for {stop_name}: {e}")
    return "MISSING", "MISSING"

def reverse_geocode(lat: str, lon: str) -> str:
    if lat == "MISSING" or lon == "MISSING":
        return "MISSING"
    try:
        url = f"https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat={lat}&lon={lon}"
        headers = {"User-Agent": "StopsCSVScript/1.0"}
        response = requests.get(url, headers=headers)
        data = response.json()
        return data.get("display_name", "MISSING")
    except Exception as e:
        print(f" Reverse geocode failed for {lat}, {lon}: {e}")
        return "MISSING"

def verify_coordinates(stop_name: str, lat: str, lon: str) -> bool:
    """Return True if the coordinates are likely correct for the stop name."""
    address = reverse_geocode(lat, lon)
    time.sleep(1)
    if address == "MISSING":
        return False
    # Check if stop name appears in the address (case insensitive)
    return clean_stop(stop_name) in clean_stop(address)

with open(input_csv, mode="r", encoding="utf-8") as infile:
    reader = csv.DictReader(infile)
    for row in reader:
        stop_name = clean_stop(row['Name'])
        lat = row['Latitude'].strip() if row['Latitude'] else "MISSING"
        lon = row['Longitude'].strip() if row['Longitude'] else "MISSING"

        if not stop_name:
            continue

        # Verify coordinates
        correct = False
        if lat != "MISSING" and lon != "MISSING":
            print(f" Verifying coordinates for {stop_name}...")
            correct = verify_coordinates(stop_name, lat, lon)

        # If missing or incorrect, fetch from Overpass
        if not correct:
            print(f" Coordinates missing or incorrect for {stop_name}, fetching from Overpass...")
            lat, lon = fetch_coords_from_overpass(stop_name)
            time.sleep(1)

        # Avoid partial duplicates (keep longest name)
        duplicate = False
        for existing in list(unique_stops.keys()):
            if stop_name in existing or existing in stop_name:
                if len(stop_name) > len(existing):
                    unique_stops.pop(existing)
                    unique_stops[stop_name] = (lat, lon)
                duplicate = True
                break

        if not duplicate:
            unique_stops[stop_name] = (lat, lon)

# Add addresses
final_data = []
for stop, coords in unique_stops.items():
    lat, lon = coords
    print(f" Reverse geocoding {stop}...")
    address = reverse_geocode(lat, lon)
    time.sleep(1)
    final_data.append((stop, lat, lon, address))

with open(output_csv, mode="w", newline="", encoding="utf-8") as outfile:
    writer = csv.writer(outfile)
    writer.writerow(['stop_name', 'Latitude', 'Longitude', 'Address'])
    for row in final_data:
        writer.writerow(row)

print(f" ✅ {len(final_data)} stops with verified coordinates and addresses saved to {output_csv}")
