package pm.google.photos.backup;

import java.io.File;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.List;

import com.google.auth.Credentials;

import pm.google.photos.backup.auth.GoogleAuthFlow;


public class GooglePhotosBackupApp {

	public static void main(String[] args) {

		File backupDir = new File("backup");

		File dataStoreDir = new File(backupDir, ".data_store");
		File credentialsDataStore = new File(dataStoreDir, "credentials");
		File photosIndexDataStore = new File(dataStoreDir, "index");

		File clientSecretJsonFile = new File("client_secret.json");
		GoogleAuthFlow authFlow = new GoogleAuthFlow(clientSecretJsonFile, credentialsDataStore);

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

			PhotosBackupRunner backup = new PhotosBackupRunner(photosLibrary, photosIndex, backupDir);
			backup.setStartDate(LocalDate.of(2020, Month.OCTOBER, 1));
			backup.run();

		} finally {
			if (photosLibrary != null)
				photosLibrary.close();
			if (photosIndex != null)
				photosIndex.close();
		}
	}
}
