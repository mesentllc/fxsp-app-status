package com.fedex.smartpost.utilities;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class AppStatus {
	private static final Log logger = LogFactory.getLog(AppStatus.class);
	private static BufferedWriter bw;

	public static void main(String[] args) throws IOException {
		AppStatus appStatus = new AppStatus();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		Calendar calendar = Calendar.getInstance();

		if (args.length == 0) {
			logger.info("Please supply a starting path to search.");
			return;
		}
		bw = new BufferedWriter(new FileWriter(String.format("appStatus-%s.txt", sdf.format(calendar.getTimeInMillis()))));
		appStatus.process(args[0]);
		bw.close();
	}

	private void process(String root) throws IOException {
		File rootDir = new File(root);
		File[] files;

		if (!rootDir.isDirectory()) {
			throw new RuntimeException(root + " is not a directory -- can't search on a file.");
		}
		else {
			files = rootDir.listFiles();
			if (foundProjectRoot(files)) {
				checkProjectRoot(rootDir);
			}
		}
		iterateOver(files, null);
	}

	private boolean foundProjectRoot(File[] files) {
		boolean pomFound = false;
		for (File file : files) {
			String filename = file.getName();
			if (filename.equals("pom.xml") || filename.equals("build.gradle")) {
				pomFound = true;
				break;
			}
		}
		return pomFound;
	}

	private void checkProjectRoot(File rootDir) throws IOException {
		StringBuilder sb = new StringBuilder();
		File[] files = rootDir.listFiles();
		for (File file : files) {
			switch (file.getName()) {
				case "pom.xml":
					sb.append(logProperties(file));
					sb.append(logPomDependencies(file));
					break;
				case "build.gradle":
					sb.append(logGradleDependencies(file));
					break;
				case "JenkinsFile":
					sb.append(checkJenkinsFile(file));
			}
		}
		if (sb.length() > 0) {
			bw.write("Processing " + rootDir + "\n");
			bw.write(sb.toString() + "\n");
		}
	}

	private String logProperties(File file) throws IOException {
		BufferedReader br = logFileAndGetReader(file);
		StringBuilder sb = new StringBuilder();
		boolean inProperties = false;
		boolean inProfiles = false;

		while (br.ready()) {
			String line = br.readLine().trim();

			if (line.toLowerCase().trim().contains("<profiles>")) {
				inProfiles = true;
			}
			if (line.toLowerCase().trim().contains("</profiles>")) {
				inProfiles = false;
			}
			if (!inProfiles && line.toLowerCase().trim().contains("<properties>")) {
				inProperties = true;
			}
			if (inProfiles || line.toLowerCase().trim().contains("</properties>")) {
				inProperties = false;
			}
			if (inProperties && !line.equals("<properties>")) {
				logger.info(line);
				sb.append("Property: ").append(line).append("\n");
			}
		}
		br.close();
		return sb.toString();
	}

	private String checkJenkinsFile(File file) {
		// TODO: Add the BL to look for the key items and log accordingly.
		return "";
	}

	private String logGradleDependencies(File file) throws IOException {
		BufferedReader br = logFileAndGetReader(file);
		StringBuilder sb = new StringBuilder();

		while (br.ready()) {
			String line = br.readLine().trim();
			if (line.toLowerCase().contains("compile") || line.toLowerCase().contains("implementation")) {
				logger.info(line);
				sb.append("Gradle: ").append(line).append("\n");
			}
		}
		br.close();
		return sb.toString();
	}

	private String logPomDependencies(File file) throws IOException {
		StringBuilder sb = new StringBuilder();

		String artifact = null;
		String group = null;
		String version = null;
		String tempStr;
		boolean inExclusion = false;

		BufferedReader br = logFileAndGetReader(file);
		String filename = file.getAbsolutePath();
		filename = filename.substring(0, filename.lastIndexOf('\\'));
		filename = filename.substring(filename.lastIndexOf("\\") + 1);
		while (br.ready()) {
			String line = br.readLine().trim();
			if (line.toLowerCase().contains("<exclusions>")) {
				inExclusion = true;
			}
			if (line.toLowerCase().contains("</exclusions>")) {
				inExclusion = false;
			}
			if (!inExclusion) {
				tempStr = getValue(line, "<groupid>");
				if (tempStr != null) {
					if (artifact != null && !artifact.equals(filename)) {
						sb.append(dump(group, artifact, version));
					}
					group = tempStr;
					artifact = null;
					version = null;
				}
				tempStr = getValue(line, "<artifactid>");
				if (tempStr != null && !tempStr.contains("plugin")) {
					artifact = tempStr;
				}
				tempStr = getValue(line, "<version>");
				if (tempStr != null) {
					version = tempStr;
				}
			}
		}
		sb.append(dump(group, artifact, version));
		br.close();
		return sb.toString();
	}

	private BufferedReader logFileAndGetReader(File file) throws IOException {
		String filename = file.getAbsolutePath();
		logger.info("Processing " + filename);
		return new BufferedReader(new FileReader(file));
	}

	private String dump(String group, String artifact, String version) {
		if (StringUtils.isNotBlank(group) && StringUtils.isNotBlank(artifact) && StringUtils.isNotBlank(version)) {
			logger.info(String.format("POM: [%s, %s, %s]", group, artifact, version));
			return String.format("POM: [%s, %s, %s]", group, artifact, version) + "\n";
		}
		return "";
	}

	private String getValue(String line, String pattern) {
		int pos = line.toLowerCase().indexOf(pattern);
		if (pos > -1) {
			int endPos = line.toLowerCase().indexOf(pattern.replace("<", "</"));
			if (endPos > 0) {
				return line.substring(pos + pattern.length(), endPos);
			}
		}
		return null;
	}

	private void iterateOver(File[] files, String rootPath) throws IOException {
		boolean resetRoot = false;
		if (rootPath == null) {
			resetRoot = true;
		}
		for (File file : files) {
			if (file.isDirectory()) {
				File[] directoryFiles = file.listFiles();
				if (directoryFiles != null && foundProjectRoot(directoryFiles)) {
					checkProjectRoot(file);
				}
				String filename = file.getAbsolutePath();
				filename = filename.substring(filename.lastIndexOf("\\") + 1);
				if (!filename.equals(rootPath)) {
					if (resetRoot) {
						iterateOver(directoryFiles, filename);
					}
					else {
						if (filename.contains(rootPath)) {
							iterateOver(directoryFiles, rootPath);
						}
					}
				}
			}
		}
	}
}
