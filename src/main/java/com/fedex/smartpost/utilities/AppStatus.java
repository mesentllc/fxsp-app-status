package com.fedex.smartpost.utilities;

import com.fedex.smartpost.utilities.common.CommonUtils;
import com.fedex.smartpost.utilities.service.CommandService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class AppStatus {
	private static final Log logger = LogFactory.getLog(AppStatus.class);
	private static Map<String, String> arguments = new HashMap<>();
	private static CommandService commandService = new CommandService();
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
		appStatus.retrieveArguments(args);
		appStatus.process();
		bw.close();
	}

	private void retrieveArguments(String[] args) {
		if (args == null) {
			return;
		}
		for (int ptr = 0, maxPtr = args.length; ptr < maxPtr; ptr++) {
			switch (args[ptr].toLowerCase()) {
				case "-c":
					arguments.put("compile", "yes");
					break;
				case "-r":
					arguments.put("recurse", "yes");
					break;
				case "-spath":
					arguments.put("spath", args[++ptr]);
					break;
				case "-opath":
					arguments.put("opath", args[++ptr]);
					break;
				case "-dcpath":
					arguments.put("dcpath", args[++ptr]);
					break;
				case "-h":
					arguments.put("help", "true");
			}
		}
		if (arguments.containsKey("help")) {
			printHelp();
			System.exit(0);
		}
		if (!arguments.containsKey("dcpath")) {
			throw new RuntimeException("Required path to dependency-check (-dcpath argument) missing.  Use -h to display help.");
		}
		if (arguments.containsKey("opath")) {
			File file = new File(arguments.get("opath"));
			file.mkdirs();
		}
	}

	private void printHelp() {
		StringBuilder sb = new StringBuilder();
		sb.append("Usage: fxsp-app-status [switches]").append("\n\n")
		  .append("Switches:").append("\n")
		  .append("\t-c\t\t- Compile the application first, before running dependency-check").append("\n")
		  .append("\t-h\t\t- Print this help page").append("\n")
		  .append("\t-r\t\t- Recurse through all the directories of applications designated by the spath switch").append("\n")
		  .append("\t-dcpath <path>\t- [REQUIRED] - Path to the OWASP dependency-check.bat file").append("\n")
		  .append("\t-opath <path>\t- Path to put the analysis reports, if not set, it will put the report in the application's root folder").append("\n")
		  .append("\t-spath <path>\t- Path to the source files (for compile) and/or JAR files if just analyzing [DEFAULT: current directory]").append("\n\n");

		System.out.println(sb.toString());
	}

	private void process() throws IOException {
		String root = ".";

		if (arguments.containsKey("spath")) {
			root = arguments.get("spath");
		}
		File rootDir = new File(root);
		if (!rootDir.isDirectory()) {
			throw new RuntimeException(root + " is not a directory -- can't search on a file.");
		}
		if (arguments.containsKey("recurse")) {
			File[] files = rootDir.listFiles();
			if (!CommonUtils.foundProjectRoot(files)) {
				processDirectories(files);
				return;
			}
		}
		processPath(rootDir);
	}

	private void processPath(File rootDir) throws IOException {
		File[] files = rootDir.listFiles();
		if (CommonUtils.foundProjectRoot(files)) {
			checkProjectRoot(rootDir);
			iterateOver(files, null);
			compileIfAsked(rootDir);
			performOwasp(rootDir);
		}
	}

	private void processDirectories(File[] files) throws IOException {
		if (files == null) {
			return;
		}
		for (File file : files) {
			if (file.isDirectory()) {
				processPath(file);
			}
		}
	}

	private void compileIfAsked(File rootDir) throws IOException {
		if (arguments.containsKey("compile")) {
			switch (CommonUtils.getProjectType(rootDir)) {
				case "maven":
					commandService.mvnCompile(rootDir);
					break;
				case "gradle":
					commandService.gradleCompile(rootDir);
			}
		}
	}

	private void performOwasp(File rootDir) throws IOException {
		String dcCommand = arguments.get("dcpath");
		if (!dcCommand.contains("\\bin")) {
			if (!dcCommand.endsWith("\\")) {
				dcCommand += "\\";
			}
			dcCommand += "bin\\dependency-check.bat";
		}
		if (!dcCommand.endsWith("dependency-check.bat")) {
			if (!dcCommand.endsWith("\\")) {
				dcCommand += "\\";
			}
			dcCommand += "dependency-check.bat";
		}
		commandService.checkOwasp(dcCommand, rootDir, arguments.get("opath"));
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
				logDependencies(rootPath, resetRoot, file);
			}
		}
	}

	private void logDependencies(String rootPath, boolean resetRoot, File file) throws IOException {
		File[] directoryFiles = file.listFiles();
		if (directoryFiles != null && CommonUtils.foundProjectRoot(directoryFiles)) {
			checkProjectRoot(file);
		}
		String filename = CommonUtils.getFilename(file);
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
