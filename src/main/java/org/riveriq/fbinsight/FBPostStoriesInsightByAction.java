/*
 * Copyright 2019 www.riveriq.com
 * Copyright 2019 Ashish Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.riveriq.fbinsight;

import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
//import com.restfb.Version;
import com.restfb.json.JsonObject;
import com.restfb.types.Insight;
import com.restfb.types.Post;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Properties;
import org.riveriq.csvwriter.CSVWriter;
import org.riveriq.jsonparser.JSONFlattener;

/**
 * @author Ashish kumar
 * @email niits007@gmail.com
 * @website riveriq.com
 * @since June 12, 2018
 * @version 1.0
 */

public class FBPostStoriesInsightByAction {
	private static FacebookClient facebookClient;
	private static String propFilePath = "";
	List<JsonObject> list_post_fields = null;
	private static String POST_STORIES_INSIGHT_LOG_PATH = "";
	static String POST_INSIGH_FROM = "";
	static String POST_INSIGH_TO = "";
	static String PAGE_NAME = "";
	static String PAGE_ID = "";
	static String CurrentDate = "";

	public static void main(String[] args) throws Exception {
		propFilePath = args[0].toString();
		FBPostStoriesInsightByAction obj_FBPage = new FBPostStoriesInsightByAction();
		String cur_datetime = obj_FBPage.getCurrentDateTime("yyyyMMdd_HHmmss");

		POST_INSIGH_FROM = obj_FBPage.getPropValue(propFilePath, "POST_INSIGH_FROM");
		POST_INSIGH_TO = obj_FBPage.getPropValue(propFilePath, "POST_INSIGH_TO");
		PAGE_NAME = obj_FBPage.getPropValue(propFilePath, "PAGE_NAME");
		PAGE_ID = obj_FBPage.getPropValue(propFilePath, "PAGE_ID");

		if (POST_INSIGH_FROM.equalsIgnoreCase("") & POST_INSIGH_TO.equalsIgnoreCase("")) {
			POST_INSIGH_TO = obj_FBPage.getDate("yyyy-MM-dd", 0);
			POST_INSIGH_FROM = obj_FBPage.getDate("yyyy-MM-dd", -7);
		}
		CurrentDate = obj_FBPage.getDate("yyyy-MM-dd", 0);

		POST_STORIES_INSIGHT_LOG_PATH = obj_FBPage.getPropValue(propFilePath, "POST_STORIES_INSIGHT_LOG_PATH") + "_"
				+ cur_datetime + ".log";

		obj_FBPage.write(POST_STORIES_INSIGHT_LOG_PATH, "==> DATA WILL BE DOWNLOADED FOR FACEBOOK PAGE :" + PAGE_NAME
				+ "(" + PAGE_ID + ") FOR DURATION BETWEEN " + POST_INSIGH_FROM + " AND " + POST_INSIGH_TO);

		obj_FBPage.write(POST_STORIES_INSIGHT_LOG_PATH, "==> READING ACCESS TOKEN");
		String ACCESS_TOKEN = obj_FBPage.getPropValue(propFilePath, "ACCESS_TOKEN");
		/*
		 * facebookClient = new DefaultFacebookClient(ACCESS_TOKEN,
		 * Version.VERSION_2_10);
		 */
		facebookClient = new DefaultFacebookClient(ACCESS_TOKEN);
		List<Connection<Insight>> post_insights = obj_FBPage.fetchObject();
		List<Map<String, String>> clist_insights = obj_FBPage.parseJSONInsight(post_insights);
		obj_FBPage.writecsv(clist_insights);
	}

	public List<Connection<Insight>> fetchObject() throws IOException {

		String POST_STORIES_INSIGHT_BY_ACTION_MATRIX = getPropValue(propFilePath,
				"POST_STORIES_INSIGHT_BY_ACTION_MATRIX");

		String POST_FIELDS = getPropValue(propFilePath, "POST_FIELDS");
		write(POST_STORIES_INSIGHT_LOG_PATH, "==> READING POSTS ID");
		Connection<Post> posts = null;
		try {
			posts = facebookClient.fetchConnection(PAGE_ID + "/posts", Post.class,
					Parameter.with("since", POST_INSIGH_FROM), Parameter.with("until", POST_INSIGH_TO));

		} catch (Exception e) {
			write(POST_STORIES_INSIGHT_LOG_PATH, "==> EXCEPTION WHILE READING POSTS ID ==> " + e);
			System.out.println(e);
			System.exit(0);
		}
		Connection<Insight> post_insights = null;
		Iterator<List<Post>> posts_it = posts.iterator();
		List<Connection<Insight>> list_post_insights = new ArrayList<Connection<Insight>>();
		list_post_fields = new ArrayList<JsonObject>();
		while (posts_it.hasNext()) {
			write(POST_STORIES_INSIGHT_LOG_PATH, "==> READING PAGE POST INSIGHTS");
			List<Post> list_posts = posts_it.next();
			try {
				for (Post post : list_posts) {
					post_insights = facebookClient.fetchConnection(
							post.getId() + "/insights/" + POST_STORIES_INSIGHT_BY_ACTION_MATRIX, Insight.class);
					list_post_insights.add(post_insights);
					write(POST_STORIES_INSIGHT_LOG_PATH, "==> PENDING");
				}
			} catch (Exception e) {
				write(POST_STORIES_INSIGHT_LOG_PATH, "==> EXCEPTION WHILE READING PAGE POST INSIGHTS ==> " + e);
				System.out.println(e);
				System.exit(0);
			}
			write(POST_STORIES_INSIGHT_LOG_PATH, "==> READING PAGE POST FIELDS");
			try {
				for (Post post : list_posts) {
					JsonObject post_fields = facebookClient.fetchObject(post.getId(), JsonObject.class,
							Parameter.with("fields", POST_FIELDS),
							// Parameter.with("metadata", "1"),
							Parameter.with("since", POST_INSIGH_FROM), Parameter.with("until", POST_INSIGH_TO));
					list_post_fields.add(post_fields);
					write(POST_STORIES_INSIGHT_LOG_PATH, "==> PENDING");
				}
			} catch (Exception e) {
				write(POST_STORIES_INSIGHT_LOG_PATH, "==> EXCEPTION WHILE READING PAGE POST FIELDS ==> " + e);
				System.out.println(e);
				System.exit(0);
			}
		}
		return list_post_insights;
	}

	public List<Map<String, String>> parseJSONInsight(List<Connection<Insight>> post_insights) throws IOException {
		write(POST_STORIES_INSIGHT_LOG_PATH, "==> PARSING JSON INSIGHTS");
		List<List<Map<String, String>>> plist_insights = new ArrayList<List<Map<String, String>>>();
		List<Map<String, String>> clist_insights = new ArrayList<Map<String, String>>();
		List<String> list_tital_matrix = new ArrayList<String>();
		List<String> list_ppost_fields_id = new ArrayList<String>();
		/*
		 * List<String> list_ppost_fields_message = new ArrayList<String>();
		 * List<String> list_ppost_fields_type = new ArrayList<String>();
		 */
		try {
			for (JsonObject post_fields : list_post_fields) {
				list_ppost_fields_id.add(post_fields.get("id").toString());
				/*
				 * list_ppost_fields_message.add(post_fields.get("message") .toString());
				 * list_ppost_fields_type.add(post_fields.get("type" ).toString());
				 */
			}
			int k = 0;
			write(POST_STORIES_INSIGHT_LOG_PATH, "==> CONVERTING JSON INSIGHTS TO CSV FORMAT");
			for (Connection<Insight> insights : post_insights) {
				for (Insight insight : insights.getData()) {
					List<Map<String, String>> flatJson = JSONFlattener.parseJson(insight.getValues().toString());
					plist_insights.add(flatJson);
					list_tital_matrix.add(insight.getName());
				}
				LinkedHashMap<String, String> map_value_matrix = null;
				for (int i = 0; i < plist_insights.get(0).size(); i++) {
					map_value_matrix = new LinkedHashMap<String, String>();
					if (i == 0) {
						map_value_matrix.put("Post ID", list_ppost_fields_id.get(k));
					}
					for (int j = 0; j < plist_insights.size(); j++) {

						map_value_matrix.put("like", plist_insights.get(j).get(i).get("value.like"));
						map_value_matrix.put("share", plist_insights.get(j).get(i).get("value.share"));
						map_value_matrix.put("comment", plist_insights.get(j).get(i).get("value.comment"));
					}
				}
				k++;
				clist_insights.add(map_value_matrix);
				write(POST_STORIES_INSIGHT_LOG_PATH, "==> PENDING");
			}
		} catch (Exception e) {
			write(POST_STORIES_INSIGHT_LOG_PATH, "==> EXCEPTION WHILE CONVERTING JSON INSIGHTS TO CSV FORMAT ==> " + e);
			System.out.println(e);
			System.exit(0);
		}

		return clist_insights;
	}

	public void writecsv(List<Map<String, String>> clist_insights) throws Exception {
		write(POST_STORIES_INSIGHT_LOG_PATH, "==> WRITING INSIGHTS INTO CSV FILE");
		// String cur_datetime = getCurrentDateTime("yyyyMMdd_HHmmss");

		String POST_STORIES_INSIGHT_BY_ACTION_CSV_PATH = getPropValue(propFilePath,
				"POST_STORIES_INSIGHT_BY_ACTION_CSV_PATH") + "\\"
		/*
		 * +getFromatedDate(POST_INSIGH_TO,"yyyy-MM-dd","yyyyMMdd") + "\\"
		 */
				+ getPropValue(propFilePath, "POST_STORIES_INSIGHT_BY_ACTION_CSV_FILENAME") + "_"
				+ getFromatedDate(CurrentDate, "yyyy-MM-dd", "yyyyMMdd") + ".csv";
		try {
			CSVWriter.writeToFile(CSVWriter.getCSV(clist_insights), POST_STORIES_INSIGHT_BY_ACTION_CSV_PATH);
		} catch (Exception e) {
			write(POST_STORIES_INSIGHT_LOG_PATH, "==> EXCEPTION WHILE WRITING INSIGHTS INTO CSV FILE ==> " + e);
			System.out.println(e);
			System.exit(0);
		}
		write(POST_STORIES_INSIGHT_LOG_PATH, "==> CSV DOWNLOADED :" + POST_STORIES_INSIGHT_BY_ACTION_CSV_PATH);

	}

	public String getPropValue(String propFilePath, String propName) {
		String propValue = "";
		try {
			Properties prop = new Properties();
			InputStream input = null;
			input = new FileInputStream(propFilePath);
			prop.load(input);
			propValue = prop.get(propName).toString();
		} catch (Exception e) {
			System.out.println(e);
			System.exit(0);
		}
		return propValue;
	}

	public FileWriter write(String f, String s) throws IOException {
		// TimeZone tz = TimeZone.getTimeZone("EDT"); // or PST, MID, etc ...
		Date now = new Date(System.currentTimeMillis());
		DateFormat df = new SimpleDateFormat("yyyy:MM:dd hh:mm:ss");
		// df.setTimeZone(tz);
		String currentTime = df.format(now);
		FileWriter aWriter = null;
		File file = new File(f);
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		// Remove if clause if you want to overwrite file
		if (!file.exists() && !s.equalsIgnoreCase("remove")) {
			try {
				file.createNewFile();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		else {
			if (s.equalsIgnoreCase("remove")) {
				try {
					file.delete();
					return null;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		if (!s.equalsIgnoreCase("remove")) {
			aWriter = new FileWriter(f, true);

			aWriter.write(currentTime + "  " + s + "\r\n");

			aWriter.flush();
			aWriter.close();
		}
		return aWriter;
	}

	public String getCurrentDateTime(String DATETIME_FORMAT) {
		SimpleDateFormat obj_date = new SimpleDateFormat(DATETIME_FORMAT);
		Calendar obj_calendar = Calendar.getInstance();
		TimeZone estTZ = TimeZone.getTimeZone("America/New_York");
		obj_date.setTimeZone(estTZ);
		String cur_datetime = obj_date.format(obj_calendar.getTime());
		return cur_datetime;
	}

	public String getDate(String DATETIME_FORMAT, int days) {
		SimpleDateFormat obj_date = new SimpleDateFormat(DATETIME_FORMAT);
		Calendar obj_calendar = Calendar.getInstance();
		obj_calendar.add(Calendar.DATE, days);
		TimeZone estTZ = TimeZone.getTimeZone("America/New_York");
		obj_date.setTimeZone(estTZ);
		String cur_datetime = obj_date.format(obj_calendar.getTime());

		return cur_datetime;
	}

	public String getFromatedDate(String Date, String inputDateFormat, String outputDateFormat) throws ParseException {
		SimpleDateFormat dt = new SimpleDateFormat(inputDateFormat);
		java.util.Date date = dt.parse(Date);
		SimpleDateFormat dt1 = new SimpleDateFormat(outputDateFormat);
		String FormatedDate = dt1.format(date).toString();
		return FormatedDate;
	}

}
