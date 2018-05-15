package precipiReaderTest;

import java.util.HashMap;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.shapefile.OmsShapefileFeatureReader;
import org.jgrasstools.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.jgrasstools.gears.io.timedependent.OmsTimeSeriesIteratorWriter;
import org.junit.Assert;
import org.junit.Test;

import precipitation.PrecipitationReaderGRIB;

public class PrecipitationReaderTest {

	@Test
	public void test() throws Exception {


		OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
		reader.file ="/Users/marialaura/Dropbox/WebGis-NewAge/data/Basilicata/OMS/temperature.csv";
		reader.idfield = "ID";
		reader.tStart = "2018-03-09 12:00";
		reader.tTimestep = 60;
		reader.tEnd = "2018-03-09 18:00";
		reader.fileNovalue = "-9999";

		reader.initProcess();

		PrecipitationReaderGRIB precip= new PrecipitationReaderGRIB();

		precip.inFolder="/Users/marialaura/Dropbox/WebGis-NewAge/data/Basilicata/OMS/";
		
		OmsShapefileFeatureReader stationsReader = new OmsShapefileFeatureReader();
		stationsReader.file = "/Users/marialaura/Dropbox/dati_NewAge/EsercitazioniIdrologia2017/data/Basento/19/centroids_ID_19.shp";
		stationsReader.readFeatureCollection();
		SimpleFeatureCollection stationsFC = stationsReader.geodata;


		OmsTimeSeriesIteratorWriter writer = new OmsTimeSeriesIteratorWriter();
		writer.file = "/Users/marialaura/Dropbox/dati_NewAge/EsercitazioniIdrologia2017/data/Basilicata/prova_1.csv";
		writer.tStart = reader.tStart;
		writer.tTimestep = reader.tTimestep;
		


		while( reader.doProcess ) {
			reader.nextRecord();
			precip.tStart=reader.tStart;
			precip.tCurrent=reader.tCurrent;
			precip.inStations = stationsFC;
			precip.fStationsid = "ID";
			precip.dataType="tif";
			precip.LAIdataSensor="MOD15A2H";

			precip.process();

			HashMap<Integer, double[]> resultD = precip.outPrecipitationHM;

			//System.out.println(precip.inFolderPathComplete);

			writer.inData = resultD;
			writer.writeNextLine();

		}
		//
		reader.close();
		writer.close();


	}


}
