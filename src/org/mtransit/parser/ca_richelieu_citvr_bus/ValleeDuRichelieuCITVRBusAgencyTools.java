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

// https://rtm.quebec/en/about/open-data
// https://rtm.quebec/xdata/citvr/google_transit.zip
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
		System.out.printf("\nGenerating CITVR bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating CITVR bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
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
			System.out.printf("\nUnexpected route ID for %s!\n", gRoute);
			System.exit(-1);
			return -1L;
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
		map2.put(50L, new RouteTripSpec(50L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GALERIES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTRE_VILLE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"73996", // "SHY1A", // Terminus Saint-Hyacinthe
								"73765", // "SHY175B", // avenue Ste-Anne / face à l'Hôtel-Dieu
								"73765", // "SHY195D", // avenue Ste-Anne / face à l'Hôtel-Dieu
								"73767", // "SHY179G", // boul. Laframboise / rue Morison
								"73700", // "SHY191C", // Galeries St-Hyacinthe (porte #1)
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"73700", // "SHY191C", // Galeries St-Hyacinthe (porte #1)
								"73775", // "SHY193D", // boul. Laframboise / rue Nelson
								"73996", // "SHY1A", // Terminus Saint-Hyacinthe
						})) //
				.compileBothTripSort());
		map2.put(51L, new RouteTripSpec(51L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTRE_VILLE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ST_JOSEPH) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"73712", // "SHY87C", // rue Lemonde / avenue St-Louis
								"73715", // "SHY93B", // avenue de la Concorde / rue Brunette
								"73996", // "SHY1A", // Terminus Saint-Hyacinthe
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"73996", // "SHY1A", // Terminus Saint-Hyacinthe
								"73706", // "SHY74D", // avenue St-Louis / rue Brunette
								"73712", // "SHY87C", // rue Lemonde / avenue St-Louis
						})) //
				.compileBothTripSort());
		map2.put(52L, new RouteTripSpec(52L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTRE_VILLE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOUVILLE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"73731", // "SHY127D", // avenue Duchesnay / rue Garnier
								"73737", // "SHY133A", // rue Jacques-Cartier / avenue Castelneau
								"73996", // "SHY1A", // Terminus Saint-Hyacinthe
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"73996", // "SHY1A", // Terminus Saint-Hyacinthe
								"73722", // "SHY109B", // avenue Castelneau / rue Montigny
								"73731", // "SHY127D", // avenue Duchesnay / rue Garnier
						})) //
				.compileBothTripSort());
		map2.put(53L, new RouteTripSpec(53L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STE_ROSALIE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTRE_VILLE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"73996", // "SHY1A", // Terminus Saint-Hyacinthe
								"73746", // "SHY147A", // rue Jolibois / avenue Brabant
								"73754", // "SHY159D", // avenue Gosselin / boul. Laurier
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"73754", // "SHY159D", // avenue Gosselin / boul. Laurier
								"73759", // "SHY166C", // boul. Laurier / avenue Brabant
								"73996", // "SHY1A", // Terminus Saint-Hyacinthe
						})) //
				.compileBothTripSort());
		map2.put(54L, new RouteTripSpec(54L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GALERIES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTRE_VILLE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"73996", // "SHY1A", // Terminus Saint-Hyacinthe
								"73785", // "SHY202B", // boul. Choquette / rue Bourassa
								"73700", // "SHY191C", // Galeries St-Hyacinthe (porte #1)
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"73700", // "SHY191C", // Galeries St-Hyacinthe (porte #1)
								"73794", // "SHY201H", // rue Nelson / boul. Choquette
								"73996", // "SHY1A", // Terminus Saint-Hyacinthe
						})) //
				.compileBothTripSort());
		map2.put(55L, new RouteTripSpec(55L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ST_THOMAS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GALERIES) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"73700", // Galeries St-Hyacinthe (porte #1)
								"79998", // ==
								"79997", // != <>
								"79994", // != <>
								"73697", // !== <>
								"73698", // != <>
								"73699", // !== <>
								"73681", // ==
								"73688", // avenue Léon / avenue Sansoucy (2e arrêt boîte aux lettres) =>
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"73688", // avenue Léon / avenue Sansoucy (2e arrêt boîte aux lettres) <=
								"73696", // ==
								"79993", // !=
								"79997", // != <>
								"79994", // != <>
								"73697", // !== <>
								"73698", // != <>
								"73699", // !== <>
								"73701", // != !=
								"79999", // ==
								"73700", // Galeries St-Hyacinthe (porte #1)
						})) //
				.compileBothTripSort());
		map2.put(56L, new RouteTripSpec(56L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTRE_VILLE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LA_PROVIDENCE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"73649", // "SHY287B", // avenue Roy / rue St-Pierre
								"73651", // "SHY290A", // avenue Roy / rue St-Pierre
								"73996", // "SHY1A", // Terminus Saint-Hyacinthe
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"73996", // "SHY1A", // Terminus Saint-Hyacinthe
								"73814", // "SHY275B", // avenue Bourdages / face au 16720
								"73649", // "SHY287B", // avenue Roy / rue St-Pierre
						})) //
				.compileBothTripSort());
		map2.put(57L, new RouteTripSpec(57L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GALERIES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTRE_VILLE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"73996", // "SHY1A", // Terminus Saint-Hyacinthe
								"73657", // "SHY307A", // rue du Sacré-Coeur / avenue des Grandes-Orgues
								"73700", // "SHY191C", // Galeries St-Hyacinthe (porte #1)
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"73700", // "SHY191C", // Galeries St-Hyacinthe (porte #1)
								"73673", // "SHY311D", // avenue des Grandes-Orgues / face à l'École Professionnelle
								"73996", // "SHY1A", // Terminus Saint-Hyacinthe
						})) //
				.compileBothTripSort());
		map2.put(60L, new RouteTripSpec(60L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GALERIES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CÉGEP) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"73600", // "SHY135B", // Cégep Saint-Hyacinthe
								"73804", // SHY214A", // rue Prosper / face au 2975 (L'Escale)
								"73700", // "SHY191C", // Galeries St-Hyacinthe (porte #1)
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"73700", // "SHY191C", // Galeries St-Hyacinthe (porte #1)
								"73793", // "SHY202D", // boul. Choquette / rue Bourassa
								"73600", // "SHY135B", // Cégep Saint-Hyacinthe
						})) //
				.compileBothTripSort());
		map2.put(61L, new RouteTripSpec(61L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LES_SALINES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GALERIES) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"73700", // "SHY191C", // Galeries St-Hyacinthe (porte #1)
								"73700", // "SHY191C", // Galeries St-Hyacinthe (porte #1)
								"SHY335B", // Parc les Salines
								"SHY335B", // Parc les Salines
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"SHY335B", // Parc les Salines
								"SHY335B", // Parc les Salines
								"73700", // "SHY191C", // Galeries St-Hyacinthe (porte #1)
								"73700", // "SHY191C", // Galeries St-Hyacinthe (porte #1)
						})) //
				.compileBothTripSort());
		map2.put(62L, new RouteTripSpec(62L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, GALERIES, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, PARC_INDUSTRIEL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"73594", // "SHY58B", // boul. Choquette / avenue Pinard
								"73596", // "SHY349A", // rue Picard / avenue Beaudry
								"73599", // "SHY360A", // rue Picard / avenue Desjardins
								"73700", // "SHY191C", // Galeries St-Hyacinthe (porte #1)
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"73700", // "SHY191C", // Galeries St-Hyacinthe (porte #1)
								"73642", // "SHY62B", // boul. Choquette / avenue Trudeau
								"73592", // "SHY337C", // boul. Choquette / face au 6285
								"73594", // "SHY58B", // boul. Choquette / avenue Pinard
						})) //
				.compileBothTripSort());
		map2.put(70L, new RouteTripSpec(70L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GALERIES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTRE_VILLE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"73996", // "SHY1A", // Terminus Saint-Hyacinthe
								"73768", // "SHY180B", // boul. Laframboise / rue du Sacré-Coeur
								"73700", // "SHY191C", // Galeries St-Hyacinthe (porte #1)
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"73700", // "SHY191C", // Galeries St-Hyacinthe (porte #1)
								"73779", // "SHY362D", // avenue Beauparlant / rue Viger
								"73996", // "SHY1A", // Terminus Saint-Hyacinthe
						})) //
				.compileBothTripSort());
		map2.put(71L, new RouteTripSpec(71L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LES_SALINES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GALERIES) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"73700", // "SHY191C", // Galeries St-Hyacinthe (porte #1)
								"73675", // "SHY316A", // boul. Casavant / avenue T.-D. Bouchard
								"73800", // "SHY335B", // Parc les Salines
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"73800", // "SHY335B", // Parc les Salines
								"73699", // "SHY225C", // Bureau en Gros
								"73700", // "SHY191C", // Galeries St-Hyacinthe (porte #1)
						})) //
				.compileBothTripSort());
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
		System.out.printf("\nUnexpected trips to merge %s and %s.\n", mTrip, mTripToMerge);
		System.exit(-1);
		return false;
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
		System.out.printf("\nUnexpected stop ID for %s!\n", gStop);
		System.exit(-1);
		return -1;
	}
}
