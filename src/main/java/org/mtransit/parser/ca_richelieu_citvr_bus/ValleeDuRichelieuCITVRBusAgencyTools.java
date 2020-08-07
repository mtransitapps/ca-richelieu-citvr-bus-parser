package org.mtransit.parser.ca_richelieu_citvr_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.Utils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTripStop;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.mt.data.MTrip;

// https://exo.quebec/en/about/open-data
// https://exo.quebec/xdata/citvr/google_transit.zip
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
		MTLog.log("Generating CITVR bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		MTLog.log("Generating CITVR bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
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
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = CleanUtils.SAINT.matcher(routeLongName).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final long RID_ENDS_WITH_B = 2_000L;
	private static final long RID_ENDS_WITH_M = 13_000L;

	private static final long RID_STARTS_WITH_T = 20_000L;

	@Override
	public long getRouteId(GRoute gRoute) {
		if (!Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
			if (matcher.find()) {
				int digits = Integer.parseInt(matcher.group());
				if (gRoute.getRouteShortName().startsWith(T)) {
					return RID_STARTS_WITH_T + digits;
				}
				if (gRoute.getRouteShortName().endsWith(B)) {
					return RID_ENDS_WITH_B + digits;
				} else if (gRoute.getRouteShortName().endsWith(M)) {
					return RID_ENDS_WITH_M + digits;
				}
			}
			throw new MTLog.Fatal("Unexpected route ID for %s!", gRoute);
		}
		return super.getRouteId(gRoute);
	}

	private static final String AGENCY_COLOR = "1F1F1F"; // DARK GRAY (from GTFS)

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String CÉGEP = "Cégep";
	private static final String CENTRE_VILLE = "Centre-Ville";
	private static final String DOUVILLE = "Douville";
	private static final String GALERIES = "Galeries";
	private static final String LA_PROVIDENCE = "La Providence";
	private static final String LES_SALINES = "Les Salines";
	private static final String PARC_INDUSTRIEL = "Parc Ind.";
	private static final String ST_JOSEPH = "St-Joseph";
	private static final String ST_HYACINTHE = "St-Hyacinthe";
	private static final String ST_THOMAS = "St-Thomas";
	private static final String STE_ROSALIE = "Ste-Rosalie";

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == 300L) {
			if (Arrays.asList( //
					"Mont-St-Hilaire", //
					ST_HYACINTHE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(ST_HYACINTHE, mTrip.getHeadsignId());
				return true;
			}
		}
		throw new MTLog.Fatal("Unexpected trips to merge %s and %s.", mTrip, mTripToMerge);
	}

	private static final Pattern DIRECTION = Pattern.compile("(direction )", Pattern.CASE_INSENSITIVE);

	private static final Pattern SERVICE_LOCAL = Pattern.compile("(service local)", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = DIRECTION.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = SERVICE_LOCAL.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		tripHeadsign = CleanUtils.removePoints(tripHeadsign);
		return CleanUtils.cleanLabelFR(tripHeadsign);
	}

	private static final Pattern START_WITH_FACE_A = Pattern.compile("^(face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_WITH_FACE_AU = Pattern.compile("^(face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_WITH_FACE = Pattern.compile("^(face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern SPACE_FACE_A = Pattern.compile("( face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern SPACE_WITH_FACE_AU = Pattern.compile("( face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern SPACE_WITH_FACE = Pattern.compile("( face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern[] START_WITH_FACES = new Pattern[] { START_WITH_FACE_A, START_WITH_FACE_AU, START_WITH_FACE };

	private static final Pattern[] SPACE_FACES = new Pattern[] { SPACE_FACE_A, SPACE_WITH_FACE_AU, SPACE_WITH_FACE };

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = Utils.replaceAll(gStopName, START_WITH_FACES, CleanUtils.SPACE);
		gStopName = Utils.replaceAll(gStopName, SPACE_FACES, CleanUtils.SPACE);
		gStopName = CleanUtils.cleanStreetTypesFRCA(gStopName);
		return CleanUtils.cleanLabelFR(gStopName);
	}

	@Override
	public String getStopCode(GStop gStop) {
		if ("0".equals(gStop.getStopCode())) {
			return null;
		}
		return super.getStopCode(gStop);
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	private static final String A = "A";
	private static final String B = "B";
	private static final String C = "C";
	private static final String D = "D";
	private static final String E = "E";
	private static final String F = "F";
	private static final String G = "G";
	private static final String H = "H";
	private static final String M = "M";
	private static final String T = "T";

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
		String stop_id = gStop.getStopId();
		stop_id = CleanUtils.cleanMergedID(stop_id);
		Matcher matcher = DIGITS.matcher(stop_id);
		if (matcher.find()) {
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
				throw new MTLog.Fatal("Stop doesn't have an ID (start with)! " + gStop);
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
				throw new MTLog.Fatal("Stop doesn't have an ID (end with)! " + gStop);
			}
			return stopId + digits;
		}
		throw new MTLog.Fatal("Unexpected stop ID for %s!", gStop);
	}
}
