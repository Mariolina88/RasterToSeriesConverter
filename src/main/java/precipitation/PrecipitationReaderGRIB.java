package precipitation;

import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;


import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.DirectPosition2D;
import org.jgrasstools.gears.io.rasterreader.OmsRasterReader;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class PrecipitationReaderGRIB {

	@Description("Input folder with the map files")
	@In
	public String inFolder;

	@Description("The current time step.")
	@In
	public String tCurrent;

	@Description("The first time step.")
	@In
	public String tStart;

	@Description("data type: tif, tiff, asc")
	@In
	public String dataType;

	@Description("the name od the sensor of the LAI, necessary to distinguish the files in the folder")
	@In
	public String LAIdataSensor;

	@Description("The shape file with the station measuremnts")
	@In
	public SimpleFeatureCollection inStations;

	@Description("The name of the field containing the ID of the station in the shape file")
	@In
	public String fStationsid;

	@Description("The name of the Basin")
	@In
	public String basin;

	@Description(" The vetor containing the id of the station")
	Object []idStations;


	@Description("the linked HashMap with the coordinate of the stations")
	LinkedHashMap<Integer, Coordinate> stationCoordinates;

	@Description("List of the indeces of the columns of the station in the map")
	ArrayList <Integer> columnStation= new ArrayList <Integer>();

	@Description("List of the indeces of the rows of the station in the map")
	ArrayList <Integer> rowStation= new ArrayList <Integer>();

	@Description("The complete path to the input folder")
	@Out
	public String inFolderPathComplete;

	@Description("The extracted precipitation hashmap")
	@Out
	public HashMap<Integer, double[]> outPrecipitationHM;


	double [] pArray;

	private DateTimeFormatter formatter = JGTConstants.utcDateFormatterYYYYMMDDHHMM;
	Logger logger = LogManager.getLogger(PrecipitationReaderGRIB.class);



	int step;


	@Execute
	public void process() throws Exception {

		String log4jConfPath = "lib/log4j.properties";
		PropertyConfigurator.configure(log4jConfPath);

		try{


			outPrecipitationHM = new HashMap<Integer, double[]>();

			// extrapolate the date from the string, eliminating the information on the hour
			String [] dateS=tStart.split(" ");

			// extrapolate the date, eliminating the "-"
			// ex. 2013-02-15 --> 20130215
			String [] dayS=dateS[0].split("-");		
			String dayC=dayS[0]+dayS[1]+dayS[2];

			// builds the complete path to the input data, using the day
			inFolderPathComplete=inFolder+dayC;

			// reade all the fils in the folder, keeping only the required
			File curDir = new File(inFolderPathComplete);
			File[] filesList = curDir.listFiles();
			Object [] newList=list(filesList);

			// reads the file with the stations
			stationCoordinates = getCoordinate(inStations, fStationsid);



			// trasform the list of idStation into an array
			idStations= stationCoordinates.keySet().toArray();


			if(step==0){
				// creates a new array to sore the precipitation value
				pArray=new double[idStations.length];
				logger.info(tCurrent+" ****"+basin+"****");
				logger.info(tCurrent+" REAL TIME");	
			}


			for(int i=0;i<newList.length;i++){

				// from the name of the file, computes the hours after the midnight
				// to reconstruct the actual time of the file
				String name =newList[i].toString();
				String[] split1=name.split("_");
				int hourAfter=Integer.parseInt(split1[split1.length-1].substring(0, 3));


				// sday recontruct the start time from which the forecasts are computed
				// ex.  1502_00_042 are 12 hours after the midhnight and
				// sday is 2013-02-15 00:00
				String day=tStart.split(" ")[0];			
				String s=split1[split1.length-2].substring(0, 2);			
				String sday=day+" "+s+":00";

				// start is sday converted to DateTime format
				DateTime start = formatter.parseDateTime(sday);	


				DateTime tcurrent=formatter.parseDateTime(tCurrent);


				// t is the time of each forecast file
				// ex. t is 42 hours after the midhnight
				// 2013-02-15 12:00
				DateTime t=start.plusHours(hourAfter);


				if (tcurrent.equals(t)){



					OmsRasterReader map = new OmsRasterReader();
					map.file = name;
					map.fileNovalue = -9999.0;
					map.geodataNovalue = Double.NaN;
					map.process();
					GridCoverage2D mapGrid = map.outRaster;
					WritableRaster mapWR=mapsTransform(mapGrid);


					//  from pixel coordinates (in coverage image) to geographic coordinates (in coverage CRS)
					MathTransform transf = mapGrid.getGridGeometry().getCRSToGrid2D();

					// computing the reference system of the input DEM
					CoordinateReferenceSystem sourceCRS = mapGrid.getCoordinateReferenceSystem2D();


					//create the set of the coordinate of the station, so we can 
					//iterate over the set	
					Iterator<Integer> idIterator = stationCoordinates.keySet().iterator();

					for (int ii=0;ii<idStations.length;ii++){

						// compute the coordinate of the station from the linked hashMap
						Coordinate coordinate = (Coordinate) stationCoordinates.get(idIterator.next());


						// define the position, according to the CRS, of the station in the map
						DirectPosition point = new DirectPosition2D(sourceCRS, coordinate.x, coordinate.y);

						// trasform the position in two the indices of row and column 
						DirectPosition gridPoint = transf.transform(point, null);

						// add the indices to a list
						columnStation.add((int) gridPoint.getCoordinate()[0]);
						rowStation.add((int) gridPoint.getCoordinate()[1]);


						pArray[ii]=(mapWR.getSampleDouble(columnStation.get(ii), rowStation.get(ii), 0))/6;

					}
				} 

			}

			for (int ii=0;ii<idStations.length;ii++){
				outPrecipitationHM.put((Integer)idStations[ii], new double[]{pArray[ii]});
			}

			step++;


			logger.info(tCurrent+" Lettura mappe di precipitazione OK");


		} catch (Exception e){
			logger.error(e);
			logger.info(tCurrent+" Lettura mappe di precipitazione KO");
			throw e;
		}
	}









	public Object [] list(File[] filesList) throws Exception{

		Object [] list = null;

		ArrayList<String> arrayString = new ArrayList <String>();

		String measuredHour=null;

		for(int i=0;i<filesList.length;i++){
			String name =filesList[i].getName().split("\\.")[0];

			if(name.contains("PRECIPITAZIONE")){
				measuredHour=name.split("_")[3];
			}

		}




		for(int i=0;i<filesList.length;i++){


			String name =filesList[i].getName();


			if(name.endsWith(dataType)&&!name.contains(LAIdataSensor)){
				String hour=name.split("\\.")[0].split("_")[3];
				if(name.contains("PRECIPITAZIONE")){ 
					arrayString.add(filesList[i].toString());
				}else if(!name.contains("PRECIPITAZIONE")&&!hour.equals(measuredHour)){
					arrayString.add(filesList[i].toString());
				}
			}

		}

		list=arrayString.toArray();

		return list;

	}




	/**
	 * Gets the coordinate given the shp file and the field name in the shape with the coordinate of the station.
	 *
	 * @param collection is the shp file with the stations
	 * @param idField is the name of the field with the id of the stations 
	 * @return the coordinate of each station
	 * @throws Exception the exception in a linked hash map
	 */
	private LinkedHashMap<Integer, Coordinate> getCoordinate(SimpleFeatureCollection collection, String idField)
			throws Exception {
		LinkedHashMap<Integer, Coordinate> id2CoordinatesMap = new LinkedHashMap<Integer, Coordinate>();
		FeatureIterator<SimpleFeature> iterator = collection.features();
		Coordinate coordinate = null;
		try {
			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				int stationNumber = ((Number) feature.getAttribute(idField)).intValue();
				coordinate = ((Geometry) feature.getDefaultGeometry()).getCentroid().getCoordinate();
				id2CoordinatesMap.put(stationNumber, coordinate);
			}
		} finally {
			iterator.close();
		}

		return id2CoordinatesMap;
	}



	/**
	 * Maps reader transform the GrifCoverage2D in to the writable raster,
	 * replace the -9999.0 value with no value.
	 *
	 * @param inValues: the input map values
	 * @return the writable raster of the given map
	 */
	private WritableRaster mapsTransform ( GridCoverage2D inValues){	
		RenderedImage inValuesRenderedImage = inValues.getRenderedImage();
		WritableRaster inValuesWR = CoverageUtilities.replaceNovalue(inValuesRenderedImage, -9999.0);
		inValuesRenderedImage = null;
		return inValuesWR;
	}


}


