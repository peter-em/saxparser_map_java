# saxparser_map_java
University project using SAXParser to scan openstreetmap file in order to create graph data.

Pproject finished and presented for evaluation in early november, 2016 by Piotr Matusz.
It scans a piece of map from openstreetmap.org saved in osm xml file format.
Then it produces txt file with lines containing info about:
 - rectangle bounds of map (in degrees)
 - number of edges (N) - roads available for car traffic
 - N blocks of lines where:
  	- first line represents edge data separated by colon
  	  (Street Name:length in metres:number V of vertices in this road)
  	- V lines of coordinates (latitude:longitude)
 
Example file krk_small.osm is included.
