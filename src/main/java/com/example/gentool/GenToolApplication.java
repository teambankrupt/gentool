package com.example.gentool;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.stream.IntStream;

record Args(
		GenerationTypes genType,
		String domainName,
		String srcDirPath,
		String genDirPath
) {
}

public class GenToolApplication {

	public static void main(String[] args) {
		if (args == null || args.length < 2) {
			print("You must provide source and destination paths as first and second arguments");
			return;
		}

		Pair<GenerationTypes, String> genTypeAndName = inputGenTypeAndName();

		process(new Args(
				genTypeAndName.getLeft(),
				StringUtils.capitalize(genTypeAndName.getRight()),
				args[0],
				args[1]
		));

//        WebComponentGenerator generator = new FormGenerator();
//        generator.generate(TestClass.class);
	}

	private static Pair<GenerationTypes, String> inputGenTypeAndName() {
		System.out.println("\nWhat do you wanna generate?");
		var genTypes = GenerationTypes.values();
		IntStream.range(0, genTypes.length).forEach(i -> {
			System.out.println((i + 1) + ". " + genTypes[i]);
		});
		System.out.println("\nInput number: ");

		GenerationTypes selectedType = null;
		String name = null;

		try {
			var type = new Scanner(System.in).nextInt();
			selectedType = genTypes[type - 1];

			System.out.println("Name your " + selectedType.getLabel() + ":");

			name = new Scanner(System.in).nextLine();

			System.out.println("Thanks. Generating");
			IntStream.range(0, 100).forEach(i -> {
				try {
					System.out.print("-");
					Thread.sleep(10);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			});
		} catch (Exception ne) {
			System.out.println("\nWrong input. Lets start again..");
			inputGenTypeAndName();
		}

		return new ImmutablePair<>(selectedType, name);
	}


	private static void process(Args args) {

		try {
			print("Starting process...");
			File genDir = genDir(args.genType(), args.domainName(), args.genDirPath());
			copy(exampleDir(args.srcDirPath()), genDir);

			print("Replacing file name and file contents with your provided domain name...");
			replace(args.genType(), genDir(args.genType(), args.domainName(), args.genDirPath()), args.domainName());
			print("Completed!");

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static File genDir(GenerationTypes genType, String packageName, String genPath) throws IOException {
		File genDir = new File(genPath);
		if (!genDir.exists()) {
			throw new IOException("Generation directory doesn't exist:\nDir: " + genDir.getAbsolutePath());
		}
		genDir = new File(genDir + File.separator + packageName.toLowerCase() + genType.getSuffixForPackage());
		if (!genDir.exists()) genDir.mkdir();
		return genDir;
	}

	public static File exampleDir(String examplePackagePath) throws IOException {
		if (examplePackagePath == null)
			throw new IllegalArgumentException("You must provide an example package path!");

		File exampleDir = new File(examplePackagePath);
		if (!exampleDir.exists()) {
			throw new IOException("Example CRUD package doesn't exist:\nDir: " + exampleDir.getAbsolutePath());
		}
		return exampleDir;
	}

	public static void copy(File srcDir, File genDir) throws IOException {
		System.out.println("Copying example directory..");
		FileUtils.copyDirectory(srcDir, genDir);
		System.out.println("Directory copied successfully..\n");
		System.out.println("--------------------------------------------------------\n");
	}

	private static void replace(GenerationTypes genType, File genDir, String domainName) throws IOException {
		if (genDir == null || !genDir.exists()) {
			print("Generation directory doesn't exist:\nDir: " + genDir.getAbsolutePath());
			return;
		}

		File[] listOfFiles = genDir.listFiles();

		for (int i = 0; i < (listOfFiles != null ? listOfFiles.length : 0); i++) {
			File file = listOfFiles[i];

			System.out.println("Rename file: " + file.getName());
			file = changeFilename(genType, file, domainName);
			System.out.println("Successfully renamed to: " + file.getName() + "\n\nReplacing file contents with domain name..");

			if (file.isFile()) {
				replaceTexts(genType, file, domainName);
				System.out.println("\nSuccessfully Replaced!");
			} else if (file.isDirectory()) {
				System.out.println("\n\nFound a directory named: " + file.getName() + " instead of a file. Processing files recursively: \n\n");
				replace(genType, file, domainName);
			}
		}
	}

	private static void replaceTexts(GenerationTypes genType, File file, String domainName) throws IOException {
		if (genType == GenerationTypes.MODULE) {
			replaceText(file, "examplemodule", domainName.toLowerCase());
			replaceText(file, "ExampleApplication", domainName + "Application");
			replaceText(file, "Example", domainName);
			replaceText(file, "example", domainName.toLowerCase());
			replaceText(file, "EXAMPLE", domainName.toUpperCase());
		} else if (genType == GenerationTypes.CRUD) {
			replaceText(file, "CrudExample", domainName);
			replaceText(file, "crudExample", Character.toLowerCase(domainName.charAt(0)) + domainName.substring(1));
			replaceText(file, "crudexample", domainName.toLowerCase());
			replaceText(file, "CRUDEXAMPLE", domainName.toUpperCase());
		}
	}

	private static File changeFilename(GenerationTypes genType, File file, String domainName) throws IOException {
		String filePath = file.getAbsolutePath();
		if (genType == GenerationTypes.MODULE) {
			filePath = filePath.replace("examplemodule", domainName.toLowerCase());
			filePath = filePath.replace("ExampleApplication", domainName + "Application");
			filePath = filePath.replace("Example", domainName);
			filePath = filePath.replace("example", domainName.toLowerCase());
		} else if (genType == GenerationTypes.CRUD) {
			filePath = filePath.replace("CrudExample", domainName);
			filePath = filePath.replace("crudexample", domainName.toLowerCase());
		}
		File renamedFile = new File(filePath);
		file.renameTo(renamedFile);
		return renamedFile;
	}

	private static void replaceText(File file, String placeholder, String replacement) throws IOException {
		String content = IOUtils.toString(new FileInputStream(file), Charset.defaultCharset());
		content = content.replaceAll(placeholder, replacement);
		IOUtils.write(content, new FileOutputStream(file), Charset.defaultCharset());
	}


	private static void print(String message) {
		System.out.println("!!!!!!!!-------------------------!!!!!!!");
		System.out.println(message);
		System.out.println("!!!!!!!!-------------------------!!!!!!!");

	}
}
