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

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
//import com.restfb.Version;
import com.restfb.json.JsonObject;
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
import java.util.Properties;
import java.util.TimeZone;
import org.riveriq.csvwriter.CSVWriter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Ashish kumar
 * @email niits007@gmail.com
 * @website riveriq.com
 * @since June 12, 2018
 * @version 1.0
 */
public class FBAdInsight {
	private static FacebookClient facebookClient;
	private static String propFilePath = "";
	private static String AD_INSIGH_KEYMETRICS_LOG_PATH = "";
	static String AD_INSIGH_FROM = "";
	static String AD_INSIGH_TO = "";
	static String AD_ACCOUNT_NAME = "";
	static String AD_ACCOUNT_ID = "";
	static String CurrentDate = "";
	JsonNode rootNode;
	ObjectMapper objectMapper;
	ArrayList<String> list_ad_id = new ArrayList<String>();
	ArrayList<String> list_ad_name = new ArrayList<String>();
	ArrayList<String> list_date_start = new ArrayList<String>();
	ArrayList<String> list_date_stop = new ArrayList<String>();
	ArrayList<String> list_publisher_platform = new ArrayList<String>();
	ArrayList<String> list_reach = new ArrayList<String>();
	ArrayList<String> list_spend = new ArrayList<String>();
	ArrayList<String> list_clicks = new ArrayList<String>();
	ArrayList<String> list_impressions = new ArrayList<String>();
	ArrayList<String> list_actions_video_view = new ArrayList<String>();
	ArrayList<String> list_actions_post = new ArrayList<String>();
	ArrayList<String> list_actions_comment = new ArrayList<String>();
	ArrayList<String> list_actions_post_reaction = new ArrayList<String>();
	ArrayList<String> list_actions_link_click = new ArrayList<String>();
	ArrayList<String> list_video_avg_time_watched_actions_video_view = new ArrayList<String>();
	ArrayList<String> list_video_p100_watched_actions_video_view = new ArrayList<String>();
	ArrayList<String> list_action_type_value = new ArrayList<String>();

	public static void main(String[] args) throws Exception {
		propFilePath = args[0].toString();
		FBAdInsight obj_FBPage = new FBAdInsight();
		AD_ACCOUNT_NAME = obj_FBPage.getPropValue(propFilePath, "AD_ACCOUNT_NAME");
		AD_ACCOUNT_ID = obj_FBPage.getPropValue(propFilePath, "AD_ACCOUNT_ID");
		String cur_datetime = obj_FBPage.getCurrentDateTime("yyyyMMdd_HHmmss");
		AD_INSIGH_FROM = obj_FBPage.getPropValue(propFilePath, "AD_INSIGH_FROM");
		AD_INSIGH_TO = obj_FBPage.getPropValue(propFilePath, "AD_INSIGH_TO");
		if (AD_INSIGH_FROM.equalsIgnoreCase("") & AD_INSIGH_TO.equalsIgnoreCase("")) {
			AD_INSIGH_TO = obj_FBPage.getDate("yyyy-MM-dd", 0);
			AD_INSIGH_FROM = obj_FBPage.getDate("yyyy-MM-dd", -7);
		}

		CurrentDate = obj_FBPage.getDate("yyyy-MM-dd", 0);

		AD_INSIGH_KEYMETRICS_LOG_PATH = obj_FBPage.getPropValue(propFilePath, "AD_INSIGH_KEYMETRICS_LOG_PATH") + "_"
				+ cur_datetime + ".log";

		obj_FBPage.write(AD_INSIGH_KEYMETRICS_LOG_PATH,
				"==> DATA WILL BE DOWNLOADED FOR FACEBOOK AD ACCOUNT :" + AD_ACCOUNT_NAME + "(" + AD_ACCOUNT_ID
						+ ") FOR DURATION BETWEEN " + AD_INSIGH_FROM + " AND " + AD_INSIGH_FROM);

		obj_FBPage.write(AD_INSIGH_KEYMETRICS_LOG_PATH, "==> READING ACCESS TOKEN");

		String ACCESS_TOKEN = obj_FBPage.getPropValue(propFilePath, "ACCESS_TOKEN");
		// facebookClient = new DefaultFacebookClient(ACCESS_TOKEN,
		// Version.VERSION_2_10);
		facebookClient = new DefaultFacebookClient(ACCESS_TOKEN);
		JsonObject adInsight_fields = obj_FBPage.fetchObject();
		obj_FBPage.parseJSONInsight(adInsight_fields.toString());
		List<Map<String, String>> list_Adinsights = obj_FBPage.fromatJsonDataToCSV();
		obj_FBPage.writecsv(list_Adinsights);
	}

	public JsonObject fetchObject() throws IOException {
		String AD_INSIGHT_MATRIX = getPropValue(propFilePath, "AD_INSIGHT_MATRIX");
		String AD_INSIGHT_BREAKDOWNS = getPropValue(propFilePath, "AD_INSIGHT_BREAKDOWNS");

		write(AD_INSIGH_KEYMETRICS_LOG_PATH, "==> READING AD INSIGHT");
		JsonObject adInsight_fields = null;
		try {
			adInsight_fields = facebookClient.fetchObject(AD_ACCOUNT_ID + "/ads", JsonObject.class, Parameter.with(
					"fields",
					"insights.breakdowns(" + AD_INSIGHT_BREAKDOWNS + ").time_range({\"since\":\"" + AD_INSIGH_FROM
							+ "\",\"until\":\"" + AD_INSIGH_TO + "\"}){" + AD_INSIGHT_MATRIX + "}"),
					Parameter.with("filtering",
							"[{\"field\":\"effective_status\",\"operator\":\"IN\", \"value\":[\"ACTIVE\"]}]"),
					Parameter.with("limit", "5000"));
		} catch (Exception e) {
			write(AD_INSIGH_KEYMETRICS_LOG_PATH, "==> EXCEPTION WHILE READING AD INSIGHT ==> " + e);
			System.out.println(e);
			System.exit(0);
		}
		return adInsight_fields;
	}

	public void parseJSONInsight(String adInsight_fields) throws IOException {
		objectMapper = new ObjectMapper();
		write(AD_INSIGH_KEYMETRICS_LOG_PATH, "==> PARSING JSON INSIGHTS");
		try {
			rootNode = objectMapper.readTree(adInsight_fields);
			JsonNode n_data = rootNode.path("data");
			Iterator<JsonNode> itr_n_data = n_data.elements();
			while (itr_n_data.hasNext()) {
				JsonNode n_insights = itr_n_data.next();
				JsonNode n_insights_fields = n_insights.path("insights").path("data");
				JsonNode n_ad_id = n_insights.path("id");
				Iterator<JsonNode> itr_n_insights_fields = n_insights_fields.elements();
				while (itr_n_insights_fields.hasNext()) {
					JsonNode valuenode_l1 = itr_n_insights_fields.next();
					JsonNode n_ad_name = valuenode_l1.path("ad_name");
					JsonNode n_date_start = valuenode_l1.path("date_start");
					JsonNode n_date_stop = valuenode_l1.path("date_stop");
					JsonNode n_reach = valuenode_l1.path("reach");
					JsonNode n_spend = valuenode_l1.path("spend");
					JsonNode n_clicks = valuenode_l1.path("clicks");
					JsonNode n_impressions = valuenode_l1.path("impressions");
					JsonNode n_publisher_platform = valuenode_l1.path("publisher_platform");
					JsonNode n_actions = valuenode_l1.path("actions");
					JsonNode n_video_avg_time_watched_actions = valuenode_l1.path("video_avg_time_watched_actions");
					JsonNode n_list_video_p100_watched_actions = valuenode_l1.path("video_p100_watched_actions");
					String ad_id = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(n_ad_id)
							.replace("\"", "");
					String ad_name = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(n_ad_name);
					String date_start = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(n_date_start)
							.replace("\"", "");
					String date_stop = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(n_date_stop)
							.replace("\"", "");
					String publisher_platform = objectMapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(n_publisher_platform);
					String reach = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(n_reach);
					String spend = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(n_spend);
					String clicks = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(n_clicks);
					String impressions = objectMapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(n_impressions);
					list_ad_id.add(String.valueOf(Long.parseLong(ad_id)));
					list_ad_name.add(ad_name);
					list_date_start.add(date_start);
					list_date_stop.add(date_stop);
					list_publisher_platform.add(publisher_platform);
					list_reach.add(reach);
					list_spend.add(spend);
					list_clicks.add(clicks);
					list_impressions.add(impressions);

					Iterator<JsonNode> itr_n_actions = n_actions.elements();
					Iterator<JsonNode> itr_n_video_avg_time_watched_actions = n_video_avg_time_watched_actions
							.elements();
					Iterator<JsonNode> itr_n_list_video_p100_watched_actions = n_list_video_p100_watched_actions
							.elements();
					int list_actions_video_view_flag = 0;
					int list_actions_post_flag = 0;
					int list_actions_post_reaction_flag = 0;
					int list_actions_comment_flag = 0;
					int list_actions_link_click_flag = 0;
					int list_video_avg_time_watched_actions_click_flag = 0;
					int list_video_p100_watched_actions_video_view_flag = 0;
					while (itr_n_actions.hasNext()) {
						JsonNode valuenode_l2 = itr_n_actions.next();
						JsonNode n_action_type = valuenode_l2.path("action_type");
						JsonNode n_action_type_value = valuenode_l2.path("value");
						String action_type = objectMapper.writerWithDefaultPrettyPrinter()
								.writeValueAsString(n_action_type).replace("\"", "");
						String value = objectMapper.writerWithDefaultPrettyPrinter()
								.writeValueAsString(n_action_type_value);
						if (action_type.equalsIgnoreCase("video_view")) {
							list_actions_video_view.add(value);
							list_actions_video_view_flag = 1;
						} else if (action_type.equalsIgnoreCase("post")) {
							list_actions_post.add(value);
							list_actions_post_flag = 1;
						} else if (action_type.equalsIgnoreCase("post_reaction")) {
							list_actions_post_reaction.add(value);
							list_actions_post_reaction_flag = 1;
						} else if (action_type.equalsIgnoreCase("comment")) {
							list_actions_comment.add(value);
							list_actions_comment_flag = 1;
						} else if (action_type.equalsIgnoreCase("link_click")) {
							list_actions_link_click.add(value);
							list_actions_link_click_flag = 1;
						}
					}
					while (itr_n_video_avg_time_watched_actions.hasNext()) {
						JsonNode valuenode_l3 = itr_n_video_avg_time_watched_actions.next();
						JsonNode n_action_type = valuenode_l3.path("action_type");
						JsonNode n_action_type_value = valuenode_l3.path("value");
						String action_type = objectMapper.writerWithDefaultPrettyPrinter()
								.writeValueAsString(n_action_type).replace("\"", "");
						String value = objectMapper.writerWithDefaultPrettyPrinter()
								.writeValueAsString(n_action_type_value).replace("\"", "");
						if (action_type.equalsIgnoreCase("video_view")) {
							list_video_avg_time_watched_actions_video_view.add(value);
							list_video_avg_time_watched_actions_click_flag = 1;
						}
					}
					while (itr_n_list_video_p100_watched_actions.hasNext()) {
						JsonNode valuenode_l4 = itr_n_list_video_p100_watched_actions.next();
						JsonNode n_action_type = valuenode_l4.path("action_type");
						JsonNode n_action_type_value = valuenode_l4.path("value");
						String action_type = objectMapper.writerWithDefaultPrettyPrinter()
								.writeValueAsString(n_action_type).replace("\"", "");
						String value = objectMapper.writerWithDefaultPrettyPrinter()
								.writeValueAsString(n_action_type_value);
						if (action_type.equalsIgnoreCase("video_view")) {
							list_video_p100_watched_actions_video_view.add(value);
							list_video_p100_watched_actions_video_view_flag = 1;
						}
					}
					if (list_actions_video_view_flag == 0) {
						list_actions_video_view.add("");
					}
					if (list_actions_post_flag == 0) {
						list_actions_post.add("");
					}
					if (list_actions_post_reaction_flag == 0) {
						list_actions_post_reaction.add("");
					}
					if (list_actions_comment_flag == 0) {
						list_actions_comment.add("");
					}
					if (list_actions_link_click_flag == 0) {
						list_actions_link_click.add("");
					}
					if (list_video_avg_time_watched_actions_click_flag == 0) {
						list_video_avg_time_watched_actions_video_view.add("0");
					}
					if (list_video_p100_watched_actions_video_view_flag == 0) {
						list_video_p100_watched_actions_video_view.add("");
					}
				}
			}
		} catch (Exception e) {
			write(AD_INSIGH_KEYMETRICS_LOG_PATH, "==> EXCEPTION WHILE PARSING JSON INSIGHTS ==> " + e);
			System.out.println(e);
			System.exit(0);
		}
	}

	public List<Map<String, String>> fromatJsonDataToCSV() throws IOException {
		List<Map<String, String>> list_Adinsights = new ArrayList<Map<String, String>>();
		try {
			write(AD_INSIGH_KEYMETRICS_LOG_PATH, "==> CONVERTING JSON INSIGHTS TO CSV FORMAT");
			LinkedHashMap<String, String> map_value_matrix = null;

			for (int i = 0; i < list_ad_id.size(); i++) {
				map_value_matrix = new LinkedHashMap<String, String>();
				// map_value_matrix.put("Ad_ID", list_ad_id.get(i).toString());

				map_value_matrix.put("Reporting_Starts", list_date_start.get(i).toString());
				map_value_matrix.put("Reporting_Ends", list_date_stop.get(i));
				map_value_matrix.put("Ad_Name", list_ad_name.get(i));
				map_value_matrix.put("Platform", list_publisher_platform.get(i));
				map_value_matrix.put("Reach", list_reach.get(i));
				map_value_matrix.put("Amount_Spent_USD", list_spend.get(i));
				map_value_matrix.put("Clicks_All", list_clicks.get(i));
				map_value_matrix.put("Impressions", list_impressions.get(i));
				map_value_matrix.put("Link_Clicks", list_actions_link_click.get(i));
				// Need to add after validating Photo_view data
				map_value_matrix.put("Photo_Views", "");
				map_value_matrix.put("Post_Shares", list_actions_post.get(i));
				map_value_matrix.put("Post_Comments", list_actions_comment.get(i));
				map_value_matrix.put("Post_Reactions", list_actions_post_reaction.get(i));
				map_value_matrix.put("3_Second_Video_Views", list_actions_video_view.get(i));
				map_value_matrix.put("Video_Average_Watch_Time",
						getConvertTime(list_video_avg_time_watched_actions_video_view.get(i)));
				map_value_matrix.put("Video_Watches_at_100", list_video_p100_watched_actions_video_view.get(i));
				list_Adinsights.add(map_value_matrix);

			}

		} catch (Exception e) {
			write(AD_INSIGH_KEYMETRICS_LOG_PATH, "==> EXCEPTION WHILE CONVERTING JSON INSIGHTS TO CSV FORMAT ==> " + e);
			System.out.println(e);
			System.exit(0);
		}
		return list_Adinsights;
	}

	public void writecsv(List<Map<String, String>> clist_insights) throws Exception {
		write(AD_INSIGH_KEYMETRICS_LOG_PATH, "==> WRITING INSIGHTS INTO CSV FILE");
		String AD_INSIGH_KEYMETRICS_CSV_PATH = getPropValue(propFilePath, "AD_INSIGH_KEYMETRICS_CSV_PATH")
				/*
				 * + "\\" +getFromatedDate(AD_INSIGH_TO,"yyyy-MM-dd","yyyyMMdd")
				 */
				+ "\\" + getPropValue(propFilePath, "AD_INSIGH_KEYMETRICS_CSV_FILENAME") + "_"
				+ getFromatedDate(CurrentDate, "yyyy-MM-dd", "yyyyMMdd") + ".csv";

		try {
			CSVWriter.writeToFile(CSVWriter.getCSV(clist_insights), AD_INSIGH_KEYMETRICS_CSV_PATH);
		} catch (Exception e) {
			write(AD_INSIGH_KEYMETRICS_LOG_PATH, "==> EXCEPTION WHILE WRITING INSIGHTS INTO CSV FILE ==> " + e);
			System.out.println(e);
			System.exit(0);
		}
		write(AD_INSIGH_KEYMETRICS_LOG_PATH, "==> CSV DOWNLOADED :" + AD_INSIGH_KEYMETRICS_CSV_PATH);
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

	public String getConvertTime(String Date) {
		int millis = Integer.parseInt(Date) * 1000;
		TimeZone tz = TimeZone.getTimeZone("UTC");
		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
		df.setTimeZone(tz);
		String Formatedtime = df.format(new Date(millis));
		return Formatedtime;

	}

	public String getFromatedDate(String Date, String inputDateFormat, String outputDateFormat) throws ParseException {
		SimpleDateFormat dt = new SimpleDateFormat(inputDateFormat);
		java.util.Date date = dt.parse(Date);
		SimpleDateFormat dt1 = new SimpleDateFormat(outputDateFormat);
		String FormatedDate = dt1.format(date).toString();
		return FormatedDate;
	}

}
