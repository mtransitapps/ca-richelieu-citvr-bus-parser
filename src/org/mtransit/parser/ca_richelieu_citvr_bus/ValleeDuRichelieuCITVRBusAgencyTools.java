package org.mtransit.parser.ca_richelieu_citvr_bus;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MTrip;

// https://www.amt.qc.ca/en/about/open-data
// http://www.amt.qc.ca/xdata/citvr/google_transit.zip
public class ValleeDuRichelieuCITVRBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-richelieu-citvr-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new ValleeDuRichelieuCITVRBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("Generating CITVR bus data...\n");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("Generating CITVR bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.route_long_name;
		routeLongName = MSpec.SAINT.matcher(routeLongName).replaceAll(MSpec.SAINT_REPLACEMENT);
		return MSpec.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR = "ABBE00";

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String DASH = " - ";

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		String stationName = cleanTripHeadsign(gTrip.trip_headsign);
		if (mRoute.id == 7l) {
			if (gTrip.direction_id == 1) {
				stationName += " 2";
			}
		} else if (mRoute.id == 30l) {
			stationName = stationName.substring(stationName.indexOf(DASH) + DASH.length());
		}
		mTrip.setHeadsignString(stationName, gTrip.direction_id);
	}

	private static final Pattern DIRECTION = Pattern.compile("(direction )", Pattern.CASE_INSENSITIVE);
	private static final String DIRECTION_REPLACEMENT = "";

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = DIRECTION.matcher(tripHeadsign).replaceAll(DIRECTION_REPLACEMENT);
		return MSpec.cleanLabelFR(tripHeadsign);
	}

	private static final Pattern START_WITH_FACE_A = Pattern.compile("^(face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_WITH_FACE_AU = Pattern.compile("^(face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_WITH_FACE = Pattern.compile("^(face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern SPACE_FACE_A = Pattern.compile("( face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern SPACE_WITH_FACE_AU = Pattern.compile("( face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern SPACE_WITH_FACE = Pattern.compile("( face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern[] START_WITH_FACES = new Pattern[] { START_WITH_FACE_A, START_WITH_FACE_AU, START_WITH_FACE };

	private static final Pattern[] SPACE_FACES = new Pattern[] { SPACE_FACE_A, SPACE_WITH_FACE_AU, SPACE_WITH_FACE };

	private static final Pattern AVENUE = Pattern.compile("( avenue)", Pattern.CASE_INSENSITIVE);
	private static final String AVENUE_REPLACEMENT = " av.";

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = AVENUE.matcher(gStopName).replaceAll(AVENUE_REPLACEMENT);
		gStopName = Utils.replaceAll(gStopName, START_WITH_FACES, MSpec.SPACE);
		gStopName = Utils.replaceAll(gStopName, SPACE_FACES, MSpec.SPACE);
		return super.cleanStopNameFR(gStopName);
	}


	@Override
	public String getStopCode(GStop gStop) {
		if ("0".equals(gStop.stop_code)) {
			return null;
		}
		return super.getStopCode(gStop);
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	private static final String _MERGED = "_merged_";

	private static final String A = "A";
	private static final String B = "B";
	private static final String C = "C";
	private static final String D = "D";
	private static final String E = "E";
	private static final String F = "F";
	private static final String G = "G";
	private static final String H = "H";

	private static final String LON = "LON";
	private static final String SHY = "SHY";
	private static final String SJU = "SJU";
	private static final String SBA = "SBA";
	private static final String OTP = "OTP";
	private static final String MSH = "MSH";
	private static final String MMS = "MMS";
	private static final String BEL = "BEL";

	@Override
	public int getStopId(GStop gStop) {
		String stopCode = getStopCode(gStop);
		if (stopCode != null && stopCode.length() > 0) {
			return Integer.valueOf(stopCode); // using stop code as stop ID
		}
		String stop_id = gStop.stop_id;
		int indexOf = stop_id.indexOf(_MERGED);
		if (indexOf >= 0) {
			stop_id = stop_id.substring(0, indexOf);
		}
		Matcher matcher = DIGITS.matcher(stop_id);
		matcher.find();
		int digits = Integer.parseInt(matcher.group());
		int stopId;
		if (stop_id.startsWith(BEL)) {
			stopId = 100000;
		} else if (stop_id.startsWith(MMS)) {
			stopId = 200000;
		} else if (stop_id.startsWith(MSH)) {
			stopId = 300000;
		} else if (stop_id.startsWith(OTP)) {
			stopId = 400000;
		} else if (stop_id.startsWith(SBA)) {
			stopId = 500000;
		} else if (stop_id.startsWith(SJU)) {
			stopId = 600000;
		} else if (stop_id.startsWith(SHY)) {
			stopId = 700000;
		} else if (stop_id.startsWith(LON)) {
			stopId = 800000;
		} else {
			System.out.println("Stop doesn't have an ID (start with)! " + gStop);
			System.exit(-1);
			stopId = -1;
		}
		if (stop_id.endsWith(A)) {
			stopId += 1000;
		} else if (stop_id.endsWith(B)) {
			stopId += 2000;
		} else if (stop_id.endsWith(C)) {
			stopId += 3000;
		} else if (stop_id.endsWith(D)) {
			stopId += 4000;
		} else if (stop_id.endsWith(E)) {
			stopId += 5000;
		} else if (stop_id.endsWith(F)) {
			stopId += 6000;
		} else if (stop_id.endsWith(G)) {
			stopId += 7000;
		} else if (stop_id.endsWith(H)) {
			stopId += 8000;
		} else {
			System.out.println("Stop doesn't have an ID (end with)! " + gStop);
			System.exit(-1);
		}
		return stopId + digits;
	}
}
