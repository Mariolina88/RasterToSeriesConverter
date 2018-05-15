package LAIreaderTest;

import java.util.HashMap;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.shapefile.OmsShapefileFeatureReader;
import org.jgrasstools.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.jgrasstools.gears.io.timedependent.OmsTimeSeriesIteratorWriter;
import org.junit.Assert;
import org.junit.Test;
import LAI.LAIreaderMODISwebGIS;

public class LAIreaderWebGISTest {

	@Test
	public void test() throws Exception {


		OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
		reader.file ="resources/input/LAI_1.csv";
		reader.idfield = "ID";
		reader.tStart = "2013-12-19 00:00";
		reader.tTimestep = 60;
		reader.tEnd = "2013-12-19 02:00";
		reader.fileNovalue = "-9999";

		reader.initProcess();

		LAIreaderMODISwebGIS LAI= new LAIreaderMODISwebGIS();

		LAI.inFolder="/Users/marialaura/Dropbox/WebGis-NewAge/data/Basilicata/OMS";

		OmsShapefileFeatureReader stationsReader = new OmsShapefileFeatureReader();
		stationsReader.file = "/Users/marialaura/Dropbox/dati_NewAge/EsercitazioniIdrologia2017/data/Basento/17/centroids_ID_17.shp";
		stationsReader.readFeatureCollection();
		SimpleFeatureCollection stationsFC = stationsReader.geodata;


		OmsTimeSeriesIteratorWriter writer = new OmsTimeSeriesIteratorWriter();
		writer.file = "resources/output/LAI_prova.csv ";
		writer.tStart = reader.tStart;
		writer.tTimestep = reader.tTimestep;



		while( reader.doProcess ) {
			reader.nextRecord();
			LAI.inStations = stationsFC;
			LAI.fStationsid = "ID";
			LAI.dataType="tif";
			LAI.dataSensor="MOD15A2H";
			LAI.scaleFactor=0.1;

			LAI.process();

			HashMap<Integer, double[]> resultD = LAI.outLAIHM;



			writer.inData = resultD;
			writer.writeNextLine();

		}
		//
		reader.close();
		writer.close();


	}


}
