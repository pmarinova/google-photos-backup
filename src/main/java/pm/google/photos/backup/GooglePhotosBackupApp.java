package pm.google.photos.backup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.auth.Credentials;

import pm.google.photos.backup.auth.GoogleAuthFlow;


public class GooglePhotosBackupApp {
	
	private static final String CMD_OPTION_CLIENT_SECRET = "client_secret";
	private static final String CMD_OPTION_CLIENT_SECRET_DEFAULT = "client_secret.json";

	private static final String CMD_OPTION_BACKUP_DIR = "backup_dir";
	private static final String CMD_OPTION_BACKUP_DIR_DEFAULT = "backup";

	private static final String CMD_OPTION_START_DATE = "start_date";
	private static final String CMD_OPTION_END_DATE = "end_date";
	
	private static final String CMD_OPTION_HELP = "help";


	public static void main(String[] args) {

		Options options = new Options();
		
		options.addOption(CMD_OPTION_CLIENT_SECRET, true, "the path to the client_secret.json file");
		options.addOption(CMD_OPTION_BACKUP_DIR, 	true, "the backup directory where photos will be downloaded");
		options.addOption(CMD_OPTION_START_DATE, 	true, "optional start date (YYYY-MM-DD), if specified only photos created after this date will be backed up");
		options.addOption(CMD_OPTION_END_DATE, 		true, "optional end date (YYYY-MM-DD), if specified only photos created before this date will be backed up");
		
		options.addOption(CMD_OPTION_HELP, "print usage");
		
		
		try {
			CommandLine cmdLine = new DefaultParser().parse(options, args);
			
			if (cmdLine.hasOption(CMD_OPTION_HELP)) {
				printHelp(options);
				System.exit(0);
			}

			File clientSecretFile = getClientSecretFile(cmdLine);
			File backupDir = getBackupDir(cmdLine);
			LocalDate startDate = getStartDate(cmdLine);
			LocalDate endDate = getEndDate(cmdLine);

			runGooglePhotosBackup(clientSecretFile, backupDir, startDate, endDate);
			System.exit(0);
		}
		catch (ParseException ex) {
			ex.printStackTrace();
			printHelp(options);
			System.exit(1);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("Backup failed: " + ex.getMessage());
			System.exit(2);
		}
	}


	public static void runGooglePhotosBackup(
			File clientSecretFile,
			File backupDir,
			LocalDate startDate,
			LocalDate endDate) {

		System.out.println("Client secret file is " + clientSecretFile.getAbsolutePath());
		System.out.println("Backup directory is " + backupDir.getAbsolutePath());
		
		System.out.println(String.format("Backing up photos from %s till %s",
				(startDate != null ? startDate : "the beginning"),
				(endDate != null ? endDate : "now")));


		File dataStoreDir = new File(backupDir, ".data_store");
		File credentialsDataStore = new File(dataStoreDir, "credentials");
		File photosIndexDataStore = new File(dataStoreDir, "index");
		File photosBackupDir = new File(backupDir, "photos");

		GoogleAuthFlow authFlow = new GoogleAuthFlow(clientSecretFile, credentialsDataStore);

		String userId = System.getProperty("user.name");
		List<String> scopes = Arrays.asList("https://www.googleapis.com/auth/photoslibrary.readonly");
		Credentials credentials = authFlow.authorize(userId, scopes);

		GooglePhotosLibrary photosLibrary = null;
		DownloadedPhotosJDS photosIndex = null;

		try {

			photosLibrary = new GooglePhotosLibrary(credentials);
			photosLibrary.initialize();

			photosIndex = DownloadedPhotosJDS.create(photosIndexDataStore);
			photosIndex.initialize();

			PhotosBackupRunner backup = new PhotosBackupRunner(photosLibrary, photosIndex, photosBackupDir);
			backup.setStartDate(startDate);
			backup.setEndDate(endDate);
			backup.setMediaItemType(MediaItemType.PHOTO); //TODO: Add command line option for this
			backup.run();

		} finally {
			if (photosLibrary != null)
				photosLibrary.close();
			if (photosIndex != null)
				photosIndex.close();
		}
	}
	
	
	private static File getClientSecretFile(CommandLine cmdLine) {

		File clientSecretFile = new File(cmdLine.getOptionValue(
				CMD_OPTION_CLIENT_SECRET, CMD_OPTION_CLIENT_SECRET_DEFAULT));

		if (!clientSecretFile.exists())
			throw new RuntimeException("Invalid client secret file: " + clientSecretFile.getAbsolutePath());

		return clientSecretFile;
	}


	private static File getBackupDir(CommandLine cmdLine) {

		File backupDir = new File(cmdLine.getOptionValue(
				CMD_OPTION_BACKUP_DIR, CMD_OPTION_BACKUP_DIR_DEFAULT));
		try {
			Files.createDirectories(backupDir.toPath());
			return backupDir;
		} catch (IOException ex) {
			throw new RuntimeException("Invalid backup directory: " + backupDir.getAbsolutePath(), ex);
		}
	}


	private static LocalDate getStartDate(CommandLine cmdLine) {
		return cmdLine.hasOption(CMD_OPTION_START_DATE) ?
				parseLocalDate(cmdLine.getOptionValue(CMD_OPTION_START_DATE)) : null;
	}


	private static LocalDate getEndDate(CommandLine cmdLine) {
		return cmdLine.hasOption(CMD_OPTION_END_DATE) ?
				parseLocalDate(cmdLine.getOptionValue(CMD_OPTION_END_DATE)) : null;
	}


	private static LocalDate parseLocalDate(String date) {
		try {
			return LocalDate.parse(date);
		} catch (DateTimeParseException ex) {
			throw new RuntimeException("Invalid date: " + date);
		}
	}


	private static void printHelp(Options options) {
		HelpFormatter help = new HelpFormatter();
		help.setOptionComparator(null);
		help.printHelp("google-photos-backup", options);
	}
}
