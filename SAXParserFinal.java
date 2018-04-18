/**
 * Projekt uczelniany ukończony i zaprezentowany do oceny
 * na początku listopada, 2016, wykonany przez Piotr Matusz.
 * Program skanuje fragment mapy z openstreetmap.org
 * zapisanego w pliku formatu osm xml.
 * Następnie produkuje plik txt zawierający natstępujące informacje:
 * - współrzędne prostokątnej granicy wycinka (w stopniach),
 * - liczbę krawędzi (N) - dróg przeznaczonych do ruchu samochodowego,
 * - N bloków, gdzie:
 * 	- pierwsza linia reprezentuje dane krawędzi rozdzielone dwukropkiem
 * 	 (Nazwa ulicy:długość w metrach:liczba V wierzchołków w drodze)
 * 	- V linii ze współrzędnymi wierzchołków (szerokość:długość) w stopniach
 */

import java.awt.*;
import javax.swing.*;
import java.awt.geom.*;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.util.*;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

//klasa nowego obszaru rysowania
class Obszar extends JPanel {
	private int w, h;
	private double xDist, yDist, xLm, yLm;
	private Handler objh;

	Obszar(Handler hndlr) {
		objh = hndlr;
	}
	@Override
	public void paintComponent(Graphics gOb) {
		w = getWidth();
		h = getHeight();
		xLm = objh.minLon;
		yLm = objh.minLat;
		xDist = objh.maxLon - xLm;
		yDist = objh.maxLat - yLm;

		super.paintComponent(gOb);
		doDrawing(gOb);
	}
	private void doDrawing(Graphics gOb) {

		Graphics2D g2Ob = (Graphics2D) gOb.create();

		g2Ob.translate(0.0, (h-1));
		double xdsc, ydsc;
		xdsc = (w/xDist);
		ydsc = (h/yDist);
		BasicStroke strk = new BasicStroke(0.00006f);
		g2Ob.setStroke(strk);
		g2Ob.scale(xdsc, -ydsc);
		g2Ob.setColor(new Color(0,100,200));
		Line2D.Double dline = new Line2D.Double();

		VertCoord crd1, crd2;
		for (int i = 0; i < objh.getEdges().size(); i++) {
			MapEdge edgeTmp = objh.getEdges().get(i);
			//if (eTmp.toDelete)
				//g2Ob.setColor(new Color(0,255,0));
			crd1 = edgeTmp.edgeCoords.get(0);
			for (int j = 1; j < edgeTmp.edgeCoords.size(); j++) {
				crd2 = edgeTmp.edgeCoords.get(j);
				dline.setLine(crd1.getLon()-xLm, crd1.getLat()-yLm, crd2.getLon()-xLm, crd2.getLat()-yLm);
				g2Ob.draw(dline);
				crd1 = crd2;
			}
			//g2Ob.setColor(new Color(0,100,200));
		}

		/*g2Ob.setColor(new Color(255,0,0));
		Rectangle2D.Double dpoint = new Rectangle2D.Double();
		for (int i = 0; i < objh.vertArray.size(); i++) {
			vTmp = objh.vertArray.get(i);
			dpoint.setRect(vTmp.vertLon-xLm, vTmp.vertLat-yLm, 0.00001, 0.00001);
			g2Ob.draw(dpoint);
		}*/

		g2Ob.dispose();

	}
}

//klasa nowego okna
class NewWindow extends JFrame {
	NewWindow(Handler hndlr) {
		initUI(hndlr);
	}
	private void initUI(Handler hndlr) {
		add(new Obszar(hndlr));
		setTitle("Mapa_drogi");
		setSize(1600+16, 900+39);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}
}

public class SAXParserFinal {

	static public String fileName = "krk_small.osm";

	public static void main(String[] args) {
		final long startTime = System.currentTimeMillis();
		//utworzenie obiektu testowego handlera TestHandler
		Handler thandler = new Handler();
		try {
			//deklaracja obiektu File, wczytanie pliku xml mapy
			File inputFile = new File(fileName);
			//utworzenie nowej instancji typu SAXParserFactory
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			//wywolanie parsera, przekazujac mu obiekt czytanego pliku oraz obiekt handlera
			saxParser.parse(inputFile, thandler);
			//System.out.println("Liczba wezlow w pliku: " + thandler.nodeCounter);
		} catch (Exception e) {
			e.printStackTrace();
		}
		final long endTime = System.currentTimeMillis();
		System.out.println("Czas wykonania w milisekundach: " + (endTime - startTime));

		//rysowanie drog w mapie
		NewWindow ex = new NewWindow(thandler);
		ex.setVisible(true);

	}
}

//klasa przechowujaca dane pojedynczej krawedzi
class MapEdge {
	ArrayList<String> edgeRefs = new ArrayList<>(8);
	ArrayList<VertCoord> edgeCoords = new ArrayList<>(8);
	String edgeName = "no_name";
	float length;
}

//klasa przechowujaca dane wierzcholka (skrzyzowania)
class MapVert {
	String vertId;
	double vertLat, vertLon;
}

class Handler extends DefaultHandler {

	private boolean isHighway = false;
	private boolean isWay = false;
	private boolean readBounds = true;
	private int highwayCounter = 0;
	private int nodeCounter = 0;
	private int countEdges = 0;							//licznik dzielonych drog
	double minLat, minLon, maxLat, maxLon;				//wspolrzedne krancow mapy
	private MapVert tmpVert;
	private String strtName = "no_name";
	private DistanceMetres dist = new DistanceMetres();

	private ArrayList<MapEdge> tmpEdgeArray = new ArrayList<>(300);		//poczatkowy zbior krawedzi przed ew. dzieleniem
	private ArrayList<MapEdge> edgeArray = new ArrayList<>(600);		 	//zbior krawedzi
	private ArrayList<MapVert> vertArray = new ArrayList<>(500);			//zbior wierzcholkow
	private ArrayList<String> nodeIds = new ArrayList<>(50000);			//tablica numerow wszystkich wezlow
	private ArrayList<VertCoord> nodeCoords = new ArrayList<>(50000);	//tablica wspolrzednych wszystkich wezlow
	private ArrayList<String> tmpEdge = new ArrayList<>(10);				//tablica tymczasowa refow
	private Set<String> uniqRefsAll = new HashSet<>(50);					//kontener unikalnych wierzcholkow czytanych krancow


	//przeslonieta standardowa metoda startElement DefaultHandlera
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes)
			  throws SAXException {

		//wczytanie krancowych wymiarow fragmentu mapy
		if (readBounds && qName.equalsIgnoreCase("bounds")) {
			minLat = Double.parseDouble(attributes.getValue("minlat"));
			minLon = Double.parseDouble(attributes.getValue("minlon"));
			maxLat = Double.parseDouble(attributes.getValue("maxlat"));
			maxLon = Double.parseDouble(attributes.getValue("maxlon"));
			readBounds = false;

			//zliczanie tagow <node>, dodawanie numerow ref do kontenera nodeIds
			//dodawanie odpowiadajacych im wspolrzednych to kontenera nodeCoords
		} else if (qName.equalsIgnoreCase("node")) {
			nodeIds.add(attributes.getValue("id"));

			double lat = Double.parseDouble(attributes.getValue("lat"));
			double lon = Double.parseDouble(attributes.getValue("lon"));
			nodeCoords.add(new VertCoord(lat, lon));
			nodeCounter++;

			//warunek kluczowy, czy tag jest poczatkiem drogi
		} else if (qName.equalsIgnoreCase("way")) {
			isWay = true;

			//instrukcje wykonywane tylko kiedy parser jest w bloku <way></way>
		} else if (isWay) {

			//zapisywanie wartosci ref z tagow <nd>
			//w bloku <way> do tymczasowej tablicy tmpEdge
			if (qName.equalsIgnoreCase("nd"))
				tmpEdge.add(attributes.getValue("ref"));

				//przeszukiwanie tagow <tag>
			else if (qName.equalsIgnoreCase("tag")) {
				String tagName = attributes.getValue("k");

				//sprawdzenie wystapienia klucza "area", jezeli tak, odrzuca droge
				if (tagName.equalsIgnoreCase("area"))
					isWay = false;

					//sprawdzanie klucza k w tagu w poszukiwaniu slowa kluczowego "highway"
				else if (tagName.equalsIgnoreCase("highway")) {
					tagName = attributes.getValue("v");
					//sprawdzanie wartosci v, jezeli jest:
					//"footway", "service", "cycleway", "steps", "path"
					//to odrzuc droge
					//w innym wypadku akceptuje droge,
					//co pozwala na poszukiwanie klucza "name" w kolejnych tagach
					if (tagName.equalsIgnoreCase("footway") || tagName.equalsIgnoreCase("service")
							  || tagName.equalsIgnoreCase("platform") || tagName.equalsIgnoreCase("cycleway")
							  || tagName.equalsIgnoreCase("path") || tagName.equalsIgnoreCase("steps")
							  || tagName.equalsIgnoreCase("bus_stop") || tagName.equalsIgnoreCase("track"))
						isWay = false;
					else
						isHighway = true;
				} else if (isHighway && tagName.equalsIgnoreCase("name")) {
					//odczytuje nazwe drogi, jezeli flaga isHighway jest prawda i "name" jest kluczem
					strtName = attributes.getValue("v");
				}
			}
		}
	}

	private boolean checkVert(double dLat, double dLon) {
		return (dLat < minLat) || (dLat > maxLat) || (dLon < minLon) || (dLon > maxLon);
	}

	private void addVert(String vertRef, double tVertLat, double tVertLon) {
		//jezeli nie ma jeszcze takiego wierzcholka, dodaj go do
		//tablicy vertArray jesli jego wspolrzedne mieszcza sie w granicach <bound>
		if (uniqRefsAll.add(vertRef)) {
			if (checkVert(tVertLat, tVertLon)) {
				tmpVert = new MapVert();
				tmpVert.vertId = vertRef;
				tmpVert.vertLat = tVertLat;
				tmpVert.vertLon = tVertLon;
				vertArray.add(tmpVert);
			}
		}
	}

	//przeslonieta standardowa metoda endElement handlera
	@Override
	public void endElement(String uri, String localName, String qName)
			  throws SAXException {
		//operacje wykonywane przy wychodzeniu z bloku <way>
		if (qName.equalsIgnoreCase("way")) {
			//obsluga prawidlowej drogi - flaga isHighway
			if (isHighway) {

				//utworzenie nowego obiektu MapEdge oraz ustawienie jego skladowych
				MapEdge tmpMapEdge = new MapEdge();

				//poszerzenie kontenerow w obiekcie MapEdge do wymaganych rozmiarow jesli trzeba
				tmpMapEdge.edgeCoords.ensureCapacity(tmpEdge.size());
				tmpMapEdge.edgeRefs.ensureCapacity(tmpEdge.size());

				//skopiowanie zawartosci kontenera tmpEdge do kontenera edgeRefs w nowym obiekcie MapEdge
				tmpMapEdge.edgeRefs.addAll(tmpEdge);

				//dodanie wspolrzednych odpowiadajacych numerom Ref z kontenera edgeRefs
				//do kontenera edgeCoords nowego obiektu MapEdge
				for (String tmpStr : tmpEdge) {

					VertCoord tmpCoord = nodeCoords.get(nodeIds.indexOf(tmpStr));
					tmpMapEdge.edgeCoords.add(new VertCoord(tmpCoord.getLat(), tmpCoord.getLon()));
				}

				//zaktualizowanie zmiennej nazwy drogi obiektu MapEdge, jezeli zostala jakas wczytana
				if (!strtName.equalsIgnoreCase("no_name")) {
					tmpMapEdge.edgeName = strtName;
					strtName = "no_name";
				}

				//dodanie utworzonego obiektu MapEdge do tablicy tmpEdgeArray
				tmpEdgeArray.add(tmpMapEdge);
				//zwiekszanie licznika znalezionych drog w calosci
				highwayCounter++;
			}
			//przestawienie flag do wartosci wyjsciowych oraz oproznienie tablicy tymczasowej
			tmpEdge.clear();
			isWay = false;
			isHighway = false;
		}
	}

	public ArrayList<MapEdge> getEdges() { return edgeArray;	}

	void printToFile()
			  throws FileNotFoundException {

		String [] fName = (SAXParserFinal.fileName).split("\\.");
		PrintWriter toFile = new PrintWriter(fName[0].concat("_wynik.txt"));

		//Wypisanie danych statystycznych start
		toFile.println("Wymiary krancowe fragmentu mapy:");
		toFile.println("MinLat: " + minLat + ", MaxLat: " + maxLat);
		toFile.println("MinLon: " + minLon + ", MaxLon: " + maxLon);
		toFile.println();
		toFile.println("Liczba wczytanych drog: " + edgeArray.size());
		toFile.println("Liczba wczytanych wierzcholkow: " + vertArray.size());
		toFile.println();
		//wypisanie danych statystycznych koniec

		//wypisywanie danych wczytanych drog: nazwa, dlugosc drogi
		toFile.println("Wczytane drogi:");
		MapEdge tmpEdg;
		for (int i = 0; i < edgeArray.size(); i++) {
			tmpEdg = edgeArray.get(i);
			toFile.println((i + 1) + ". Nazwa drogi: " + tmpEdg.edgeName + ", dlugosc: " + tmpEdg.length + " m");
		}
		toFile.println();

		//wypisywanie danych wczytanych wierzcholkow:
		toFile.println("Wczytane wierzcholki:");
		toFile.println("\tNr.Ref.\t\tWspolrzedne:");
		for (int i = 0; i < vertArray.size(); i++) {
			tmpVert = vertArray.get(i);
			toFile.println((i+1) + ".\t" + tmpVert.vertId + "\t" + tmpVert.vertLat + ",\t" + tmpVert.vertLon);
		}
		toFile.println();

		toFile.close();
	}

	// drukuje dane wykorzystywane w projekcie poszukiwania sciezki
	private void printDataToFile()
			  throws FileNotFoundException {

		String [] fName = (SAXParserFinal.fileName).split("\\.");
		PrintWriter toFile = new PrintWriter(fName[0].concat("_graph_data.txt"));

		toFile.println(minLat + ":" + minLon + ":" + maxLat + ":" + maxLon);
		toFile.println(edgeArray.size());
		//wypisywanie danych wczytanych drog: nazwa, dlugosc,
		//liczba wspolrzednych/refow, wypisanie listy: nr ref, dlugosc, szerokosc

		VertCoord tmpVrt;
		for (MapEdge tmpEdg : edgeArray) {
			int refsSize = tmpEdg.edgeRefs.size();
			toFile.println(tmpEdg.edgeName + ":" + tmpEdg.length + ":" + refsSize);
			for (int j = 0; j < refsSize; j++) {
				tmpVrt = tmpEdg.edgeCoords.get(j);
				toFile.println(tmpVrt.getLat() + ":" + tmpVrt.getLon());
			}
		}

		toFile.close();
	}

	//----------------------- DANE DO PLIKU KACPER ---------------------------//
	void printDataKacper()
			  throws FileNotFoundException {

		int nodesSize = nodeIds.size();
		VertCoord tCoord;
		int i, j;

		String [] fName = (SAXParserFinal.fileName).split("\\.");
		PrintWriter toFile = new PrintWriter(fName[0].concat("_dane_dla_kacpra.txt"));

		toFile.println("#bounds");
		toFile.println(minLat + ":" + minLon + ":" + maxLat + ":" + maxLon);
		toFile.println("#node");

		for (i = 0; i < nodesSize; i++) {
			tCoord = nodeCoords.get(i);
			toFile.println(nodeIds.get(i) + ":" + tCoord.getLat() + ":" + tCoord.getLon());
		}

		int countNoNames = 1;
		toFile.println("#way");
		MapEdge tEdge;
		for (i = 0; i < edgeArray.size(); i++) {
			tEdge = edgeArray.get(i);
			if (tEdge.edgeName.equalsIgnoreCase("brak")) {
				toFile.print("No Name nr " + (countNoNames++) + ":");
			} else {
				toFile.print(tEdge.edgeName + ":");
			}
			for (j = 0; j < tEdge.edgeRefs.size(); j++) {
				toFile.print(tEdge.edgeRefs.get(j) + ":");
			}
			toFile.println();

		}
		toFile.close();
	}

	//------------------------------ DANE NA KONSOLE ------------------------------//
	private void printToConsole() {
		// metoda uzywana do debugowania
		//Wypisanie danych statystycznych start
		//System.out.println("Wymiary krancowe fragmentu mapy:");
		//System.out.println("MinLat: " + minLat + ", MaxLat: " + maxLat);
		//System.out.println("MinLon: " + minLon + ", MaxLon: " + maxLon);
		System.out.println("Liczba wszystkich wspolrzednych <node>: " + nodeCounter);
		System.out.println();
		System.out.println("Liczba wczytanych drog poczatkowo: " + highwayCounter);
		System.out.println("Liczba wczytanych drog ostatecznie: " + edgeArray.size());
		System.out.println("Liczba podzielonych drog: " + countEdges);
		System.out.println("Liczba wczytanych wierzcholkow: " + vertArray.size());
		System.out.println();
		//wypisanie danych statystycznych koniec

		//wypisywanie danych wczytanych drog: nazwa, ilosc refow, id drogi, lista wspolrzednych
		//System.out.println("Zawartosc wektora krawedzi:");
		//MapEdge tmpEdg;
		//VertCoord tmpVC;
		//int cntr = 0;
		/*for (int i = 0; i < edgeArray.size(); i++) {
			tmpEdg = edgeArray.get(i);
			//System.out.println((i + 1) + ". Nazwa drogi: " + tmpEdg.edgeName + ", dlugosc: " + tmpEdg.length + " m");
			//System.out.println("Numer Id drogi: " + tmpEdg.wayId);
			//System.out.println("Lista wspolrzednych:");
			//for (int j = 0; j < tmpEdg.edgeCoords.size(); j++) {
			//	tmpVC = tmpEdg.edgeCoords.get(j);
			//	System.out.println("\t" + (j + 1) + ". " + tmpVC.latVal + ",\t" + tmpVC.lonVal);
		}*/
		System.out.println();
		//}
	}

	private boolean findRefInEdge(String ref, int ArraySize, int current) {
		MapEdge findE;
		for (int i = 0; i < ArraySize; i++) {
			if (i == current)
				continue;
			findE = tmpEdgeArray.get(i);
			for (int j = 0; j < findE.edgeRefs.size(); j++) {
				if (ref.equalsIgnoreCase(findE.edgeRefs.get(j)))
					return true;
			}
		}
		return false;
	}

	//przeslonieta standardowa metoda endDocument DefaultHandlera
	@Override
	public void endDocument()
			  throws SAXException {
		//przejscie tablicy drog i ewentualne tworzenie z nich krotszych
		boolean hasCrossing = false;
		int newBegin, readSize;
		int tmpEdgArrSize = tmpEdgeArray.size();
		for (int i = 0; i < tmpEdgArrSize; i++) {
			//zerowanie zmiennej poczatku nowej drogi
			newBegin = 0;
			//czytanie obiektu MapEdge z kontenera
			MapEdge readEdge = tmpEdgeArray.get(i);
			readSize = readEdge.edgeRefs.size();

			//dodawanie krancow drogi do listy wierzcholkow
			VertCoord tmpCoord = readEdge.edgeCoords.get(0);
			addVert(readEdge.edgeRefs.get(0), tmpCoord.getLat(), tmpCoord.getLon());
			tmpCoord = readEdge.edgeCoords.get(readSize-1);
			addVert(readEdge.edgeRefs.get(readSize-1), tmpCoord.getLat(), tmpCoord.getLon());

			//sprawdzanie wewnetrznych wierzcholkow drogi (bez krancowych)
			//czy wystepuja w innych drogach, z pominieciem aktualnie
			//czytanej krawedzi (sprawdzanie w funkcji findRefInEdge)
			MapEdge newE;
			for (int j = 1; j < readSize-1; j++) {
				if (findRefInEdge(readEdge.edgeRefs.get(j), tmpEdgArrSize, i)) {
					//dodawanie znalezionego skrzyzowania do listy wierzcholkow
					tmpCoord = readEdge.edgeCoords.get(j);
					addVert(readEdge.edgeRefs.get(j), tmpCoord.getLat(), tmpCoord.getLon());

					//inicjalizacja nowej krotszej krawedzi danymi z dzielonej drogi
					newE = new MapEdge();
					newE.edgeName = readEdge.edgeName;
					for (int c = newBegin; c < j+1; c++) {
						newE.edgeRefs.add(readEdge.edgeRefs.get(c));
						newE.edgeCoords.add(readEdge.edgeCoords.get(c));
					}
					//aktualizacja zmiennej poczatku kolejnej krotszej drogi
					newBegin = j;
					//dodanie nowej krawedzi do kontenera edgeArray
					edgeArray.add(newE);
					hasCrossing = true;
				}
			}
			//dodawanie ostatniego krotszego odcinka z dzielonej drogi
			//do kontenera edgeArray jezeli takie dzielenie wystapilo
			if (hasCrossing) {
				newE = new MapEdge();
				newE.edgeName = readEdge.edgeName;
				for (int c = newBegin; c < readSize; c++) {
					newE.edgeRefs.add(readEdge.edgeRefs.get(c));
					newE.edgeCoords.add(readEdge.edgeCoords.get(c));
				}
				edgeArray.add(newE);
				hasCrossing = false;
				countEdges++;
			} else
			//dodanie drogi w calosci jezeli zadna inna droga jej nie przecina
				edgeArray.add(readEdge);
		}
		//skasowanie drog z kontenera tmpEdgeArray, usuwane sa referencje
		//jezeli referencje wskazywaly na drogi ktore zostaly podzielone
		//to garbageCollector usunie obiekty tych drog (brak innych referencji)
		//zmniejszenie pojemnosci kontenera do 0 prez metode trimToSize
		tmpEdgeArray.clear();
		tmpEdgeArray.trimToSize();

		//obliczanie dlugosci kazdej drogi w kontenerze edgeArray
		VertCoord crd1, crd2;
		for (MapEdge readEdge : edgeArray) {
			double tmpLengthNew = 0.0;
			for (int i = 0; i < readEdge.edgeCoords.size() - 1; i++) {
				crd1 = readEdge.edgeCoords.get(i);
				crd2 = readEdge.edgeCoords.get(i + 1);
				tmpLengthNew += dist.calcDistance(crd1, crd2);
			}
			readEdge.length = (float)tmpLengthNew;
		}

		//wypisanie danych statystycznych, nazw i dlugosci drog
		//oraz wspolrzedne wierzcholkow do pliku
		try {
			//printToFile();
			printDataToFile();
			//printDataKacper();
		} catch (Exception e) {
			e.printStackTrace();
		}

		//wypisanie danych statystycznych oraz nazwy
		//i dlugosci drog na terminal
		printToConsole();

		//classEnd
	}
}