package com.fedex.smartpost.utilities;

import com.fedex.smartpost.utilities.common.CommandExecutor;

import java.io.File;
import java.io.IOException;

public class BuildAll extends CommandExecutor {
	public static void main(String[] args) throws IOException {
		BuildAll buildAll = new BuildAll();
		buildAll.build();
	}

	@Override
	protected void build() throws IOException {
		File root = new File("D:\\Projects");
		File[] fileList = root.listFiles();
		for (File file : fileList) {
			if (file.isDirectory()) {
				String[] command = {"cmd.exe", "/c", "mvn -f " + file.getAbsolutePath() + " clean package -DskipTests"};
				executeAndLogStdOut(command);
			}
		}
	}
}
