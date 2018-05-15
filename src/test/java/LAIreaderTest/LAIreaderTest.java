package LAIreaderTest;

import java.util.HashMap;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.shapefile.OmsShapefileFeatureReader;
import org.jgrasstools.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.jgrasstools.gears.io.timedependent.OmsTimeSeriesIteratorWriter;
import org.junit.Assert;
import org.junit.Test;
import LAI.LAIreaderMODIS;

public class LAIreaderTest {

	@Test
	public void test() throws Exception {


		OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
		reader.file ="resources/input/LAI_1.csv";
		reader.idfield = "ID";
		reader.tStart = "2013-12-19 00:00";
		reader.tTimestep = 60;
		reader.tEnd = "2013-12-27 23:00";
		reader.fileNovalue = "-9999";

		reader.initProcess();

		LAIreaderMODIS LAI= new LAIreaderMODIS();

		LAI.inFolder="/Users/marialaura/Dropbox/dati_NewAge/EsercitazioniIdrologia2017/data/Basilicata/lai";
		
		OmsShapefileFeatureReader stationsReader = new OmsShapefileFeatureReader();
		stationsReader.file = "/Users/marialaura/Dropbox/dati_NewAge/EsercitazioniIdrologia2017/data/Basento/centroids_basento_quote_OK.shp";
		stationsReader.readFeatureCollection();
		SimpleFeatureCollection stationsFC = stationsReader.geodata;


		OmsTimeSeriesIteratorWriter writer = new OmsTimeSeriesIteratorWriter();
		writer.file = "resources/output/LAI_prova.csv ";
		writer.tStart = reader.tStart;
		writer.tTimestep = reader.tTimestep;
		


		while( reader.doProcess ) {
			reader.nextRecord();
			LAI.tCurrent=reader.tCurrent;
			LAI.inStations = stationsFC;
			LAI.fStationsid = "ID";
			LAI.dataType="tif";
			LAI.scaleFactor=0.1;
			LAI.prj="/Users/marialaura/Dropbox/dati_NewAge/EsercitazioniIdrologia2017/data/Basento/prova.prj";

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
