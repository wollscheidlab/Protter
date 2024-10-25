package at.omasits.proteomics.protter.ranges;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.omasits.util.Config;
import at.omasits.util.Log;
import at.omasits.util.UOUniProtEntry;
import at.omasits.util.Util;

public class RangeParserPeptideCutter implements IRangeParser {
	private static HashMap<List<String>,List<Range>> buffer = new HashMap<List<String>, List<Range>>();

	public static String extractPositions(String line) {
		// Function to extract cleavage postions returned by peptidecutter.pl
		// The relevant HTML line is of the form:
		// <tr><td><a href="/peptide_cutter/peptidecutter_enzymes.html#LysC">LysC</a></td><td>10</td><td>6 9 35 67 94 99 100 114 139 140</td><tr>
		String regex = "</td><td>(\\d+(?: \\d+)*)</td><tr>";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(line);

		String positions = null;
		while (matcher.find()) {
			positions = matcher.group(1); // Update with the latest match
		}
		return positions;
	}

	@Override
	public boolean matches(String rangeString) {
		return rangeString.toLowerCase().startsWith("peptidecutter");
	}

	@Override
	public List<? extends Range> parse(String rangeString, String sequence, List<UOUniProtEntry> up, Map<String,String> parms) throws Exception {
		List<Range> cutPositions = new ArrayList<Range>();
		String enzyme = rangeString.substring(14);
		
		if (buffer.containsKey(Arrays.asList(sequence, enzyme)))
			cutPositions = buffer.get(Arrays.asList(sequence, enzyme));
		else {
			try {
				StringBuilder strUrl = new StringBuilder(Config.get("peptideCutter","https://web.expasy.org/cgi-bin/peptide_cutter/peptidecutter.pl"));
				strUrl.append("?alphtable=alphtable&cleave_number=all");
				strUrl.append("&protein=").append(sequence);
				strUrl.append("&enzyme=").append(enzyme);
		
				URL url = new URL(strUrl.toString());
				Log.debug("loading "+url);
				BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
				String line;
				while ((line = in.readLine()) != null) {
					if (line.startsWith("<tr><td><a href=\"/peptide_cutter/peptidecutter_enzymes.html")) {
						String strCutPositions = extractPositions(line);
						for (String strCutPosition : strCutPositions.split(" ")) {
							int cutPos = Integer.valueOf(strCutPosition);
							cutPositions.add(new Range(cutPos, cutPos));
						}
						break;
					}
				}
				buffer.put(Arrays.asList(sequence, enzyme), cutPositions);
			} catch (Exception e) {
				Log.errorThrow("Could not access PeptideCutter. Check the internet connection and the ProtterServer configuration file.");
			}
		}
		return cutPositions;
	}

}
