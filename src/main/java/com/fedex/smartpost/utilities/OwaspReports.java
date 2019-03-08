package com.fedex.smartpost.utilities;

import com.fedex.smartpost.utilities.common.CommandExecutor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class OwaspReports extends CommandExecutor {
	public static void main(String[] args) throws IOException {
		OwaspReports owaspReports = new OwaspReports();
		owaspReports.build();
	}

	@Override
	protected void build() throws IOException {
		int iteration = 0;
		File root = new File("D:\\Projects");
		File[] fileList = root.listFiles();
		for (File file : fileList) {
			if (file.isDirectory()) {
				String filename = file.getAbsolutePath();
				filename = filename.substring(filename.lastIndexOf("\\") + 1);
				String executeString = "D:\\dependency-check\\bin\\dependency-check.bat -s " + file.getAbsolutePath() + "\\**\\*.jar " +
				                       "--proxyserver internet.proxy.fedex.com --proxyport 3128 -o D:\\OWASP\\" + filename;
				// -n = don't want it to check for updates, though the app might now ignore updates less than 24 hours old.
				if (iteration++ > 0) {
					executeString += " -n";
				}
				String[] command = {"cmd.exe", "/c", executeString};
				executeAndLogStdOut(command);
			}
		}
	}
}
