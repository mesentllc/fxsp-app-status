package com.fedex.smartpost.utilities.service;

import com.fedex.smartpost.utilities.common.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class CommandService {
	private static final Log logger = LogFactory.getLog(CommandService.class);

	public void mvnCompile(File rootPath) throws IOException {
		if (rootPath.isDirectory()) {
			logger.info("Attempting to call maven build on " + rootPath.getAbsolutePath());
			String[] command = {"cmd.exe", "/c", "mvn -f " + rootPath.getAbsolutePath() + " clean package -DskipTests"};
			executeAndLogStdOut(command);
		}
		else {
			logger.warn(rootPath.getAbsolutePath() + " is not an acceptable path to run maven on.");
		}
	}

	public void gradleCompile(File rootPath) throws IOException {
		if (rootPath.isDirectory()) {
			logger.info("Attempting to call gradle build on " + rootPath.getAbsolutePath());
			String[] command = {"cmd.exe", "/c", "gradle -p " + rootPath.getAbsolutePath() + " clean build --exclude-task test"};
			executeAndLogStdOut(command);
		}
		else {
			logger.warn(rootPath.getAbsolutePath() + " is not an acceptable path to run gradle on.");
		}
	}

	public void checkOwasp(String dcPath, File rootPath, String outputDir) throws IOException {
		String path = rootPath.getAbsolutePath();
		if (rootPath.isDirectory()) {
			String filename = CommonUtils.getFilename(rootPath);
			String executeString = dcPath + " -s " + path + "\\**\\*.jar -p DepAnalysis -proxyserver internet.proxy.fedex.com --proxyport 3128 -o ";
			if (StringUtils.isNotBlank(outputDir)) {
				executeString += outputDir + "\\" + filename;
			}
			else {
				executeString += path;
			}
			String[] command = {"cmd.exe", "/c", executeString};
			executeAndLogStdOut(command);
		}
	}

	private void executeAndLogStdOut(String[] command) throws IOException {
		String output;

		Process process = Runtime.getRuntime().exec(command);
		BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		while ((output = br.readLine()) != null) {
			System.out.println(output);
		}
		br.close();
	}
}
